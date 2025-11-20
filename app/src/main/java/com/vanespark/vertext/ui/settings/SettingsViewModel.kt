package com.vanespark.vertext.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadAppVersion()
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
                        showContactNames = prefs.getBoolean("show_contact_names", true)
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
