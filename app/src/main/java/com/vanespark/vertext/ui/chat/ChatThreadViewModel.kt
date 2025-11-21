package com.vanespark.vertext.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.model.Thread
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import com.vanespark.vertext.domain.service.ContactService
import com.vanespark.vertext.domain.service.MessagingService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
    private val messageRepository: MessageRepository,
    private val threadRepository: ThreadRepository,
    private val contactService: ContactService,
    private val messagingService: MessagingService,
    @Assisted private val threadId: Long
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(threadId: Long): ChatThreadViewModel
    }

    private val _uiState = MutableStateFlow(ChatThreadUiState(isLoading = true))
    val uiState: StateFlow<ChatThreadUiState> = _uiState.asStateFlow()

    init {
        loadThread()
        loadMessages()
        markThreadAsRead()
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
     * Load messages for this thread
     * Limited to most recent 100 messages for performance
     */
    private fun loadMessages() {
        viewModelScope.launch {
            messageRepository.getMessagesForThreadLimit(threadId, 100)
                .catch { e: Throwable ->
                    Timber.e(e, "Error loading messages")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load messages"
                        )
                    }
                }
                .collect { messages: List<Message> ->
                    val thread = _uiState.value.thread
                    val isGroupConversation = thread?.isGroup == true

                    // Convert to UI items
                    val messageUiItems = messages.map { message ->
                        val displayName = when {
                            // For group conversations, show individual sender names
                            isGroupConversation -> {
                                if (message.type == 2) {
                                    // Outgoing message from user
                                    "You"
                                } else {
                                    // Incoming message - look up contact name from system
                                    contactService.getContactName(message.address) ?: message.address
                                }
                            }
                            // For 1-on-1 conversations, use thread recipient name
                            else -> thread?.recipientName ?: message.address
                        }

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

                    if (isGroupConversation) {
                        Timber.d("Loaded ${messages.size} messages for group conversation")
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
