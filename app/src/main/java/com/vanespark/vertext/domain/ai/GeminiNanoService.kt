package com.vanespark.vertext.domain.ai

import android.content.Context
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.content
import com.google.ai.edge.aicore.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Availability status for Gemini Nano
 */
enum class GeminiNanoStatus {
    UNKNOWN,            // Not yet checked
    AVAILABLE,          // Ready to use immediately
    NOT_AVAILABLE,      // Device doesn't support it
    ERROR              // Error occurred
}

/**
 * Service for interacting with Gemini Nano on-device LLM via AI Edge SDK
 *
 * Gemini Nano is Google's lightweight LLM that runs entirely on-device via AICore.
 * Available on:
 * - Google Pixel 9 series (experimental SDK access)
 * - Google Pixel 8/8 Pro/8a (via ML Kit)
 * - Samsung Galaxy S24 series
 * - Other flagship devices with Android 14+
 *
 * Note: This uses the experimental AI Edge SDK (0.0.1-exp02)
 */
@Singleton
class GeminiNanoService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _status = MutableStateFlow(GeminiNanoStatus.UNKNOWN)
    val status: StateFlow<GeminiNanoStatus> = _status.asStateFlow()

    private var generativeModel: GenerativeModel? = null

    // Conversation history for multi-turn chat (as simple text)
    private val conversationHistory = mutableListOf<Pair<String, String>>()

    /**
     * Check if Gemini Nano is available on this device
     *
     * Note: The AI Edge SDK (experimental) doesn't provide explicit availability checking.
     * We try to initialize and catch exceptions.
     */
    suspend fun checkAvailability(): GeminiNanoStatus = withContext(Dispatchers.IO) {
        try {
            // Attempt to create a model instance
            // If this succeeds, Gemini Nano is available
            val config = generationConfig {
                temperature = 0.7f
                topK = 40
                maxOutputTokens = 1024
            }

            val testModel = GenerativeModel(
                generationConfig = config
            )

            Timber.d("Gemini Nano is AVAILABLE - test model created successfully")
            _status.value = GeminiNanoStatus.AVAILABLE
            GeminiNanoStatus.AVAILABLE

        } catch (e: IllegalStateException) {
            // Model not available on this device
            Timber.w("Gemini Nano is NOT_AVAILABLE: ${e.message}")
            _status.value = GeminiNanoStatus.NOT_AVAILABLE
            GeminiNanoStatus.NOT_AVAILABLE

        } catch (e: Exception) {
            Timber.e(e, "Error checking Gemini Nano availability")
            _status.value = GeminiNanoStatus.ERROR
            GeminiNanoStatus.ERROR
        }
    }

    /**
     * Initialize Gemini Nano for use
     * Must be called before generate()
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (_status.value != GeminiNanoStatus.AVAILABLE) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini Nano is not available. Status: ${_status.value}")
                )
            }

            // Create generative model instance
            val config = generationConfig {
                temperature = 0.7f
                topK = 40
                maxOutputTokens = 1024
            }

            generativeModel = GenerativeModel(
                generationConfig = config
            )

            Timber.d("Gemini Nano initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Gemini Nano")
            Result.failure(e)
        }
    }

    /**
     * Generate a single response from a prompt
     * Use this for one-off queries
     */
    suspend fun generate(prompt: String): Result<String> = withContext(Dispatchers.Default) {
        try {
            val model = generativeModel
                ?: return@withContext Result.failure(
                    IllegalStateException("Gemini Nano not initialized. Call initialize() first.")
                )

            Timber.d("Generating response for prompt: ${prompt.take(100)}...")

            val inputContent = content {
                text(prompt)
            }

            val response = model.generateContent(inputContent)
            val text = response.text ?: ""

            Timber.d("Generated response: ${text.take(100)}...")
            Result.success(text)
        } catch (e: Exception) {
            Timber.e(e, "Error generating response")
            Result.failure(e)
        }
    }

    /**
     * Start a chat session for multi-turn conversations
     * This clears conversation history
     */
    fun startChat() {
        conversationHistory.clear()
        Timber.d("Chat session started")
    }

    /**
     * Send a message in the current chat session
     * Maintains conversation history for context
     */
    suspend fun sendMessage(message: String): Result<String> = withContext(Dispatchers.Default) {
        try {
            val model = generativeModel
                ?: return@withContext Result.failure(
                    IllegalStateException("Gemini Nano not initialized. Call initialize() first.")
                )

            Timber.d("Sending message: ${message.take(100)}...")

            // Add user message to history
            conversationHistory.add("user" to message)

            // Build full prompt with conversation history
            val fullPrompt = buildConversationPrompt()
            val inputContent = content {
                text(fullPrompt)
            }

            val response = model.generateContent(inputContent)
            val text = response.text ?: ""

            // Add model response to history
            conversationHistory.add("model" to text)

            Timber.d("Received response: ${text.take(100)}...")
            Result.success(text)
        } catch (e: Exception) {
            Timber.e(e, "Error sending message")
            Result.failure(e)
        }
    }

    /**
     * Build conversation prompt from history
     */
    private fun buildConversationPrompt(): String {
        val sb = StringBuilder()
        for ((role, text) in conversationHistory) {
            sb.append("$role: $text\n")
        }
        return sb.toString()
    }

    /**
     * End the current chat session
     */
    fun endChat() {
        conversationHistory.clear()
        Timber.d("Chat session ended")
    }

    /**
     * Check if a chat session is active
     */
    fun hasActiveChat(): Boolean = conversationHistory.isNotEmpty()

    /**
     * Get the current status
     */
    fun getCurrentStatus(): GeminiNanoStatus = _status.value

    /**
     * Check if Gemini Nano is ready to use
     */
    fun isReady(): Boolean = _status.value == GeminiNanoStatus.AVAILABLE && generativeModel != null
}
