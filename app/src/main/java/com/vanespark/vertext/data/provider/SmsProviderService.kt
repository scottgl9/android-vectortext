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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for reading SMS/MMS messages from Android's Telephony provider
 * Provides methods to sync existing messages into VectorText database
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
     * Builds threads from messages instead of using Telephony.Threads.CONTENT_URI
     * to avoid device-specific schema issues (especially Samsung devices)
     */
    suspend fun readAllThreads(): List<Thread> = withContext(Dispatchers.IO) {
        val threadsMap = mutableMapOf<Long, Thread>()
        val uri = Telephony.Sms.CONTENT_URI

        // Group messages by thread_id to build thread list
        val projection = arrayOf(
            Telephony.Sms._ID,
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
                val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val threadId = cursor.getLong(threadIdIndex)

                    // Only add thread if we haven't seen it yet
                    // (messages are sorted DESC so first occurrence is latest)
                    if (!threadsMap.containsKey(threadId)) {
                        val address = cursor.getString(addressIndex) ?: "Unknown"
                        val body = cursor.getString(bodyIndex) ?: ""
                        val date = cursor.getLong(dateIndex)

                        threadsMap[threadId] = Thread(
                            id = threadId,
                            recipient = address,
                            lastMessage = body,
                            lastMessageDate = date,
                            messageCount = 0 // Will be updated later
                        )
                    }
                }
            }
            Timber.d("Read ${threadsMap.size} threads from messages")
        } catch (e: Exception) {
            Timber.e(e, "Error reading threads from messages")
        }

        return@withContext threadsMap.values.toList()
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
