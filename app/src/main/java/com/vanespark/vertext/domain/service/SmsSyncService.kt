package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.model.Thread
import com.vanespark.vertext.data.provider.SmsProviderService
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for synchronizing SMS messages from Android system to VectorText database
 * Handles initial sync and incremental updates
 */
@Singleton
class SmsSyncService @Inject constructor(
    private val smsProviderService: SmsProviderService,
    private val messageRepository: MessageRepository,
    private val threadRepository: ThreadRepository,
    private val threadCategorizationService: ThreadCategorizationService
) {

    /**
     * Sync progress data
     */
    data class SyncProgress(
        val currentStep: SyncStep,
        val progress: Float, // 0.0 to 1.0
        val itemsProcessed: Int,
        val totalItems: Int,
        val message: String
    )

    enum class SyncStep {
        READING_THREADS,
        SYNCING_THREADS,
        READING_MESSAGES,
        SYNCING_MESSAGES,
        CATEGORIZING_THREADS,
        COMPLETED,
        FAILED
    }

    /**
     * Perform full sync of all messages and threads from system SMS provider
     * Emits progress updates as a Flow
     */
    fun performFullSync(): Flow<SyncProgress> = flow {
        try {
            // Step 1: Read threads from system
            emit(
                SyncProgress(
                    currentStep = SyncStep.READING_THREADS,
                    progress = 0.0f,
                    itemsProcessed = 0,
                    totalItems = 0,
                    message = "Reading conversation threads..."
                )
            )

            val systemThreads = smsProviderService.readAllThreads()
            Timber.d("Read ${systemThreads.size} threads from system")

            // Step 2: Sync threads to database
            emit(
                SyncProgress(
                    currentStep = SyncStep.SYNCING_THREADS,
                    progress = 0.25f,
                    itemsProcessed = 0,
                    totalItems = systemThreads.size,
                    message = "Syncing ${systemThreads.size} conversations..."
                )
            )

            systemThreads.forEach { thread ->
                // Check if thread already exists
                val existing = threadRepository.getThreadById(thread.id)
                if (existing == null) {
                    // Insert new thread (preserve system thread ID)
                    threadRepository.insertThread(thread)
                } else {
                    // Update existing thread metadata
                    threadRepository.updateThread(
                        existing.copy(
                            lastMessage = thread.lastMessage,
                            lastMessageDate = thread.lastMessageDate,
                            messageCount = thread.messageCount
                        )
                    )
                }
            }

            Timber.d("Synced ${systemThreads.size} threads")

            // Step 3: Read messages from system
            emit(
                SyncProgress(
                    currentStep = SyncStep.READING_MESSAGES,
                    progress = 0.5f,
                    itemsProcessed = 0,
                    totalItems = 0,
                    message = "Reading messages..."
                )
            )

            // Read messages in batches to avoid memory issues
            val batchSize = 500
            val smsMessages = smsProviderService.readAllSmsMessages(limit = null)
            val mmsMessages = smsProviderService.readAllMmsMessages(limit = null)
            val allMessages = smsMessages + mmsMessages
            val totalMessages = allMessages.size

            Timber.d("Read ${smsMessages.size} SMS + ${mmsMessages.size} MMS = $totalMessages total messages from system")

            // Step 4: Sync messages to database
            emit(
                SyncProgress(
                    currentStep = SyncStep.SYNCING_MESSAGES,
                    progress = 0.75f,
                    itemsProcessed = 0,
                    totalItems = totalMessages,
                    message = "Syncing $totalMessages messages..."
                )
            )

            var processedCount = 0
            allMessages.chunked(batchSize).forEach { batch ->
                // Filter out messages that already exist
                val newMessages = batch.filter { message ->
                    messageRepository.getMessageById(message.id) == null
                }

                if (newMessages.isNotEmpty()) {
                    messageRepository.insertMessages(newMessages)
                }

                processedCount += batch.size

                // Emit progress update
                val progress = 0.75f + (0.25f * (processedCount.toFloat() / totalMessages))
                emit(
                    SyncProgress(
                        currentStep = SyncStep.SYNCING_MESSAGES,
                        progress = progress,
                        itemsProcessed = processedCount,
                        totalItems = totalMessages,
                        message = "Synced $processedCount of $totalMessages messages..."
                    )
                )
            }

            Timber.d("Synced $totalMessages messages")

            // Update thread metadata after sync
            updateThreadMetadata()

            // Step 5: Categorize threads
            emit(
                SyncProgress(
                    currentStep = SyncStep.CATEGORIZING_THREADS,
                    progress = 0.95f,
                    itemsProcessed = 0,
                    totalItems = systemThreads.size,
                    message = "Categorizing conversations..."
                )
            )

            val categorizedCount = threadCategorizationService.categorizeAllThreads()
            Timber.d("Categorized $categorizedCount threads")

            // Step 6: Completed
            emit(
                SyncProgress(
                    currentStep = SyncStep.COMPLETED,
                    progress = 1.0f,
                    itemsProcessed = totalMessages,
                    totalItems = totalMessages,
                    message = "Sync completed successfully"
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Error during sync")
            emit(
                SyncProgress(
                    currentStep = SyncStep.FAILED,
                    progress = 0.0f,
                    itemsProcessed = 0,
                    totalItems = 0,
                    message = "Sync failed: ${e.message}"
                )
            )
        }
    }

    /**
     * Perform incremental sync for new messages since last sync
     */
    suspend fun performIncrementalSync(lastSyncTimestamp: Long): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Read recent messages from system
            val allMessages = smsProviderService.readAllSmsMessages(limit = 100)
            val newMessages = allMessages.filter { it.date > lastSyncTimestamp }

            if (newMessages.isEmpty()) {
                return@withContext Result.success(0)
            }

            // Insert new messages
            newMessages.forEach { message ->
                val exists = messageRepository.getMessageById(message.id)
                if (exists == null) {
                    messageRepository.insertMessage(message)
                }
            }

            // Update thread metadata
            updateThreadMetadata()

            // Categorize affected threads
            val affectedThreadIds = newMessages.map { it.threadId }.distinct()
            affectedThreadIds.forEach { threadId ->
                threadCategorizationService.categorizeThread(threadId)
            }

            Timber.d("Incremental sync: ${newMessages.size} new messages, categorized ${affectedThreadIds.size} threads")
            Result.success(newMessages.size)
        } catch (e: Exception) {
            Timber.e(e, "Error during incremental sync")
            Result.failure(e)
        }
    }

    /**
     * Update thread metadata (message count, last message, unread count)
     */
    private suspend fun updateThreadMetadata() {
        try {
            val threads = threadRepository.getThreadsLimit(Int.MAX_VALUE)

            threads.forEach { thread ->
                // Get message count
                val messageCount = messageRepository.getMessageCountForThread(thread.id)

                // Get latest message
                val latestMessage = messageRepository.getLatestMessageForThread(thread.id)

                // Get unread count
                val unreadCount = messageRepository.getUnreadCountForThread(thread.id)

                // Update thread
                if (latestMessage != null) {
                    threadRepository.updateThread(
                        thread.copy(
                            messageCount = messageCount,
                            lastMessage = latestMessage.body,
                            lastMessageDate = latestMessage.date,
                            unreadCount = unreadCount
                        )
                    )
                }
            }

            Timber.d("Updated metadata for ${threads.size} threads")
        } catch (e: Exception) {
            Timber.e(e, "Error updating thread metadata")
        }
    }

    /**
     * Check if initial sync has been completed
     */
    suspend fun hasCompletedInitialSync(): Boolean {
        return try {
            val messageCount = messageRepository.getTotalMessageCount()
            messageCount > 0
        } catch (e: Exception) {
            Timber.e(e, "Error checking sync status")
            false
        }
    }

    /**
     * Clear all synced data (for testing or reset)
     */
    suspend fun clearAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            messageRepository.deleteAllMessages()
            threadRepository.deleteAllThreads()
            Timber.d("Cleared all synced data")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error clearing data")
            Result.failure(e)
        }
    }
}
