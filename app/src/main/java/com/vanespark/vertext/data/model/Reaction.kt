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
            // Common emoji reactions are usually short (up to a few codepoints)
            // but we need to check codepoint count, not character count
            val codePoints = trimmed.codePoints().toArray()

            // Allow up to 5 codepoints (accounts for emoji with modifiers/skin tones)
            if (codePoints.size > 5) return null

            // Check if it contains only emoji characters
            // This is a simple heuristic - emoji are in specific Unicode ranges
            val hasOnlyEmoji = codePoints.all { codePoint ->
                // Emoji ranges (using full Unicode code points, not UTF-16 chars)
                codePoint in 0x1F300..0x1F9FF || // Miscellaneous Symbols and Pictographs
                codePoint in 0x2600..0x26FF ||   // Miscellaneous Symbols
                codePoint in 0x2700..0x27BF ||   // Dingbats
                codePoint == 0xFE0F ||           // Variation Selector
                codePoint == 0x200D ||           // Zero Width Joiner (for complex emoji)
                codePoint in 0x1F1E6..0x1F1FF || // Regional Indicator Symbols (flags)
                codePoint in 0x1F600..0x1F64F || // Emoticons
                codePoint in 0x1F680..0x1F6FF || // Transport and Map Symbols
                codePoint in 0x1F900..0x1F9FF || // Supplemental Symbols and Pictographs
                codePoint in 0x2B50..0x2BFF ||   // Miscellaneous Symbols (includes ⭐)
                codePoint in 0x1F004..0x1F0CF    // Mahjong, playing cards
            }

            return if (hasOnlyEmoji) trimmed else null
        }
    }
}
