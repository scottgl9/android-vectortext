package com.vanespark.vertext.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a reaction to a message (emoji response)
 * Similar to iMessage and Google Messages tapback/reactions
 */
data class Reaction(
    /** Emoji character(s) used as reaction */
    val emoji: String,

    /** Phone number of person who reacted */
    val sender: String,

    /** Timestamp when reaction was added */
    val timestamp: Long,

    /** Optional: Display name of sender (for UI) */
    val senderName: String? = null
) {
    companion object {
        /**
         * Parse reactions from JSON string stored in database
         * Format: [{"emoji":"👍","sender":"+1234567890","timestamp":1234567890,"senderName":"John"}]
         */
        fun fromJson(json: String?): List<Reaction> {
            if (json.isNullOrBlank()) return emptyList()

            return try {
                val jsonArray = JSONArray(json)
                List(jsonArray.length()) { index ->
                    val obj = jsonArray.getJSONObject(index)
                    Reaction(
                        emoji = obj.getString("emoji"),
                        sender = obj.getString("sender"),
                        timestamp = obj.getLong("timestamp"),
                        senderName = obj.optString("senderName").takeIf { it.isNotBlank() }
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * Serialize reactions to JSON string for database storage
         */
        fun toJson(reactions: List<Reaction>): String {
            if (reactions.isEmpty()) return ""

            val jsonArray = JSONArray()
            reactions.forEach { reaction ->
                val obj = JSONObject().apply {
                    put("emoji", reaction.emoji)
                    put("sender", reaction.sender)
                    put("timestamp", reaction.timestamp)
                    reaction.senderName?.let { put("senderName", it) }
                }
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }

        /**
         * Detect if a message text is a single emoji reaction
         * Returns the emoji if it is, null otherwise
         */
        fun detectEmojiReaction(text: String): String? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return null

            // Check if it's a single emoji or emoji sequence
            // Common emoji reactions are usually 1-3 characters
            if (trimmed.length > 10) return null

            // Check if it contains only emoji characters
            // This is a simple heuristic - emoji are in specific Unicode ranges
            val hasOnlyEmoji = trimmed.all { char ->
                // Emoji ranges (simplified check)
                char.code in 0x1F300..0x1F9FF || // Miscellaneous Symbols and Pictographs
                char.code in 0x2600..0x26FF ||   // Miscellaneous Symbols
                char.code in 0x2700..0x27BF ||   // Dingbats
                char.code == 0xFE0F ||           // Variation Selector
                char.code == 0x200D ||           // Zero Width Joiner (for complex emoji)
                char.code in 0x1F1E6..0x1F1FF    // Regional Indicator Symbols (flags)
            }

            return if (hasOnlyEmoji) trimmed else null
        }
    }
}
