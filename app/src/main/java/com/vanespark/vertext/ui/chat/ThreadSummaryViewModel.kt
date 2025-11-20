package com.vanespark.vertext.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.domain.service.ThreadSummary
import com.vanespark.vertext.domain.service.ThreadSummaryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for thread summary
 */
data class ThreadSummaryUiState(
    val isLoading: Boolean = false,
    val summary: ThreadSummary? = null,
    val error: String? = null,
    val isVisible: Boolean = false
)

/**
 * ViewModel for thread summary
 */
@HiltViewModel
class ThreadSummaryViewModel @Inject constructor(
    private val threadSummaryService: ThreadSummaryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThreadSummaryUiState())
    val uiState: StateFlow<ThreadSummaryUiState> = _uiState.asStateFlow()

    /**
     * Generate summary for a thread
     */
    fun generateSummary(threadId: Long) {
        viewModelScope.launch {
            try {
                Timber.d("Generating summary for thread $threadId")

                _uiState.update {
                    it.copy(
                        isLoading = true,
                        error = null,
                        isVisible = true
                    )
                }

                val result = threadSummaryService.generateSummary(
                    threadId = threadId,
                    maxMessages = 1000,
                    includeExcerpts = true
                )

                result.fold(
                    onSuccess = { summary ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                summary = summary,
                                error = null
                            )
                        }
                        Timber.d("Summary generated successfully")
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to generate summary"
                            )
                        }
                        Timber.e(error, "Failed to generate summary")
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An error occurred"
                    )
                }
                Timber.e(e, "Error generating summary")
            }
        }
    }

    /**
     * Dismiss the summary bottom sheet
     */
    fun dismissSummary() {
        _uiState.update {
            it.copy(isVisible = false)
        }
    }

    /**
     * Retry generating summary after error
     */
    fun retry(threadId: Long) {
        generateSummary(threadId)
    }
}
