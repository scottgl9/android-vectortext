package com.vanespark.vertext.data.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.TelephonyManager
import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.model.Thread
import com.vanespark.vertext.data.repository.ContactRepository
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
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository
) {

    private val contentResolver: ContentResolver = context.contentResolver

    // Cache the user's phone number to avoid repeated lookups
    private val userPhoneNumber: String?
        @Suppress("DEPRECATION") // line1Number deprecated in API 33, but still works for our use case
        @SuppressLint("MissingPermission") // Permission is handled by try-catch and app requires READ_SMS
        get() = try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            telephonyManager?.line1Number?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Timber.e(e, "Error getting user phone number")
            null
        }

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
     * Read all MMS messages from the system provider
     */
    suspend fun readAllMmsMessages(limit: Int? = null): List<Message> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<Message>()
        val uri = Uri.parse("content://mms")

        val projection = arrayOf(
            "_id",
            "thread_id",
            "date",
            "read",
            "sub",
            "msg_box"  // Message box type: 1=inbox, 2=sent, 3=draft, 4=outbox
        )

        val sortOrder = "date DESC${if (limit != null) " LIMIT $limit" else ""}"

        try {
            contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("_id")
                val threadIdIndex = cursor.getColumnIndex("thread_id")
                val dateIndex = cursor.getColumnIndex("date")
                val readIndex = cursor.getColumnIndex("read")
                val subIndex = cursor.getColumnIndex("sub")
                val msgBoxIndex = cursor.getColumnIndex("msg_box")

                while (cursor.moveToNext()) {
                    val mmsId = if (idIndex >= 0) cursor.getLong(idIndex) else continue
                    val threadId = if (threadIdIndex >= 0) cursor.getLong(threadIdIndex) else 0L
                    val date = if (dateIndex >= 0) cursor.getLong(dateIndex) * 1000 else 0L // Convert to milliseconds
                    val isRead = if (readIndex >= 0) cursor.getInt(readIndex) == 1 else false
                    val subject = if (subIndex >= 0) cursor.getString(subIndex) else null
                    val msgBox = if (msgBoxIndex >= 0) cursor.getInt(msgBoxIndex) else 1

                    // Get MMS body text
                    val body = getMmsBody(mmsId) ?: continue

                    // Map msg_box to message type
                    // msg_box: 1=inbox, 2=sent, 3=draft, 4=outbox
                    // Message.TYPE: 1=inbox, 2=sent, 3=draft, 4=outbox, 5=failed
                    val messageType = when (msgBox) {
                        2 -> 2  // Sent
                        3 -> 3  // Draft
                        4 -> 3  // Outbox -> treat as draft
                        else -> 1  // Inbox (default)
                    }

                    // Get sender/recipient address based on message type
                    // For incoming messages (msgBox=1): use FROM address (type=137)
                    // For outgoing messages (msgBox=2): use TO addresses (type=130)
                    val address = if (msgBox == 1) {
                        // Incoming: get sender
                        getMmsSender(mmsId) ?: "Unknown"
                    } else {
                        // Outgoing: get first recipient
                        getMmsRecipients(mmsId).firstOrNull() ?: "Unknown"
                    }

                    messages.add(
                        Message(
                            id = mmsId,
                            threadId = threadId,
                            address = address,
                            body = body,
                            date = date,
                            type = messageType,
                            isRead = isRead,
                            subject = subject
                        )
                    )
                }
            }
            Timber.d("Read ${messages.size} MMS messages from provider")
        } catch (e: Exception) {
            Timber.e(e, "Error reading MMS messages")
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
     * Read both SMS and MMS messages for a specific thread (combined and sorted)
     * This is the primary method for loading messages in the UI - queries provider directly
     */
    suspend fun readMessagesForThread(
        threadId: Long,
        limit: Int = 100
    ): List<Message> = withContext(Dispatchers.IO) {
        try {
            // Read both SMS and MMS for this thread
            val smsMessages = readSmsMessagesForThread(threadId, limit)
            val mmsMessages = readMmsMessagesForThread(threadId, limit)

            // Combine and sort by date (newest first), then take limit
            val allMessages = (smsMessages + mmsMessages)
                .sortedByDescending { it.date }
                .take(limit)

            Timber.d("Read ${allMessages.size} messages for thread $threadId (${smsMessages.size} SMS + ${mmsMessages.size} MMS)")
            allMessages
        } catch (e: Exception) {
            Timber.e(e, "Error reading messages for thread $threadId")
            emptyList()
        }
    }

    /**
     * Read MMS messages for a specific thread
     */
    private suspend fun readMmsMessagesForThread(
        threadId: Long,
        limit: Int? = null
    ): List<Message> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<Message>()
        val uri = Uri.parse("content://mms")

        val projection = arrayOf(
            "_id",
            "thread_id",
            "date",
            "read",
            "sub",
            "msg_box"
        )

        val selection = "thread_id = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val sortOrder = "date DESC${if (limit != null) " LIMIT $limit" else ""}"

        try {
            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("_id")
                val threadIdIndex = cursor.getColumnIndex("thread_id")
                val dateIndex = cursor.getColumnIndex("date")
                val readIndex = cursor.getColumnIndex("read")
                val subIndex = cursor.getColumnIndex("sub")
                val msgBoxIndex = cursor.getColumnIndex("msg_box")

                while (cursor.moveToNext()) {
                    val mmsId = if (idIndex >= 0) cursor.getLong(idIndex) else continue
                    val msgThreadId = if (threadIdIndex >= 0) cursor.getLong(threadIdIndex) else threadId
                    val date = if (dateIndex >= 0) cursor.getLong(dateIndex) * 1000 else 0L
                    val isRead = if (readIndex >= 0) cursor.getInt(readIndex) == 1 else false
                    val subject = if (subIndex >= 0) cursor.getString(subIndex) else null
                    val msgBox = if (msgBoxIndex >= 0) cursor.getInt(msgBoxIndex) else 1

                    // Get MMS body and media
                    val body = getMmsBody(mmsId) ?: ""
                    val mediaUris = getMmsMedia(mmsId)

                    val messageType = when (msgBox) {
                        2 -> 2  // Sent
                        3 -> 3  // Draft
                        4 -> 3  // Outbox -> treat as draft
                        else -> 1  // Inbox
                    }

                    val address = if (msgBox == 1) {
                        getMmsSender(mmsId) ?: "Unknown"
                    } else {
                        getMmsRecipients(mmsId).firstOrNull() ?: "Unknown"
                    }

                    messages.add(
                        Message(
                            id = mmsId,
                            threadId = msgThreadId,
                            address = address,
                            body = body,
                            date = date,
                            type = messageType,
                            isRead = isRead,
                            subject = subject,
                            mediaUris = mediaUris
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading MMS messages for thread $threadId")
        }

        return@withContext messages
    }

    /**
     * Get media attachments from an MMS message as JSON string
     */
    private fun getMmsMedia(mmsId: Long): String? {
        val attachments = mutableListOf<com.vanespark.vertext.data.model.MediaAttachment>()
        val uri = Uri.parse("content://mms/part")
        val selection = "mid = ?"
        val selectionArgs = arrayOf(mmsId.toString())

        try {
            contentResolver.query(
                uri,
                arrayOf("_id", "ct", "_data", "text"),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("_id")
                val ctIndex = cursor.getColumnIndex("ct")

                while (cursor.moveToNext()) {
                    val partId = if (idIndex >= 0) cursor.getLong(idIndex) else continue
                    val contentType = if (ctIndex >= 0) cursor.getString(ctIndex) else null

                    // Skip text parts (already got body)
                    if (contentType == "text/plain") continue

                    // Only include media types (image, video, audio)
                    if (contentType != null && (
                        contentType.startsWith("image/") ||
                        contentType.startsWith("video/") ||
                        contentType.startsWith("audio/")
                    )) {
                        val partUri = "content://mms/part/$partId"

                        attachments.add(
                            com.vanespark.vertext.data.model.MediaAttachment(
                                uri = partUri,
                                mimeType = contentType,
                                fileName = null,
                                fileSize = null
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting MMS media for $mmsId")
        }

        return if (attachments.isEmpty()) null else com.vanespark.vertext.data.model.MediaAttachment.toJson(attachments)
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
            val allRecipients = getRecipientsForThread(threadId)

            // Filter out the user's own phone number from the recipients list
            val recipients = allRecipients.filter { phone ->
                !isUserPhoneNumber(phone)
            }

            if (allRecipients.size > 1) {
                // This is a group conversation
                val recipientsJson = JSONArray(allRecipients).toString()

                // Look up contact names for each recipient from Android Contacts
                val recipientNames = recipients.map { phone ->
                    getContactNameForPhone(phone)
                }

                // Generate display name from contact names
                val displayName = when {
                    recipientNames.isEmpty() -> "Group Chat" // Fallback if only user was in the list
                    recipientNames.size <= 3 -> recipientNames.joinToString(", ")
                    else -> "${recipientNames.take(3).joinToString(", ")} +${recipientNames.size - 3}"
                }

                threadsMap[threadId] = thread.copy(
                    recipient = displayName,
                    isGroup = true,
                    recipients = recipientsJson
                )
                Timber.d("Thread $threadId is a group with ${allRecipients.size} total recipients (${recipients.size} excluding user): $recipientNames")
            } else if (thread.recipient == "Loading...") {
                // Single recipient MMS - update with actual recipient (name or phone)
                val phone = recipients.firstOrNull() ?: allRecipients.firstOrNull() ?: "Unknown"
                val displayName = getContactNameForPhone(phone)

                threadsMap[threadId] = thread.copy(
                    recipient = displayName
                )
            }
        }
    }

    /**
     * Check if a phone number matches the user's phone number
     */
    private fun isUserPhoneNumber(phone: String): Boolean {
        val userPhone = userPhoneNumber ?: return false

        // Normalize both numbers for comparison (remove non-digits)
        val normalizedPhone = phone.replace(Regex("[^0-9]"), "")
        val normalizedUserPhone = userPhone.replace(Regex("[^0-9]"), "")

        // Compare last 10 digits (to handle country codes)
        val phoneDigits = normalizedPhone.takeLast(10)
        val userPhoneDigits = normalizedUserPhone.takeLast(10)

        return phoneDigits == userPhoneDigits
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

        // Get MMS recipients and senders
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
                    // Add both recipients and sender to get all participants
                    recipients.addAll(getMmsRecipients(mmsId))
                    getMmsSender(mmsId)?.let { recipients.add(it) }
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
    /**
     * Get the sender address for an MMS message
     * Returns the FROM address (type=137)
     */
    private fun getMmsSender(mmsId: Long): String? {
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
                    val type = if (typeIndex >= 0) cursor.getInt(typeIndex) else 0
                    val address = if (addressIndex >= 0) cursor.getString(addressIndex) else null

                    // Type 137 = FROM (sender)
                    if (!address.isNullOrBlank() && type == 137) {
                        return address
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting MMS sender for $mmsId")
        }

        return null
    }

    /**
     * Get recipient addresses for an MMS message
     * Returns TO addresses (type=130) and BCC (type=151)
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
                    val type = if (typeIndex >= 0) cursor.getInt(typeIndex) else 0
                    val address = if (addressIndex >= 0) cursor.getString(addressIndex) else null

                    // Type 130 = TO (recipient), Type 151 = BCC
                    if (!address.isNullOrBlank() && (type == 130 || type == 151)) {
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
     * Look up contact name from Android's Contacts Provider
     * Falls back to phone number if no contact found
     */
    private fun getContactNameForPhone(phoneNumber: String): String {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        try {
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error looking up contact name for $phoneNumber")
        }

        return phoneNumber // Fallback to phone number
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
