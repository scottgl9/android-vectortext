package com.vanespark.vertext.domain.mcp

import com.google.gson.annotations.SerializedName

/**
 * MCP (Model Context Protocol) data models
 * Implements JSON-RPC 2.0 protocol for tool calling
 */

/**
 * MCP JSON-RPC Request
 */
data class McpRequest(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: Map<String, Any>? = null
)

/**
 * MCP JSON-RPC Response
 */
data class McpResponse(
    val jsonrpc: String = "2.0",
    val id: String,
    val result: Any? = null,
    val error: McpError? = null
)

/**
 * MCP Error object
 */
data class McpError(
    val code: Int,
    val message: String,
    val data: Any? = null
) {
    companion object {
        const val PARSE_ERROR = -32700
        const val INVALID_REQUEST = -32600
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602
        const val INTERNAL_ERROR = -32603
    }
}

/**
 * Tool definition for MCP server
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>
)

/**
 * Tool parameter definition
 */
data class ToolParameter(
    val name: String,
    val type: ParameterType,
    val description: String,
    val required: Boolean = false,
    val default: String? = null
)

/**
 * Parameter type enumeration
 */
enum class ParameterType {
    @SerializedName("string")
    STRING,

    @SerializedName("number")
    NUMBER,

    @SerializedName("boolean")
    BOOLEAN,

    @SerializedName("object")
    OBJECT,

    @SerializedName("array")
    ARRAY
}

/**
 * Tool execution result
 */
data class ToolResult(
    val success: Boolean,
    val data: Any? = null,
    val error: String? = null
)

/**
 * Abstract tool interface
 */
interface Tool {
    val name: String
    val description: String
    val parameters: List<ToolParameter>

    suspend fun execute(arguments: Map<String, Any>): ToolResult

    fun toDefinition(): ToolDefinition {
        return ToolDefinition(
            name = name,
            description = description,
            parameters = parameters
        )
    }
}

/**
 * List tools response
 */
data class ListToolsResponse(
    val tools: List<ToolDefinition>
)

/**
 * Call tool request params
 */
data class CallToolParams(
    val name: String,
    val arguments: Map<String, Any>
)
