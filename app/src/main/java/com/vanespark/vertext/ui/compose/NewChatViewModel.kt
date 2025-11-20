package com.vanespark.vertext.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.domain.service.ContactInfo
import com.vanespark.vertext.domain.service.ContactService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for new chat screen
 * Manages contact loading and search
 */
@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val contactService: ContactService
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<ContactInfo>>(emptyList())
    val contacts: StateFlow<List<ContactInfo>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Load all contacts
     */
    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allContacts = contactService.getAllContacts()
                _contacts.value = allContacts
                Timber.d("Loaded ${allContacts.size} contacts")
            } catch (e: Exception) {
                Timber.e(e, "Error loading contacts")
                _contacts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Search contacts by query
     */
    fun searchContacts(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = contactService.searchContacts(query)
                _contacts.value = results
                Timber.d("Found ${results.size} contacts for query: $query")
            } catch (e: Exception) {
                Timber.e(e, "Error searching contacts")
                _contacts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
