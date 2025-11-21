package com.vanespark.vertext.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.domain.ai.GeminiNanoRagAssistant
import com.vanespark.vertext.domain.ai.GeminiNanoService
import com.vanespark.vertext.domain.ai.GeminiNanoStatus
import com.vanespark.vertext.domain.mcp.BuiltInMcpServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for AI assistant
 * Handles natural language queries with Gemini Nano RAG or fallback MCP tools
 */
@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val geminiNano: GeminiNanoService,
    private val geminiRagAssistant: GeminiNanoRagAssistant,
    private val mcpServer: BuiltInMcpServer
) : ViewModel() {

    private var isGeminiNanoAvailable = false
    private var isGeminiNanoInitialized = false

    private val _uiState = MutableStateFlow(AIAssistantUiState())
    val uiState: StateFlow<AIAssistantUiState> = _uiState.asStateFlow()

    init {
        // Check Gemini Nano availability on init
        checkGeminiNanoAvailability()
    }

    /**
     * Check if Gemini Nano is available and initialize it
     */
    private fun checkGeminiNanoAvailability() {
        viewModelScope.launch {
            try {
                val status = geminiNano.checkAvailability()

                when (status) {
                    GeminiNanoStatus.AVAILABLE -> {
                        Timber.d("Gemini Nano is available, initializing...")
                        val initResult = geminiNano.initialize()

                        if (initResult.isSuccess) {
                            isGeminiNanoAvailable = true
                            isGeminiNanoInitialized = true
                            geminiRagAssistant.startConversation()
                            _uiState.update { it.copy(
                                backendMode = AIBackendMode.GEMINI_NANO,
                                isInitializing = false
                            )}
                            Timber.d("Gemini Nano initialized successfully - RAG mode enabled!")
                        } else {
                            Timber.w("Gemini Nano initialization failed: ${initResult.exceptionOrNull()?.message}")
                            _uiState.update { it.copy(
                                backendMode = AIBackendMode.FALLBACK,
                                isInitializing = false
                            )}
                        }
                    }
                    GeminiNanoStatus.NOT_AVAILABLE -> {
                        Timber.d("Gemini Nano is not available on this device - using fallback")
                        isGeminiNanoAvailable = false
                        _uiState.update { it.copy(
                            backendMode = AIBackendMode.FALLBACK,
                            isInitializing = false
                        )}
                    }
                    else -> {
                        Timber.d("Gemini Nano status unknown: $status")
                        isGeminiNanoAvailable = false
                        _uiState.update { it.copy(
                            backendMode = AIBackendMode.FALLBACK,
                            isInitializing = false
                        )}
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking Gemini Nano availability")
                isGeminiNanoAvailable = false
                _uiState.update { it.copy(
                    backendMode = AIBackendMode.FALLBACK,
                    isInitializing = false
                )}
            }
        }
    }

    /**
     * Show the assistant bottom sheet
     */
    fun show() {
        _uiState.update { it.copy(isVisible = true) }
    }

    /**
     * Hide the assistant bottom sheet
     */
    fun dismiss() {
        _uiState.update { it.copy(isVisible = false) }
    }

    /**
     * Update input text
     */
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Process user query
     */
    fun sendMessage(query: String) {
        if (query.isBlank()) return

        val trimmedQuery = query.trim()

        // Add user message
        val userMessage = AIMessage(
            id = UUID.randomUUID().toString(),
            content = trimmedQuery,
            isUser = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isProcessing = true,
                error = null
            )
        }

        // Process query and get response
        viewModelScope.launch {
            try {
                val response = processQuery(trimmedQuery)

                val assistantMessage = AIMessage(
                    id = UUID.randomUUID().toString(),
                    content = response.content,
                    isUser = false,
                    toolUsed = response.toolUsed,
                    isError = false
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing query")

                val errorMessage = AIMessage(
                    id = UUID.randomUUID().toString(),
                    content = "Sorry, I encountered an error: ${e.message}",
                    isUser = false,
                    isError = true
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMessage,
                        isProcessing = false,
                        error = e.message
                    )
                }
            }
        }
    }

    /**
     * Process user query with Gemini Nano RAG or fallback to rule-based
     */
    private suspend fun processQuery(query: String): AssistantResponse {
        // If Gemini Nano is available and initialized, use RAG with conversation
        if (isGeminiNanoInitialized) {
            return try {
                Timber.d("Using Gemini Nano RAG for query: $query")

                // Use conversation-based RAG for multi-turn support
                val ragResult = geminiRagAssistant.sendMessageInConversation(
                    userMessage = query,
                    maxContextMessages = 10
                )

                if (ragResult.isSuccess) {
                    val response = ragResult.getOrNull()!!
                    val contextInfo = if (response.contextMessages > 0)
                        "${response.contextMessages} messages"
                    else
                        "chat"
                    AssistantResponse(
                        content = response.answer,
                        toolUsed = "gemini_nano ($contextInfo)"
                    )
                } else {
                    // Fallback to rule-based on error
                    Timber.w("Gemini Nano RAG failed, using fallback: ${ragResult.exceptionOrNull()?.message}")
                    processQueryFallback(query)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error using Gemini Nano RAG")
                processQueryFallback(query)
            }
        }

        // Fallback to rule-based system
        return processQueryFallback(query)
    }

    /**
     * Fallback rule-based query processing (original implementation)
     */
    private suspend fun processQueryFallback(query: String): AssistantResponse {
        val lowerQuery = query.lowercase()

        // Determine which tool to use based on query
        return when {
            // Search messages
            lowerQuery.contains("search") || lowerQuery.contains("find") -> {
                val searchQuery = extractSearchQuery(query)
                searchMessages(searchQuery)
            }

            // List conversations/threads
            lowerQuery.contains("list") && (lowerQuery.contains("conversation") || lowerQuery.contains("thread")) -> {
                listThreads()
            }

            // Get thread summary
            lowerQuery.contains("summar") -> {
                // Try to extract thread ID or ask for clarification
                val summary = "To get a thread summary, please specify which conversation you'd like summarized. You can say 'summarize conversation with [contact name]' or provide a thread ID."
                AssistantResponse(summary, null)
            }

            // Recent messages
            lowerQuery.contains("recent") || lowerQuery.contains("latest") -> {
                listRecentMessages()
            }

            // Default: perform search
            else -> {
                searchMessages(query)
            }
        }
    }

    /**
     * Extract search query from user input
     */
    private fun extractSearchQuery(query: String): String {
        // Remove common command words
        return query
            .replace(Regex("(search|find|look for|show me)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(messages|texts|conversations)\\s+(about|with|from)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    /**
     * Search messages using MCP search_messages tool
     */
    private suspend fun searchMessages(query: String): AssistantResponse {
        val params = mapOf(
            "query" to query,
            "max_results" to 10,
            "similarity_threshold" to 0.15
        )

        val result = mcpServer.callTool("search_messages", params)

        return if (result.success) {
            val data = result.data as? Map<*, *>
            val results = data?.get("results") as? List<*> ?: emptyList<Any>()

            if (results.isEmpty()) {
                AssistantResponse(
                    "I couldn't find any messages matching \"$query\". Try a different search term.",
                    "search_messages"
                )
            } else {
                val formattedResults = formatSearchResults(results, query)
                AssistantResponse(formattedResults, "search_messages")
            }
        } else {
            AssistantResponse(
                "Search failed: ${result.error ?: "Unknown error"}",
                "search_messages"
            )
        }
    }

    /**
     * List threads using MCP list_threads tool
     */
    private suspend fun listThreads(): AssistantResponse {
        val params = mapOf("limit" to 20)
        val result = mcpServer.callTool("list_threads", params)

        return if (result.success) {
            val data = result.data as? Map<*, *>
            val threads = data?.get("threads") as? List<*> ?: emptyList<Any>()

            val formatted = formatThreadList(threads)
            AssistantResponse(formatted, "list_threads")
        } else {
            AssistantResponse(
                "Failed to list threads: ${result.error ?: "Unknown error"}",
                "list_threads"
            )
        }
    }

    /**
     * List recent messages using MCP list_messages tool
     */
    private suspend fun listRecentMessages(): AssistantResponse {
        val params = mapOf("limit" to 20)
        val result = mcpServer.callTool("list_messages", params)

        return if (result.success) {
            val data = result.data as? Map<*, *>
            val messages = data?.get("messages") as? List<*> ?: emptyList<Any>()

            val formatted = formatMessageList(messages)
            AssistantResponse(formatted, "list_messages")
        } else {
            AssistantResponse(
                "Failed to list messages: ${result.error ?: "Unknown error"}",
                "list_messages"
            )
        }
    }

    /**
     * Format search results for display
     */
    private fun formatSearchResults(results: List<*>, query: String): String {
        val builder = StringBuilder()
        builder.append("Found ${results.size} messages matching \"$query\":\n\n")

        results.take(5).forEachIndexed { index, result ->
            val resultMap = result as? Map<*, *> ?: return@forEachIndexed
            val body = resultMap["body"] as? String ?: ""
            val sender = resultMap["sender"] as? String ?: "Unknown"
            val similarity = resultMap["similarity"] as? Number ?: 0.0

            builder.append("${index + 1}. From $sender (${String.format("%.0f", similarity.toDouble() * 100)}% match)\n")
            builder.append("   \"${body.take(100)}${if (body.length > 100) "..." else ""}\"\n\n")
        }

        if (results.size > 5) {
            builder.append("... and ${results.size - 5} more results")
        }

        return builder.toString()
    }

    /**
     * Format thread list for display
     */
    private fun formatThreadList(threads: List<*>): String {
        val builder = StringBuilder()
        builder.append("Here are your conversations:\n\n")

        threads.take(10).forEachIndexed { index, thread ->
            val threadMap = thread as? Map<*, *> ?: return@forEachIndexed
            val recipientName = threadMap["recipient_name"] as? String
            val recipient = threadMap["recipient"] as? String ?: "Unknown"
            val messageCount = threadMap["message_count"] as? Number ?: 0
            val unreadCount = threadMap["unread_count"] as? Number ?: 0

            val displayName = recipientName ?: recipient
            val unreadText = if (unreadCount.toInt() > 0) " (${unreadCount} unread)" else ""

            builder.append("${index + 1}. $displayName$unreadText\n")
            builder.append("   $messageCount messages\n\n")
        }

        if (threads.size > 10) {
            builder.append("... and ${threads.size - 10} more conversations")
        }

        return builder.toString()
    }

    /**
     * Format message list for display
     */
    private fun formatMessageList(messages: List<*>): String {
        val builder = StringBuilder()
        builder.append("Here are your recent messages:\n\n")

        messages.take(10).forEachIndexed { index, message ->
            val messageMap = message as? Map<*, *> ?: return@forEachIndexed
            val body = messageMap["body"] as? String ?: ""
            val sender = messageMap["address"] as? String ?: "Unknown"
            val type = messageMap["type"] as? String ?: "received"

            val prefix = if (type == "sent") "To" else "From"
            builder.append("${index + 1}. $prefix $sender\n")
            builder.append("   \"${body.take(80)}${if (body.length > 80) "..." else ""}\"\n\n")
        }

        if (messages.size > 10) {
            builder.append("... and ${messages.size - 10} more messages")
        }

        return builder.toString()
    }

    /**
     * Clear conversation history
     */
    fun clearHistory() {
        _uiState.update { it.copy(messages = emptyList()) }
        // Also reset Gemini Nano conversation if active
        if (isGeminiNanoInitialized) {
            geminiRagAssistant.endConversation()
            geminiRagAssistant.startConversation()
        }
    }

    /**
     * Assistant response data
     */
    private data class AssistantResponse(
        val content: String,
        val toolUsed: String?
    )
}
