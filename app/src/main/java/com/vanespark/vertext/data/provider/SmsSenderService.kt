package com.vanespark.vertext.data.provider

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for sending SMS/MMS messages
 * Handles message sending via Android SmsManager
 */
@Singleton
class SmsSenderService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val ACTION_SMS_SENT = "com.vanespark.vertext.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.vanespark.vertext.SMS_DELIVERED"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val MAX_SMS_LENGTH = 160
    }

    /**
     * Send an SMS message
     * Returns true if sent successfully, false otherwise
     */
    suspend fun sendSms(
        destinationAddress: String,
        messageText: String,
        messageId: Long? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)

            // Create pending intents for sent and delivery tracking
            val sentIntent = createSentIntent(messageId)
            val deliveryIntent = createDeliveryIntent(messageId)

            // Check if message needs to be split into multiple parts
            if (messageText.length > MAX_SMS_LENGTH) {
                val parts = smsManager.divideMessage(messageText)
                val sentIntents = ArrayList<PendingIntent>()
                val deliveryIntents = ArrayList<PendingIntent>()

                repeat(parts.size) {
                    sentIntents.add(sentIntent)
                    deliveryIntents.add(deliveryIntent)
                }

                smsManager.sendMultipartTextMessage(
                    destinationAddress,
                    null,
                    parts,
                    sentIntents,
                    deliveryIntents
                )

                Timber.d("Sent multipart SMS (${parts.size} parts) to $destinationAddress")
            } else {
                smsManager.sendTextMessage(
                    destinationAddress,
                    null,
                    messageText,
                    sentIntent,
                    deliveryIntent
                )

                Timber.d("Sent SMS to $destinationAddress")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error sending SMS to $destinationAddress")
            Result.failure(e)
        }
    }

    /**
     * Send an SMS message to multiple recipients
     */
    suspend fun sendSmsToMultiple(
        recipients: List<String>,
        messageText: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        var successCount = 0
        var lastError: Exception? = null

        recipients.forEach { recipient ->
            val result = sendSms(recipient, messageText)
            if (result.isSuccess) {
                successCount++
            } else {
                lastError = result.exceptionOrNull() as? Exception
            }
        }

        if (successCount == recipients.size) {
            Result.success(successCount)
        } else if (successCount > 0) {
            // Partial success
            Timber.w("Sent to $successCount/${recipients.size} recipients")
            Result.success(successCount)
        } else {
            Result.failure(lastError ?: Exception("Failed to send to any recipients"))
        }
    }

    /**
     * Check if the device supports SMS
     */
    fun isSmsSupported(): Boolean {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager != null
        } catch (e: Exception) {
            Timber.e(e, "Error checking SMS support")
            false
        }
    }

    /**
     * Get the maximum SMS length for the current carrier
     */
    fun getMaxSmsLength(): Int {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            // Most carriers support 160 characters for single SMS
            // but this can vary
            MAX_SMS_LENGTH
        } catch (e: Exception) {
            Timber.e(e, "Error getting max SMS length")
            MAX_SMS_LENGTH
        }
    }

    /**
     * Estimate the number of SMS parts needed for a message
     */
    fun estimateSmsPartCount(messageText: String): Int {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(messageText)
            parts.size
        } catch (e: Exception) {
            Timber.e(e, "Error estimating SMS part count")
            // Fallback calculation
            (messageText.length + MAX_SMS_LENGTH - 1) / MAX_SMS_LENGTH
        }
    }

    private fun createSentIntent(messageId: Long?): PendingIntent {
        val intent = Intent(ACTION_SMS_SENT).apply {
            messageId?.let { putExtra(EXTRA_MESSAGE_ID, it) }
        }
        return PendingIntent.getBroadcast(
            context,
            messageId?.toInt() ?: 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createDeliveryIntent(messageId: Long?): PendingIntent {
        val intent = Intent(ACTION_SMS_DELIVERED).apply {
            messageId?.let { putExtra(EXTRA_MESSAGE_ID, it) }
        }
        return PendingIntent.getBroadcast(
            context,
            messageId?.toInt() ?: 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
