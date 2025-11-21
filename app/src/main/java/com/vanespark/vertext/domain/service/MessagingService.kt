package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.model.Thread
import com.vanespark.vertext.data.provider.SmsSenderService
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level messaging service that orchestrates SMS operations
 * Provides a clean API for sending and managing messages
 */
@Singleton
class MessagingService @Inject constructor(
    private val messageRepository: MessageRepository,
    private val threadRepository: ThreadRepository,
    private val smsSenderService: SmsSenderService,
    private val reactionDetectionService: ReactionDetectionService
) {

    /**
     * Send an SMS message and save it to the database
     * Returns the message ID if successful
     */
    suspend fun sendSmsMessage(
        recipientAddress: String,
        messageText: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Get or create thread for recipient
            val thread = threadRepository.getOrCreateThread(
                recipient = recipientAddress,
                recipientName = null
            )

            // Create message entity (initially as OUTBOX, will be updated to SENT on success)
            val message = Message(
                threadId = thread.id,
                address = recipientAddress,
                body = messageText,
                date = System.currentTimeMillis(),
                type = Message.TYPE_OUTBOX,
                isRead = true // Sent messages are always "read"
            )

            // Save message to database
            val messageId = messageRepository.insertMessage(message)
            Timber.d("Created outbox message with ID: $messageId")

            // Send via SmsManager
            val sendResult = smsSenderService.sendSms(
                destinationAddress = recipientAddress,
                messageText = messageText,
                messageId = messageId
            )

            if (sendResult.isSuccess) {
                // Update message type to SENT
                val sentMessage = message.copy(id = messageId, type = Message.TYPE_SENT)
                messageRepository.updateMessage(sentMessage)

                // Check if this is an emoji reaction to a previous message
                try {
                    val isReaction = reactionDetectionService.processMessageForReaction(sentMessage)
                    if (isReaction) {
                        Timber.d("Outgoing message $messageId was processed as a reaction")
                        // Don't update thread metadata for reactions
                        // The message was deleted and stored as a reaction
                        return@withContext Result.success(messageId)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error detecting reaction for outgoing message $messageId")
                }

                // Update thread metadata (only if not a reaction)
                threadRepository.updateLastMessage(
                    threadId = thread.id,
                    message = messageText,
                    date = System.currentTimeMillis()
                )

                Timber.d("Successfully sent SMS to $recipientAddress")
                Result.success(messageId)
            } else {
                // Mark message as failed
                messageRepository.updateMessage(
                    message.copy(id = messageId, type = Message.TYPE_FAILED)
                )

                val error = sendResult.exceptionOrNull() ?: Exception("Failed to send SMS")
                Timber.e(error, "Failed to send SMS to $recipientAddress")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in sendSmsMessage")
            Result.failure(e)
        }
    }

    /**
     * Send SMS to multiple recipients
     * Returns map of recipient to message ID
     */
    suspend fun sendSmsToMultiple(
        recipients: List<String>,
        messageText: String
    ): Result<Map<String, Long>> = withContext(Dispatchers.IO) {
        try {
            val results = mutableMapOf<String, Long>()
            var hasError = false
            var lastError: Exception? = null

            recipients.forEach { recipient ->
                val result = sendSmsMessage(recipient, messageText)
                if (result.isSuccess) {
                    results[recipient] = result.getOrThrow()
                } else {
                    hasError = true
                    lastError = result.exceptionOrNull() as? Exception
                }
            }

            if (hasError) {
                Result.failure(lastError ?: Exception("Failed to send to some recipients"))
            } else {
                Result.success(results)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending to multiple recipients")
            Result.failure(e)
        }
    }

    /**
     * Mark a message as read
     */
    suspend fun markMessageAsRead(messageId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            messageRepository.markMessageAsRead(messageId)

            // Update thread unread count
            val message = messageRepository.getMessageById(messageId)
            message?.let {
                val unreadCount = messageRepository.getUnreadCountForThread(it.threadId)
                threadRepository.updateUnreadCount(it.threadId, unreadCount)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error marking message as read")
            Result.failure(e)
        }
    }

    /**
     * Mark all messages in a thread as read
     */
    suspend fun markThreadAsRead(threadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            messageRepository.markThreadAsRead(threadId)
            threadRepository.clearUnreadCount(threadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error marking thread as read")
            Result.failure(e)
        }
    }

    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val message = messageRepository.getMessageById(messageId)
            message?.let {
                messageRepository.deleteMessageById(messageId)

                // Update thread message count
                val count = messageRepository.getMessageCountForThread(it.threadId)
                threadRepository.updateMessageCount(it.threadId, count)

                // Update last message if needed
                val latestMessage = messageRepository.getLatestMessageForThread(it.threadId)
                latestMessage?.let { latest ->
                    threadRepository.updateLastMessage(
                        threadId = it.threadId,
                        message = latest.body,
                        date = latest.date
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting message")
            Result.failure(e)
        }
    }

    /**
     * Delete a conversation thread and all its messages
     */
    suspend fun deleteThread(threadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            messageRepository.deleteMessagesInThread(threadId)
            threadRepository.deleteThreadById(threadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting thread")
            Result.failure(e)
        }
    }

    /**
     * Archive a thread
     */
    suspend fun archiveThread(threadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            threadRepository.archiveThread(threadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error archiving thread")
            Result.failure(e)
        }
    }

    /**
     * Unarchive a thread
     */
    suspend fun unarchiveThread(threadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            threadRepository.unarchiveThread(threadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error unarchiving thread")
            Result.failure(e)
        }
    }

    /**
     * Pin a thread to the top
     */
    suspend fun pinThread(threadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            threadRepository.pinThread(threadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error pinning thread")
            Result.failure(e)
        }
    }

    /**
     * Unpin a thread
     */
    suspend fun unpinThread(threadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            threadRepository.unpinThread(threadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error unpinning thread")
            Result.failure(e)
        }
    }

    /**
     * Mute a thread (disable notifications)
     */
    suspend fun muteThread(threadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            threadRepository.muteThread(threadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error muting thread")
            Result.failure(e)
        }
    }

    /**
     * Unmute a thread
     */
    suspend fun unmuteThread(threadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            threadRepository.unmuteThread(threadId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error unmuting thread")
            Result.failure(e)
        }
    }

    /**
     * Estimate the number of SMS parts for a message
     */
    fun estimateSmsPartCount(messageText: String): Int {
        return smsSenderService.estimateSmsPartCount(messageText)
    }

    /**
     * Check if SMS is supported on this device
     */
    fun isSmsSupported(): Boolean {
        return smsSenderService.isSmsSupported()
    }
}
