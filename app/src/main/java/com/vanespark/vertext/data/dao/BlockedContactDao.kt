package com.vanespark.vertext.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vanespark.vertext.data.model.BlockedContact
import kotlinx.coroutines.flow.Flow

/**
 * DAO for BlockedContact entity
 * Provides database operations for managing blocked contacts
 */
@Dao
interface BlockedContactDao {

    /**
     * Insert a new blocked contact
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blockedContact: BlockedContact): Long

    /**
     * Delete a blocked contact
     */
    @Delete
    suspend fun delete(blockedContact: BlockedContact)

    /**
     * Delete blocked contact by phone number
     */
    @Query("DELETE FROM blocked_contacts WHERE phone_number = :phoneNumber")
    suspend fun deleteByPhoneNumber(phoneNumber: String)

    /**
     * Get all blocked contacts ordered by blocked date (most recent first)
     */
    @Query("SELECT * FROM blocked_contacts ORDER BY blocked_date DESC")
    fun getAllBlockedContacts(): Flow<List<BlockedContact>>

    /**
     * Get all blocked contacts as a snapshot (not reactive)
     */
    @Query("SELECT * FROM blocked_contacts ORDER BY blocked_date DESC")
    suspend fun getAllBlockedContactsSnapshot(): List<BlockedContact>

    /**
     * Check if a phone number is blocked
     */
    @Query("SELECT EXISTS(SELECT 1 FROM blocked_contacts WHERE phone_number = :phoneNumber)")
    suspend fun isPhoneNumberBlocked(phoneNumber: String): Boolean

    /**
     * Get blocked contact by phone number
     */
    @Query("SELECT * FROM blocked_contacts WHERE phone_number = :phoneNumber LIMIT 1")
    suspend fun getByPhoneNumber(phoneNumber: String): BlockedContact?

    /**
     * Get count of blocked contacts
     */
    @Query("SELECT COUNT(*) FROM blocked_contacts")
    suspend fun getBlockedCount(): Int

    /**
     * Search blocked contacts by name or phone number
     */
    @Query("""
        SELECT * FROM blocked_contacts
        WHERE contact_name LIKE '%' || :query || '%'
        OR phone_number LIKE '%' || :query || '%'
        ORDER BY blocked_date DESC
    """)
    suspend fun searchBlockedContacts(query: String): List<BlockedContact>

    /**
     * Delete all blocked contacts
     */
    @Query("DELETE FROM blocked_contacts")
    suspend fun deleteAll()
}
