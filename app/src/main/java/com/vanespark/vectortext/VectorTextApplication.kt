package com.vanespark.vectortext

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Main Application class for VectorText
 * Initializes Hilt dependency injection and other app-wide services
 */
@HiltAndroidApp
class VectorTextApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workManagerConfiguration: Configuration

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("VectorText Application started")
    }

    override val workManagerConfiguration: Configuration
        get() = workManagerConfiguration
}
