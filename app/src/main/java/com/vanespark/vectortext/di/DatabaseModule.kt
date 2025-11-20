package com.vanespark.vectortext.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module providing database-related dependencies
 * Will be populated in Phase 1 with Room database setup
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // TODO: Add Room database providers in Phase 1
}
