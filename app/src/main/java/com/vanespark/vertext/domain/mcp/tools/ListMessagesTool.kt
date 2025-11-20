package com.vanespark.vertext.domain.mcp.tools

import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.domain.mcp.ParameterType
import com.vanespark.vertext.domain.mcp.Tool
import com.vanespark.vertext.domain.mcp.ToolParameter
import com.vanespark.vertext.domain.mcp.ToolResult
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP Tool: list_messages
 * Lists recent messages from a thread or all threads
 */
@Singleton
class ListMessagesTool @Inject constructor(
    private val messageRepository: MessageRepository
) : Tool {

    override val name = "list_messages"

    override val description = "Lists recent messages from a specific thread or all threads"

    override val parameters = listOf(
        ToolParameter(
            name = "thread_id",
            type = ParameterType.NUMBER,
            description = "Specific thread ID to list messages from (optional, if not provided lists from all threads)",
            required = false
        ),
        ToolParameter(
            name = "limit",
            type = ParameterType.NUMBER,
            description = "Maximum number of messages to return (default: 20, max: 100)",
            required = false,
            default = "20"
        )
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override suspend fun execute(arguments: Map<String, Any>): ToolResult {
        return try {
            val threadId = (arguments["thread_id"] as? Number)?.toLong()
            val limit = (arguments["limit"] as? Number)?.toInt() ?: 20

            // Clamp limit to reasonable range
            val clampedLimit = limit.coerceIn(1, 100)

            Timber.d("Listing messages: threadId=$threadId, limit=$clampedLimit")

            val messages = if (threadId != null) {
                messageRepository.getMessagesForThreadLimit(threadId, clampedLimit)
            } else {
                messageRepository.getRecentMessages(clampedLimit)
            }

            // Format messages for output
            val formattedMessages = messages.map { message ->
                mapOf(
                    "message_id" to message.id,
                    "thread_id" to message.threadId,
                    "address" to message.address,
                    "body" to message.body,
                    "date" to message.date,
                    "formatted_date" to dateFormat.format(Date(message.date)),
                    "type" to when (message.type) {
                        1 -> "received"
                        2 -> "sent"
                        else -> "unknown"
                    },
                    "read" to message.isRead
                )
            }

            val result = mapOf(
                "messages" to formattedMessages,
                "count" to formattedMessages.size,
                "thread_id" to threadId,
                "limit" to clampedLimit
            )

            Timber.d("Listed ${formattedMessages.size} messages")

            ToolResult(
                success = true,
                data = result
            )
        } catch (e: Exception) {
            Timber.e(e, "Error listing messages")
            ToolResult(
                success = false,
                error = "Failed to list messages: ${e.message}"
            )
        }
    }
}
