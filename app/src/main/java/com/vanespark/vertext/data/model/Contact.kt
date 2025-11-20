package com.vanespark.vertext.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Contact entity for storing contact information
 */
@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["phone"], unique = true),
        Index(value = ["name"])
    ]
)
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Display name of the contact */
    val name: String,

    /** Phone number (normalized) */
    val phone: String,

    /** URI to contact's avatar/photo */
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String? = null,

    /** Contact lookup key from Android Contacts Provider */
    @ColumnInfo(name = "lookup_key")
    val lookupKey: String? = null,

    /** Notes about this contact (user-editable) */
    val notes: String? = null,

    /** Last time contact info was synced */
    @ColumnInfo(name = "last_synced")
    val lastSynced: Long = System.currentTimeMillis()
)
