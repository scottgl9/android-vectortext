package com.vanespark.vertext.domain.mcp.tools

import com.vanespark.vertext.data.repository.ThreadRepository
import com.vanespark.vertext.domain.mcp.ParameterType
import com.vanespark.vertext.domain.mcp.Tool
import com.vanespark.vertext.domain.mcp.ToolParameter
import com.vanespark.vertext.domain.mcp.ToolResult
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP Tool: list_threads
 * Lists all message threads (conversations)
 */
@Singleton
class ListThreadsTool @Inject constructor(
    private val threadRepository: ThreadRepository
) : Tool {

    override val name = "list_threads"

    override val description = "Lists all message threads (conversations) with basic metadata"

    override val parameters = listOf(
        ToolParameter(
            name = "limit",
            type = ParameterType.NUMBER,
            description = "Maximum number of threads to return (default: 50, max: 200)",
            required = false,
            default = "50"
        ),
        ToolParameter(
            name = "include_archived",
            type = ParameterType.BOOLEAN,
            description = "Include archived threads in results (default: false)",
            required = false,
            default = "false"
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult {
        return try {
            val limit = (arguments["limit"] as? Number)?.toInt() ?: 50
            val includeArchived = arguments["include_archived"] as? Boolean ?: false

            // Clamp limit to reasonable range
            val clampedLimit = limit.coerceIn(1, 200)

            Timber.d("Listing threads: limit=$clampedLimit, includeArchived=$includeArchived")

            val threads = if (includeArchived) {
                threadRepository.getAllThreadsSnapshot()
                    .take(clampedLimit)
            } else {
                threadRepository.getThreadsLimit(clampedLimit)
            }

            // Format threads for output
            val formattedThreads = threads.map { thread ->
                mapOf(
                    "thread_id" to thread.id,
                    "recipient" to thread.recipient,
                    "recipient_name" to (thread.recipientName ?: thread.recipient),
                    "last_message" to (thread.lastMessage ?: ""),
                    "last_message_date" to thread.lastMessageDate,
                    "message_count" to thread.messageCount,
                    "unread_count" to thread.unreadCount,
                    "is_pinned" to thread.isPinned,
                    "is_archived" to thread.isArchived,
                    "is_muted" to thread.isMuted
                )
            }

            val result = mapOf(
                "threads" to formattedThreads,
                "count" to formattedThreads.size,
                "limit" to clampedLimit,
                "include_archived" to includeArchived
            )

            Timber.d("Listed ${formattedThreads.size} threads")

            ToolResult(
                success = true,
                data = result
            )
        } catch (e: Exception) {
            Timber.e(e, "Error listing threads")
            ToolResult(
                success = false,
                error = "Failed to list threads: ${e.message}"
            )
        }
    }
}
