package com.vanespark.vertext.ui.assistant

/**
 * AI backend mode
 */
enum class AIBackendMode {
    GEMINI_NANO,  // On-device Gemini Nano with RAG
    FALLBACK      // Rule-based MCP tool calls
}

/**
 * UI state for AI assistant
 */
data class AIAssistantUiState(
    val isVisible: Boolean = false,
    val messages: List<AIMessage> = emptyList(),
    val isProcessing: Boolean = false,
    val inputText: String = "",
    val error: String? = null,
    val backendMode: AIBackendMode = AIBackendMode.FALLBACK,
    val isInitializing: Boolean = true
)

/**
 * Represents a message in the AI assistant conversation
 */
data class AIMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val toolUsed: String? = null, // Which MCP tool was used (if any)
    val isError: Boolean = false
)
