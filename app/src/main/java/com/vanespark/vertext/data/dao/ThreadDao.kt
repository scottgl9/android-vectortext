package com.vanespark.vertext.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vanespark.vertext.data.model.Thread
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Thread entities
 * Provides queries for managing conversation threads
 */
@Dao
interface ThreadDao {

    // === Basic CRUD Operations ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(thread: Thread): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(threads: List<Thread>): List<Long>

    @Update
    suspend fun update(thread: Thread)

    @Delete
    suspend fun delete(thread: Thread)

    @Query("DELETE FROM threads WHERE id = :threadId")
    suspend fun deleteById(threadId: Long)

    // === Query Operations ===

    @Query("SELECT * FROM threads WHERE id = :threadId")
    suspend fun getById(threadId: Long): Thread?

    @Query("SELECT * FROM threads WHERE id = :threadId")
    fun getByIdFlow(threadId: Long): Flow<Thread?>

    @Query("SELECT * FROM threads WHERE recipient = :recipient LIMIT 1")
    suspend fun getByRecipient(recipient: String): Thread?

    @Query("SELECT * FROM threads WHERE is_archived = 0 ORDER BY is_pinned DESC, last_message_date DESC")
    fun getAllThreads(): Flow<List<Thread>>

    @Query("SELECT * FROM threads WHERE is_archived = 0 ORDER BY is_pinned DESC, last_message_date DESC LIMIT :limit")
    suspend fun getThreadsLimit(limit: Int): List<Thread>

    @Query("SELECT * FROM threads ORDER BY is_pinned DESC, last_message_date DESC")
    fun getAllThreadsIncludingArchived(): Flow<List<Thread>>

    // === Archived Threads ===

    @Query("SELECT * FROM threads WHERE is_archived = 1 ORDER BY last_message_date DESC")
    fun getArchivedThreads(): Flow<List<Thread>>

    @Query("UPDATE threads SET is_archived = :isArchived WHERE id = :threadId")
    suspend fun setArchived(threadId: Long, isArchived: Boolean)

    // === Pinned Threads ===

    @Query("SELECT * FROM threads WHERE is_pinned = 1 ORDER BY last_message_date DESC")
    fun getPinnedThreads(): Flow<List<Thread>>

    @Query("UPDATE threads SET is_pinned = :isPinned WHERE id = :threadId")
    suspend fun setPinned(threadId: Long, isPinned: Boolean)

    // === Muted Threads ===

    @Query("UPDATE threads SET is_muted = :isMuted WHERE id = :threadId")
    suspend fun setMuted(threadId: Long, isMuted: Boolean)

    // === Unread Operations ===

    @Query("SELECT * FROM threads WHERE unread_count > 0 ORDER BY last_message_date DESC")
    fun getThreadsWithUnread(): Flow<List<Thread>>

    @Query("UPDATE threads SET unread_count = :count WHERE id = :threadId")
    suspend fun updateUnreadCount(threadId: Long, count: Int)

    @Query("UPDATE threads SET unread_count = 0 WHERE id = :threadId")
    suspend fun clearUnreadCount(threadId: Long)

    @Query("SELECT SUM(unread_count) FROM threads WHERE is_archived = 0")
    fun getTotalUnreadCount(): Flow<Int?>

    // === Thread Metadata Updates ===

    @Query("""
        UPDATE threads
        SET last_message = :message,
            last_message_date = :date,
            message_count = message_count + 1
        WHERE id = :threadId
    """)
    suspend fun updateLastMessage(threadId: Long, message: String, date: Long)

    @Query("UPDATE threads SET message_count = :count WHERE id = :threadId")
    suspend fun updateMessageCount(threadId: Long, count: Int)

    @Query("UPDATE threads SET recipient_name = :name WHERE id = :threadId")
    suspend fun updateRecipientName(threadId: Long, name: String)

    @Query("UPDATE threads SET category = :category WHERE id = :threadId")
    suspend fun updateCategory(threadId: Long, category: String)

    // === Category Operations ===

    @Query("SELECT * FROM threads WHERE category = :category ORDER BY last_message_date DESC")
    fun getThreadsByCategory(category: String): Flow<List<Thread>>

    @Query("SELECT DISTINCT category FROM threads WHERE category IS NOT NULL")
    suspend fun getAllCategories(): List<String>

    // === Search Operations ===

    @Query("""
        SELECT * FROM threads
        WHERE recipient LIKE '%' || :query || '%'
           OR recipient_name LIKE '%' || :query || '%'
           OR last_message LIKE '%' || :query || '%'
        ORDER BY last_message_date DESC
        LIMIT :limit
    """)
    suspend fun searchThreads(query: String, limit: Int): List<Thread>

    // === Statistics ===

    @Query("SELECT COUNT(*) FROM threads WHERE is_archived = 0")
    suspend fun getActiveThreadCount(): Int

    @Query("SELECT COUNT(*) FROM threads")
    suspend fun getTotalThreadCount(): Int

    // === Cleanup Operations ===

    @Query("DELETE FROM threads WHERE message_count = 0 AND last_message_date < :timestamp")
    suspend fun deleteEmptyThreadsOlderThan(timestamp: Long): Int

    @Query("DELETE FROM threads")
    suspend fun deleteAll()
}
