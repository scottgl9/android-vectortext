package com.vanespark.vertext.ui.assistant

/**
 * UI state for AI assistant
 */
data class AIAssistantUiState(
    val isVisible: Boolean = false,
    val messages: List<AIMessage> = emptyList(),
    val isProcessing: Boolean = false,
    val inputText: String = "",
    val error: String? = null
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
