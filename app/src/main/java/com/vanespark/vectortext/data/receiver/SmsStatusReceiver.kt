package com.vanespark.vectortext.data.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.vanespark.vectortext.data.model.Message
import com.vanespark.vectortext.data.provider.SmsSenderService
import com.vanespark.vectortext.data.repository.MessageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Broadcast receiver for SMS sent and delivery status
 * Updates message status based on delivery reports
 */
@AndroidEntryPoint
class SmsStatusReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(SmsSenderService.EXTRA_MESSAGE_ID, -1L)

        when (intent.action) {
            SmsSenderService.ACTION_SMS_SENT -> handleSmsSent(messageId, resultCode)
            SmsSenderService.ACTION_SMS_DELIVERED -> handleSmsDelivered(messageId, resultCode)
        }
    }

    private fun handleSmsSent(messageId: Long, resultCode: Int) {
        val pendingResult = goAsync()

        scope.launch {
            try {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Timber.d("SMS sent successfully (messageId: $messageId)")
                        // Update message status to sent
                        if (messageId > 0) {
                            val message = messageRepository.getMessageById(messageId)
                            message?.let {
                                messageRepository.updateMessage(
                                    it.copy(type = Message.TYPE_SENT)
                                )
                            }
                        }
                    }
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                        Timber.e("SMS send failed: Generic failure (messageId: $messageId)")
                        updateMessageAsFailed(messageId)
                    }
                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        Timber.e("SMS send failed: No service (messageId: $messageId)")
                        updateMessageAsFailed(messageId)
                    }
                    SmsManager.RESULT_ERROR_NULL_PDU -> {
                        Timber.e("SMS send failed: Null PDU (messageId: $messageId)")
                        updateMessageAsFailed(messageId)
                    }
                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        Timber.e("SMS send failed: Radio off (messageId: $messageId)")
                        updateMessageAsFailed(messageId)
                    }
                    else -> {
                        Timber.w("SMS send unknown result: $resultCode (messageId: $messageId)")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling SMS sent status")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSmsDelivered(messageId: Long, resultCode: Int) {
        val pendingResult = goAsync()

        scope.launch {
            try {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Timber.d("SMS delivered successfully (messageId: $messageId)")
                        // Could add a "delivered" flag to Message model in future
                    }
                    Activity.RESULT_CANCELED -> {
                        Timber.w("SMS delivery failed (messageId: $messageId)")
                    }
                    else -> {
                        Timber.w("SMS delivery unknown result: $resultCode (messageId: $messageId)")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling SMS delivery status")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateMessageAsFailed(messageId: Long) {
        if (messageId > 0) {
            val message = messageRepository.getMessageById(messageId)
            message?.let {
                messageRepository.updateMessage(
                    it.copy(type = Message.TYPE_FAILED)
                )
            }
        }
    }
}
