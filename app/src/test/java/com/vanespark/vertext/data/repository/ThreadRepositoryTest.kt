package com.vanespark.vertext.data.repository

import com.vanespark.vertext.data.dao.ThreadDao
import com.vanespark.vertext.data.model.Thread
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for ThreadRepository
 * Tests all CRUD operations, queries, and convenience methods
 */
class ThreadRepositoryTest {

    private lateinit var threadRepository: ThreadRepository
    private lateinit var threadDao: ThreadDao

    @Before
    fun setup() {
        threadDao = mock()
        threadRepository = ThreadRepository(threadDao)
    }

    // ========== Basic Operations Tests ==========

    @Test
    fun `insertThread should delegate to DAO and return ID`() = runTest {
        // Given
        val thread = createTestThread(id = 0)
        val expectedId = 123L

        whenever(threadDao.insert(thread))
            .thenReturn(expectedId)

        // When
        val result = threadRepository.insertThread(thread)

        // Then
        assertEquals(expectedId, result)
        verify(threadDao).insert(thread)
    }

    @Test
    fun `insertThreads should delegate to DAO and return IDs`() = runTest {
        // Given
        val threads = listOf(
            createTestThread(id = 0),
            createTestThread(id = 0)
        )
        val expectedIds = listOf(1L, 2L)

        whenever(threadDao.insertAll(threads))
            .thenReturn(expectedIds)

        // When
        val result = threadRepository.insertThreads(threads)

        // Then
        assertEquals(expectedIds, result)
        verify(threadDao).insertAll(threads)
    }

    @Test
    fun `updateThread should delegate to DAO`() = runTest {
        // Given
        val thread = createTestThread(id = 1)

        whenever(threadDao.update(thread))
            .thenReturn(Unit)

        // When
        threadRepository.updateThread(thread)

        // Then
        verify(threadDao).update(thread)
    }

    @Test
    fun `deleteThread should delegate to DAO`() = runTest {
        // Given
        val thread = createTestThread(id = 1)

        whenever(threadDao.delete(thread))
            .thenReturn(Unit)

        // When
        threadRepository.deleteThread(thread)

        // Then
        verify(threadDao).delete(thread)
    }

    @Test
    fun `deleteThread by ID should delegate to DAO`() = runTest {
        // Given
        val threadId = 1L

        whenever(threadDao.deleteById(threadId))
            .thenReturn(Unit)

        // When
        threadRepository.deleteThread(threadId)

        // Then
        verify(threadDao).deleteById(threadId)
    }

    // ========== Query Operations Tests ==========

    @Test
    fun `getThreadById should return thread from DAO`() = runTest {
        // Given
        val threadId = 1L
        val expectedThread = createTestThread(id = threadId)

        whenever(threadDao.getById(threadId))
            .thenReturn(expectedThread)

        // When
        val result = threadRepository.getThreadById(threadId)

        // Then
        assertEquals(expectedThread, result)
        verify(threadDao).getById(threadId)
    }

    @Test
    fun `getThreadById should return null when thread not found`() = runTest {
        // Given
        val threadId = 999L

        whenever(threadDao.getById(threadId))
            .thenReturn(null)

        // When
        val result = threadRepository.getThreadById(threadId)

        // Then
        assertNull(result)
        verify(threadDao).getById(threadId)
    }

    @Test
    fun `getThreadByIdFlow should return Flow from DAO`() = runTest {
        // Given
        val threadId = 1L
        val expectedThread = createTestThread(id = threadId)

        whenever(threadDao.getByIdFlow(threadId))
            .thenReturn(flowOf(expectedThread))

        // When
        val result = threadRepository.getThreadByIdFlow(threadId).first()

        // Then
        assertEquals(expectedThread, result)
        verify(threadDao).getByIdFlow(threadId)
    }

    @Test
    fun `getThreadByRecipient should return thread from DAO`() = runTest {
        // Given
        val recipient = "+1234567890"
        val expectedThread = createTestThread(recipient = recipient)

        whenever(threadDao.getByRecipient(recipient))
            .thenReturn(expectedThread)

        // When
        val result = threadRepository.getThreadByRecipient(recipient)

        // Then
        assertEquals(expectedThread, result)
        verify(threadDao).getByRecipient(recipient)
    }

    @Test
    fun `getThreadsByRecipient should return list from DAO`() = runTest {
        // Given
        val recipient = "+1234567890"
        val expectedThreads = listOf(
            createTestThread(id = 1, recipient = recipient),
            createTestThread(id = 2, recipient = recipient)
        )

        whenever(threadDao.getAllByRecipient(recipient))
            .thenReturn(expectedThreads)

        // When
        val result = threadRepository.getThreadsByRecipient(recipient)

        // Then
        assertEquals(expectedThreads, result)
        verify(threadDao).getAllByRecipient(recipient)
    }

