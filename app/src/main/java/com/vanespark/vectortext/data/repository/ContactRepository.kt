package com.vanespark.vectortext.data.repository

import com.vanespark.vectortext.data.dao.ContactDao
import com.vanespark.vectortext.data.model.Contact
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Contact operations
 * Provides a clean API for accessing contact data
 */
@Singleton
class ContactRepository @Inject constructor(
    private val contactDao: ContactDao
) {

    // === Basic Operations ===

    suspend fun insertContact(contact: Contact): Long {
        return contactDao.insert(contact)
    }

    suspend fun insertContacts(contacts: List<Contact>): List<Long> {
        return contactDao.insertAll(contacts)
    }

    suspend fun updateContact(contact: Contact) {
        contactDao.update(contact)
    }

    suspend fun deleteContact(contact: Contact) {
        contactDao.delete(contact)
    }

    suspend fun deleteContactById(contactId: Long) {
        contactDao.deleteById(contactId)
    }

    // === Query Operations ===

    suspend fun getContactById(contactId: Long): Contact? {
        return contactDao.getById(contactId)
    }

    fun getContactByIdFlow(contactId: Long): Flow<Contact?> {
        return contactDao.getByIdFlow(contactId)
    }

    suspend fun getContactByPhone(phone: String): Contact? {
        return contactDao.getByPhone(phone)
    }

    fun getContactByPhoneFlow(phone: String): Flow<Contact?> {
        return contactDao.getByPhoneFlow(phone)
    }

    fun getAllContacts(): Flow<List<Contact>> {
        return contactDao.getAllContacts()
    }

    suspend fun getContactsLimit(limit: Int): List<Contact> {
        return contactDao.getContactsLimit(limit)
    }

    // === Search Operations ===

    suspend fun searchContacts(query: String, limit: Int = 50): List<Contact> {
        return contactDao.searchContacts(query, limit)
    }

    // === Update Operations ===

    suspend fun updateContactName(contactId: Long, name: String) {
        contactDao.updateName(contactId, name)
    }

    suspend fun updateContactAvatar(contactId: Long, avatarUri: String) {
        contactDao.updateAvatarUri(contactId, avatarUri)
    }

    suspend fun updateContactNotes(contactId: Long, notes: String) {
        contactDao.updateNotes(contactId, notes)
    }

    suspend fun updateContactSyncTime(contactId: Long, timestamp: Long) {
        contactDao.updateLastSynced(contactId, timestamp)
    }

    // === Statistics ===

    suspend fun getContactCount(): Int {
        return contactDao.getContactCount()
    }

    suspend fun getContactsNeedingSync(olderThan: Long): List<Contact> {
        return contactDao.getContactsNeedingSync(olderThan)
    }

    // === Cleanup Operations ===

    suspend fun deleteAllContacts() {
        contactDao.deleteAll()
    }

    // === Convenience Methods ===

    /**
     * Get or create a contact for a given phone number
     */
    suspend fun getOrCreateContact(phone: String, name: String): Contact {
        val existing = getContactByPhone(phone)
        if (existing != null) {
            return existing
        }

        val newContact = Contact(
            name = name,
            phone = phone
        )
        val contactId = insertContact(newContact)
        return newContact.copy(id = contactId)
    }

    /**
     * Sync contact from Android Contacts Provider
     */
    suspend fun syncContact(
        phone: String,
        name: String,
        avatarUri: String? = null,
        lookupKey: String? = null
    ): Contact {
        val existing = getContactByPhone(phone)

        return if (existing != null) {
            // Update existing contact
            val updated = existing.copy(
                name = name,
                avatarUri = avatarUri ?: existing.avatarUri,
                lookupKey = lookupKey ?: existing.lookupKey,
                lastSynced = System.currentTimeMillis()
            )
            updateContact(updated)
            updated
        } else {
            // Create new contact
            val newContact = Contact(
                name = name,
                phone = phone,
                avatarUri = avatarUri,
                lookupKey = lookupKey,
                lastSynced = System.currentTimeMillis()
            )
            val contactId = insertContact(newContact)
            newContact.copy(id = contactId)
        }
    }
}
