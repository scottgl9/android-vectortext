package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.model.Thread
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing group messaging conversations
 * Handles group creation, management, and message sending
 */
@Singleton
class GroupMessagingService @Inject constructor(
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val messagingService: MessagingService
) {

    /**
     * Create a new group conversation
     * @param recipients List of phone numbers to include in the group
     * @param groupName Optional custom name for the group
     * @return The created Thread ID
     */
    suspend fun createGroupConversation(
        recipients: List<String>,
        groupName: String? = null
    ): Long = withContext(Dispatchers.IO) {
        require(recipients.size >= 2) { "Group must have at least 2 recipients" }

        Timber.d("Creating group conversation with ${recipients.size} recipients")

        // Generate thread ID (using a hash of sorted recipients for consistency)
        val threadId = generateGroupThreadId(recipients)

        // Check if group already exists
        val existingThread = threadRepository.getThreadById(threadId)
        if (existingThread != null) {
            Timber.d("Group conversation already exists with ID: $threadId")
            return@withContext threadId
        }

        // Create recipients JSON
        val recipientsJson = encodeRecipientsToJson(recipients)

        // Create display name
        val displayRecipient = if (groupName != null) {
            groupName
        } else {
            recipients.take(3).joinToString(", ") +
                if (recipients.size > 3) " +${recipients.size - 3}" else ""
        }

        // Create new group thread
        val groupThread = Thread(
            id = threadId,
            recipient = displayRecipient,
            recipientName = groupName,
            lastMessage = null,
            lastMessageDate = System.currentTimeMillis(),
            unreadCount = 0,
            isGroup = true,
            groupName = groupName,
            recipients = recipientsJson
        )

        threadRepository.insertThread(groupThread)

        Timber.d("Created group conversation: $threadId with name: ${groupName ?: "unnamed"}")
        return@withContext threadId
    }

    /**
     * Update group name
     */
    suspend fun updateGroupName(threadId: Long, groupName: String) = withContext(Dispatchers.IO) {
        val thread = threadRepository.getThreadById(threadId)
            ?: throw IllegalArgumentException("Thread not found: $threadId")

        require(thread.isGroup) { "Thread is not a group conversation" }

        val updatedThread = thread.copy(
            groupName = groupName,
            recipientName = groupName
        )

        threadRepository.updateThread(updatedThread)
        Timber.d("Updated group name for thread $threadId: $groupName")
    }

    /**
     * Add recipients to an existing group
     */
    suspend fun addRecipientsToGroup(
        threadId: Long,
        newRecipients: List<String>
    ) = withContext(Dispatchers.IO) {
        val thread = threadRepository.getThreadById(threadId)
            ?: throw IllegalArgumentException("Thread not found: $threadId")

        require(thread.isGroup) { "Thread is not a group conversation" }

        // Get current recipients
        val currentRecipients = thread.getRecipientsList().toMutableList()

        // Add new recipients (avoiding duplicates)
        val recipientsToAdd = newRecipients.filter { it !in currentRecipients }
        if (recipientsToAdd.isEmpty()) {
            Timber.d("No new recipients to add")
            return@withContext
        }

        currentRecipients.addAll(recipientsToAdd)

        // Update thread
        val updatedThread = thread.copy(
            recipients = encodeRecipientsToJson(currentRecipients)
        )

        threadRepository.updateThread(updatedThread)
        Timber.d("Added ${recipientsToAdd.size} recipients to group $threadId")
    }

    /**
     * Remove recipients from a group
     */
    suspend fun removeRecipientsFromGroup(
        threadId: Long,
        recipientsToRemove: List<String>
    ) = withContext(Dispatchers.IO) {
        val thread = threadRepository.getThreadById(threadId)
            ?: throw IllegalArgumentException("Thread not found: $threadId")

        require(thread.isGroup) { "Thread is not a group conversation" }

        // Get current recipients
        val currentRecipients = thread.getRecipientsList().toMutableList()

        // Remove recipients
        currentRecipients.removeAll(recipientsToRemove)

        require(currentRecipients.size >= 2) { "Group must have at least 2 recipients" }

        // Update thread
        val updatedThread = thread.copy(
            recipients = encodeRecipientsToJson(currentRecipients)
        )

        threadRepository.updateThread(updatedThread)
        Timber.d("Removed ${recipientsToRemove.size} recipients from group $threadId")
    }

    /**
     * Send message to a group
     * @return List of message IDs (one per recipient)
     */
    suspend fun sendGroupMessage(
        threadId: Long,
        messageBody: String
    ): List<Long> = withContext(Dispatchers.IO) {
        val thread = threadRepository.getThreadById(threadId)
            ?: throw IllegalArgumentException("Thread not found: $threadId")

        require(thread.isGroup) { "Thread is not a group conversation" }

        val recipients = thread.getRecipientsList()
        Timber.d("Sending group message to ${recipients.size} recipients")

        // Send message to each recipient
        val messageIds = mutableListOf<Long>()

        for (recipient in recipients) {
            try {
                val result = messagingService.sendSmsMessage(
                    recipientAddress = recipient,
                    messageText = messageBody
                )

                result.onSuccess { messageId ->
                    messageIds.add(messageId)
                    Timber.d("Sent message to $recipient: $messageId")
                }.onFailure { error ->
                    Timber.e(error, "Failed to send message to $recipient")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error sending message to $recipient")
            }
        }

        Timber.d("Group message sent to ${messageIds.size}/${recipients.size} recipients")
        return@withContext messageIds
    }

    /**
     * Get all group conversations
     */
    suspend fun getAllGroupConversations(): List<Thread> = withContext(Dispatchers.IO) {
        threadRepository.getAllThreadsSnapshot().filter { it.isGroup }
    }

    /**
     * Check if a thread is a group conversation
     */
    suspend fun isGroupConversation(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        val thread = threadRepository.getThreadById(threadId)
        thread?.isGroup ?: false
    }

    /**
     * Get group members (recipients)
     */
    suspend fun getGroupMembers(threadId: Long): List<String> = withContext(Dispatchers.IO) {
        val thread = threadRepository.getThreadById(threadId)
            ?: throw IllegalArgumentException("Thread not found: $threadId")

        thread.getRecipientsList()
    }

    // === Private Helper Methods ===

    /**
     * Generate a consistent thread ID for a group based on its members
     */
    private fun generateGroupThreadId(recipients: List<String>): Long {
        // Sort recipients to ensure consistent ID regardless of order
        val sortedRecipients = recipients.sorted().joinToString(",")
        return sortedRecipients.hashCode().toLong() and 0x7FFFFFFF // Positive long
    }

    /**
     * Encode list of recipients to JSON string
     */
    private fun encodeRecipientsToJson(recipients: List<String>): String {
        val jsonArray = JSONArray()
        recipients.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    /**
     * Decode recipients from JSON string
     */
    private fun decodeRecipientsFromJson(json: String): List<String> {
        return try {
            val jsonArray = JSONArray(json)
            List(jsonArray.length()) { i -> jsonArray.getString(i) }
        } catch (e: Exception) {
            Timber.e(e, "Error decoding recipients JSON")
            emptyList()
        }
    }
}
