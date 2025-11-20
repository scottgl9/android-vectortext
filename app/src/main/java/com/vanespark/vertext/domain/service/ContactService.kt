package com.vanespark.vertext.domain.service

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for interacting with Android Contacts
 * Provides contact name resolution and contact queries
 */
@Singleton
class ContactService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Get contact name for a phone number
     * Returns null if contact not found
     */
    suspend fun getContactName(phoneNumber: String): String? = withContext(Dispatchers.IO) {
        try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(phoneNumber)
                .build()

            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME
            )

            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return@withContext cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error looking up contact name for $phoneNumber")
        }
        return@withContext null
    }

    /**
     * Get contact photo URI for a phone number
     * Returns null if contact not found or has no photo
     */
    suspend fun getContactPhotoUri(phoneNumber: String): String? = withContext(Dispatchers.IO) {
        try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(phoneNumber)
                .build()

            val projection = arrayOf(
                ContactsContract.PhoneLookup.PHOTO_URI
            )

            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val photoIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                    if (photoIndex >= 0) {
                        return@withContext cursor.getString(photoIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error looking up contact photo for $phoneNumber")
        }
        return@withContext null
    }

    /**
     * Get all contacts with phone numbers
     * Returns list of ContactInfo
     */
    suspend fun getAllContacts(): List<ContactInfo> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactInfo>()

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (cursor.moveToNext()) {
                    val id = if (idIndex >= 0) cursor.getString(idIndex) else continue
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Unknown"
                    val number = if (numberIndex >= 0) cursor.getString(numberIndex) else continue
                    val photoUri = if (photoIndex >= 0) cursor.getString(photoIndex) else null

                    contacts.add(
                        ContactInfo(
                            id = id,
                            name = name,
                            phoneNumber = normalizePhoneNumber(number),
                            photoUri = photoUri
                        )
                    )
                }
            }
            Timber.d("Loaded ${contacts.size} contacts")
        } catch (e: Exception) {
            Timber.e(e, "Error loading contacts")
        }

        return@withContext contacts
    }

    /**
     * Search contacts by name or phone number
     */
    suspend fun searchContacts(query: String): List<ContactInfo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext emptyList()
        }

        val contacts = mutableListOf<ContactInfo>()

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR " +
                    "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            val selectionArgs = arrayOf("%$query%", "%$query%")

            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (cursor.moveToNext()) {
                    val id = if (idIndex >= 0) cursor.getString(idIndex) else continue
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Unknown"
                    val number = if (numberIndex >= 0) cursor.getString(numberIndex) else continue
                    val photoUri = if (photoIndex >= 0) cursor.getString(photoIndex) else null

                    contacts.add(
                        ContactInfo(
                            id = id,
                            name = name,
                            phoneNumber = normalizePhoneNumber(number),
                            photoUri = photoUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error searching contacts")
        }

        return@withContext contacts
    }

    /**
     * Normalize phone number for comparison
     * Removes spaces, dashes, parentheses
     */
    private fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[\\s\\-\\(\\)]"), "")
    }
}

/**
 * Contact information data class
 */
data class ContactInfo(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)
