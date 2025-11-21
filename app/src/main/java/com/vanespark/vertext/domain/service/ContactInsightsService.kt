package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.model.Contact
import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for generating insights about contacts
 * Analyzes conversation history to provide meaningful insights
 */
@Singleton
class ContactInsightsService @Inject constructor(
    private val messageRepository: MessageRepository,
    private val threadRepository: ThreadRepository
) {

    /**
     * Generate comprehensive insights for a contact
     */
    suspend fun generateInsights(contact: Contact): ContactInsights {
        Timber.d("Generating insights for contact: ${contact.name}")

        // Get all threads with this contact
        val threads = threadRepository.getThreadsByRecipient(contact.phone)
        if (threads.isEmpty()) {
            return ContactInsights.empty(contact)
        }

        // Get all messages with this contact
        val allMessages = threads.flatMap { thread ->
            messageRepository.getMessagesForThreadLimit(thread.id, 1000)
        }

        if (allMessages.isEmpty()) {
            return ContactInsights.empty(contact)
        }

        // Calculate statistics
        val stats = calculateStats(allMessages)

        // Find important messages (long messages, contains important keywords)
        val importantMessages = findImportantMessages(allMessages)

        // Generate relationship insights
        val relationshipInsights = generateRelationshipInsights(allMessages, stats)

        // Generate conversation summary
        val summary = generateConversationSummary(allMessages, stats)

        return ContactInsights(
            contact = contact,
            stats = stats,
            importantMessages = importantMessages,
            relationshipInsights = relationshipInsights,
            conversationSummary = summary
        )
    }

    /**
     * Calculate conversation statistics
     */
    private fun calculateStats(messages: List<Message>): ConversationStats {
        val sentMessages = messages.filter { it.type == Message.TYPE_SENT }
        val receivedMessages = messages.filter { it.type == Message.TYPE_INBOX }

        val sortedMessages = messages.sortedBy { it.date }
        val firstMessageDate = sortedMessages.firstOrNull()?.date ?: 0L
        val lastMessageDate = sortedMessages.lastOrNull()?.date ?: 0L

        // Calculate average response time (time between received and sent messages)
        val responseTimes = mutableListOf<Long>()
        for (i in 1 until sortedMessages.size) {
            val current = sortedMessages[i]
            val previous = sortedMessages[i - 1]
            if (current.type == Message.TYPE_SENT && previous.type == Message.TYPE_INBOX) {
                responseTimes.add(current.date - previous.date)
            }
        }
        val avgResponseTime = if (responseTimes.isNotEmpty()) {
            responseTimes.average().toLong()
        } else {
            0L
        }

        // Calculate conversation length
        val conversationLengthDays = TimeUnit.MILLISECONDS.toDays(
            lastMessageDate - firstMessageDate
        )

        // Average message length
        val avgMessageLength = messages.map { it.body.length }.average().toInt()

        return ConversationStats(
            totalMessages = messages.size,
            sentMessages = sentMessages.size,
            receivedMessages = receivedMessages.size,
            firstMessageDate = firstMessageDate,
            lastMessageDate = lastMessageDate,
            conversationLengthDays = conversationLengthDays,
            averageResponseTime = avgResponseTime,
            averageMessageLength = avgMessageLength
        )
    }

    /**
     * Find important messages based on various criteria
     */
    private fun findImportantMessages(messages: List<Message>): List<ImportantMessage> {
        val importantKeywords = listOf(
            "important", "urgent", "asap", "emergency", "congratulations",
            "love you", "miss you", "thank you", "sorry", "birthday",
            "anniversary", "meeting", "appointment", "deadline"
        )

        return messages
            .filter { message ->
                // Long messages (more than 200 characters)
                message.body.length > 200 ||
                // Contains important keywords
                importantKeywords.any { keyword ->
                    message.body.contains(keyword, ignoreCase = true)
                }
            }
            .sortedByDescending { it.date }
            .take(5)
            .map { message ->
                val reason = when {
                    message.body.length > 200 -> "Long message"
                    else -> {
                        val keyword = importantKeywords.find {
                            message.body.contains(it, ignoreCase = true)
                        }
                        "Contains: $keyword"
                    }
                }
                ImportantMessage(
                    message = message,
                    reason = reason
                )
            }
    }

    /**
     * Generate relationship insights based on messaging patterns
     */
    private fun generateRelationshipInsights(
        messages: List<Message>,
        stats: ConversationStats
    ): List<String> {
        val insights = mutableListOf<String>()

        // Frequency insight
        if (stats.conversationLengthDays > 0) {
            val messagesPerDay = stats.totalMessages.toFloat() / stats.conversationLengthDays
            when {
                messagesPerDay >= 10 -> insights.add("Very active conversation - you message frequently")
                messagesPerDay >= 5 -> insights.add("Regular communication - you stay in touch often")
                messagesPerDay >= 1 -> insights.add("Moderate contact - you message occasionally")
                else -> insights.add("Infrequent contact - you message rarely")
            }
        }

        // Response time insight
        if (stats.averageResponseTime > 0) {
            val hours = TimeUnit.MILLISECONDS.toHours(stats.averageResponseTime)
            when {
                hours < 1 -> insights.add("Quick responder - usually replies within an hour")
                hours < 6 -> insights.add("Responsive - typically replies within a few hours")
                hours < 24 -> insights.add("Daily responder - usually replies within a day")
                else -> insights.add("Slow responder - takes more than a day to reply")
            }
        }

        // Balance insight
        val sentRatio = stats.sentMessages.toFloat() / stats.totalMessages
        when {
            sentRatio > 0.7 -> insights.add("You send most messages - consider if this feels balanced")
            sentRatio < 0.3 -> insights.add("They send most messages - you might want to reach out more")
            else -> insights.add("Balanced conversation - both of you contribute equally")
        }

        // Message length insight
        when {
            stats.averageMessageLength > 200 -> insights.add("Detailed conversations - you both share a lot")
            stats.averageMessageLength > 100 -> insights.add("Moderate detail - good conversation depth")
            else -> insights.add("Brief messages - quick and to-the-point communication")
        }

        // Longevity insight
        when {
            stats.conversationLengthDays > 365 -> insights.add("Long-term connection - you've been talking for over a year")
            stats.conversationLengthDays > 180 -> insights.add("Established connection - several months of conversation")
            stats.conversationLengthDays > 30 -> insights.add("Growing connection - a few months of messaging")
            else -> insights.add("New connection - recent conversation history")
        }

        return insights
    }

    /**
     * Generate a natural language summary of the conversation
     */
    private fun generateConversationSummary(
        messages: List<Message>,
        stats: ConversationStats
    ): String {
        return buildString {
            append("You have ")
            append(stats.totalMessages)
            append(" messages with this contact spanning ")

            when {
                stats.conversationLengthDays > 365 -> {
                    val years = stats.conversationLengthDays / 365
                    append("$years ${if (years == 1L) "year" else "years"}")
                }
                stats.conversationLengthDays > 30 -> {
                    val months = stats.conversationLengthDays / 30
                    append("$months ${if (months == 1L) "month" else "months"}")
                }
                else -> {
                    append("${stats.conversationLengthDays} ${if (stats.conversationLengthDays == 1L) "day" else "days"}")
                }
            }

            append(". ")

            append("You've sent ${stats.sentMessages} messages and received ${stats.receivedMessages}. ")

            if (stats.averageResponseTime > 0) {
                val hours = TimeUnit.MILLISECONDS.toHours(stats.averageResponseTime)
                append("Average response time is ")
                when {
                    hours < 1 -> append("less than an hour")
                    hours < 24 -> append("$hours hours")
                    else -> {
                        val days = hours / 24
                        append("$days ${if (days == 1L) "day" else "days"}")
                    }
                }
                append(".")
            }
        }
    }
}

