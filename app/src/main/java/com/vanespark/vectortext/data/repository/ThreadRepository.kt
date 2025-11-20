package com.vanespark.vectortext.data.repository

import com.vanespark.vectortext.data.dao.ThreadDao
import com.vanespark.vectortext.data.model.Thread
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Thread operations
 * Provides a clean API for accessing conversation thread data
 */
@Singleton
class ThreadRepository @Inject constructor(
    private val threadDao: ThreadDao
) {

    // === Basic Operations ===

    suspend fun insertThread(thread: Thread): Long {
        return threadDao.insert(thread)
    }

    suspend fun insertThreads(threads: List<Thread>): List<Long> {
        return threadDao.insertAll(threads)
    }

    suspend fun updateThread(thread: Thread) {
        threadDao.update(thread)
    }

    suspend fun deleteThread(thread: Thread) {
        threadDao.delete(thread)
    }

    suspend fun deleteThreadById(threadId: Long) {
        threadDao.deleteById(threadId)
    }

    // === Query Operations ===

    suspend fun getThreadById(threadId: Long): Thread? {
        return threadDao.getById(threadId)
    }

    fun getThreadByIdFlow(threadId: Long): Flow<Thread?> {
        return threadDao.getByIdFlow(threadId)
    }

    suspend fun getThreadByRecipient(recipient: String): Thread? {
        return threadDao.getByRecipient(recipient)
    }

    fun getAllThreads(): Flow<List<Thread>> {
        return threadDao.getAllThreads()
    }

    suspend fun getThreadsLimit(limit: Int): List<Thread> {
        return threadDao.getThreadsLimit(limit)
    }

    fun getAllThreadsIncludingArchived(): Flow<List<Thread>> {
        return threadDao.getAllThreadsIncludingArchived()
    }

    // === Archived Threads ===

    fun getArchivedThreads(): Flow<List<Thread>> {
        return threadDao.getArchivedThreads()
    }

    suspend fun archiveThread(threadId: Long) {
        threadDao.setArchived(threadId, true)
    }

    suspend fun unarchiveThread(threadId: Long) {
        threadDao.setArchived(threadId, false)
    }

    // === Pinned Threads ===

    fun getPinnedThreads(): Flow<List<Thread>> {
        return threadDao.getPinnedThreads()
    }

    suspend fun pinThread(threadId: Long) {
        threadDao.setPinned(threadId, true)
    }

    suspend fun unpinThread(threadId: Long) {
        threadDao.setPinned(threadId, false)
    }

    // === Muted Threads ===

    suspend fun muteThread(threadId: Long) {
        threadDao.setMuted(threadId, true)
    }

    suspend fun unmuteThread(threadId: Long) {
        threadDao.setMuted(threadId, false)
    }

    // === Unread Operations ===

    fun getThreadsWithUnread(): Flow<List<Thread>> {
        return threadDao.getThreadsWithUnread()
    }

    suspend fun updateUnreadCount(threadId: Long, count: Int) {
        threadDao.updateUnreadCount(threadId, count)
    }

    suspend fun clearUnreadCount(threadId: Long) {
        threadDao.clearUnreadCount(threadId)
    }

    fun getTotalUnreadCount(): Flow<Int?> {
        return threadDao.getTotalUnreadCount()
    }

    // === Thread Metadata Updates ===

    suspend fun updateLastMessage(threadId: Long, message: String, date: Long) {
        threadDao.updateLastMessage(threadId, message, date)
    }

    suspend fun updateMessageCount(threadId: Long, count: Int) {
        threadDao.updateMessageCount(threadId, count)
    }

    suspend fun updateRecipientName(threadId: Long, name: String) {
        threadDao.updateRecipientName(threadId, name)
    }

    suspend fun updateCategory(threadId: Long, category: String) {
        threadDao.updateCategory(threadId, category)
    }

    // === Category Operations ===

    fun getThreadsByCategory(category: String): Flow<List<Thread>> {
        return threadDao.getThreadsByCategory(category)
    }

    suspend fun getAllCategories(): List<String> {
        return threadDao.getAllCategories()
    }

    // === Search Operations ===

    suspend fun searchThreads(query: String, limit: Int = 50): List<Thread> {
        return threadDao.searchThreads(query, limit)
    }

    // === Statistics ===

    suspend fun getActiveThreadCount(): Int {
        return threadDao.getActiveThreadCount()
    }

    suspend fun getTotalThreadCount(): Int {
        return threadDao.getTotalThreadCount()
    }

    // === Cleanup Operations ===

    suspend fun deleteEmptyThreadsOlderThan(timestamp: Long): Int {
        return threadDao.deleteEmptyThreadsOlderThan(timestamp)
    }

    suspend fun deleteAllThreads() {
        threadDao.deleteAll()
    }

    // === Convenience Methods ===

    /**
     * Get or create a thread for a given recipient
     */
    suspend fun getOrCreateThread(recipient: String, recipientName: String? = null): Thread {
        val existing = getThreadByRecipient(recipient)
        if (existing != null) {
            return existing
        }

        val newThread = Thread(
            recipient = recipient,
            recipientName = recipientName,
            lastMessageDate = System.currentTimeMillis()
        )
        val threadId = insertThread(newThread)
        return newThread.copy(id = threadId)
    }
}
