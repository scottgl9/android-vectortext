package com.vanespark.vertext.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.domain.service.SmsSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for sync screen
 */
data class SyncUiState(
    val isComplete: Boolean = false,
    val isFailed: Boolean = false,
    val progress: Float = 0.0f,
    val itemsProcessed: Int = 0,
    val totalItems: Int = 0,
    val statusMessage: String = "Preparing to sync...",
    val currentStep: SmsSyncService.SyncStep = SmsSyncService.SyncStep.READING_THREADS
)

/**
 * ViewModel for SMS sync screen
 * Manages sync progress and state
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val smsSyncService: SmsSyncService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    /**
     * Start the sync process
     */
    fun startSync() {
        // Reset state
        _uiState.update {
            SyncUiState(
                statusMessage = "Starting sync..."
            )
        }

        viewModelScope.launch {
            smsSyncService.performFullSync()
                .catch { e ->
                    Timber.e(e, "Sync flow error")
                    _uiState.update {
                        it.copy(
                            isFailed = true,
                            statusMessage = "Sync failed: ${e.message}"
                        )
                    }
                }
                .collect { progress ->
                    when (progress.currentStep) {
                        SmsSyncService.SyncStep.READING_THREADS -> {
                            _uiState.update {
                                it.copy(
                                    progress = progress.progress,
                                    statusMessage = progress.message,
                                    currentStep = progress.currentStep
                                )
                            }
                        }

                        SmsSyncService.SyncStep.SYNCING_THREADS -> {
                            _uiState.update {
                                it.copy(
                                    progress = progress.progress,
                                    itemsProcessed = progress.itemsProcessed,
                                    totalItems = progress.totalItems,
                                    statusMessage = progress.message,
                                    currentStep = progress.currentStep
                                )
                            }
                        }

                        SmsSyncService.SyncStep.READING_MESSAGES -> {
                            _uiState.update {
                                it.copy(
                                    progress = progress.progress,
                                    statusMessage = progress.message,
                                    currentStep = progress.currentStep
                                )
                            }
                        }

                        SmsSyncService.SyncStep.SYNCING_MESSAGES -> {
                            _uiState.update {
                                it.copy(
                                    progress = progress.progress,
                                    itemsProcessed = progress.itemsProcessed,
                                    totalItems = progress.totalItems,
                                    statusMessage = progress.message,
                                    currentStep = progress.currentStep
                                )
                            }
                        }

                        SmsSyncService.SyncStep.COMPLETED -> {
                            _uiState.update {
                                it.copy(
                                    isComplete = true,
                                    progress = 1.0f,
                                    statusMessage = progress.message,
                                    currentStep = progress.currentStep
                                )
                            }
                            Timber.d("Sync completed successfully")
                        }

                        SmsSyncService.SyncStep.FAILED -> {
                            _uiState.update {
                                it.copy(
                                    isFailed = true,
                                    statusMessage = progress.message,
                                    currentStep = progress.currentStep
                                )
                            }
                            Timber.e("Sync failed")
                        }
                    }
                }
        }
    }
}
