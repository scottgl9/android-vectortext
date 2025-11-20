package com.vanespark.vertext.data.repository

import com.vanespark.vertext.data.dao.BlockedContactDao
import com.vanespark.vertext.data.model.BlockedContact
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for BlockedContact operations
 * Provides a clean API for managing blocked contacts
 */
@Singleton
class BlockedContactRepository @Inject constructor(
    private val blockedContactDao: BlockedContactDao
) {

    /**
     * Get all blocked contacts as a Flow
     */
    fun getAllBlockedContacts(): Flow<List<BlockedContact>> {
        return blockedContactDao.getAllBlockedContacts()
    }

    /**
     * Get all blocked contacts as a snapshot
     */
    suspend fun getAllBlockedContactsSnapshot(): List<BlockedContact> {
        return blockedContactDao.getAllBlockedContactsSnapshot()
    }

    /**
     * Block a contact by phone number
     */
    suspend fun blockContact(phoneNumber: String, contactName: String? = null, reason: String? = null): Long {
        val blockedContact = BlockedContact(
            phoneNumber = phoneNumber,
            contactName = contactName,
            reason = reason,
            blockedDate = System.currentTimeMillis()
        )
        return blockedContactDao.insert(blockedContact)
    }

    /**
     * Unblock a contact by phone number
     */
    suspend fun unblockContact(phoneNumber: String) {
        blockedContactDao.deleteByPhoneNumber(phoneNumber)
    }

    /**
     * Check if a phone number is blocked
     */
    suspend fun isPhoneNumberBlocked(phoneNumber: String): Boolean {
        return blockedContactDao.isPhoneNumberBlocked(phoneNumber)
    }

    /**
     * Get blocked contact by phone number
     */
    suspend fun getByPhoneNumber(phoneNumber: String): BlockedContact? {
        return blockedContactDao.getByPhoneNumber(phoneNumber)
    }

    /**
     * Get count of blocked contacts
     */
    suspend fun getBlockedCount(): Int {
        return blockedContactDao.getBlockedCount()
    }

    /**
     * Search blocked contacts
     */
    suspend fun searchBlockedContacts(query: String): List<BlockedContact> {
        return blockedContactDao.searchBlockedContacts(query)
    }

    /**
     * Delete all blocked contacts
     */
    suspend fun deleteAllBlockedContacts() {
        blockedContactDao.deleteAll()
    }
}
