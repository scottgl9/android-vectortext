package com.vanespark.vertext.ui.blocked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.data.model.BlockedContact
import com.vanespark.vertext.data.repository.BlockedContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for blocked contacts screen
 */
data class BlockedContactsUiState(
    val blockedContacts: List<BlockedContact> = emptyList(),
    val isLoading: Boolean = false,
    val selectedContacts: Set<String> = emptySet(), // Phone numbers
    val isSelectionMode: Boolean = false
)

/**
 * ViewModel for blocked contacts screen
 * Manages blocked contacts list and selection state
 */
@HiltViewModel
class BlockedContactsViewModel @Inject constructor(
    private val blockedContactRepository: BlockedContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedContactsUiState())
    val uiState: StateFlow<BlockedContactsUiState> = _uiState.asStateFlow()

    init {
        loadBlockedContacts()
    }

    /**
     * Load blocked contacts from repository
     */
    private fun loadBlockedContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                blockedContactRepository.getAllBlockedContacts().collect { contacts ->
                    _uiState.update {
                        it.copy(
                            blockedContacts = contacts,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load blocked contacts")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        blockedContacts = emptyList()
                    )
                }
            }
        }
    }

    /**
     * Toggle selection for a contact
     */
    fun toggleSelection(phoneNumber: String) {
        _uiState.update { state ->
            val selectedContacts = state.selectedContacts.toMutableSet()

            if (selectedContacts.contains(phoneNumber)) {
                selectedContacts.remove(phoneNumber)
            } else {
                selectedContacts.add(phoneNumber)
            }

            state.copy(
                selectedContacts = selectedContacts,
                isSelectionMode = selectedContacts.isNotEmpty()
            )
        }
    }

    /**
     * Clear all selections
     */
    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedContacts = emptySet(),
                isSelectionMode = false
            )
        }
    }

    /**
     * Unblock selected contacts
     */
    fun unblockSelected() {
        viewModelScope.launch {
            val phoneNumbers = _uiState.value.selectedContacts.toList()

            try {
                phoneNumbers.forEach { phoneNumber ->
                    blockedContactRepository.unblockContact(phoneNumber)
                    Timber.d("Unblocked contact: $phoneNumber")
                }

                // Clear selection after unblocking
                clearSelection()
            } catch (e: Exception) {
                Timber.e(e, "Failed to unblock contacts")
            }
        }
    }

    /**
     * Unblock a single contact
     */
    fun unblockContact(phoneNumber: String) {
        viewModelScope.launch {
            try {
                blockedContactRepository.unblockContact(phoneNumber)
                Timber.d("Unblocked contact: $phoneNumber")

                // Remove from selection if it was selected
                _uiState.update { state ->
                    val selectedContacts = state.selectedContacts.toMutableSet()
                    selectedContacts.remove(phoneNumber)

                    state.copy(
                        selectedContacts = selectedContacts,
                        isSelectionMode = selectedContacts.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to unblock contact")
            }
        }
    }
}
