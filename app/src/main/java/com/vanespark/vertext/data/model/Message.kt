package com.vanespark.vertext.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Message entity representing an SMS/MMS message
 * Includes embedding fields for semantic search
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["thread_id"]),
        Index(value = ["address"]),
        Index(value = ["date"]),
        Index(value = ["last_indexed"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Thread::class,
            parentColumns = ["id"],
            childColumns = ["thread_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "thread_id")
    val threadId: Long,

    /** Phone number or address of the message sender/recipient */
    val address: String,

    /** Message body/content */
    val body: String,

    /** Timestamp in milliseconds */
    val date: Long,

    /**
     * Message type:
     * 1 = Inbox (received)
     * 2 = Sent
     * 3 = Draft
     * 4 = Outbox
     * 5 = Failed
     * 6 = Queued
     */
    val type: Int,

    /** True if message has been read */
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,

    /** Subject for MMS messages */
    val subject: String? = null,

    /** Content type for MMS */
    @ColumnInfo(name = "content_type")
    val contentType: String? = null,

    /** Media URIs for MMS attachments (JSON array as string) */
    @ColumnInfo(name = "media_uris")
    val mediaUris: String? = null,

    // === RAG/Embedding fields ===

    /**
     * 384-dimensional embedding vector stored as comma-separated floats
     * Generated using TF-IDF word hashing
     */
    val embedding: String? = null,

    /**
     * Embedding model version:
     * 1 = TF-IDF word hashing
     * 2 = Neural model (future)
     */
    @ColumnInfo(name = "embedding_version")
    val embeddingVersion: Int = 1,

    /**
     * Timestamp when this message was last indexed/embedded
     */
    @ColumnInfo(name = "last_indexed")
    val lastIndexed: Long? = null
) {
    companion object {
        const val TYPE_INBOX = 1
        const val TYPE_SENT = 2
        const val TYPE_DRAFT = 3
        const val TYPE_OUTBOX = 4
        const val TYPE_FAILED = 5
        const val TYPE_QUEUED = 6
    }

    /** Helper to check if message is received */
    val isReceived: Boolean
        get() = type == TYPE_INBOX

    /** Helper to check if message is sent */
    val isSent: Boolean
        get() = type == TYPE_SENT

    /** Helper to check if message has embedding */
    val hasEmbedding: Boolean
        get() = !embedding.isNullOrEmpty()
}
