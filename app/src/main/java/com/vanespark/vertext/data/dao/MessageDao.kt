package com.vanespark.vertext.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vanespark.vertext.data.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Message entities
 * Provides queries for CRUD operations and semantic search
 */
@Dao
interface MessageDao {

    // === Basic CRUD Operations ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<Message>): List<Long>

    @Update
    suspend fun update(message: Message)

    @Delete
    suspend fun delete(message: Message)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)

    @Query("DELETE FROM messages WHERE thread_id = :threadId")
    suspend fun deleteByThread(threadId: Long)

    // === Query Operations ===

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getById(messageId: Long): Message?

    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getByIdFlow(messageId: Long): Flow<Message?>

    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY date DESC")
    fun getMessagesByThread(threadId: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY date DESC LIMIT :limit")
    fun getMessagesByThreadLimitFlow(threadId: Long, limit: Int): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY date DESC LIMIT :limit")
    suspend fun getMessagesByThreadLimit(threadId: Long, limit: Int): List<Message>

    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesByThreadPaged(threadId: Long, limit: Int, offset: Int): List<Message>

    @Query("SELECT * FROM messages ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<Message>

    @Query("SELECT * FROM messages WHERE type = :type ORDER BY date DESC")
    fun getMessagesByType(type: Int): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE address = :address ORDER BY date DESC")
    fun getMessagesByAddress(address: String): Flow<List<Message>>

    // === Read/Unread Operations ===

    @Query("UPDATE messages SET is_read = 1 WHERE id = :messageId")
    suspend fun markAsRead(messageId: Long)

    @Query("UPDATE messages SET is_read = 1 WHERE thread_id = :threadId")
    suspend fun markThreadAsRead(threadId: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE is_read = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE thread_id = :threadId AND is_read = 0")
    suspend fun getUnreadCountForThread(threadId: Long): Int

    // === Embedding/RAG Operations ===

    @Query("SELECT * FROM messages WHERE embedding IS NULL OR embedding = '' ORDER BY date DESC LIMIT :limit")
    suspend fun getMessagesNeedingEmbedding(limit: Int): List<Message>

    @Query("SELECT COUNT(*) FROM messages WHERE embedding IS NOT NULL AND embedding != ''")
    suspend fun getEmbeddedMessageCount(): Int

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getTotalMessageCount(): Int

    /**
     * Get messages in batches for similarity search
     * Used for semantic search to avoid loading all embeddings at once
     */
    @Query("SELECT * FROM messages WHERE embedding IS NOT NULL AND embedding != '' ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getEmbeddedMessagesBatch(limit: Int, offset: Int): List<Message>

    @Query("UPDATE messages SET embedding = :embedding, embedding_version = :version, last_indexed = :timestamp WHERE id = :messageId")
    suspend fun updateEmbedding(messageId: Long, embedding: String, version: Int, timestamp: Long)

    /**
     * Get all messages that have embeddings (for semantic search)
     */
    @Query("SELECT * FROM messages WHERE embedding IS NOT NULL AND embedding != '' ORDER BY date DESC")
    suspend fun getMessagesWithEmbeddings(): List<Message>

    /**
     * Get messages with embeddings in paginated batches
     */
    @Query("SELECT * FROM messages WHERE embedding IS NOT NULL AND embedding != '' ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesWithEmbeddingsPaged(limit: Int, offset: Int): List<Message>

    // === Search Operations ===

    @Query("""
        SELECT * FROM messages
        WHERE body LIKE '%' || :query || '%'
        ORDER BY date DESC
        LIMIT :limit
    """)
    suspend fun searchMessages(query: String, limit: Int): List<Message>

    @Query("""
        SELECT * FROM messages
        WHERE thread_id = :threadId AND body LIKE '%' || :query || '%'
        ORDER BY date DESC
        LIMIT :limit
    """)
    suspend fun searchMessagesInThread(threadId: Long, query: String, limit: Int): List<Message>

    // === Statistics ===

    @Query("SELECT COUNT(*) FROM messages WHERE thread_id = :threadId")
    suspend fun getMessageCountForThread(threadId: Long): Int

    @Query("SELECT MAX(date) FROM messages WHERE thread_id = :threadId")
    suspend fun getLatestMessageDateForThread(threadId: Long): Long?

    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestMessageForThread(threadId: Long): Message?

    // === Cleanup Operations ===

    @Query("DELETE FROM messages WHERE date < :timestamp")
    suspend fun deleteMessagesOlderThan(timestamp: Long): Int

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    /**
     * Get all messages (for corpus building)
     */
    @Query("SELECT * FROM messages ORDER BY date DESC")
    suspend fun getAllMessages(): List<Message>

    // === Reaction Operations ===

    /**
     * Update reactions for a message
     */
    @Query("UPDATE messages SET reactions = :reactions WHERE id = :messageId")
    suspend fun updateReactions(messageId: Long, reactions: String?)

    /**
     * Get the most recent message in a thread before a given timestamp
     * Used for detecting which message a reaction applies to
     */
    @Query("SELECT * FROM messages WHERE thread_id = :threadId AND date < :beforeTimestamp AND body != '' ORDER BY date DESC LIMIT 1")
    suspend fun getLastMessageBeforeTimestamp(threadId: Long, beforeTimestamp: Long): Message?

    /**
     * Get a message by its exact timestamp in a thread
     * Used for finding the target message of a reaction
     */
    @Query("SELECT * FROM messages WHERE thread_id = :threadId AND date = :timestamp LIMIT 1")
    suspend fun getMessageByTimestamp(threadId: Long, timestamp: Long): Message?
}
