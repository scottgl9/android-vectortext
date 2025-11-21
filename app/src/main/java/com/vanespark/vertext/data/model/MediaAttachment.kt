package com.vanespark.vertext.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Represents a media attachment in an MMS message
 */
data class MediaAttachment(
    /** URI of the media file */
    val uri: String,

    /** MIME type of the media (e.g., "image/jpeg", "video/mp4", "audio/mpeg") */
    val mimeType: String,

    /** Optional file name */
    val fileName: String? = null,

    /** File size in bytes */
    val fileSize: Long? = null
) {
    companion object {
        private val gson = Gson()

        /**
         * Parse JSON string to list of MediaAttachment
         */
        fun fromJson(json: String?): List<MediaAttachment> {
            if (json.isNullOrBlank()) return emptyList()

            return try {
                val type = object : TypeToken<List<MediaAttachment>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * Convert list of MediaAttachment to JSON string
         */
        fun toJson(attachments: List<MediaAttachment>): String {
            return if (attachments.isEmpty()) {
                ""
            } else {
                gson.toJson(attachments)
            }
        }
    }

    /**
     * Get the media type category
     */
    val mediaType: MediaType
        get() = when {
            mimeType.startsWith("image/") -> MediaType.IMAGE
            mimeType.startsWith("video/") -> MediaType.VIDEO
            mimeType.startsWith("audio/") -> MediaType.AUDIO
            else -> MediaType.OTHER
        }

    /**
     * Check if this is an image attachment
     */
    val isImage: Boolean
        get() = mediaType == MediaType.IMAGE

    /**
     * Check if this is a video attachment
     */
    val isVideo: Boolean
        get() = mediaType == MediaType.VIDEO

    /**
     * Check if this is an audio attachment
     */
    val isAudio: Boolean
        get() = mediaType == MediaType.AUDIO
}

/**
 * Media type categories for attachments
 */
enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    OTHER
}
