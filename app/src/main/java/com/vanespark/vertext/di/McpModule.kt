package com.vanespark.vertext.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vanespark.vertext.domain.mcp.BuiltInMcpServer
import com.vanespark.vertext.domain.mcp.tools.ListMessagesTool
import com.vanespark.vertext.domain.mcp.tools.ListThreadsTool
import com.vanespark.vertext.domain.mcp.tools.SendMessageTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

/**
 * Hilt module for MCP (Model Context Protocol) dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object McpModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setPrettyPrinting()
            .create()
    }

    @Provides
    @Singleton
    fun provideBuiltInMcpServer(
        gson: Gson,
        listThreadsTool: ListThreadsTool,
        listMessagesTool: ListMessagesTool,
        sendMessageTool: SendMessageTool
    ): BuiltInMcpServer {
        val server = BuiltInMcpServer(gson)

        // Register all tools
        server.registerTool(listThreadsTool)
        server.registerTool(listMessagesTool)
        server.registerTool(sendMessageTool)

        Timber.d("MCP Server initialized with ${3} tools")
        Timber.d("Server URL: ${BuiltInMcpServer.BUILTIN_SERVER_URL}")

        return server
    }
}
