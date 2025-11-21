package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.model.Thread
import com.vanespark.vertext.data.model.ThreadCategory
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for automatically categorizing message threads
 * Uses rule-based logic to detect patterns
 */
@Singleton
class ThreadCategorizationService @Inject constructor(
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository
) {

    companion object {
        // Keywords for category detection
        private val WORK_KEYWORDS = listOf(
            "meeting", "conference", "project", "deadline", "office", "team",
            "manager", "client", "presentation", "report", "email", "work",
            "schedule", "business", "contract", "proposal"
        )

        private val PROMOTIONS_KEYWORDS = listOf(
            "sale", "discount", "offer", "deal", "promo", "promotion",
            "limited time", "exclusive", "save", "% off", "free shipping",
            "coupon", "clearance", "special offer", "buy now", "shop now"
        )

        private val FINANCE_KEYWORDS = listOf(
            "bank", "account", "balance", "payment", "transaction", "transfer",
            "deposit", "withdraw", "credit", "debit", "invoice", "bill",
            "statement", "card", "paypal", "venmo", "zelle", "cash app",
            "dollar", "$", "paid", "charge"
        )

        private val SHOPPING_KEYWORDS = listOf(
            "order", "shipped", "delivery", "tracking", "amazon", "ebay",
            "package", "arrived", "out for delivery", "confirmed",
            "receipt", "purchase", "bought", "cart"
        )

        private val TRAVEL_KEYWORDS = listOf(
            "flight", "hotel", "booking", "reservation", "airport",
            "boarding", "check-in", "itinerary", "travel", "vacation",
            "trip", "airline", "airbnb", "uber", "lyft"
        )

        private val ALERTS_KEYWORDS = listOf(
            "alert", "urgent", "warning", "important", "security",
            "verification", "code", "otp", "password", "login",
            "suspicious", "blocked", "fraud", "confirm", "verify"
        )

        private val SPAM_KEYWORDS = listOf(
            "congratulations you won", "claim your prize", "free gift",
            "click here now", "limited offer", "act now", "call now",
            "100% free", "guaranteed", "risk free", "no obligation",
            "winner", "prize", "lottery", "inheritance"
        )

        // Sender patterns (phone numbers/short codes)
        private val PROMOTION_SENDERS = listOf(
            "^\\d{5,6}$".toRegex() // 5-6 digit short codes (common for promotions)
        )

        private val ALERT_SENDERS = listOf(
            "^\\d{5,6}$".toRegex(), // Short codes for 2FA, alerts
            "^\\+1\\d{10}$".toRegex() // Standard US numbers for alerts
        )
    }

    /**
     * Categorize a single thread
     */
    suspend fun categorizeThread(threadId: Long): ThreadCategory {
        return try {
            val thread = threadRepository.getThreadById(threadId)
                ?: return ThreadCategory.UNCATEGORIZED

            // Get recent messages for analysis (up to 20)
            val messages = messageRepository.getMessagesForThreadLimitSnapshot(threadId, 20)

            if (messages.isEmpty()) {
                return ThreadCategory.UNCATEGORIZED
            }

            val category = detectCategory(thread, messages)

            // Update thread category
            threadRepository.updateThreadCategory(threadId, category.name)

            Timber.d("Categorized thread $threadId as $category")
            category
        } catch (e: Exception) {
            Timber.e(e, "Error categorizing thread $threadId")
            ThreadCategory.UNCATEGORIZED
        }
    }

    /**
     * Categorize all threads
     */
    suspend fun categorizeAllThreads(): Int {
        return try {
            val threads = threadRepository.getAllThreadsSnapshot()
            var categorizedCount = 0

            threads.forEach { thread ->
                val category = categorizeThread(thread.id)
                if (category != ThreadCategory.UNCATEGORIZED) {
                    categorizedCount++
                }
            }

            Timber.d("Categorized $categorizedCount of ${threads.size} threads")
            categorizedCount
        } catch (e: Exception) {
            Timber.e(e, "Error categorizing all threads")
            0
        }
    }

    /**
     * Detect category based on thread and messages
     */
    private fun detectCategory(thread: Thread, messages: List<Message>): ThreadCategory {
        val allText = buildString {
            // Include recipient name/number
            append(thread.recipientName ?: thread.recipient)
            append(" ")

            // Include message bodies
            messages.forEach {
                append(it.body)
                append(" ")
            }
        }.lowercase()

        val sender = thread.recipient

        // Check for spam first (highest priority)
        if (containsKeywords(allText, SPAM_KEYWORDS)) {
            return ThreadCategory.SPAM
        }

        // Check sender patterns for promotions/alerts
        if (matchesSenderPattern(sender, PROMOTION_SENDERS)) {
            return if (containsKeywords(allText, PROMOTIONS_KEYWORDS)) {
                ThreadCategory.PROMOTIONS
            } else {
                ThreadCategory.ALERTS
            }
        }

        // Check for specific categories by keywords
        return when {
            containsKeywords(allText, FINANCE_KEYWORDS) -> ThreadCategory.FINANCE
            containsKeywords(allText, SHOPPING_KEYWORDS) -> ThreadCategory.SHOPPING
            containsKeywords(allText, TRAVEL_KEYWORDS) -> ThreadCategory.TRAVEL
            containsKeywords(allText, WORK_KEYWORDS) -> ThreadCategory.WORK
            containsKeywords(allText, PROMOTIONS_KEYWORDS) -> ThreadCategory.PROMOTIONS
            containsKeywords(allText, ALERTS_KEYWORDS) -> ThreadCategory.ALERTS

            // Check if it's a regular phone number (likely personal)
            isRegularPhoneNumber(sender) -> ThreadCategory.PERSONAL

            // Check if contact name exists (likely personal/social)
            thread.recipientName != null -> {
                // If has a contact name and doesn't match other categories,
                // it's likely personal or social
                if (messages.size > 10) {
                    ThreadCategory.SOCIAL
                } else {
                    ThreadCategory.PERSONAL
                }
            }

            else -> ThreadCategory.UNCATEGORIZED
        }
    }

    /**
     * Check if text contains any of the keywords
     */
    private fun containsKeywords(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * Check if sender matches any of the patterns
     */
    private fun matchesSenderPattern(sender: String, patterns: List<Regex>): Boolean {
        return patterns.any { pattern ->
            pattern.matches(sender)
        }
    }

    /**
     * Check if sender is a regular 10-digit phone number
     */
    private fun isRegularPhoneNumber(sender: String): Boolean {
        // Match standard US phone numbers (with or without +1)
        val phonePattern = "^(\\+1)?\\d{10}$".toRegex()
        return phonePattern.matches(sender)
    }
}