    @Test
    fun `getAllThreads should return Flow from DAO`() = runTest {
        // Given
        val expectedThreads = listOf(
            createTestThread(id = 1),
            createTestThread(id = 2)
        )

        whenever(threadDao.getAllThreads())
            .thenReturn(flowOf(expectedThreads))

        // When
        val result = threadRepository.getAllThreads().first()

        // Then
        assertEquals(expectedThreads, result)
        verify(threadDao).getAllThreads()
    }

    @Test
    fun `getThreadsLimit should return limited list from DAO`() = runTest {
        // Given
        val limit = 10
        val expectedThreads = (1..10).map { createTestThread(id = it.toLong()) }

        whenever(threadDao.getThreadsLimit(limit))
            .thenReturn(expectedThreads)

        // When
        val result = threadRepository.getThreadsLimit(limit)

        // Then
        assertEquals(expectedThreads, result)
        assertEquals(limit, result.size)
        verify(threadDao).getThreadsLimit(limit)
    }

    // ========== Archived Threads Tests ==========

    @Test
    fun `getArchivedThreads should return Flow from DAO`() = runTest {
        // Given
        val expectedThreads = listOf(
            createTestThread(id = 1, isArchived = true),
            createTestThread(id = 2, isArchived = true)
        )

        whenever(threadDao.getArchivedThreads())
            .thenReturn(flowOf(expectedThreads))

        // When
        val result = threadRepository.getArchivedThreads().first()

        // Then
        assertEquals(expectedThreads, result)
        assertEquals(2, result.size)
        verify(threadDao).getArchivedThreads()
    }

    @Test
    fun `archiveThread should set archived to true`() = runTest {
        // Given
        val threadId = 1L

        whenever(threadDao.setArchived(threadId, true))
            .thenReturn(Unit)

        // When
        threadRepository.archiveThread(threadId)

        // Then
        verify(threadDao).setArchived(threadId, true)
    }

    @Test
    fun `unarchiveThread should set archived to false`() = runTest {
        // Given
        val threadId = 1L

        whenever(threadDao.setArchived(threadId, false))
            .thenReturn(Unit)

        // When
        threadRepository.unarchiveThread(threadId)

        // Then
        verify(threadDao).setArchived(threadId, false)
    }

    // ========== Pinned Threads Tests ==========

    @Test
    fun `getPinnedThreads should return Flow from DAO`() = runTest {
        // Given
        val expectedThreads = listOf(
            createTestThread(id = 1, isPinned = true),
            createTestThread(id = 2, isPinned = true)
        )

        whenever(threadDao.getPinnedThreads())
            .thenReturn(flowOf(expectedThreads))

        // When
        val result = threadRepository.getPinnedThreads().first()

        // Then
        assertEquals(expectedThreads, result)
        verify(threadDao).getPinnedThreads()
    }

    @Test
    fun `pinThread should set pinned to true`() = runTest {
        // Given
        val threadId = 1L

        whenever(threadDao.setPinned(threadId, true))
            .thenReturn(Unit)

        // When
        threadRepository.pinThread(threadId)

        // Then
        verify(threadDao).setPinned(threadId, true)
    }

    @Test
    fun `unpinThread should set pinned to false`() = runTest {
        // Given
        val threadId = 1L

        whenever(threadDao.setPinned(threadId, false))
            .thenReturn(Unit)

        // When
        threadRepository.unpinThread(threadId)

        // Then
        verify(threadDao).setPinned(threadId, false)
    }

    // ========== Muted Threads Tests ==========

    @Test
    fun `muteThread should set muted to true`() = runTest {
        // Given
        val threadId = 1L

        whenever(threadDao.setMuted(threadId, true))
            .thenReturn(Unit)

        // When
        threadRepository.muteThread(threadId)

        // Then
        verify(threadDao).setMuted(threadId, true)
    }

    @Test
    fun `unmuteThread should set muted to false`() = runTest {
        // Given
        val threadId = 1L

        whenever(threadDao.setMuted(threadId, false))
            .thenReturn(Unit)

        // When
        threadRepository.unmuteThread(threadId)

        // Then
        verify(threadDao).setMuted(threadId, false)
    }

    // ========== Unread Operations Tests ==========

    @Test
    fun `getThreadsWithUnread should return Flow from DAO`() = runTest {
        // Given
        val expectedThreads = listOf(
            createTestThread(id = 1, unreadCount = 3),
            createTestThread(id = 2, unreadCount = 5)
        )

        whenever(threadDao.getThreadsWithUnread())
            .thenReturn(flowOf(expectedThreads))

        // When
        val result = threadRepository.getThreadsWithUnread().first()

        // Then
        assertEquals(expectedThreads, result)
        verify(threadDao).getThreadsWithUnread()
    }

