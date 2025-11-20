package com.vanespark.vertext.domain.mcp.tools

import com.vanespark.vertext.domain.mcp.ParameterType
import com.vanespark.vertext.domain.mcp.Tool
import com.vanespark.vertext.domain.mcp.ToolParameter
import com.vanespark.vertext.domain.mcp.ToolResult
import com.vanespark.vertext.domain.service.MessagingService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP Tool: send_message
 * Sends a new SMS/MMS message
 */
@Singleton
class SendMessageTool @Inject constructor(
    private val messagingService: MessagingService
) : Tool {

    override val name = "send_message"

    override val description = "Sends a new SMS/MMS message to a phone number"

    override val parameters = listOf(
        ToolParameter(
            name = "phone_number",
            type = ParameterType.STRING,
            description = "Recipient phone number (E.164 format recommended, e.g. +1234567890)",
            required = true
        ),
        ToolParameter(
            name = "text",
            type = ParameterType.STRING,
            description = "Message content to send",
            required = true
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult {
        return try {
            val phoneNumber = arguments["phone_number"] as? String
                ?: return ToolResult(
                    success = false,
                    error = "Missing required parameter: phone_number"
                )

            val text = arguments["text"] as? String
                ?: return ToolResult(
                    success = false,
                    error = "Missing required parameter: text"
                )

            // Validate phone number (basic check)
            if (phoneNumber.isBlank()) {
                return ToolResult(
                    success = false,
                    error = "Phone number cannot be empty"
                )
            }

            // Validate message text
            if (text.isBlank()) {
                return ToolResult(
                    success = false,
                    error = "Message text cannot be empty"
                )
            }

            // Message length check
            if (text.length > 1600) {
                return ToolResult(
                    success = false,
                    error = "Message too long (max 1600 characters for MMS fallback)"
                )
            }

            Timber.d("Sending message to $phoneNumber: ${text.take(50)}...")

            val result = messagingService.sendSmsMessage(phoneNumber, text)

            result.fold(
                onSuccess = { messageId ->
                    Timber.d("Message sent successfully: messageId=$messageId")
                    ToolResult(
                        success = true,
                        data = mapOf(
                            "status" to "sent",
                            "message_id" to messageId,
                            "phone_number" to phoneNumber,
                            "text_length" to text.length,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to send message")
                    ToolResult(
                        success = false,
                        error = "Failed to send message: ${error.message}"
                    )
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error sending message")
            ToolResult(
                success = false,
                error = "Failed to send message: ${e.message}"
            )
        }
    }
}
