package com.vanespark.vertext.di

import android.content.Context
import androidx.room.Room
import com.vanespark.vertext.data.dao.ContactDao
import com.vanespark.vertext.data.dao.MessageDao
import com.vanespark.vertext.data.dao.ThreadDao
import com.vanespark.vertext.data.database.VectorTextDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVectorTextDatabase(
        @ApplicationContext context: Context
    ): VectorTextDatabase {
        return Room.databaseBuilder(
            context,
            VectorTextDatabase::class.java,
            VectorTextDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development; remove in production
            .build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: VectorTextDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideThreadDao(database: VectorTextDatabase): ThreadDao {
        return database.threadDao()
    }

    @Provides
    @Singleton
    fun provideContactDao(database: VectorTextDatabase): ContactDao {
        return database.contactDao()
    }
}