    @Test
    fun `updateUnreadCount should delegate to DAO`() = runTest {
        // Given
        val threadId = 1L
        val count = 5

        whenever(threadDao.updateUnreadCount(threadId, count))
            .thenReturn(Unit)

        // When
        threadRepository.updateUnreadCount(threadId, count)

        // Then
        verify(threadDao).updateUnreadCount(threadId, count)
    }

    @Test
    fun `clearUnreadCount should set count to 0`() = runTest {
        // Given
        val threadId = 1L

        whenever(threadDao.clearUnreadCount(threadId))
            .thenReturn(Unit)

        // When
        threadRepository.clearUnreadCount(threadId)

        // Then
        verify(threadDao).clearUnreadCount(threadId)
    }

    @Test
    fun `getTotalUnreadCount should return Flow from DAO`() = runTest {
        // Given
        val expectedCount = 15

        whenever(threadDao.getTotalUnreadCount())
            .thenReturn(flowOf(expectedCount))

        // When
        val result = threadRepository.getTotalUnreadCount().first()

        // Then
        assertEquals(expectedCount, result)
        verify(threadDao).getTotalUnreadCount()
    }

    // ========== Thread Metadata Updates Tests ==========

    @Test
    fun `updateLastMessage should delegate to DAO`() = runTest {
        // Given
        val threadId = 1L
        val message = "New message"
        val date = System.currentTimeMillis()

        whenever(threadDao.updateLastMessage(threadId, message, date))
            .thenReturn(Unit)

        // When
        threadRepository.updateLastMessage(threadId, message, date)

        // Then
        verify(threadDao).updateLastMessage(threadId, message, date)
    }

    @Test
    fun `updateMessageCount should delegate to DAO`() = runTest {
        // Given
        val threadId = 1L
        val count = 42

        whenever(threadDao.updateMessageCount(threadId, count))
            .thenReturn(Unit)

        // When
        threadRepository.updateMessageCount(threadId, count)

        // Then
        verify(threadDao).updateMessageCount(threadId, count)
    }

    @Test
    fun `updateRecipientName should delegate to DAO`() = runTest {
        // Given
        val threadId = 1L
        val name = "John Doe"

        whenever(threadDao.updateRecipientName(threadId, name))
            .thenReturn(Unit)

        // When
        threadRepository.updateRecipientName(threadId, name)

        // Then
        verify(threadDao).updateRecipientName(threadId, name)
    }

    @Test
    fun `updateCategory should delegate to DAO`() = runTest {
        // Given
        val threadId = 1L
        val category = "Work"

        whenever(threadDao.updateCategory(threadId, category))
            .thenReturn(Unit)

        // When
        threadRepository.updateCategory(threadId, category)

        // Then
        verify(threadDao).updateCategory(threadId, category)
    }

    // ========== Category Operations Tests ==========

    @Test
    fun `getThreadsByCategory should return Flow from DAO`() = runTest {
        // Given
        val category = "Work"
        val expectedThreads = listOf(
            createTestThread(id = 1, category = category),
            createTestThread(id = 2, category = category)
        )

        whenever(threadDao.getThreadsByCategory(category))
            .thenReturn(flowOf(expectedThreads))

        // When
        val result = threadRepository.getThreadsByCategory(category).first()

        // Then
        assertEquals(expectedThreads, result)
        verify(threadDao).getThreadsByCategory(category)
    }

    @Test
    fun `getAllCategories should return list from DAO`() = runTest {
        // Given
        val expectedCategories = listOf("Personal", "Work", "Finance")

        whenever(threadDao.getAllCategories())
            .thenReturn(expectedCategories)

        // When
        val result = threadRepository.getAllCategories()

        // Then
        assertEquals(expectedCategories, result)
        verify(threadDao).getAllCategories()
    }

    // ========== Search Operations Tests ==========

    @Test
    fun `searchThreads should return results from DAO`() = runTest {
        // Given
        val query = "John"
        val limit = 20
        val expectedThreads = listOf(
            createTestThread(id = 1, recipientName = "John Doe"),
            createTestThread(id = 2, recipientName = "Johnny Smith")
        )

        whenever(threadDao.searchThreads(query, limit))
            .thenReturn(expectedThreads)

        // When
        val result = threadRepository.searchThreads(query, limit)

        // Then
        assertEquals(expectedThreads, result)
        verify(threadDao).searchThreads(query, limit)
    }

    @Test
    fun `searchThreads should use default limit when not specified`() = runTest {
        // Given
        val query = "test"
        val defaultLimit = 50

        whenever(threadDao.searchThreads(query, defaultLimit))
            .thenReturn(emptyList())

        // When
        threadRepository.searchThreads(query)

        // Then
        verify(threadDao).searchThreads(query, defaultLimit)
    }

    // ========== Statistics Tests ==========

