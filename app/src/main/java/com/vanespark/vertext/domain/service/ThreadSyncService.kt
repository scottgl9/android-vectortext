package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.provider.SmsProviderService
import com.vanespark.vertext.data.repository.ThreadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight thread synchronization service
 * OPTIMIZED: Only syncs thread metadata (not messages) for fast initial sync
 * Messages are queried directly from SMS provider when needed
 */
@Singleton
class ThreadSyncService @Inject constructor(
    private val smsProviderService: SmsProviderService,
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
        CATEGORIZING_THREADS,
        COMPLETED,
        FAILED
    }

    /**
     * Perform fast sync of thread metadata only
     * Messages are NOT synced - they're queried on-demand from SMS provider
     * This is 90%+ faster than full sync
     */
    fun performThreadSync(): Flow<SyncProgress> = flow {
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

            // Step 2: Sync thread metadata to database
            emit(
                SyncProgress(
                    currentStep = SyncStep.SYNCING_THREADS,
                    progress = 0.33f,
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
                    // Update existing thread metadata only
                    threadRepository.updateThread(
                        existing.copy(
                            recipient = thread.recipient,
                            lastMessage = thread.lastMessage,
                            lastMessageDate = thread.lastMessageDate,
                            messageCount = thread.messageCount,
                            isGroup = thread.isGroup,
                            recipients = thread.recipients
                        )
                    )
                }
            }

            Timber.d("Synced ${systemThreads.size} threads (metadata only)")

            // Step 3: Categorize threads
            emit(
                SyncProgress(
                    currentStep = SyncStep.CATEGORIZING_THREADS,
                    progress = 0.66f,
                    itemsProcessed = 0,
                    totalItems = systemThreads.size,
                    message = "Categorizing conversations..."
                )
            )

            val categorizedCount = threadCategorizationService.categorizeAllThreads()
            Timber.d("Categorized $categorizedCount threads")

            // Step 4: Completed
            emit(
                SyncProgress(
                    currentStep = SyncStep.COMPLETED,
                    progress = 1.0f,
                    itemsProcessed = systemThreads.size,
                    totalItems = systemThreads.size,
                    message = "Sync completed - ${systemThreads.size} conversations ready"
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Error during thread sync")
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
     * Update a single thread's metadata from SMS provider
     * Useful for refreshing after sending/receiving messages
     */
    suspend fun updateSingleThread(threadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Query latest message from SMS provider
            val latestMessage = smsProviderService.readMessagesForThread(
                threadId = threadId,
                limit = 1
            ).firstOrNull()

            if (latestMessage != null) {
                val existing = threadRepository.getThreadById(threadId)
                if (existing != null) {
                    threadRepository.updateThread(
                        existing.copy(
                            lastMessage = latestMessage.body,
                            lastMessageDate = latestMessage.date
                        )
                    )
                    Timber.d("Updated thread $threadId metadata")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating thread $threadId")
            Result.failure(e)
        }
    }

    /**
     * Check if initial thread sync has been completed
     */
    suspend fun hasCompletedInitialSync(): Boolean {
        return try {
            val threadCount = threadRepository.getThreadsLimit(1).size
            threadCount > 0
        } catch (e: Exception) {
            Timber.e(e, "Error checking sync status")
            false
        }
    }

    /**
     * Clear all thread data (for testing or reset)
     */
    suspend fun clearAllThreads(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            threadRepository.deleteAllThreads()
            Timber.d("Cleared all thread data")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error clearing threads")
            Result.failure(e)
        }
    }
}
