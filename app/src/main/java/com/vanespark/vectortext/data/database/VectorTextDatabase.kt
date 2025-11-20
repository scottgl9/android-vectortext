package com.vanespark.vectortext.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vanespark.vectortext.data.dao.ContactDao
import com.vanespark.vectortext.data.dao.MessageDao
import com.vanespark.vectortext.data.dao.ThreadDao
import com.vanespark.vectortext.data.model.Contact
import com.vanespark.vectortext.data.model.Message
import com.vanespark.vectortext.data.model.Thread

/**
 * Main Room database for VectorText
 * Stores messages, threads, and contacts with support for semantic search
 */
@Database(
    entities = [
        Message::class,
        Thread::class,
        Contact::class
    ],
    version = 1,
    exportSchema = true
)
abstract class VectorTextDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun threadDao(): ThreadDao
    abstract fun contactDao(): ContactDao

    companion object {
        const val DATABASE_NAME = "vectortext.db"
    }
}
