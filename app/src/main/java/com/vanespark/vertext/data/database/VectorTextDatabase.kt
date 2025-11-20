package com.vanespark.vertext.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vanespark.vertext.data.dao.BlockedContactDao
import com.vanespark.vertext.data.dao.ContactDao
import com.vanespark.vertext.data.dao.MessageDao
import com.vanespark.vertext.data.dao.RuleDao
import com.vanespark.vertext.data.dao.ThreadDao
import com.vanespark.vertext.data.model.BlockedContact
import com.vanespark.vertext.data.model.Contact
import com.vanespark.vertext.data.model.Message
import com.vanespark.vertext.data.model.Rule
import com.vanespark.vertext.data.model.RuleTypeConverters
import com.vanespark.vertext.data.model.Thread

/**
 * Main Room database for VectorText
 * Stores messages, threads, contacts, blocked contacts, and automation rules with support for semantic search
 */
@Database(
    entities = [
        Message::class,
        Thread::class,
        Contact::class,
        BlockedContact::class,
        Rule::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(RuleTypeConverters::class)
abstract class VectorTextDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun threadDao(): ThreadDao
    abstract fun contactDao(): ContactDao
    abstract fun blockedContactDao(): BlockedContactDao
    abstract fun ruleDao(): RuleDao

    companion object {
        const val DATABASE_NAME = "vectortext.db"
    }
}