/**
 * Comprehensive insights about a contact
 */
data class ContactInsights(
    val contact: Contact,
    val stats: ConversationStats,
    val importantMessages: List<ImportantMessage>,
    val relationshipInsights: List<String>,
    val conversationSummary: String
) {
    companion object {
        fun empty(contact: Contact) = ContactInsights(
            contact = contact,
            stats = ConversationStats.empty(),
            importantMessages = emptyList(),
            relationshipInsights = listOf("No conversation history yet"),
            conversationSummary = "No messages yet with this contact."
        )
    }
}

/**
 * Conversation statistics
 */
data class ConversationStats(
    val totalMessages: Int,
    val sentMessages: Int,
    val receivedMessages: Int,
    val firstMessageDate: Long,
    val lastMessageDate: Long,
    val conversationLengthDays: Long,
    val averageResponseTime: Long,
    val averageMessageLength: Int
) {
    companion object {
        fun empty() = ConversationStats(
            totalMessages = 0,
            sentMessages = 0,
            receivedMessages = 0,
            firstMessageDate = 0L,
            lastMessageDate = 0L,
            conversationLengthDays = 0L,
            averageResponseTime = 0L,
            averageMessageLength = 0
        )
    }
}

/**
 * An important message with context
 */
data class ImportantMessage(
    val message: Message,
    val reason: String
)
