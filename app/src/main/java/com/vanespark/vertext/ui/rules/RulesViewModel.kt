package com.vanespark.vertext.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.data.model.Rule
import com.vanespark.vertext.data.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for rules screen
 */
data class RulesUiState(
    val isLoading: Boolean = false,
    val rules: List<Rule> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val selectedRule: Rule? = null,
    val showEditor: Boolean = false
)

/**
 * ViewModel for automation rules management
 */
@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    init {
        loadRules()
    }

    /**
     * Load all rules
     */
    fun loadRules() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            ruleRepository.getAllRules().collect { rules ->
                Timber.d("Loaded ${rules.size} rules")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        rules = rules,
                        error = null
                    )
                }
            }
        }
    }

    /**
     * Create new rule
     */
    fun createRule(rule: Rule) {
        viewModelScope.launch {
            try {
                val ruleId = ruleRepository.createRule(rule)
                Timber.d("Created rule with ID: $ruleId")
                _uiState.update {
                    it.copy(
                        successMessage = "Rule created successfully",
                        showEditor = false,
                        selectedRule = null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create rule")
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to create rule")
                }
            }
        }
    }

    /**
     * Update existing rule
     */
    fun updateRule(rule: Rule) {
        viewModelScope.launch {
            try {
                ruleRepository.updateRule(rule)
                Timber.d("Updated rule: ${rule.id}")
                _uiState.update {
                    it.copy(
                        successMessage = "Rule updated successfully",
                        showEditor = false,
                        selectedRule = null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update rule")
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update rule")
                }
            }
        }
    }

    /**
     * Delete rule
     */
    fun deleteRule(ruleId: Long) {
        viewModelScope.launch {
            try {
                ruleRepository.deleteRuleById(ruleId)
                Timber.d("Deleted rule: $ruleId")
                _uiState.update {
                    it.copy(successMessage = "Rule deleted successfully")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete rule")
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to delete rule")
                }
            }
        }
    }

    /**
     * Toggle rule enabled/disabled
     */
    fun toggleRuleEnabled(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            try {
                ruleRepository.setRuleEnabled(ruleId, enabled)
                Timber.d("Toggled rule $ruleId enabled: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle rule")
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to toggle rule")
                }
            }
        }
    }

    /**
     * Show rule editor for new rule
     */
    fun showNewRuleEditor() {
        _uiState.update {
            it.copy(
                showEditor = true,
                selectedRule = null
            )
        }
    }

    /**
     * Show rule editor for existing rule
     */
    fun showEditRuleEditor(rule: Rule) {
        _uiState.update {
            it.copy(
                showEditor = true,
                selectedRule = rule
            )
        }
    }

    /**
     * Hide rule editor
     */
    fun hideEditor() {
        _uiState.update {
            it.copy(
                showEditor = false,
                selectedRule = null
            )
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

    /**
     * Get rule statistics
     */
    suspend fun getRuleStats(): RuleStats {
        val totalRules = ruleRepository.getRulesCount()
        val enabledRules = ruleRepository.getEnabledRulesCount()
        return RuleStats(
            totalRules = totalRules,
            enabledRules = enabledRules,
            disabledRules = totalRules - enabledRules
        )
    }
}

/**
 * Rule statistics
 */
data class RuleStats(
    val totalRules: Int,
    val enabledRules: Int,
    val disabledRules: Int
)
