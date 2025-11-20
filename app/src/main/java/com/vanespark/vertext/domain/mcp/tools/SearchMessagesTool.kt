package com.vanespark.vertext.domain.mcp.tools

import com.vanespark.vertext.domain.mcp.ParameterType
import com.vanespark.vertext.domain.mcp.Tool
import com.vanespark.vertext.domain.mcp.ToolParameter
import com.vanespark.vertext.domain.mcp.ToolResult
import com.vanespark.vertext.domain.service.MessageRetrievalService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP Tool: search_messages
 * Searches messages using semantic similarity with TF-IDF embeddings
 */
@Singleton
class SearchMessagesTool @Inject constructor(
    private val messageRetrievalService: MessageRetrievalService
) : Tool {

    override val name = "search_messages"

    override val description = "Searches messages using semantic similarity. " +
            "Finds relevant messages based on meaning, not just keywords. " +
            "Use natural language queries like 'messages about dinner plans' or 'when did Sarah send the gate code?'"

    override val parameters = listOf(
        ToolParameter(
            name = "query",
            type = ParameterType.STRING,
            description = "The search query or question. Can be natural language (e.g., 'messages about the roof repair', 'gate code from Sarah')",
            required = true
        ),
        ToolParameter(
            name = "max_results",
            type = ParameterType.NUMBER,
            description = "Maximum number of message excerpts to return (default: 5, max: 20)",
            required = false,
            default = "5"
        ),
        ToolParameter(
            name = "similarity_threshold",
            type = ParameterType.NUMBER,
            description = "Minimum similarity score for results, between 0.0 and 1.0 (default: 0.15 for better recall)",
            required = false,
            default = "0.15"
        )
    )

    override suspend fun execute(arguments: Map<String, Any>): ToolResult {
        return try {
            // Extract and validate parameters
            val query = arguments["query"] as? String
                ?: return ToolResult(
                    success = false,
                    error = "Missing required parameter: query"
                )

            if (query.isBlank()) {
                return ToolResult(
                    success = false,
                    error = "Query cannot be empty"
                )
            }

            val maxResults = (arguments["max_results"] as? Number)?.toInt() ?: 5
            val similarityThreshold = (arguments["similarity_threshold"] as? Number)?.toFloat() ?: 0.15f

            // Validate parameters
            val clampedMaxResults = maxResults.coerceIn(1, 20)
            val clampedThreshold = similarityThreshold.coerceIn(0.0f, 1.0f)

            Timber.d("Semantic search: query='$query', maxResults=$clampedMaxResults, threshold=$clampedThreshold")

            // Perform semantic search
            val results = messageRetrievalService.retrieveRelevantMessages(
                query = query,
                maxResults = clampedMaxResults,
                similarityThreshold = clampedThreshold
            )

            // Format response
            val response = if (results.isEmpty()) {
                mapOf(
                    "found" to false,
                    "message" to "No messages found matching the query. Try lowering the similarity threshold or using different search terms.",
                    "query" to query,
                    "threshold" to clampedThreshold
                )
            } else {
                mapOf(
                    "found" to true,
                    "count" to results.size,
                    "query" to query,
                    "threshold" to clampedThreshold,
                    "results" to results.joinToString("\n\n---\n\n")
                )
            }

            Timber.d("Search complete: ${results.size} results found")

            ToolResult(
                success = true,
                data = response
            )
        } catch (e: Exception) {
            Timber.e(e, "Error executing search_messages tool")
            ToolResult(
                success = false,
                error = "Failed to search messages: ${e.message}"
            )
        }
    }
}
