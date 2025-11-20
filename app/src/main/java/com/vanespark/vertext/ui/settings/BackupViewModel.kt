package com.vanespark.vertext.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.domain.service.BackupMetadata
import com.vanespark.vertext.domain.service.BackupProgress
import com.vanespark.vertext.domain.service.BackupService
import com.vanespark.vertext.domain.service.RestoreProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for backup screen
 */
data class BackupUiState(
    val isLoading: Boolean = false,
    val backups: List<BackupMetadata> = emptyList(),
    val backupProgress: BackupProgressUi? = null,
    val restoreProgress: RestoreProgressUi? = null,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * Backup progress UI state
 */
data class BackupProgressUi(
    val progress: Int,
    val message: String
)

/**
 * Restore progress UI state
 */
data class RestoreProgressUi(
    val progress: Int,
    val message: String
)

/**
 * ViewModel for backup management
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupService: BackupService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        loadBackups()
    }

    /**
     * Load available backups
     */
    fun loadBackups() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            backupService.getBackups()
                .onSuccess { backups ->
                    Timber.d("Loaded ${backups.size} backups")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            backups = backups,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to load backups")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load backups"
                        )
                    }
                }
        }
    }

    /**
     * Create new backup
     */
    fun createBackup() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    backupProgress = BackupProgressUi(0, "Starting backup..."),
                    error = null,
                    successMessage = null
                )
            }

            backupService.createBackup().collect { progress ->
                when (progress) {
                    is BackupProgress.InProgress -> {
                        Timber.d("Backup progress: ${progress.progress}% - ${progress.message}")
                        _uiState.update {
                            it.copy(
                                backupProgress = BackupProgressUi(
                                    progress = progress.progress,
                                    message = progress.message
                                )
                            )
                        }
                    }

                    is BackupProgress.Success -> {
                        Timber.d("Backup completed: ${progress.backup.filename}")
                        _uiState.update {
                            it.copy(
                                backupProgress = null,
                                successMessage = "Backup created successfully",
                                backups = listOf(progress.backup) + it.backups
                            )
                        }
                    }

                    is BackupProgress.Error -> {
                        Timber.e("Backup failed: ${progress.message}")
                        _uiState.update {
                            it.copy(
                                backupProgress = null,
                                error = progress.message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Restore from backup
     */
    fun restoreBackup(backupPath: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    restoreProgress = RestoreProgressUi(0, "Starting restore..."),
                    error = null,
                    successMessage = null
                )
            }

            backupService.restoreBackup(backupPath).collect { progress ->
                when (progress) {
                    is RestoreProgress.InProgress -> {
                        Timber.d("Restore progress: ${progress.progress}% - ${progress.message}")
                        _uiState.update {
                            it.copy(
                                restoreProgress = RestoreProgressUi(
                                    progress = progress.progress,
                                    message = progress.message
                                )
                            )
                        }
                    }

                    is RestoreProgress.Success -> {
                        Timber.d("Restore completed successfully")
                        _uiState.update {
                            it.copy(
                                restoreProgress = null,
                                successMessage = "Restore completed successfully. Please restart the app."
                            )
                        }
                    }

                    is RestoreProgress.Error -> {
                        Timber.e("Restore failed: ${progress.message}")
                        _uiState.update {
                            it.copy(
                                restoreProgress = null,
                                error = progress.message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Delete backup
     */
    fun deleteBackup(backupPath: String) {
        viewModelScope.launch {
            backupService.deleteBackup(backupPath)
                .onSuccess {
                    Timber.d("Backup deleted: $backupPath")
                    _uiState.update {
                        it.copy(
                            backups = it.backups.filter { backup -> backup.path != backupPath },
                            successMessage = "Backup deleted"
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to delete backup")
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to delete backup")
                    }
                }
        }
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
