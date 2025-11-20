package com.vanespark.vectortext.ui.conversations

import com.vanespark.vectortext.data.model.Thread

/**
 * UI state for the conversation list screen
 */
data class ConversationListUiState(
    val conversations: List<ConversationUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0,
    val selectedConversations: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val searchQuery: String = ""
)

/**
 * UI model for a conversation item
 */
data class ConversationUiItem(
    val threadId: Long,
    val recipientName: String,
    val recipientAddress: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isMuted: Boolean,
    val avatarUri: String?,
    val category: String?
) {
    companion object {
        fun fromThread(thread: Thread): ConversationUiItem {
            return ConversationUiItem(
                threadId = thread.id,
                recipientName = thread.recipientName ?: thread.recipient,
                recipientAddress = thread.recipient,
                lastMessage = thread.lastMessage ?: "",
                timestamp = thread.lastMessageDate,
                unreadCount = thread.unreadCount,
                isPinned = thread.isPinned,
                isArchived = thread.isArchived,
                isMuted = thread.isMuted,
                avatarUri = null, // Will be populated from contacts
                category = thread.category
            )
        }
    }

    val hasUnread: Boolean
        get() = unreadCount > 0

    val displayName: String
        get() = recipientName.ifEmpty { recipientAddress }
}
