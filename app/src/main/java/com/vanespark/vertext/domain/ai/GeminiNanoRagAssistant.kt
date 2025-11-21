package com.vanespark.vertext.domain.ai

import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.domain.mcp.BuiltInMcpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAG-based AI Assistant using Gemini Nano
 *
 * Combines:
 * 1. Semantic search (retrieval) from user's messages
 * 2. Gemini Nano (generation) for natural language responses
 *
 * This provides a true RAG (Retrieval Augmented Generation) system
 * that answers questions about the user's text messages.
 */
@Singleton
class GeminiNanoRagAssistant @Inject constructor(
    private val geminiNano: GeminiNanoService,
    private val mcpServer: BuiltInMcpServer
) {
    /**
     * Process a user query with RAG
     *
     * Flow:
     * 1. Search for relevant messages using semantic search
     * 2. Build context from retrieved messages
     * 3. Generate response using Gemini Nano with context
     */
    suspend fun query(
        userQuery: String,
        maxContextMessages: Int = 10
    ): Result<AssistantResponse> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Retrieve relevant messages using semantic search
            Timber.d("Searching for relevant messages for query: $userQuery")

            val searchResult = mcpServer.callTool(
                "search_messages",
                mapOf(
                    "query" to userQuery,
                    "max_results" to maxContextMessages,
                    "similarity_threshold" to 0.3 // Lower threshold for broader results
                )
            )

            if (!searchResult.success) {
                Timber.w("Search failed: ${searchResult.error}")
                return@withContext Result.failure(Exception(searchResult.error ?: "Search failed"))
            }

            // Extract messages from search results
            val searchData = searchResult.data as? Map<*, *>
            val results = searchData?.get("results") as? List<*> ?: emptyList<Any>()

            // Step 2: Build context from retrieved messages
            val context = buildContext(results, userQuery)

            // Step 3: Generate response using Gemini Nano
            Timber.d("Generating response with Gemini Nano")
            val prompt = buildPrompt(userQuery, context)

            val responseResult = geminiNano.generate(prompt)

            if (responseResult.isFailure) {
                return@withContext Result.failure(
                    responseResult.exceptionOrNull() ?: Exception("Generation failed")
                )
            }

            val response = AssistantResponse(
                answer = responseResult.getOrNull() ?: "",
                contextMessages = results.size,
                toolsUsed = listOf("search_messages", "gemini_nano")
            )

            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "RAG query failed")
            Result.failure(e)
        }
    }

    /**
     * Build context string from search results
     */
    private fun buildContext(searchResults: List<*>, query: String): String {
        if (searchResults.isEmpty()) {
            return "No relevant messages found for this query."
        }

        val contextBuilder = StringBuilder()
        contextBuilder.append("Here are the relevant text messages:\n\n")

        searchResults.take(10).forEachIndexed { index, result ->
            val resultMap = result as? Map<*, *> ?: return@forEachIndexed

            val body = resultMap["body"] as? String ?: ""
            val sender = resultMap["sender"] as? String ?: "Unknown"
            val type = resultMap["type"] as? String ?: ""
            val date = resultMap["date"] as? Long ?: 0
            val similarity = resultMap["similarity"] as? Number ?: 0.0

            val direction = when (type) {
                "sent" -> "You sent to $sender"
                "received" -> "From $sender"
                else -> "Message with $sender"
            }

            contextBuilder.append("${index + 1}. $direction (${formatDate(date)}):\n")
            contextBuilder.append("   \"$body\"\n")
            contextBuilder.append("   (Relevance: ${(similarity.toDouble() * 100).toInt()}%)\n\n")
        }

        return contextBuilder.toString()
    }

    /**
     * Build the complete prompt for Gemini Nano
     */
    private fun buildPrompt(userQuery: String, context: String): String {
        return """
            You are a helpful AI assistant that answers questions about the user's text messages.

            Context:
            $context

            User Question: $userQuery

            Instructions:
            - Answer the user's question based on the text messages provided above
            - Be concise and direct
            - If the messages don't contain enough information to answer, say so
            - Quote specific messages when relevant
            - If asked about dates, times, or specific details, reference the message dates
            - Format your response in a natural, conversational way

            Answer:
        """.trimIndent()
    }

    /**
     * Format Unix timestamp to readable date
     */
    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "Unknown date"

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} minutes ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            diff < 604800_000 -> "${diff / 86400_000} days ago"
            else -> {
                val date = java.util.Date(timestamp)
                val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                formatter.format(date)
            }
        }
    }

    /**
     * Start a conversational chat session with RAG
     * This maintains conversation history for follow-up questions
     */
    fun startConversation() {
        geminiNano.startChat()
    }

    /**
     * Send a message in the conversation with RAG support
     */
    suspend fun sendMessageInConversation(
        userMessage: String,
        maxContextMessages: Int = 5
    ): Result<AssistantResponse> = withContext(Dispatchers.IO) {
        try {
            // Check if this is a question that needs message context
            val needsContext = detectNeedsContext(userMessage)

            if (needsContext) {
                // Perform RAG: search + context + generate
                val searchResult = mcpServer.callTool(
                    "search_messages",
                    mapOf(
                        "query" to userMessage,
                        "max_results" to maxContextMessages,
                        "similarity_threshold" to 0.3
                    )
                )

                val searchData = searchResult.data as? Map<*, *>
                val results = searchData?.get("results") as? List<*> ?: emptyList<Any>()

                val context = buildContext(results, userMessage)
                val messageWithContext = buildConversationalPrompt(userMessage, context)

                val responseResult = geminiNano.sendMessage(messageWithContext)

                if (responseResult.isFailure) {
                    return@withContext Result.failure(
                        responseResult.exceptionOrNull() ?: Exception("Generation failed")
                    )
                }

                val response = AssistantResponse(
                    answer = responseResult.getOrNull() ?: "",
                    contextMessages = results.size,
                    toolsUsed = listOf("search_messages", "gemini_nano")
                )

                Result.success(response)
            } else {
                // Direct question without needing message context
                val responseResult = geminiNano.sendMessage(userMessage)

                if (responseResult.isFailure) {
                    return@withContext Result.failure(
                        responseResult.exceptionOrNull() ?: Exception("Generation failed")
                    )
                }

                val response = AssistantResponse(
                    answer = responseResult.getOrNull() ?: "",
                    contextMessages = 0,
                    toolsUsed = listOf("gemini_nano")
                )

                Result.success(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Conversation message failed")
            Result.failure(e)
        }
    }

    /**
     * End the conversation session
     */
    fun endConversation() {
        geminiNano.endChat()
    }

    /**
     * Detect if a query needs message context
     */
    private fun detectNeedsContext(query: String): Boolean {
        val lowerQuery = query.lowercase()

        // Keywords that indicate needing message context
        val contextKeywords = listOf(
            "message", "text", "said", "told", "asked", "mentioned",
            "conversation", "chat", "sms", "talk", "discuss",
            "when did", "what did", "who", "where", "why",
            "find", "search", "show", "get"
        )

        return contextKeywords.any { lowerQuery.contains(it) }
    }

    /**
     * Build prompt for conversational RAG
     */
    private fun buildConversationalPrompt(userMessage: String, context: String): String {
        return if (context.contains("No relevant messages")) {
            userMessage
        } else {
            """
                Context from user's messages:
                $context

                User: $userMessage
            """.trimIndent()
        }
    }
}

/**
 * Response from the RAG assistant
 */
data class AssistantResponse(
    val answer: String,
    val contextMessages: Int,
    val toolsUsed: List<String>
)
