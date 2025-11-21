package com.vanespark.vertext.data.provider

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.model.Thread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for reading SMS/MMS messages from Android's Telephony provider
 * Provides methods to sync existing messages into VectorText database
 * Now includes MMS support for group conversations
 */
@Singleton
class SmsProviderService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Read all SMS messages from the system provider
     */
    suspend fun readAllSmsMessages(limit: Int? = null): List<Message> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<Message>()
        val uri = Telephony.Sms.CONTENT_URI

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.SUBJECT
        )

        val sortOrder = "${Telephony.Sms.DATE} DESC${if (limit != null) " LIMIT $limit" else ""}"

        try {
            contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                val readIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
                val subjectIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.SUBJECT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val threadId = cursor.getLong(threadIdIndex)
                    val address = cursor.getString(addressIndex) ?: ""
                    val body = cursor.getString(bodyIndex) ?: ""
                    val date = cursor.getLong(dateIndex)
                    val type = cursor.getInt(typeIndex)
                    val isRead = cursor.getInt(readIndex) == 1
                    val subject = cursor.getString(subjectIndex)

                    messages.add(
                        Message(
                            id = id,
                            threadId = threadId,
                            address = address,
                            body = body,
                            date = date,
                            type = type,
                            isRead = isRead,
                            subject = subject
                        )
                    )
                }
            }
            Timber.d("Read ${messages.size} SMS messages from provider")
        } catch (e: Exception) {
            Timber.e(e, "Error reading SMS messages")
        }

        return@withContext messages
    }

    /**
     * Read SMS messages for a specific thread
     */
    suspend fun readSmsMessagesForThread(threadId: Long, limit: Int? = null): List<Message> =
        withContext(Dispatchers.IO) {
            val messages = mutableListOf<Message>()
            val uri = Telephony.Sms.CONTENT_URI

            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
                Telephony.Sms.SUBJECT
            )

            val selection = "${Telephony.Sms.THREAD_ID} = ?"
            val selectionArgs = arrayOf(threadId.toString())
            val sortOrder = "${Telephony.Sms.DATE} DESC${if (limit != null) " LIMIT $limit" else ""}"

            try {
                contentResolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val threadIdIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                    val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                    val readIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
                    val subjectIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.SUBJECT)

                    while (cursor.moveToNext()) {
                        messages.add(
                            Message(
                                id = cursor.getLong(idIndex),
                                threadId = cursor.getLong(threadIdIndex),
                                address = cursor.getString(addressIndex) ?: "",
                                body = cursor.getString(bodyIndex) ?: "",
                                date = cursor.getLong(dateIndex),
                                type = cursor.getInt(typeIndex),
                                isRead = cursor.getInt(readIndex) == 1,
                                subject = cursor.getString(subjectIndex)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error reading SMS messages for thread $threadId")
            }

            return@withContext messages
        }

    /**
     * Read all conversation threads from the system provider
     * Now includes MMS support and group conversation detection
     */
    suspend fun readAllThreads(): List<Thread> = withContext(Dispatchers.IO) {
        val threadsMap = mutableMapOf<Long, Thread>()

        // Read SMS threads
        readSmsThreads(threadsMap)

        // Read MMS threads
        readMmsThreads(threadsMap)

        // Enhance threads with group information
        enhanceThreadsWithGroupInfo(threadsMap)

        Timber.d("Read ${threadsMap.size} total threads (SMS + MMS)")
        return@withContext threadsMap.values.toList()
    }

    /**
     * Read SMS threads into the threads map
     */
    private fun readSmsThreads(threadsMap: MutableMap<Long, Thread>) {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        try {
            contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val threadIdIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val threadId = cursor.getLong(threadIdIndex)

                    if (!threadsMap.containsKey(threadId)) {
                        val address = cursor.getString(addressIndex) ?: "Unknown"
                        val body = cursor.getString(bodyIndex) ?: ""
                        val date = cursor.getLong(dateIndex)

                        threadsMap[threadId] = Thread(
                            id = threadId,
                            recipient = address,
                            lastMessage = body,
                            lastMessageDate = date,
                            messageCount = 0
                        )
                    }
                }
            }
            Timber.d("Read ${threadsMap.size} SMS threads")
        } catch (e: Exception) {
            Timber.e(e, "Error reading SMS threads")
        }
    }

    /**
     * Read MMS threads into the threads map
     */
    private fun readMmsThreads(threadsMap: MutableMap<Long, Thread>) {
        val uri = Uri.parse("content://mms")
        val projection = arrayOf("_id", "thread_id", "date", "sub")

        try {
            contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("_id")
                val threadIdIndex = cursor.getColumnIndex("thread_id")
                val dateIndex = cursor.getColumnIndex("date")
                val subIndex = cursor.getColumnIndex("sub")

                while (cursor.moveToNext()) {
                    val mmsId = cursor.getLong(idIndex)
                    val threadId = cursor.getLong(threadIdIndex)
                    val date = cursor.getLong(dateIndex) * 1000 // MMS dates are in seconds
                    val subject = if (subIndex >= 0) cursor.getString(subIndex) else null

                    if (!threadsMap.containsKey(threadId)) {
                        // Get message body from parts
                        val body = getMmsBody(mmsId) ?: subject ?: "MMS"

                        threadsMap[threadId] = Thread(
                            id = threadId,
                            recipient = "Loading...", // Will be filled by enhanceThreadsWithGroupInfo
                            lastMessage = body,
                            lastMessageDate = date,
                            messageCount = 0
                        )
                    }
                }
            }
            Timber.d("Read ${threadsMap.size} total threads after MMS")
        } catch (e: Exception) {
            Timber.e(e, "Error reading MMS threads")
        }
    }

    /**
     * Get the text body of an MMS message
     */
    private fun getMmsBody(mmsId: Long): String? {
        val uri = Uri.parse("content://mms/part")
        val selection = "mid = ?"
        val selectionArgs = arrayOf(mmsId.toString())

        try {
            contentResolver.query(
                uri,
                arrayOf("_id", "ct", "text"),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val ctIndex = cursor.getColumnIndex("ct")
                val textIndex = cursor.getColumnIndex("text")

                while (cursor.moveToNext()) {
                    val contentType = if (ctIndex >= 0) cursor.getString(ctIndex) else null
                    if (contentType == "text/plain") {
                        return if (textIndex >= 0) cursor.getString(textIndex) else null
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting MMS body for $mmsId")
        }
        return null
    }

    /**
     * Enhance threads with group information by checking recipients
     */
    private fun enhanceThreadsWithGroupInfo(threadsMap: MutableMap<Long, Thread>) {
        threadsMap.forEach { (threadId, thread) ->
            val recipients = getRecipientsForThread(threadId)

            if (recipients.size > 1) {
                // This is a group conversation
                val recipientsJson = JSONArray(recipients).toString()
                val displayName = recipients.joinToString(", ").take(50) +
                    if (recipients.joinToString(", ").length > 50) "..." else ""

                threadsMap[threadId] = thread.copy(
                    recipient = displayName,
                    isGroup = true,
                    recipients = recipientsJson
                )
                Timber.d("Thread $threadId is a group with ${recipients.size} recipients: $recipients")
            } else if (thread.recipient == "Loading...") {
                // Single recipient MMS - update with actual recipient
                threadsMap[threadId] = thread.copy(
                    recipient = recipients.firstOrNull() ?: "Unknown"
                )
            }
        }
    }

    /**
     * Get all unique recipients for a thread
     */
    private fun getRecipientsForThread(threadId: Long): List<String> {
        val recipients = mutableSetOf<String>()

        // Get SMS recipients
        val smsUri = Telephony.Sms.CONTENT_URI
        val smsProjection = arrayOf(Telephony.Sms.ADDRESS)
        val smsSelection = "${Telephony.Sms.THREAD_ID} = ?"
        val smsSelectionArgs = arrayOf(threadId.toString())

        try {
            contentResolver.query(
                smsUri,
                smsProjection,
                smsSelection,
                smsSelectionArgs,
                null
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                while (cursor.moveToNext()) {
                    val address = if (addressIndex >= 0) cursor.getString(addressIndex) else null
                    if (!address.isNullOrBlank()) {
                        recipients.add(address)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading SMS recipients for thread $threadId")
        }

        // Get MMS recipients
        val mmsUri = Uri.parse("content://mms")
        val mmsProjection = arrayOf("_id")
        val mmsSelection = "thread_id = ?"
        val mmsSelectionArgs = arrayOf(threadId.toString())

        try {
            contentResolver.query(
                mmsUri,
                mmsProjection,
                mmsSelection,
                mmsSelectionArgs,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("_id")
                while (cursor.moveToNext()) {
                    val mmsId = if (idIndex >= 0) cursor.getLong(idIndex) else continue
                    recipients.addAll(getMmsRecipients(mmsId))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading MMS for thread $threadId")
        }

        return recipients.toList()
    }

    /**
     * Get recipients for a specific MMS message
     */
    private fun getMmsRecipients(mmsId: Long): List<String> {
        val recipients = mutableListOf<String>()
        val uri = Uri.parse("content://mms/$mmsId/addr")

        try {
            contentResolver.query(
                uri,
                arrayOf("address", "type"),
                null,
                null,
                null
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex("address")
                val typeIndex = cursor.getColumnIndex("type")

                while (cursor.moveToNext()) {
                    // Type 151 = TO (recipient), Type 137 = FROM (sender)
                    val type = if (typeIndex >= 0) cursor.getInt(typeIndex) else 0
                    val address = if (addressIndex >= 0) cursor.getString(addressIndex) else null

                    // Include both TO and FROM to get all participants
                    if (!address.isNullOrBlank() && (type == 151 || type == 137)) {
                        recipients.add(address)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting MMS recipients for $mmsId")
        }

        return recipients
    }

    /**
     * Get the recipient address for a thread by reading the most recent message
     */
    private fun getRecipientForThread(threadId: Long): String {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.ADDRESS)
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC LIMIT 1"

        try {
            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    return cursor.getString(addressIndex) ?: "Unknown"
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting recipient for thread $threadId")
        }

        return "Unknown"
    }

    /**
     * Get unread message count
     */
    suspend fun getUnreadMessageCount(): Int = withContext(Dispatchers.IO) {
        val uri = Telephony.Sms.CONTENT_URI
        val selection = "${Telephony.Sms.READ} = ?"
        val selectionArgs = arrayOf("0")

        try {
            contentResolver.query(
                uri,
                arrayOf(Telephony.Sms._ID),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                return@withContext cursor.count
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting unread count")
        }

        return@withContext 0
    }

    /**
     * Mark a message as read in the system provider
     */
    suspend fun markMessageAsRead(messageId: Long): Boolean = withContext(Dispatchers.IO) {
        val uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId.toString())
        val values = android.content.ContentValues().apply {
            put(Telephony.Sms.READ, 1)
        }

        try {
            val updated = contentResolver.update(uri, values, null, null)
            return@withContext updated > 0
        } catch (e: Exception) {
            Timber.e(e, "Error marking message $messageId as read")
            return@withContext false
        }
    }

    /**
     * Delete a message from the system provider
     */
    suspend fun deleteMessage(messageId: Long): Boolean = withContext(Dispatchers.IO) {
        val uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId.toString())

        try {
            val deleted = contentResolver.delete(uri, null, null)
            return@withContext deleted > 0
        } catch (e: Exception) {
            Timber.e(e, "Error deleting message $messageId")
            return@withContext false
        }
    }
}
