package com.vanespark.vertext.domain.mcp.tools

import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import com.vanespark.vertext.domain.mcp.ParameterType
import com.vanespark.vertext.domain.mcp.Tool
import com.vanespark.vertext.domain.mcp.ToolParameter
import com.vanespark.vertext.domain.mcp.ToolResult
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP Tool: get_thread_summary
 * Generates a summary of a message thread with statistics and key messages
 */
@Singleton
class GetThreadSummaryTool @Inject constructor(
    private val messageRepository: MessageRepository,
    private val threadRepository: ThreadRepository
) : Tool {

    override val name = "get_thread_summary"

    override val description = "Generates a comprehensive summary of a message thread including statistics, " +
            "date range, message count, and key excerpts"

    override val parameters = listOf(
        ToolParameter(
            name = "thread_id",
            type = ParameterType.NUMBER,
            description = "The ID of the thread to summarize",
            required = true
        ),
        ToolParameter(
            name = "max_messages",
            type = ParameterType.NUMBER,
            description = "Maximum number of messages to analyze (default: 1000)",
            required = false,
            default = "1000"
        ),
        ToolParameter(
            name = "include_excerpts",
            type = ParameterType.BOOLEAN,
            description = "Include message excerpts in summary (default: true)",
            required = false,
            default = "true"
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult {
        return try {
            val threadId = (arguments["thread_id"] as? Number)?.toLong()
                ?: return ToolResult(
                    success = false,
                    error = "thread_id is required and must be a number"
                )

            val maxMessages = (arguments["max_messages"] as? Number)?.toInt() ?: 1000
            val includeExcerpts = arguments["include_excerpts"] as? Boolean ?: true

            Timber.d("Generating summary for thread $threadId (max=$maxMessages, excerpts=$includeExcerpts)")

            // Get thread metadata
            val thread = threadRepository.getThreadById(threadId)
                ?: return ToolResult(
                    success = false,
                    error = "Thread not found: $threadId"
                )

            // Get messages for analysis
            val messages = messageRepository.getMessagesForThreadLimitSnapshot(threadId, maxMessages)

            if (messages.isEmpty()) {
                return ToolResult(
                    success = true,
                    data = mapOf(
                        "thread_id" to threadId,
                        "recipient" to (thread.recipientName ?: thread.recipient),
                        "message_count" to 0,
                        "summary" to "No messages in this thread"
                    )
                )
            }

            // Generate summary
            val summary = generateSummary(thread.recipientName ?: thread.recipient, messages, includeExcerpts)

            Timber.d("Generated summary for thread $threadId: ${summary["message_count"]} messages")

            ToolResult(
                success = true,
                data = summary
            )
        } catch (e: Exception) {
            Timber.e(e, "Error generating thread summary")
            ToolResult(
                success = false,
                error = "Failed to generate thread summary: ${e.message}"
            )
        }
    }

    private fun generateSummary(
        recipient: String,
        messages: List<Message>,
        includeExcerpts: Boolean
    ): Map<String, Any> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        // Count sent vs received
        val sentMessages = messages.count { it.type == Message.TYPE_SENT }
        val receivedMessages = messages.count { it.type == Message.TYPE_INBOX }

        // Date range
        val sortedMessages = messages.sortedBy { it.date }
        val firstMessageDate = sortedMessages.firstOrNull()?.date ?: 0L
        val lastMessageDate = sortedMessages.lastOrNull()?.date ?: 0L

        // Calculate time span
        val timeSpanMs = lastMessageDate - firstMessageDate
        val timeSpanDays = timeSpanMs / (1000 * 60 * 60 * 24)

        // Build summary map
        val summary = mutableMapOf<String, Any>(
            "thread_id" to (messages.firstOrNull()?.threadId ?: 0L),
            "recipient" to recipient,
            "message_count" to messages.size,
            "sent_count" to sentMessages,
            "received_count" to receivedMessages,
            "first_message_date" to dateFormat.format(Date(firstMessageDate)),
            "last_message_date" to dateFormat.format(Date(lastMessageDate)),
            "time_span_days" to timeSpanDays,
            "average_message_length" to messages.map { it.body.length }.average().toInt()
        )

        // Add conversation description
        val description = buildString {
            append("Conversation with $recipient spanning $timeSpanDays days. ")
            append("Total of ${messages.size} messages: ")
            append("$sentMessages sent, $receivedMessages received. ")

            if (timeSpanDays > 0) {
                val avgPerDay = messages.size.toFloat() / timeSpanDays
                append(String.format(Locale.US, "Average %.1f messages per day.", avgPerDay))
            }
        }
        summary["description"] = description

        // Add excerpts if requested
        if (includeExcerpts) {
            val excerpts = mutableListOf<Map<String, String>>()

            // First few messages (up to 3)
            sortedMessages.take(3).forEach { message ->
                excerpts.add(
                    mapOf(
                        "date" to dateFormat.format(Date(message.date)),
                        "type" to if (message.type == Message.TYPE_SENT) "sent" else "received",
                        "body" to message.body.take(200) // Limit excerpt length
                    )
                )
            }

            // Last few messages (up to 3)
            if (sortedMessages.size > 6) {
                excerpts.add(mapOf("separator" to "... (${sortedMessages.size - 6} messages omitted) ..."))
            }

            sortedMessages.takeLast(3).forEach { message ->
                excerpts.add(
                    mapOf(
                        "date" to dateFormat.format(Date(message.date)),
                        "type" to if (message.type == Message.TYPE_SENT) "sent" else "received",
                        "body" to message.body.take(200)
                    )
                )
            }

            summary["excerpts"] = excerpts
        }

        return summary
    }
}
