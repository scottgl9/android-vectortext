package com.vanespark.vertext.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import com.vanespark.vertext.domain.service.ReactionDetectionService
import com.vanespark.vertext.domain.service.RuleEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Broadcast receiver for incoming SMS messages
 * Processes received SMS and stores them in the database
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var threadRepository: ThreadRepository

    @Inject
    lateinit var ruleEngine: RuleEngine

    @Inject
    lateinit var reactionDetectionService: ReactionDetectionService

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        Timber.d("SMS received")

        // Extract SMS messages from intent
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Timber.w("No SMS messages in intent")
            return
        }

        // Process each SMS message
        messages.forEach { smsMessage ->
            processSmsMessage(smsMessage)
        }
    }

    private fun processSmsMessage(smsMessage: SmsMessage) {
        val address = smsMessage.originatingAddress ?: "Unknown"
        val body = smsMessage.messageBody ?: ""
        val timestamp = smsMessage.timestampMillis

        Timber.d("Processing SMS from $address: ${body.take(50)}...")

        // Use goAsync to handle async operations in BroadcastReceiver
        val pendingResult = goAsync()

        scope.launch {
            try {
                // Get or create thread for this sender
                val thread = threadRepository.getOrCreateThread(
                    recipient = address,
                    recipientName = null // Will be resolved later from contacts
                )

                // Create message entity
                val message = Message(
                    threadId = thread.id,
                    address = address,
                    body = body,
                    date = timestamp,
                    type = Message.TYPE_INBOX,
                    isRead = false
                )

                // Insert message into database
                val messageId = messageRepository.insertMessage(message)
                Timber.d("Inserted received SMS with ID: $messageId")

                val savedMessage = message.copy(id = messageId)

                // Check if this is an emoji reaction to a previous message
                try {
                    val isReaction = reactionDetectionService.processMessageForReaction(savedMessage)
                    if (isReaction) {
                        Timber.d("Message $messageId was processed as a reaction")
                        // Don't process reactions through rules or update thread
                        // The message was deleted and stored as a reaction
                        return@launch
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error detecting reaction for message $messageId")
                }

                // Process message through rule engine
                try {
                    ruleEngine.processMessage(savedMessage)
                    Timber.d("Processed message $messageId through rule engine")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing message through rule engine")
                }

                // Update thread metadata
                threadRepository.updateLastMessage(
                    threadId = thread.id,
                    message = body,
                    date = timestamp
                )

                // Update unread count
                val unreadCount = messageRepository.getUnreadCountForThread(thread.id)
                threadRepository.updateUnreadCount(thread.id, unreadCount)

                Timber.d("Updated thread ${thread.id} with new message")
            } catch (e: Exception) {
                Timber.e(e, "Error processing received SMS")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
