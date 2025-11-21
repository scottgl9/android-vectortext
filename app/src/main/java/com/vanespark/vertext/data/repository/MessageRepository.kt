package com.vanespark.vertext.data.repository

import com.vanespark.vertext.data.dao.MessageDao
import com.vanespark.vertext.data.model.Message
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Message operations
 * Provides a clean API for accessing message data
 */
@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {

    // === Basic Operations ===

    suspend fun insertMessage(message: Message): Long {
        return messageDao.insert(message)
    }

    suspend fun insertMessages(messages: List<Message>): List<Long> {
        return messageDao.insertAll(messages)
    }

    suspend fun updateMessage(message: Message) {
        messageDao.update(message)
    }

    suspend fun deleteMessage(message: Message) {
        messageDao.delete(message)
    }

    suspend fun deleteMessageById(messageId: Long) {
        messageDao.deleteById(messageId)
    }

    suspend fun deleteMessagesInThread(threadId: Long) {
        messageDao.deleteByThread(threadId)
    }

    // === Query Operations ===

    suspend fun getMessageById(messageId: Long): Message? {
        return messageDao.getById(messageId)
    }

    fun getMessageByIdFlow(messageId: Long): Flow<Message?> {
        return messageDao.getByIdFlow(messageId)
    }

    fun getMessagesForThread(threadId: Long): Flow<List<Message>> {
        return messageDao.getMessagesByThread(threadId)
    }

    fun getMessagesForThreadLimit(threadId: Long, limit: Int): Flow<List<Message>> {
        return messageDao.getMessagesByThreadLimitFlow(threadId, limit)
    }

    suspend fun getMessagesForThreadLimitSnapshot(threadId: Long, limit: Int): List<Message> {
        return messageDao.getMessagesByThreadLimit(threadId, limit)
    }

    suspend fun getMessagesForThreadPaged(
        threadId: Long,
        limit: Int,
        offset: Int
    ): List<Message> {
        return messageDao.getMessagesByThreadPaged(threadId, limit, offset)
    }

    suspend fun getRecentMessages(limit: Int): List<Message> {
        return messageDao.getRecentMessages(limit)
    }

    fun getMessagesByType(type: Int): Flow<List<Message>> {
        return messageDao.getMessagesByType(type)
    }

    fun getMessagesByAddress(address: String): Flow<List<Message>> {
        return messageDao.getMessagesByAddress(address)
    }

    // === Read/Unread Operations ===

    suspend fun markMessageAsRead(messageId: Long) {
        messageDao.markAsRead(messageId)
    }

    suspend fun markThreadAsRead(threadId: Long) {
        messageDao.markThreadAsRead(threadId)
    }

    fun getUnreadCount(): Flow<Int> {
        return messageDao.getUnreadCount()
    }

    suspend fun getUnreadCountForThread(threadId: Long): Int {
        return messageDao.getUnreadCountForThread(threadId)
    }

    // === Embedding/RAG Operations ===

    suspend fun getMessagesNeedingEmbedding(limit: Int): List<Message> {
        return messageDao.getMessagesNeedingEmbedding(limit)
    }

    suspend fun getEmbeddedMessageCount(): Int {
        return messageDao.getEmbeddedMessageCount()
    }

    suspend fun getTotalMessageCount(): Int {
        return messageDao.getTotalMessageCount()
    }

    suspend fun getEmbeddedMessagesBatch(limit: Int, offset: Int): List<Message> {
        return messageDao.getEmbeddedMessagesBatch(limit, offset)
    }

    suspend fun updateEmbedding(
        messageId: Long,
        embedding: String,
        version: Int = 1,
        timestamp: Long = System.currentTimeMillis()
    ) {
        messageDao.updateEmbedding(messageId, embedding, version, timestamp)
    }

    suspend fun getMessagesWithEmbeddings(): List<Message> {
        return messageDao.getMessagesWithEmbeddings()
    }

    suspend fun getMessagesWithEmbeddingsPaged(limit: Int, offset: Int): List<Message> {
        return messageDao.getMessagesWithEmbeddingsPaged(limit, offset)
    }

    suspend fun getAllMessagesSnapshot(): List<Message> {
        return messageDao.getAllMessages()
    }

    // === Search Operations ===

    suspend fun searchMessages(query: String, limit: Int = 50): List<Message> {
        return messageDao.searchMessages(query, limit)
    }

    suspend fun searchMessagesInThread(
        threadId: Long,
        query: String,
        limit: Int = 50
    ): List<Message> {
        return messageDao.searchMessagesInThread(threadId, query, limit)
    }

    // === Statistics ===

    suspend fun getMessageCountForThread(threadId: Long): Int {
        return messageDao.getMessageCountForThread(threadId)
    }

    suspend fun getLatestMessageDateForThread(threadId: Long): Long? {
        return messageDao.getLatestMessageDateForThread(threadId)
    }

    suspend fun getLatestMessageForThread(threadId: Long): Message? {
        return messageDao.getLatestMessageForThread(threadId)
    }

    // === Cleanup Operations ===

    suspend fun deleteMessagesOlderThan(timestamp: Long): Int {
        return messageDao.deleteMessagesOlderThan(timestamp)
    }

    suspend fun deleteAllMessages() {
        messageDao.deleteAll()
    }
}
