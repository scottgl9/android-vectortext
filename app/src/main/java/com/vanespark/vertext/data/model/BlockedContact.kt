package com.vanespark.vertext.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a blocked contact
 * Messages from blocked contacts are filtered and not shown in conversations
 */
@Entity(
    tableName = "blocked_contacts",
    indices = [
        Index(value = ["phone_number"], unique = true),
        Index(value = ["blocked_date"])
    ]
)
data class BlockedContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Phone number of the blocked contact */
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    /** Display name of the contact (if available) */
    @ColumnInfo(name = "contact_name")
    val contactName: String? = null,

    /** Timestamp when contact was blocked */
    @ColumnInfo(name = "blocked_date")
    val blockedDate: Long = System.currentTimeMillis(),

    /** Optional reason for blocking */
    val reason: String? = null
)
