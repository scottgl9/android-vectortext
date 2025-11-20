package com.vanespark.vertext.domain.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
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
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting embedding generation worker")

            // Get all messages that need embeddings
            val messagesToEmbed = messageRepository.getMessagesNeedingEmbedding(Int.MAX_VALUE)
            val totalMessages = messagesToEmbed.size

            if (totalMessages == 0) {
                Timber.d("No messages need embedding")
                return Result.success()
            }

            Timber.d("Found $totalMessages messages needing embeddings")

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
}
