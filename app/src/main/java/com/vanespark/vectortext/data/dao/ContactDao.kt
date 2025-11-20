package com.vanespark.vectortext.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vanespark.vectortext.data.model.Contact
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Contact entities
 * Provides queries for managing contact information
 */
@Dao
interface ContactDao {

    // === Basic CRUD Operations ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<Contact>): List<Long>

    @Update
    suspend fun update(contact: Contact)

    @Delete
    suspend fun delete(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteById(contactId: Long)

    // === Query Operations ===

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getById(contactId: Long): Contact?

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    fun getByIdFlow(contactId: Long): Flow<Contact?>

    @Query("SELECT * FROM contacts WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): Contact?

    @Query("SELECT * FROM contacts WHERE phone = :phone LIMIT 1")
    fun getByPhoneFlow(phone: String): Flow<Contact?>

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts ORDER BY name ASC LIMIT :limit")
    suspend fun getContactsLimit(limit: Int): List<Contact>

    // === Search Operations ===

    @Query("""
        SELECT * FROM contacts
        WHERE name LIKE '%' || :query || '%'
           OR phone LIKE '%' || :query || '%'
        ORDER BY name ASC
        LIMIT :limit
    """)
    suspend fun searchContacts(query: String, limit: Int): List<Contact>

    // === Update Operations ===

    @Query("UPDATE contacts SET name = :name WHERE id = :contactId")
    suspend fun updateName(contactId: Long, name: String)

    @Query("UPDATE contacts SET avatar_uri = :avatarUri WHERE id = :contactId")
    suspend fun updateAvatarUri(contactId: Long, avatarUri: String)

    @Query("UPDATE contacts SET notes = :notes WHERE id = :contactId")
    suspend fun updateNotes(contactId: Long, notes: String)

    @Query("UPDATE contacts SET last_synced = :timestamp WHERE id = :contactId")
    suspend fun updateLastSynced(contactId: Long, timestamp: Long)

    // === Statistics ===

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getContactCount(): Int

    @Query("SELECT * FROM contacts WHERE last_synced < :timestamp")
    suspend fun getContactsNeedingSync(timestamp: Long): List<Contact>

    // === Cleanup Operations ===

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}
