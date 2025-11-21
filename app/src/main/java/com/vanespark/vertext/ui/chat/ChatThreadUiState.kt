package com.vanespark.vertext.ui.chat

import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.model.Reaction
import com.vanespark.vertext.data.model.Thread
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI state for the chat thread screen
 */
data class ChatThreadUiState(
    val thread: Thread? = null,
    val messages: List<MessageUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null,
    val composedMessage: String = ""
)

/**
 * UI model for a single message
 */
data class MessageUiItem(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val isIncoming: Boolean,
    val isSending: Boolean,
    val isFailed: Boolean,
    val displayName: String,
    val formattedTime: String,
    val formattedDate: String,
    val reactions: List<Reaction> = emptyList(),
    val isFirstInGroup: Boolean = false,
    val isLastInGroup: Boolean = false
) {
    companion object {
        private const val TYPE_INBOX = 1
        private const val TYPE_SENT = 2
        private const val TYPE_OUTBOX = 3
        private const val TYPE_FAILED = 5

        private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        private val fullDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        /**
         * Convert Message entity to UI model
         */
        fun fromMessage(message: Message, displayName: String = message.address): MessageUiItem {
            val isIncoming = message.type == TYPE_INBOX
            val isSending = message.type == TYPE_OUTBOX
            val isFailed = message.type == TYPE_FAILED

            return MessageUiItem(
                id = message.id,
                threadId = message.threadId,
                address = message.address,
                body = message.body,
                timestamp = message.date,
                isIncoming = isIncoming,
                isSending = isSending,
                isFailed = isFailed,
                displayName = displayName,
                formattedTime = formatTime(message.date),
                formattedDate = formatDate(message.date),
                reactions = message.parseReactions()
            )
        }

        /**
         * Format timestamp as time (e.g., "3:45 PM")
         */
        private fun formatTime(timestamp: Long): String {
            return timeFormat.format(Date(timestamp))
        }

        /**
         * Format timestamp as date
         * - Today: returns empty string (show time only)
         * - This year: "Jan 15"
         * - Older: "Jan 15, 2023"
         */
        private fun formatDate(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val messageDate = Date(timestamp)
            val today = Date(now)

            // Check if today
            val isSameDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(messageDate) ==
                    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(today)

            if (isSameDay) {
                return ""
            }

            // Check if same year
            val isSameYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(messageDate) ==
                    SimpleDateFormat("yyyy", Locale.getDefault()).format(today)

            return if (isSameYear) {
                dateFormat.format(messageDate)
            } else {
                fullDateFormat.format(messageDate)
            }
        }

        /**
         * Group messages by time proximity and sender
         * Messages are grouped if they're from the same sender and within 2 minutes
         */
        fun groupMessages(messages: List<MessageUiItem>): List<MessageUiItem> {
            if (messages.isEmpty()) return emptyList()

            val grouped = mutableListOf<MessageUiItem>()
            val groupTimeThreshold = 2 * 60 * 1000 // 2 minutes in milliseconds

            for (i in messages.indices) {
                val current = messages[i]
                val previous = messages.getOrNull(i - 1)
                val next = messages.getOrNull(i + 1)

                // Check if first in group
                // Messages should be grouped only if they're from the same sender (address)
                // and within the time threshold
                val isFirstInGroup = previous == null ||
                        previous.isIncoming != current.isIncoming ||
                        previous.address != current.address ||
                        (current.timestamp - previous.timestamp) > groupTimeThreshold

                // Check if last in group
                val isLastInGroup = next == null ||
                        next.isIncoming != current.isIncoming ||
                        next.address != current.address ||
                        (next.timestamp - current.timestamp) > groupTimeThreshold

                grouped.add(
                    current.copy(
                        isFirstInGroup = isFirstInGroup,
                        isLastInGroup = isLastInGroup
                    )
                )
            }

            return grouped
        }
    }
}
