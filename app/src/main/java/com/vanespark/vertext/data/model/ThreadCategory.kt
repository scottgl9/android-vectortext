package com.vanespark.vertext.data.model

/**
 * Categories for organizing message threads
 */
enum class ThreadCategory(val displayName: String, val icon: String) {
    PERSONAL("Personal", "👤"),
    WORK("Work", "💼"),
    PROMOTIONS("Promotions", "📢"),
    FINANCE("Finance", "💰"),
    SHOPPING("Shopping", "🛍️"),
    TRAVEL("Travel", "✈️"),
    SOCIAL("Social", "🎉"),
    ALERTS("Alerts", "⚠️"),
    SPAM("Spam", "🚫"),
    UNCATEGORIZED("Uncategorized", "📁");

    companion object {
        /**
         * Get category from string (case-insensitive)
         */
        fun fromString(value: String?): ThreadCategory {
            return try {
                value?.let { valueOf(it.uppercase()) } ?: UNCATEGORIZED
            } catch (e: IllegalArgumentException) {
                UNCATEGORIZED
            }
        }
    }
}