    @Test
    fun `getActiveThreadCount should return count from DAO`() = runTest {
        // Given
        val expectedCount = 25

        whenever(threadDao.getActiveThreadCount())
            .thenReturn(expectedCount)

        // When
        val result = threadRepository.getActiveThreadCount()

        // Then
        assertEquals(expectedCount, result)
        verify(threadDao).getActiveThreadCount()
    }

    @Test
    fun `getTotalThreadCount should return count from DAO`() = runTest {
        // Given
        val expectedCount = 100

        whenever(threadDao.getTotalThreadCount())
            .thenReturn(expectedCount)

        // When
        val result = threadRepository.getTotalThreadCount()

        // Then
        assertEquals(expectedCount, result)
        verify(threadDao).getTotalThreadCount()
    }

    // ========== Cleanup Operations Tests ==========

    @Test
    fun `deleteEmptyThreadsOlderThan should return deleted count`() = runTest {
        // Given
        val timestamp = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) // 30 days ago
        val expectedDeleted = 5

        whenever(threadDao.deleteEmptyThreadsOlderThan(timestamp))
            .thenReturn(expectedDeleted)

        // When
        val result = threadRepository.deleteEmptyThreadsOlderThan(timestamp)

        // Then
        assertEquals(expectedDeleted, result)
        verify(threadDao).deleteEmptyThreadsOlderThan(timestamp)
    }

    @Test
    fun `deleteAllThreads should delegate to DAO`() = runTest {
        // Given
        whenever(threadDao.deleteAll())
            .thenReturn(Unit)

        // When
        threadRepository.deleteAllThreads()

        // Then
        verify(threadDao).deleteAll()
    }

    // ========== Convenience Methods Tests ==========

    @Test
    fun `getOrCreateThread should return existing thread if found`() = runTest {
        // Given
        val recipient = "+1234567890"
        val existingThread = createTestThread(id = 123, recipient = recipient)

        whenever(threadDao.getByRecipient(recipient))
            .thenReturn(existingThread)

        // When
        val result = threadRepository.getOrCreateThread(recipient)

        // Then
        assertEquals(existingThread, result)
        verify(threadDao).getByRecipient(recipient)
        verify(threadDao, never()).insert(any())
    }

    @Test
    fun `getOrCreateThread should create new thread if not found`() = runTest {
        // Given
        val recipient = "+1234567890"
        val recipientName = "John Doe"

        whenever(threadDao.getByRecipient(recipient))
            .thenReturn(null)
        whenever(threadDao.insert(any()))
            .thenReturn(1L)

        // When
        val result = threadRepository.getOrCreateThread(recipient, recipientName)

        // Then
        assertNotNull(result)
        assertEquals(recipient, result.recipient)
        assertEquals(recipientName, result.recipientName)
        verify(threadDao).getByRecipient(recipient)
        verify(threadDao).insert(argThat {
            this.recipient == recipient && this.recipientName == recipientName
        })
    }

    @Test
    fun `getOrCreateThread should generate unique thread ID`() = runTest {
        // Given
        val recipient1 = "+1234567890"
        val recipient2 = "+0987654321"

        whenever(threadDao.getByRecipient(any()))
            .thenReturn(null)
        whenever(threadDao.insert(any()))
            .thenReturn(1L)

        // When
        val thread1 = threadRepository.getOrCreateThread(recipient1)
        val thread2 = threadRepository.getOrCreateThread(recipient2)

        // Then - IDs should be different
        assert(thread1.id != thread2.id)
    }

    @Test
    fun `getOrCreateThread should work without recipientName`() = runTest {
        // Given
        val recipient = "+1234567890"

        whenever(threadDao.getByRecipient(recipient))
            .thenReturn(null)
        whenever(threadDao.insert(any()))
            .thenReturn(1L)

        // When
        val result = threadRepository.getOrCreateThread(recipient)

        // Then
        assertNotNull(result)
        assertEquals(recipient, result.recipient)
        assertNull(result.recipientName)
        verify(threadDao).insert(argThat {
            this.recipient == recipient && this.recipientName == null
        })
    }

    // ========== Helper Methods ==========

    private fun createTestThread(
        id: Long = 1,
        recipient: String = "+1234567890",
        recipientName: String? = null,
        lastMessage: String = "Test message",
        lastMessageDate: Long = System.currentTimeMillis(),
        unreadCount: Int = 0,
        messageCount: Int = 1,
        isPinned: Boolean = false,
        isArchived: Boolean = false,
        category: String = "Uncategorized"
    ): Thread {
        return Thread(
            id = id,
            recipient = recipient,
            recipientName = recipientName,
            lastMessage = lastMessage,
            lastMessageDate = lastMessageDate,
            unreadCount = unreadCount,
            messageCount = messageCount,
            isPinned = isPinned,
            isArchived = isArchived,
            category = category
        )
    }
}
