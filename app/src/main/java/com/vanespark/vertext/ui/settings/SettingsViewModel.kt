package com.vanespark.vertext.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.vanespark.vertext.data.repository.MessageRepository
import com.vanespark.vertext.data.repository.ThreadRepository
import com.vanespark.vertext.domain.service.ThreadCategorizationService
import com.vanespark.vertext.domain.worker.EmbeddingGenerationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for settings screen
 */
data class SettingsUiState(
    // Appearance
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val useAmoledBlack: Boolean = false,

    // Notifications
    val notificationsEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,

    // Messages
    val showDeliveryReports: Boolean = true,
    val showContactNames: Boolean = true,
    val messageLoadLimit: Int = 100,

    // Search & Indexing
    val embeddedMessageCount: Int = 0,
    val totalMessageCount: Int = 0,
    val indexingProgress: Float? = null, // null when not indexing, 0.0-1.0 when indexing
    val lastIndexedTimestamp: Long? = null,

    // Categorization
    val isCategorizing: Boolean = false,
    val categorizedThreadCount: Int = 0,
    val totalThreadCount: Int = 0,

    // About
    val appVersion: String = "1.0.0",

    // Dialog states
    val showBackupDialog: Boolean = false,
    val showRestoreDialog: Boolean = false
)

/**
 * ViewModel for settings screen
 * Manages user preferences and settings
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageRepository: MessageRepository,
    private val threadRepository: ThreadRepository,
    private val threadCategorizationService: ThreadCategorizationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    init {
        loadSettings()
        loadAppVersion()
        loadIndexingStats()
        loadCategoryStats()
    }

    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

                _uiState.update {
                    it.copy(
                        theme = ThemeMode.valueOf(
                            prefs.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
                        ),
                        useAmoledBlack = prefs.getBoolean("use_amoled_black", false),
                        notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
                        vibrateEnabled = prefs.getBoolean("vibrate_enabled", true),
                        showDeliveryReports = prefs.getBoolean("show_delivery_reports", true),
                        showContactNames = prefs.getBoolean("show_contact_names", true),
                        messageLoadLimit = prefs.getInt("message_load_limit", 100)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load settings")
            }
        }
    }

    /**
     * Load app version from BuildConfig
     */
    private fun loadAppVersion() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            _uiState.update { it.copy(appVersion = packageInfo.versionName ?: "1.0.0") }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load app version")
        }
    }

    /**
     * Save a setting to SharedPreferences
     */
    private fun saveSetting(key: String, value: Any) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    when (value) {
                        is Boolean -> putBoolean(key, value)
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Float -> putFloat(key, value)
                        is Long -> putLong(key, value)
                    }
                    apply()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save setting: $key")
            }
        }
    }

    /**
     * Update theme setting
     */
    fun updateTheme(theme: ThemeMode) {
        _uiState.update { it.copy(theme = theme) }
        saveSetting("theme", theme.name)
        Timber.d("Theme updated to: $theme")
    }

    /**
     * Update AMOLED black setting
     */
    fun updateAmoledBlack(enabled: Boolean) {
        _uiState.update { it.copy(useAmoledBlack = enabled) }
        saveSetting("use_amoled_black", enabled)
        Timber.d("AMOLED black: $enabled")
    }

    /**
     * Update notifications enabled setting
     */
    fun updateNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        saveSetting("notifications_enabled", enabled)
        Timber.d("Notifications: $enabled")
    }

    /**
     * Update vibrate setting
     */
    fun updateVibrate(enabled: Boolean) {
        _uiState.update { it.copy(vibrateEnabled = enabled) }
        saveSetting("vibrate_enabled", enabled)
        Timber.d("Vibrate: $enabled")
    }

    /**
     * Update show delivery reports setting
     */
    fun updateDeliveryReports(enabled: Boolean) {
        _uiState.update { it.copy(showDeliveryReports = enabled) }
        saveSetting("show_delivery_reports", enabled)
        Timber.d("Show delivery reports: $enabled")
    }

    /**
     * Update show contact names setting
     */
    fun updateShowContactNames(enabled: Boolean) {
        _uiState.update { it.copy(showContactNames = enabled) }
        saveSetting("show_contact_names", enabled)
        Timber.d("Show contact names: $enabled")
    }

    /**
     * Update message load limit setting
     */
    fun updateMessageLoadLimit(limit: Int) {
        _uiState.update { it.copy(messageLoadLimit = limit) }
        saveSetting("message_load_limit", limit)
        Timber.d("Message load limit: $limit")
    }

    /**
     * Backup messages
     */
    fun backupMessages() {
        _uiState.update { it.copy(showBackupDialog = true) }
    }

    /**
     * Confirm backup
     */
    fun confirmBackup() {
        viewModelScope.launch {
            try {
                Timber.d("Starting message backup...")
                // TODO: Implement actual backup logic
                _uiState.update { it.copy(showBackupDialog = false) }
                Timber.d("Backup completed")
            } catch (e: Exception) {
                Timber.e(e, "Backup failed")
            }
        }
    }

    /**
     * Dismiss backup dialog
     */
    fun dismissBackupDialog() {
        _uiState.update { it.copy(showBackupDialog = false) }
    }

    /**
     * Restore messages
     */
    fun restoreMessages() {
        _uiState.update { it.copy(showRestoreDialog = true) }
    }

    /**
     * Confirm restore
     */
    fun confirmRestore() {
        viewModelScope.launch {
            try {
                Timber.d("Starting message restore...")
                // TODO: Implement actual restore logic
                _uiState.update { it.copy(showRestoreDialog = false) }
                Timber.d("Restore completed")
            } catch (e: Exception) {
                Timber.e(e, "Restore failed")
            }
        }
    }

    /**
     * Dismiss restore dialog
     */
    fun dismissRestoreDialog() {
        _uiState.update { it.copy(showRestoreDialog = false) }
    }

    /**
     * Clear cache
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                Timber.d("Clearing cache...")
                context.cacheDir.deleteRecursively()
                Timber.d("Cache cleared")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear cache")
            }
        }
    }

    /**
     * Load indexing statistics
     */
    private fun loadIndexingStats() {
        viewModelScope.launch {
            try {
                val embeddedCount = messageRepository.getEmbeddedMessageCount()
                val totalCount = messageRepository.getTotalMessageCount()

                // Load last indexed timestamp from SharedPreferences
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val lastIndexed = prefs.getLong("last_indexed_timestamp", 0L)

                _uiState.update {
                    it.copy(
                        embeddedMessageCount = embeddedCount,
                        totalMessageCount = totalCount,
                        lastIndexedTimestamp = if (lastIndexed > 0) lastIndexed else null
                    )
                }

                Timber.d("Indexing stats loaded: $embeddedCount/$totalCount messages indexed")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load indexing stats")
            }
        }
    }

    /**
     * Refresh indexing statistics
     */
    fun refreshIndexingStats() {
        loadIndexingStats()
    }

    /**
     * Trigger re-indexing of all messages
     */
    fun triggerReindexing() {
        viewModelScope.launch {
            try {
                Timber.d("Triggering re-indexing of all messages")

                val workRequest = OneTimeWorkRequestBuilder<EmbeddingGenerationWorker>()
                    .addTag("embedding_generation")
                    .build()

                workManager.enqueue(workRequest)

                // Observe work progress
                workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                    when (workInfo?.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress
                            val processedCount = progress.getInt("processed", 0)
                            val totalCount = progress.getInt("total", 1)
                            val progressPercent = if (totalCount > 0) {
                                processedCount.toFloat() / totalCount
                            } else 0f

                            _uiState.update {
                                it.copy(indexingProgress = progressPercent)
                            }
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            // Update last indexed timestamp
                            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                            prefs.edit().putLong("last_indexed_timestamp", System.currentTimeMillis()).apply()

                            _uiState.update {
                                it.copy(
                                    indexingProgress = null,
                                    lastIndexedTimestamp = System.currentTimeMillis()
                                )
                            }
                            loadIndexingStats()
                            Timber.d("Re-indexing completed successfully")
                        }
                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            _uiState.update {
                                it.copy(indexingProgress = null)
                            }
                            Timber.e("Re-indexing failed or was cancelled")
                        }
                        else -> {
                            // ENQUEUED, BLOCKED - do nothing yet
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to trigger re-indexing")
                _uiState.update {
                    it.copy(indexingProgress = null)
                }
            }
        }
    }

    /**
     * Load category statistics
     */
    private fun loadCategoryStats() {
        viewModelScope.launch {
            try {
                val allThreads = threadRepository.getAllThreadsSnapshot()
                val categorizedThreads = allThreads.count { !it.category.isNullOrEmpty() }

                _uiState.update {
                    it.copy(
                        categorizedThreadCount = categorizedThreads,
                        totalThreadCount = allThreads.size
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load category stats")
            }
        }
    }

    /**
     * Trigger thread categorization
     */
    fun triggerCategorization() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isCategorizing = true) }

                val categorizedCount = threadCategorizationService.categorizeAllThreads()
                Timber.d("Categorized $categorizedCount threads")

                // Reload stats after categorization
                loadCategoryStats()

                _uiState.update { it.copy(isCategorizing = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to categorize threads")
                _uiState.update { it.copy(isCategorizing = false) }
            }
        }
    }

    /**
     * Refresh category stats
     */
    fun refreshCategoryStats() {
        loadCategoryStats()
    }

    /**
     * Open privacy policy
     */
    fun openPrivacyPolicy() {
        Timber.d("Opening privacy policy")
        // TODO: Implement privacy policy view
    }

    /**
     * Open terms of service
     */
    fun openTermsOfService() {
        Timber.d("Opening terms of service")
        // TODO: Implement terms of service view
    }
}
