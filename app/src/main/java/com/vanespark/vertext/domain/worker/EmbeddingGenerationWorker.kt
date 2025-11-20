package com.vanespark.vertext.domain.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.vanespark.vertext.R
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.domain.service.TextEmbeddingService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background worker for generating embeddings for messages
 * Uses WorkManager for reliable background processing
 *
 * Processes messages in batches to avoid memory issues
 * Updates progress for UI feedback
 */
@HiltWorker
class EmbeddingGenerationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageRepository: MessageRepository,
    private val textEmbeddingService: TextEmbeddingService
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "embedding_generation"
        const val BATCH_SIZE = 100  // Messages to process per batch
        const val KEY_PROGRESS = "progress"
        const val KEY_PROCESSED = "processed"
        const val KEY_TOTAL = "total"
        const val KEY_MESSAGE = "message"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "embedding_generation"
        private const val CHANNEL_NAME = "Message Indexing"
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting embedding generation worker")

            // Create notification channel (no-op on Android < 8.0)
            createNotificationChannel()

            // Get all messages that need embeddings
            val messagesToEmbed = messageRepository.getMessagesNeedingEmbedding(Int.MAX_VALUE)
            val totalMessages = messagesToEmbed.size

            if (totalMessages == 0) {
                Timber.d("No messages need embedding")
                return Result.success()
            }

            Timber.d("Found $totalMessages messages needing embeddings")

            // Set foreground to show notification
            setForeground(createForegroundInfo(0, totalMessages))

            // Update corpus with all message bodies for IDF calculation
            val allMessages = messageRepository.getAllMessagesSnapshot()
            val corpus = allMessages.map { it.body }
            textEmbeddingService.updateCorpus(corpus)

            Timber.d("Corpus updated with ${corpus.size} documents")

            // Process messages in batches
            var processedCount = 0

            messagesToEmbed.chunked(BATCH_SIZE).forEach { batch ->
                // Check if work was cancelled
                if (isStopped) {
                    Timber.d("Worker cancelled, stopping embedding generation")
                    return Result.failure()
                }

                // Generate embeddings for batch
                batch.forEach { message ->
                    try {
                        // Generate embedding
                        val embedding = textEmbeddingService.generateEmbedding(message.body)
                        val embeddingString = textEmbeddingService.embeddingToString(embedding)

                        // Save to database
                        messageRepository.updateEmbedding(
                            messageId = message.id,
                            embedding = embeddingString,
                            version = 1,
                            timestamp = System.currentTimeMillis()
                        )

                        processedCount++

                        // Update progress every 10 messages
                        if (processedCount % 10 == 0) {
                            val progress = processedCount.toFloat() / totalMessages
                            setProgressAsync(
                                workDataOf(
                                    KEY_PROGRESS to progress,
                                    KEY_PROCESSED to processedCount,
                                    KEY_TOTAL to totalMessages,
                                    KEY_MESSAGE to "Indexed $processedCount / $totalMessages messages"
                                )
                            )
                            // Update foreground notification
                            setForeground(createForegroundInfo(processedCount, totalMessages))
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to generate embedding for message ${message.id}")
                        // Continue with next message on error
                    }
                }

                Timber.d("Processed batch: $processedCount / $totalMessages messages")
            }

            // Final progress update
            setProgressAsync(
                workDataOf(
                    KEY_PROGRESS to 1.0f,
                    KEY_PROCESSED to processedCount,
                    KEY_TOTAL to totalMessages,
                    KEY_MESSAGE to "Indexing complete! $processedCount messages indexed"
                )
            )

            Timber.d("Embedding generation complete: $processedCount / $totalMessages messages")

            Result.success(
                workDataOf(
                    KEY_PROCESSED to processedCount,
                    KEY_TOTAL to totalMessages
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Error in embedding generation worker")
            Result.failure(
                workDataOf(
                    KEY_MESSAGE to "Error: ${e.message}"
                )
            )
        }
    }

    /**
     * Create notification channel for embedding generation notifications
     * Required for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress when indexing messages for search"
                setShowBadge(false)
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground info with notification showing indexing progress
     */
    private fun createForegroundInfo(processed: Int, total: Int): ForegroundInfo {
        val percentage = if (total > 0) (processed * 100 / total) else 0

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Indexing messages for search")
            .setContentText("$processed of $total messages ($percentage%)")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // You may want to create a custom icon
            .setProgress(total, processed, false)
            .setOngoing(true)
            .setSilent(true) // Don't make sound
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
