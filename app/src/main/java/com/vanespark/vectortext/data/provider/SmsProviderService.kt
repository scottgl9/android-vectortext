package com.vanespark.vectortext.data.provider

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.vanespark.vectortext.data.model.Message
import com.vanespark.vectortext.data.model.Thread
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
     */
    suspend fun readAllThreads(): List<Thread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<Thread>()
        val uri = Telephony.Threads.CONTENT_URI

        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.RECIPIENT_IDS,
            Telephony.Threads.MESSAGE_COUNT,
            Telephony.Threads.DATE,
            Telephony.Threads.SNIPPET
        )

        try {
            contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Threads.DATE} DESC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Telephony.Threads._ID)
                val recipientIdsIndex = cursor.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS)
                val messageCountIndex = cursor.getColumnIndexOrThrow(Telephony.Threads.MESSAGE_COUNT)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Threads.DATE)
                val snippetIndex = cursor.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)

                while (cursor.moveToNext()) {
                    val threadId = cursor.getLong(idIndex)
                    val recipientIds = cursor.getString(recipientIdsIndex) ?: ""
                    val messageCount = cursor.getInt(messageCountIndex)
                    val date = cursor.getLong(dateIndex)
                    val snippet = cursor.getString(snippetIndex) ?: ""

                    // Get recipient address from the thread
                    val recipient = getRecipientForThread(threadId)

                    threads.add(
                        Thread(
                            id = threadId,
                            recipient = recipient,
                            lastMessage = snippet,
                            lastMessageDate = date,
                            messageCount = messageCount
                        )
                    )
                }
            }
            Timber.d("Read ${threads.size} threads from provider")
        } catch (e: Exception) {
            Timber.e(e, "Error reading threads")
        }

        return@withContext threads
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
