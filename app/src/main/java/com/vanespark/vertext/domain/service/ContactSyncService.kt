package com.vanespark.vertext.domain.service

import com.vanespark.vertext.data.repository.ThreadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for syncing contact names to threads
 * Updates thread recipient names based on Android contacts
 */
@Singleton
class ContactSyncService @Inject constructor(
    private val threadRepository: ThreadRepository,
    private val contactService: ContactService
) {

    /**
     * Sync contact names for all threads
     * Updates threads with contact names from Android Contacts
     */
    suspend fun syncContactNamesForAllThreads(): Int = withContext(Dispatchers.IO) {
        var updatedCount = 0

        try {
            Timber.d("Starting contact name sync for all threads")

            // Get all threads
            val threads = threadRepository.getAllThreadsSnapshot()
            Timber.d("Found ${threads.size} threads to sync")

            // Update each thread with contact name if available
            for (thread in threads) {
                // Skip if already has a name
                if (thread.recipientName != null && thread.recipientName != thread.recipient) {
                    continue
                }

                // Look up contact name
                val contactName = contactService.getContactName(thread.recipient)
                if (contactName != null) {
                    threadRepository.updateRecipientName(thread.id, contactName)
                    updatedCount++
                    Timber.d("Updated thread ${thread.id}: $contactName")
                }
            }

            Timber.d("Contact sync complete. Updated $updatedCount threads")
        } catch (e: Exception) {
            Timber.e(e, "Error syncing contact names")
        }

        return@withContext updatedCount
    }

    /**
     * Sync contact name for a specific thread
     */
    suspend fun syncContactNameForThread(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val thread = threadRepository.getThreadById(threadId) ?: return@withContext false
            val contactName = contactService.getContactName(thread.recipient) ?: return@withContext false

            threadRepository.updateRecipientName(threadId, contactName)
            Timber.d("Updated contact name for thread $threadId: $contactName")
            return@withContext true
        } catch (e: Exception) {
            Timber.e(e, "Error syncing contact name for thread $threadId")
            return@withContext false
        }
    }
}
