package com.vanespark.vertext.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.model.Thread
import com.vanespark.vertext.data.provider.SmsProviderService
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import com.vanespark.vertext.domain.service.ContactService
import com.vanespark.vertext.domain.service.MessagingService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the chat thread screen
 * Manages message list state, message sending, and user interactions
 */
class ChatThreadViewModel @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    private val smsProviderService: SmsProviderService,  // Direct provider access for messages
    private val messageRepository: MessageRepository,  // Keep for compatibility (reactions, etc.)
    private val threadRepository: ThreadRepository,
    private val contactService: ContactService,
    private val messagingService: MessagingService,
    private val reactionDetectionService: com.vanespark.vertext.domain.service.ReactionDetectionService,
    @Assisted private val threadId: Long
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(threadId: Long): ChatThreadViewModel
    }

    private val _uiState = MutableStateFlow(ChatThreadUiState(isLoading = true))
    val uiState: StateFlow<ChatThreadUiState> = _uiState.asStateFlow()

    // Cache contact names to avoid repeated lookups (especially for group chats)
    private val contactNameCache = mutableMapOf<String, String>()

    init {
        loadThread()
        loadMessages()
        markThreadAsRead()
        processUnprocessedReactions()
    }

    /**
     * Process any unprocessed reaction messages in this thread
     * This handles cases where reactions arrived while the app was closed
     */
    private fun processUnprocessedReactions() {
        viewModelScope.launch {
            try {
                val processedCount = reactionDetectionService.reprocessThreadForReactions(threadId)
                if (processedCount > 0) {
                    Timber.d("Processed $processedCount unprocessed reactions in thread $threadId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing unprocessed reactions")
            }
        }
    }

    /**
     * Load thread metadata
     */
    private fun loadThread() {
        viewModelScope.launch {
            threadRepository.getThreadByIdFlow(threadId)
                .catch { e: Throwable ->
                    Timber.e(e, "Error loading thread")
                    _uiState.update { it.copy(error = "Failed to load conversation") }
                }
                .collect { thread: Thread? ->
                    _uiState.update { it.copy(thread = thread) }
                }
        }
    }

    /**
     * Get display name for a message sender, with caching for performance
     * Avoids repeated contact lookups, especially important for group chats
     */
    private suspend fun getDisplayName(address: String, isGroupConversation: Boolean, messageType: Int): String {
        // For 1-on-1 conversations, use thread recipient name
        if (!isGroupConversation) {
            return _uiState.value.thread?.recipientName ?: address
        }

        // For outgoing messages in groups, always "You"
        if (messageType == 2) {
            return "You"
        }

        // For incoming messages in groups, use cached contact name lookup
        return contactNameCache.getOrPut(address) {
            contactService.getContactName(address) ?: address
        }
    }

    /**
     * Load messages for this thread
     * OPTIMIZED: Queries SMS/MMS provider directly instead of syncing to database
     * This eliminates sync delay and reduces storage by 50%+
     * Limited to most recent N messages for performance (configurable in settings)
     */
    private fun loadMessages() {
        viewModelScope.launch {
            try {
                // Get message load limit from settings
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val messageLoadLimit = prefs.getInt("message_load_limit", 100)

                // PERFORMANCE: Query SMS provider directly - no database sync needed!
                val messages = smsProviderService.readMessagesForThread(
                    threadId = threadId,
                    limit = messageLoadLimit
                )

                val thread = _uiState.value.thread
                val isGroupConversation = thread?.isGroup == true

                // Filter out reaction messages that haven't been processed yet
                // Google Messages format: "<emoji> <verb> '<quoted text>'"
                val googleReactionPattern = Regex("^(.+?)\\s+(Liked|Disliked|Loved|Laughed at|Emphasized|Questioned|Reacted to)\\s+'.+'$")
                val displayMessages = messages.filter { message ->
                    !googleReactionPattern.matches(message.body.trim())
                }

                // Convert to UI items with cached contact name lookups
                val messageUiItems = displayMessages.map { message ->
                    val displayName = getDisplayName(message.address, isGroupConversation, message.type)

                    MessageUiItem.fromMessage(
                        message = message,
                        displayName = displayName
                    )
                }

                val groupedMessages = MessageUiItem.groupMessages(messageUiItems)

                _uiState.update {
                    it.copy(
                        messages = groupedMessages,
                        isLoading = false,
                        error = null
                    )
                }

                Timber.d("Loaded ${messages.size} messages from provider (${contactNameCache.size} contacts cached)")
            } catch (e: Exception) {
                Timber.e(e, "Error loading messages from provider")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load messages"
                    )
                }
            }
        }
    }

    /**
     * Mark this thread as read when opened
     */
    private fun markThreadAsRead() {
        viewModelScope.launch {
            messagingService.markThreadAsRead(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to mark thread as read")
                }
        }
    }

    /**
     * Update the composed message text
     */
    fun updateComposedMessage(text: String) {
        _uiState.update { it.copy(composedMessage = text) }
    }

    /**
     * Send a reaction to a specific message
     * Uses Google Messages compatible format: "<emoji> <verb> '<quoted text>'"
     */
    fun sendReaction(targetMessage: MessageUiItem, emoji: String) {
        val thread = _uiState.value.thread ?: run {
            Timber.e("Cannot send reaction: thread is null")
            _uiState.update { it.copy(error = "Failed to send reaction") }
            return
        }

        // Map emoji to Google Messages reaction verbs
        val verb = when (emoji) {
            "👍" -> "Liked"
            "👎" -> "Disliked"
            "❤️" -> "Loved"
            "😂" -> "Laughed at"
            "😮" -> "Emphasized"
            "😢" -> "Questioned"
            else -> "Reacted to"  // Fallback for custom emojis
        }

        // Google Messages format: "<emoji> <verb> '<quoted text>'"
        // Truncate message if too long to avoid SMS issues
        val quotedText = if (targetMessage.body.length > 100) {
            targetMessage.body.take(97) + "..."
        } else {
            targetMessage.body
        }

        val reactionMessage = "$emoji $verb '$quotedText'"

        viewModelScope.launch {
            messagingService.sendSmsMessage(
                recipientAddress = thread.recipient,
                messageText = reactionMessage
            )
                .onSuccess {
                    Timber.d("Reaction sent successfully to message ${targetMessage.id}")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to send reaction")
                    _uiState.update { it.copy(error = "Failed to send reaction: ${e.message}") }
                }
        }
    }

    /**
     * Send the composed message
     */
    fun sendMessage() {
        val message = _uiState.value.composedMessage.trim()
        if (message.isEmpty()) return

        val thread = _uiState.value.thread ?: run {
            Timber.e("Cannot send message: thread is null")
            _uiState.update { it.copy(error = "Failed to send message") }
            return
        }

        // Get recipient address from thread
        val recipientAddress = thread.recipient

        _uiState.update { it.copy(isSending = true) }

        viewModelScope.launch {
            messagingService.sendSmsMessage(
                recipientAddress = recipientAddress,
                messageText = message
            )
                .onSuccess {
                    Timber.d("Message sent successfully")
                    // Clear composed message
                    _uiState.update {
                        it.copy(
                            composedMessage = "",
                            isSending = false,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to send message")
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = "Failed to send message: ${e.message}"
                        )
                    }
                }
        }
    }

    /**
     * Retry sending a failed message
     */
    fun retryMessage(messageId: Long) {
        viewModelScope.launch {
            val message = messageRepository.getMessageById(messageId)
            if (message == null) {
                Timber.e("Cannot retry: message $messageId not found")
                return@launch
            }

            val thread = _uiState.value.thread ?: run {
                Timber.e("Cannot retry: thread is null")
                return@launch
            }

            messagingService.sendSmsMessage(
                recipientAddress = thread.recipient,
                messageText = message.body
            )
                .onSuccess {
                    Timber.d("Message resent successfully")
                    // Delete the failed message
                    messageRepository.deleteMessageById(messageId)
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to resend message")
                    _uiState.update { it.copy(error = "Failed to resend message") }
                }
        }
    }

    /**
     * Delete a message
     */
    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            messagingService.deleteMessage(messageId)
                .onFailure { e ->
                    Timber.e(e, "Failed to delete message")
                    _uiState.update { it.copy(error = "Failed to delete message") }
                }
        }
    }

    /**
     * Copy message text to clipboard
     */
    fun copyMessageText(text: String) {
        // Clipboard handling will be done in the UI layer
        Timber.d("Copy message text: $text")
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Archive this conversation
     */
    fun archiveConversation() {
        viewModelScope.launch {
            messagingService.archiveThread(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to archive thread")
                    _uiState.update { it.copy(error = "Failed to archive") }
                }
        }
    }

    /**
     * Delete this conversation
     */
    fun deleteConversation() {
        viewModelScope.launch {
            messagingService.deleteThread(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to delete thread")
                    _uiState.update { it.copy(error = "Failed to delete") }
                }
        }
    }

    /**
     * Mute this conversation
     */
    fun muteConversation() {
        viewModelScope.launch {
            messagingService.muteThread(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to mute thread")
                    _uiState.update { it.copy(error = "Failed to mute") }
                }
        }
    }
}
