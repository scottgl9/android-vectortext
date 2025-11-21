package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.model.Reaction
import com.vanespark.vertext.data.repository.MessageRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for detecting and processing emoji reactions to messages
 * Similar to iMessage and Google Messages tapback/reaction functionality
 */
@Singleton
class ReactionDetectionService @Inject constructor(
    private val messageRepository: MessageRepository,
    private val contactService: ContactService
) {

    companion object {
        /**
         * Maximum time window (in milliseconds) for a message to be considered a reaction
         * If a message is sent more than this time after the previous message, it won't be detected as a reaction
         */
        private const val REACTION_TIME_WINDOW_MS = 30_000L // 30 seconds
    }

    /**
     * Process a newly received or sent message to detect if it's a reaction
     * Returns true if the message was processed as a reaction, false otherwise
     *
     * Supports Google Messages compatible format: "<emoji> <verb> '<quoted text>'"
     * Examples: "👍 Liked 'See you at 5'", "❤️ Loved 'Thanks!'"
     *
     * @param message The message to check
     * @return True if message was detected and processed as a reaction
     */
    suspend fun processMessageForReaction(message: Message): Boolean {
        val messageBody = message.body.trim()

        // Google Messages reaction pattern: "<emoji> <verb> '<quoted text>'"
        // Pattern matches: emoji + space + verb + space + quoted text in single quotes
        val googleReactionPattern = Regex("^(.+?)\\s+(Liked|Disliked|Loved|Laughed at|Emphasized|Questioned|Reacted to)\\s+'(.+)'$")
        val matchResult = googleReactionPattern.find(messageBody)

        if (matchResult != null) {
            val (emojiPart, verb, quotedText) = matchResult.destructured
            val emoji = emojiPart.trim()

            Timber.d("Detected Google Messages reaction: $emoji ($verb) to '$quotedText'")

            // Find the message by searching for the quoted text
            // Try to find exact match first, then fuzzy match
            val targetMessage = messageRepository.findMessageByText(
                threadId = message.threadId,
                searchText = quotedText,
                beforeTimestamp = message.date
            )

            if (targetMessage == null) {
                Timber.d("No message found matching quoted text: '$quotedText'")
                return false
            }

            // Get sender name for display
            val senderName = if (message.type == Message.TYPE_SENT) {
                "You"
            } else {
                contactService.getContactName(message.address) ?: message.address
            }

            // Add reaction to target message
            messageRepository.addReaction(
                messageId = targetMessage.id,
                emoji = emoji,
                sender = message.address,
                timestamp = message.date,
                senderName = senderName
            )

            Timber.d("Added reaction $emoji to message ${targetMessage.id} from $senderName")

            // Delete the reaction message since it's now stored as a reaction
            messageRepository.deleteMessageById(message.id)

            return true
        }

        // Fallback: Check if this message is a single emoji (simple reaction)
        val emoji = Reaction.detectEmojiReaction(messageBody) ?: return false

        Timber.d("Detected simple emoji reaction: $emoji from ${message.address}")

        // Find the previous message in the thread
        val targetMessage = messageRepository.getLastMessageBeforeTimestamp(
            threadId = message.threadId,
            beforeTimestamp = message.date
        )

        if (targetMessage == null) {
            Timber.d("No target message found for reaction")
            return false
        }

        // Check if reaction is within time window
        val timeDiff = message.date - targetMessage.date
        if (timeDiff > REACTION_TIME_WINDOW_MS) {
            Timber.d("Reaction too far from target message (${timeDiff}ms), treating as regular message")
            return false
        }

        // Get sender name for display
        val senderName = if (message.type == Message.TYPE_SENT) {
            "You"
        } else {
            contactService.getContactName(message.address) ?: message.address
        }

        // Add reaction to target message
        messageRepository.addReaction(
            messageId = targetMessage.id,
            emoji = emoji,
            sender = message.address,
            timestamp = message.date,
            senderName = senderName
        )

        Timber.d("Added reaction $emoji to message ${targetMessage.id} from $senderName")

        // Delete the reaction message since it's now stored as a reaction
        messageRepository.deleteMessageById(message.id)

        return true
    }

    /**
     * Process existing messages in a thread to detect reactions
     * Useful for one-time migration or re-processing
     *
     * @param threadId The thread to process
     * @return Number of reactions detected and processed
     */
    suspend fun reprocessThreadForReactions(threadId: Long): Int {
        val messages = messageRepository.getMessagesForThreadLimitSnapshot(threadId, 1000)
        var reactionsProcessed = 0

        // Process messages in chronological order
        messages.sortedBy { it.date }.forEach { message ->
            if (processMessageForReaction(message)) {
                reactionsProcessed++
            }
        }

        Timber.d("Reprocessed thread $threadId: found $reactionsProcessed reactions")
        return reactionsProcessed
    }

    /**
     * Check if a message body is likely an emoji reaction
     * @param body The message body to check
     * @return True if it appears to be an emoji reaction
     */
    fun isLikelyReaction(body: String): Boolean {
        return Reaction.detectEmojiReaction(body) != null
    }
}
