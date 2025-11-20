package com.vanespark.vertext.domain.mcp

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Built-in MCP (Model Context Protocol) Server
 * Implements JSON-RPC 2.0 protocol for tool calling
 * Runs fully on-device with pseudo-URL: builtin://vertext
 */
@Singleton
class BuiltInMcpServer @Inject constructor(
    private val gson: Gson
) {

    companion object {
        const val BUILTIN_SERVER_URL = "builtin://vertext"
        const val METHOD_LIST_TOOLS = "tools/list"
        const val METHOD_CALL_TOOL = "tools/call"
    }

    private val tools = mutableMapOf<String, Tool>()

    /**
     * Register a tool with the MCP server
     */
    fun registerTool(tool: Tool) {
        tools[tool.name] = tool
        Timber.d("Registered tool: ${tool.name}")
    }

    /**
     * Call a tool directly with arguments
     * This is a convenience method for internal use (e.g., AI assistant)
     */
    suspend fun callTool(toolName: String, arguments: Map<String, Any> = emptyMap()): ToolResult {
        val tool = tools[toolName]
            ?: return ToolResult(
                success = false,
                error = "Tool not found: $toolName"
            )

        // Validate required parameters
        val missingParams = tool.parameters
            .filter { it.required && !arguments.containsKey(it.name) }
            .map { it.name }

        if (missingParams.isNotEmpty()) {
            return ToolResult(
                success = false,
                error = "Missing required parameters: ${missingParams.joinToString()}"
            )
        }

        return try {
            tool.execute(arguments)
        } catch (e: Exception) {
            Timber.e(e, "Tool execution failed: $toolName")
            ToolResult(
                success = false,
                error = "Tool execution failed: ${e.message}"
            )
        }
    }

    /**
     * Handle incoming MCP JSON-RPC request
     */
    suspend fun handleRequest(requestJson: String): String = withContext(Dispatchers.IO) {
        try {
            val request = gson.fromJson(requestJson, McpRequest::class.java)
            Timber.d("MCP Request: method=${request.method}, id=${request.id}")

            val response = when (request.method) {
                METHOD_LIST_TOOLS -> handleListToolsRequest(request)
                METHOD_CALL_TOOL -> handleCallToolRequest(request)
                else -> McpResponse(
                    id = request.id,
                    error = McpError(
                        code = McpError.METHOD_NOT_FOUND,
                        message = "Method not found: ${request.method}"
                    )
                )
            }

            gson.toJson(response)
        } catch (e: Exception) {
            Timber.e(e, "Error handling MCP request")
            val errorResponse = McpResponse(
                id = "unknown",
                error = McpError(
                    code = McpError.INTERNAL_ERROR,
                    message = "Internal server error: ${e.message}"
                )
            )
            gson.toJson(errorResponse)
        }
    }

    /**
     * Handle tools/list request
     */
    private fun handleListToolsRequest(request: McpRequest): McpResponse {
        val toolDefinitions = tools.values.map { it.toDefinition() }

        Timber.d("Listing ${toolDefinitions.size} tools")

        return McpResponse(
            id = request.id,
            result = ListToolsResponse(tools = toolDefinitions)
        )
    }

    /**
     * Handle tools/call request
     */
    private suspend fun handleCallToolRequest(request: McpRequest): McpResponse {
        try {
            val params = request.params
                ?: return McpResponse(
                    id = request.id,
                    error = McpError(
                        code = McpError.INVALID_PARAMS,
                        message = "Missing params"
                    )
                )

            // Parse tool name and arguments from params
            val toolName = params["name"] as? String
                ?: return McpResponse(
                    id = request.id,
                    error = McpError(
                        code = McpError.INVALID_PARAMS,
                        message = "Missing tool name"
                    )
                )

            @Suppress("UNCHECKED_CAST")
            val arguments = params["arguments"] as? Map<String, Any> ?: emptyMap()

            val tool = tools[toolName]
                ?: return McpResponse(
                    id = request.id,
                    error = McpError(
                        code = McpError.METHOD_NOT_FOUND,
                        message = "Tool not found: $toolName"
                    )
                )

            Timber.d("Executing tool: $toolName with arguments: $arguments")

            // Validate required parameters
            val missingParams = tool.parameters
                .filter { it.required && !arguments.containsKey(it.name) }
                .map { it.name }

            if (missingParams.isNotEmpty()) {
                return McpResponse(
                    id = request.id,
                    error = McpError(
                        code = McpError.INVALID_PARAMS,
                        message = "Missing required parameters: ${missingParams.joinToString(", ")}"
                    )
                )
            }

            // Execute the tool
            val result = tool.execute(arguments)

            return if (result.success) {
                McpResponse(
                    id = request.id,
                    result = result.data
                )
            } else {
                McpResponse(
                    id = request.id,
                    error = McpError(
                        code = McpError.INTERNAL_ERROR,
                        message = result.error ?: "Tool execution failed"
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error executing tool")
            return McpResponse(
                id = request.id,
                error = McpError(
                    code = McpError.INTERNAL_ERROR,
                    message = "Error executing tool: ${e.message}"
                )
            )
        }
    }

    /**
     * Get server information
     */
    fun getServerInfo(): Map<String, Any> {
        return mapOf(
            "name" to "VerText MCP Server",
            "version" to "1.0.0",
            "url" to BUILTIN_SERVER_URL,
            "tools" to tools.keys.toList(),
            "methods" to listOf(METHOD_LIST_TOOLS, METHOD_CALL_TOOL)
        )
    }
}
