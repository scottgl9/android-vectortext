package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for generating thread summaries
 * Uses the same logic as GetThreadSummaryTool but returns domain models
 */
@Singleton
class ThreadSummaryService @Inject constructor(
    private val messageRepository: MessageRepository,
    private val threadRepository: ThreadRepository
) {

    /**
     * Generate a summary for a thread
     */
    suspend fun generateSummary(
        threadId: Long,
        maxMessages: Int = 1000,
        includeExcerpts: Boolean = true
    ): Result<ThreadSummary> {
        return try {
            Timber.d("Generating summary for thread $threadId")

            // Get thread metadata
            val thread = threadRepository.getThreadById(threadId)
                ?: return Result.failure(Exception("Thread not found: $threadId"))

            // Get messages for analysis
            val messages = messageRepository.getMessagesForThreadLimit(threadId, maxMessages)

            if (messages.isEmpty()) {
                return Result.success(
                    ThreadSummary(
                        threadId = threadId,
                        recipient = thread.recipientName ?: thread.recipient,
                        messageCount = 0,
                        sentCount = 0,
                        receivedCount = 0,
                        firstMessageDate = null,
                        lastMessageDate = null,
                        timeSpanDays = 0,
                        averageMessageLength = 0,
                        description = "No messages in this thread",
                        excerpts = emptyList()
                    )
                )
            }

            // Generate summary
            val summary = buildSummary(
                threadId = threadId,
                recipient = thread.recipientName ?: thread.recipient,
                messages = messages,
                includeExcerpts = includeExcerpts
            )

            Timber.d("Generated summary for thread $threadId: ${summary.messageCount} messages")

            Result.success(summary)
        } catch (e: Exception) {
            Timber.e(e, "Error generating thread summary")
            Result.failure(e)
        }
    }

    private fun buildSummary(
        threadId: Long,
        recipient: String,
        messages: List<Message>,
        includeExcerpts: Boolean
    ): ThreadSummary {
        // Count sent vs received
        val sentMessages = messages.count { it.type == Message.TYPE_SENT }
        val receivedMessages = messages.count { it.type == Message.TYPE_INBOX }

        // Date range
        val sortedMessages = messages.sortedBy { it.date }
        val firstMessageDate = sortedMessages.firstOrNull()?.date
        val lastMessageDate = sortedMessages.lastOrNull()?.date

        // Calculate time span
        val timeSpanMs = if (firstMessageDate != null && lastMessageDate != null) {
            lastMessageDate - firstMessageDate
        } else 0L
        val timeSpanDays = timeSpanMs / (1000 * 60 * 60 * 24)

        // Average message length
        val averageMessageLength = if (messages.isNotEmpty()) {
            messages.map { it.body.length }.average().toInt()
        } else 0

        // Build description
        val description = buildString {
            append("Conversation with $recipient spanning $timeSpanDays days. ")
            append("Total of ${messages.size} messages: ")
            append("$sentMessages sent, $receivedMessages received. ")

            if (timeSpanDays > 0) {
                val avgPerDay = messages.size.toFloat() / timeSpanDays
                append(String.format(Locale.US, "Average %.1f messages per day.", avgPerDay))
            }
        }

        // Build excerpts if requested
        val excerpts = if (includeExcerpts) {
            buildExcerpts(sortedMessages)
        } else emptyList()

        return ThreadSummary(
            threadId = threadId,
            recipient = recipient,
            messageCount = messages.size,
            sentCount = sentMessages,
            receivedCount = receivedMessages,
            firstMessageDate = firstMessageDate,
            lastMessageDate = lastMessageDate,
            timeSpanDays = timeSpanDays,
            averageMessageLength = averageMessageLength,
            description = description,
            excerpts = excerpts
        )
    }

    private fun buildExcerpts(sortedMessages: List<Message>): List<MessageExcerpt> {
        val excerpts = mutableListOf<MessageExcerpt>()
        val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US)

        // First few messages (up to 3)
        sortedMessages.take(3).forEach { message ->
            excerpts.add(
                MessageExcerpt(
                    date = dateFormat.format(Date(message.date)),
                    type = if (message.type == Message.TYPE_SENT) ExcerptType.SENT else ExcerptType.RECEIVED,
                    body = message.body.take(200),
                    isSeparator = false
                )
            )
        }

        // Separator if there are omitted messages
        if (sortedMessages.size > 6) {
            excerpts.add(
                MessageExcerpt(
                    date = "",
                    type = ExcerptType.SEPARATOR,
                    body = "... ${sortedMessages.size - 6} messages omitted ...",
                    isSeparator = true
                )
            )
        }

        // Last few messages (up to 3)
        if (sortedMessages.size > 3) {
            sortedMessages.takeLast(3).forEach { message ->
                excerpts.add(
                    MessageExcerpt(
                        date = dateFormat.format(Date(message.date)),
                        type = if (message.type == Message.TYPE_SENT) ExcerptType.SENT else ExcerptType.RECEIVED,
                        body = message.body.take(200),
                        isSeparator = false
                    )
                )
            }
        }

        return excerpts
    }
}

/**
 * Thread summary data class
 */
data class ThreadSummary(
    val threadId: Long,
    val recipient: String,
    val messageCount: Int,
    val sentCount: Int,
    val receivedCount: Int,
    val firstMessageDate: Long?,
    val lastMessageDate: Long?,
    val timeSpanDays: Long,
    val averageMessageLength: Int,
    val description: String,
    val excerpts: List<MessageExcerpt>
)

/**
 * Message excerpt for summary
 */
data class MessageExcerpt(
    val date: String,
    val type: ExcerptType,
    val body: String,
    val isSeparator: Boolean
)

/**
 * Excerpt type
 */
enum class ExcerptType {
    SENT,
    RECEIVED,
    SEPARATOR
}
