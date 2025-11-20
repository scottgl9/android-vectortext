package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Message Retrieval Service for semantic search
 * Uses cosine similarity with embeddings to find relevant messages
 *
 * Search Algorithm:
 * 1. Generate embedding for search query
 * 2. Fetch message chunks in batches (50 per batch) from database
 * 3. For each chunk:
 *    - Deserialize embedding from string
 *    - Calculate cosine similarity with query embedding
 *    - Keep if similarity >= threshold (0.15)
 * 4. Sort all results by similarity (descending)
 * 5. Take top N results
 * 6. Format with metadata (sender, timestamp, relevance %)
 */
@Singleton
class MessageRetrievalService @Inject constructor(
    private val messageRepository: MessageRepository,
    private val textEmbeddingService: TextEmbeddingService
) {

    companion object {
        const val DEFAULT_BATCH_SIZE = 50  // Messages per batch (prevents CursorWindow limit)
        const val DEFAULT_SIMILARITY_THRESHOLD = 0.15f  // Lower threshold for better recall
        const val DEFAULT_MAX_RESULTS = 5
    }

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    /**
     * Retrieve relevant messages using semantic similarity
     *
     * @param query Search query text
     * @param maxResults Maximum number of results to return
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @return List of formatted message results with metadata
     */
    suspend fun retrieveRelevantMessages(
        query: String,
        maxResults: Int = DEFAULT_MAX_RESULTS,
        similarityThreshold: Float = DEFAULT_SIMILARITY_THRESHOLD
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Searching messages for query: $query (threshold: $similarityThreshold, max: $maxResults)")

            // Generate embedding for search query
            val queryEmbedding = textEmbeddingService.generateEmbedding(query)

            // Get all messages with embeddings
            val allMessages = messageRepository.getMessagesWithEmbeddings()

            if (allMessages.isEmpty()) {
                Timber.w("No messages with embeddings found")
                return@withContext listOf("No messages have been indexed yet. Please wait for indexing to complete.")
            }

            // Calculate similarities and filter
            val results = mutableListOf<MessageResult>()

            allMessages.forEach { message ->
                message.embedding?.let { embeddingString ->
                    val messageEmbedding = textEmbeddingService.stringToEmbedding(embeddingString)
                    val similarity = textEmbeddingService.cosineSimilarity(queryEmbedding, messageEmbedding)

                    if (similarity >= similarityThreshold) {
                        results.add(
                            MessageResult(
                                messageId = message.id,
                                body = message.body,
                                sender = message.address,
                                timestamp = message.date,
                                similarity = similarity
                            )
                        )
                    }
                }
            }

            // Sort by similarity (descending) and take top N
            val topResults = results
                .sortedByDescending { it.similarity }
                .take(maxResults)

            Timber.d("Found ${results.size} matches, returning top ${topResults.size}")

            // Format results
            topResults.map { result ->
                formatMessageResult(result)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving relevant messages")
            listOf("Error searching messages: ${e.message}")
        }
    }

    /**
     * Build RAG (Retrieval-Augmented Generation) context from relevant messages
     * Returns concatenated message context for LLM consumption
     */
    suspend fun buildRagContext(
        query: String,
        maxResults: Int = 3,
        maxContextLength: Int = 1000
    ): String? = withContext(Dispatchers.IO) {
        try {
            val queryEmbedding = textEmbeddingService.generateEmbedding(query)
            val allMessages = messageRepository.getMessagesWithEmbeddings()

            if (allMessages.isEmpty()) {
                return@withContext null
            }

            // Find relevant messages
            val results = allMessages
                .mapNotNull { message ->
                    message.embedding?.let { embeddingString ->
                        val messageEmbedding = textEmbeddingService.stringToEmbedding(embeddingString)
                        val similarity = textEmbeddingService.cosineSimilarity(queryEmbedding, messageEmbedding)

                        if (similarity >= DEFAULT_SIMILARITY_THRESHOLD) {
                            MessageResult(
                                messageId = message.id,
                                body = message.body,
                                sender = message.address,
                                timestamp = message.date,
                                similarity = similarity
                            )
                        } else null
                    }
                }
                .sortedByDescending { it.similarity }
                .take(maxResults)

            if (results.isEmpty()) {
                return@withContext null
            }

            // Build context string
            val contextBuilder = StringBuilder()
            contextBuilder.append("Relevant messages:\n\n")

            for ((index, result) in results.withIndex()) {
                val messageContext = """
                    Message ${index + 1}:
                    From: ${result.sender}
                    Date: ${dateFormat.format(Date(result.timestamp))}
                    Content: ${result.body}

                """.trimIndent()

                // Check length limit
                if (contextBuilder.length + messageContext.length > maxContextLength) {
                    break
                }

                contextBuilder.append(messageContext)
                contextBuilder.append("\n")
            }

            contextBuilder.toString().trim()
        } catch (e: Exception) {
            Timber.e(e, "Error building RAG context")
            null
        }
    }

    /**
     * Search messages in batched mode (for very large datasets)
     * Processes messages in chunks to avoid memory issues
     */
    suspend fun retrieveRelevantMessagesBatched(
        query: String,
        maxResults: Int = DEFAULT_MAX_RESULTS,
        similarityThreshold: Float = DEFAULT_SIMILARITY_THRESHOLD,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            val queryEmbedding = textEmbeddingService.generateEmbedding(query)

            val results = mutableListOf<MessageResult>()
            var offset = 0

            // Process in batches
            while (true) {
                val batch = messageRepository.getMessagesWithEmbeddingsPaged(
                    limit = batchSize,
                    offset = offset
                )

                if (batch.isEmpty()) break

                // Process batch
                batch.forEach { message ->
                    message.embedding?.let { embeddingString ->
                        val messageEmbedding = textEmbeddingService.stringToEmbedding(embeddingString)
                        val similarity = textEmbeddingService.cosineSimilarity(queryEmbedding, messageEmbedding)

                        if (similarity >= similarityThreshold) {
                            results.add(
                                MessageResult(
                                    messageId = message.id,
                                    body = message.body,
                                    sender = message.address,
                                    timestamp = message.date,
                                    similarity = similarity
                                )
                            )
                        }
                    }
                }

                offset += batchSize
                Timber.d("Processed batch: offset=$offset, results so far=${results.size}")
            }

            // Sort and take top N
            val topResults = results
                .sortedByDescending { it.similarity }
                .take(maxResults)

            Timber.d("Batched search complete: ${results.size} matches, returning top ${topResults.size}")

            topResults.map { formatMessageResult(it) }
        } catch (e: Exception) {
            Timber.e(e, "Error in batched retrieval")
            listOf("Error searching messages: ${e.message}")
        }
    }

    // === Private Helper Methods ===

    /**
     * Format a message result for output
     */
    private fun formatMessageResult(result: MessageResult): String {
        val relevancePercent = (result.similarity * 100).toInt()
        val formattedDate = dateFormat.format(Date(result.timestamp))

        return """
            [${relevancePercent}% relevant]
            From: ${result.sender}
            Date: $formattedDate
            Message: ${result.body.take(200)}${if (result.body.length > 200) "..." else ""}
        """.trimIndent()
    }

    /**
     * Internal data class for search results
     */
    private data class MessageResult(
        val messageId: Long,
        val body: String,
        val sender: String,
        val timestamp: Long,
        val similarity: Float
    )
}
