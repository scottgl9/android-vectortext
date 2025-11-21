package com.vanespark.vertext.ui.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.data.model.Contact
import com.vanespark.vertext.data.repository.ContactRepository
import com.vanespark.vertext.domain.service.ContactInsights
import com.vanespark.vertext.domain.service.ContactInsightsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Contact Profile Screen
 * Manages contact details and insights
 */
@HiltViewModel
class ContactProfileViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val insightsService: ContactInsightsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactProfileUiState())
    val uiState: StateFlow<ContactProfileUiState> = _uiState.asStateFlow()

    /**
     * Load contact profile and insights
     */
    fun loadContactProfile(contactId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Load contact
                val contact = contactRepository.getContactById(contactId)
                if (contact == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Contact not found"
                        )
                    }
                    return@launch
                }

                // Generate insights
                val insights = insightsService.generateInsights(contact)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        contact = contact,
                        insights = insights,
                        error = null
                    )
                }

                Timber.d("Loaded profile for contact: ${contact.name}")
            } catch (e: Exception) {
                Timber.e(e, "Error loading contact profile")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load contact profile: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Load contact by phone number
     */
    fun loadContactByPhone(phone: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Load or create contact
                val contact = contactRepository.getContactByPhone(phone)
                    ?: contactRepository.getOrCreateContact(phone, phone)

                // Generate insights
                val insights = insightsService.generateInsights(contact)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        contact = contact,
                        insights = insights,
                        error = null
                    )
                }

                Timber.d("Loaded profile for phone: $phone")
            } catch (e: Exception) {
                Timber.e(e, "Error loading contact by phone")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load contact: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Update contact notes
     */
    fun updateNotes(notes: String) {
        viewModelScope.launch {
            try {
                val contact = _uiState.value.contact ?: return@launch

                contactRepository.updateContactNotes(contact.id, notes)

                // Update local state
                _uiState.update {
                    it.copy(
                        contact = contact.copy(notes = notes),
                        successMessage = "Notes saved"
                    )
                }

                Timber.d("Updated notes for contact: ${contact.name}")
            } catch (e: Exception) {
                Timber.e(e, "Error updating notes")
                _uiState.update {
                    it.copy(error = "Failed to save notes: ${e.message}")
                }
            }
        }
    }

    /**
     * Update contact name
     */
    fun updateContactName(name: String) {
        viewModelScope.launch {
            try {
                val contact = _uiState.value.contact ?: return@launch

                contactRepository.updateContactName(contact.id, name)

                // Update local state
                _uiState.update {
                    it.copy(
                        contact = contact.copy(name = name),
                        successMessage = "Name updated"
                    )
                }

                Timber.d("Updated name for contact: $name")
            } catch (e: Exception) {
                Timber.e(e, "Error updating name")
                _uiState.update {
                    it.copy(error = "Failed to update name: ${e.message}")
                }
            }
        }
    }

    /**
     * Show edit dialog
     */
    fun showEditDialog() {
        _uiState.update { it.copy(showEditDialog = true) }
    }

    /**
     * Hide edit dialog
     */
    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false) }
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
     * Refresh insights
     */
    fun refreshInsights() {
        val contact = _uiState.value.contact ?: return
        loadContactProfile(contact.id)
    }
}

/**
 * UI state for contact profile screen
 */
data class ContactProfileUiState(
    val isLoading: Boolean = false,
    val contact: Contact? = null,
    val insights: ContactInsights? = null,
    val showEditDialog: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)
