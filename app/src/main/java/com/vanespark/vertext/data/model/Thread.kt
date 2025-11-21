package com.vanespark.vertext.data.model

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
    @PrimaryKey(autoGenerate = false)
    val id: Long,

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

    /** AI-generated category (stored as String, use ThreadCategory enum) */
    val category: String? = null,

    /** Total number of messages in thread */
    @ColumnInfo(name = "message_count")
    val messageCount: Int = 0,

    /** True if this is a group conversation */
    @ColumnInfo(name = "is_group")
    val isGroup: Boolean = false,

    /** Custom name for group conversations */
    @ColumnInfo(name = "group_name")
    val groupName: String? = null,

    /**
     * For group conversations: JSON array of recipient phone numbers
     * Example: ["[\"555-1234\",\"555-5678\",\"555-9012\"]"]
     */
    val recipients: String? = null
) {
    /** Helper to check if thread has unread messages */
    val hasUnread: Boolean
        get() = unreadCount > 0

    /** Get category as enum */
    val categoryEnum: ThreadCategory
        get() = ThreadCategory.fromString(category)

    /** Parse recipients list from JSON */
    fun getRecipientsList(): List<String> {
        if (recipients == null || !isGroup) return listOf(recipient)
        return try {
            // Parse JSON array of recipients
            recipients.trim()
                .removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            listOf(recipient)
        }
    }

    /** Display name for the conversation */
    val displayName: String
        get() = when {
            isGroup && !groupName.isNullOrBlank() -> groupName
            isGroup -> {
                val recipientsList = getRecipientsList()
                when {
                    recipientsList.size <= 3 -> recipientsList.joinToString(", ")
                    else -> "${recipientsList.take(3).joinToString(", ")} +${recipientsList.size - 3}"
                }
            }
            !recipientName.isNullOrBlank() -> recipientName
            else -> recipient
        }
}
