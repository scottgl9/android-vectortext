package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for aggregating messaging insights and statistics
 */
@Singleton
class InsightsService @Inject constructor(
    private val messageRepository: MessageRepository,
    private val threadRepository: ThreadRepository
) {

    /**
     * Get comprehensive messaging insights
     */
    suspend fun getInsights(): Result<MessagingInsights> = withContext(Dispatchers.IO) {
        try {
            val totalMessages = messageRepository.getTotalMessageCount()
            val allMessages = messageRepository.getAllMessagesSnapshot()
            val threads = threadRepository.getAllThreadsSnapshot()

            // Calculate statistics
            val sentCount = allMessages.count { it.type == Message.TYPE_SENT }
            val receivedCount = allMessages.count { it.type == Message.TYPE_INBOX }

            // Top contacts by message count
            val contactStats = threads
                .map { thread ->
                    ContactStats(
                        threadId = thread.id,
                        contactName = thread.recipientName ?: thread.recipient,
                        phoneNumber = thread.recipient,
                        messageCount = thread.messageCount,
                        unreadCount = thread.unreadCount
                    )
                }
                .sortedByDescending { it.messageCount }
                .take(10)

            // Activity by day of week
            val dayActivity = calculateDayActivity(allMessages)

            // Activity by hour
            val hourActivity = calculateHourActivity(allMessages)

            // Recent activity (last 7 days)
            val recentActivity = calculateRecentActivity(allMessages, 7)

            // Response time analysis
            val avgResponseTime = calculateAverageResponseTime(allMessages)

            // Message length stats
            val avgMessageLength = allMessages.map { it.body.length }.average()

            val insights = MessagingInsights(
                totalMessages = totalMessages,
                sentMessages = sentCount,
                receivedMessages = receivedCount,
                totalThreads = threads.size,
                unreadThreads = threads.count { it.unreadCount > 0 },
                topContacts = contactStats,
                activityByDay = dayActivity,
                activityByHour = hourActivity,
                recentActivity = recentActivity,
                averageResponseTimeMinutes = avgResponseTime,
                averageMessageLength = avgMessageLength.toInt()
            )

            Timber.d("Generated insights: $totalMessages total messages")
            Result.success(insights)
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate insights")
            Result.failure(e)
        }
    }

    /**
     * Calculate message activity by day of week (0=Sunday, 6=Saturday)
     */
    private fun calculateDayActivity(messages: List<Message>): Map<Int, Int> {
        val calendar = java.util.Calendar.getInstance()
        return messages.groupBy { message ->
            calendar.timeInMillis = message.date
            calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1 // 0-based index
        }.mapValues { it.value.size }
    }

    /**
     * Calculate message activity by hour (0-23)
     */
    private fun calculateHourActivity(messages: List<Message>): Map<Int, Int> {
        val calendar = java.util.Calendar.getInstance()
        return messages.groupBy { message ->
            calendar.timeInMillis = message.date
            calendar.get(java.util.Calendar.HOUR_OF_DAY)
        }.mapValues { it.value.size }
    }

    /**
     * Calculate recent activity (messages per day for last N days)
     */
    private fun calculateRecentActivity(messages: List<Message>, days: Int): List<DailyActivity> {
        val now = System.currentTimeMillis()
        val startTime = now - TimeUnit.DAYS.toMillis(days.toLong())

        val recentMessages = messages.filter { it.date >= startTime }

        val calendar = java.util.Calendar.getInstance()
        val dailyGroups = recentMessages.groupBy { message ->
            calendar.timeInMillis = message.date
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }

        // Create list for all days in range, even if no messages
        val result = mutableListOf<DailyActivity>()
        calendar.timeInMillis = startTime
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        repeat(days) {
            val dayTimestamp = calendar.timeInMillis
            val count = dailyGroups[dayTimestamp]?.size ?: 0
            result.add(DailyActivity(date = dayTimestamp, messageCount = count))
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        return result
    }

    /**
     * Calculate average response time in minutes
     * Only considers sent messages that follow received messages
     */
    private fun calculateAverageResponseTime(messages: List<Message>): Double {
        val sortedMessages = messages.sortedBy { it.date }
        val responseTimes = mutableListOf<Long>()

        for (i in 1 until sortedMessages.size) {
            val current = sortedMessages[i]
            val previous = sortedMessages[i - 1]

            // If current is sent and previous is received, calculate response time
            if (current.type == Message.TYPE_SENT && previous.type == Message.TYPE_INBOX) {
                val responseTime = current.date - previous.date
                // Only count responses within 24 hours (reasonable response window)
                if (responseTime in 1..TimeUnit.HOURS.toMillis(24)) {
                    responseTimes.add(responseTime)
                }
            }
        }

        return if (responseTimes.isNotEmpty()) {
            TimeUnit.MILLISECONDS.toMinutes(responseTimes.average().toLong()).toDouble()
        } else {
            0.0
        }
    }
}

/**
 * Comprehensive messaging insights data
 */
data class MessagingInsights(
    val totalMessages: Int,
    val sentMessages: Int,
    val receivedMessages: Int,
    val totalThreads: Int,
    val unreadThreads: Int,
    val topContacts: List<ContactStats>,
    val activityByDay: Map<Int, Int>,
    val activityByHour: Map<Int, Int>,
    val recentActivity: List<DailyActivity>,
    val averageResponseTimeMinutes: Double,
    val averageMessageLength: Int
)

/**
 * Contact statistics
 */
data class ContactStats(
    val threadId: Long,
    val contactName: String,
    val phoneNumber: String,
    val messageCount: Int,
    val unreadCount: Int
)

/**
 * Daily activity data
 */
data class DailyActivity(
    val date: Long,
    val messageCount: Int
)
