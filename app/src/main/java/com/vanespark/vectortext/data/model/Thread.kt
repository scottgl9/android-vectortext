package com.vanespark.vectortext.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Thread entity representing a conversation with a contact or group
 */
@Entity(
    tableName = "threads",
    indices = [
        Index(value = ["recipient"]),
        Index(value = ["last_message_date"]),
        Index(value = ["is_pinned"]),
        Index(value = ["is_archived"])
    ]
)
data class Thread(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Phone number or address of the conversation recipient */
    val recipient: String,

    /** Display name of the recipient (if available) */
    @ColumnInfo(name = "recipient_name")
    val recipientName: String? = null,

    /** Preview of the last message in the thread */
    @ColumnInfo(name = "last_message")
    val lastMessage: String? = null,

    /** Timestamp of the last message */
    @ColumnInfo(name = "last_message_date")
    val lastMessageDate: Long = 0,

    /** Number of unread messages in this thread */
    @ColumnInfo(name = "unread_count")
    val unreadCount: Int = 0,

    /** True if thread is pinned to the top */
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    /** True if thread is archived */
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    /** True if thread is muted (no notifications) */
    @ColumnInfo(name = "is_muted")
    val isMuted: Boolean = false,

    /** AI-generated category (Personal, Work, Promotions, etc.) */
    val category: String? = null,

    /** Total number of messages in thread */
    @ColumnInfo(name = "message_count")
    val messageCount: Int = 0
) {
    companion object {
        const val CATEGORY_PERSONAL = "Personal"
        const val CATEGORY_WORK = "Work"
        const val CATEGORY_PROMOTIONS = "Promotions"
        const val CATEGORY_TRANSACTIONS = "Transactions"
        const val CATEGORY_OTHER = "Other"
    }

    /** Helper to check if thread has unread messages */
    val hasUnread: Boolean
        get() = unreadCount > 0
}
