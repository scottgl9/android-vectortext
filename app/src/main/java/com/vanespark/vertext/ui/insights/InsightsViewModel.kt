package com.vanespark.vertext.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.domain.service.InsightsService
import com.vanespark.vertext.domain.service.MessagingInsights
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for insights screen
 */
data class InsightsUiState(
    val isLoading: Boolean = true,
    val insights: MessagingInsights? = null,
    val error: String? = null
)

/**
 * ViewModel for insights screen
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightsService: InsightsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    /**
     * Load messaging insights
     */
    fun loadInsights() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            insightsService.getInsights()
                .onSuccess { insights ->
                    Timber.d("Loaded insights: ${insights.totalMessages} messages")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            insights = insights,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to load insights")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load insights"
                        )
                    }
                }
        }
    }

    /**
     * Refresh insights
     */
    fun refresh() {
        loadInsights()
    }
}
