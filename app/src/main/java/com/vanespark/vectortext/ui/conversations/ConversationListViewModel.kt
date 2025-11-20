package com.vanespark.vectortext.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vectortext.data.repository.ThreadRepository
import com.vanespark.vectortext.domain.service.MessagingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the conversation list screen
 * Manages conversation list state and user interactions
 */
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val threadRepository: ThreadRepository,
    private val messagingService: MessagingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState(isLoading = true))
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    /**
     * Load conversations from repository
     */
    private fun loadConversations() {
        viewModelScope.launch {
            combine(
                threadRepository.getAllThreads(),
                threadRepository.getTotalUnreadCount()
            ) { threads, unreadCount ->
                threads to (unreadCount ?: 0)
            }
                .catch { e ->
                    Timber.e(e, "Error loading conversations")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { (threads, unreadCount) ->
                    val conversations = threads.map { ConversationUiItem.fromThread(it) }
                    _uiState.update {
                        it.copy(
                            conversations = conversations,
                            isLoading = false,
                            unreadCount = unreadCount,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * Mark a conversation as read
     */
    fun markConversationAsRead(threadId: Long) {
        viewModelScope.launch {
            messagingService.markThreadAsRead(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to mark thread as read")
                    _uiState.update { it.copy(error = "Failed to mark as read") }
                }
        }
    }

    /**
     * Archive a conversation
     */
    fun archiveConversation(threadId: Long) {
        viewModelScope.launch {
            messagingService.archiveThread(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to archive thread")
                    _uiState.update { it.copy(error = "Failed to archive") }
                }
        }
    }

    /**
     * Unarchive a conversation
     */
    fun unarchiveConversation(threadId: Long) {
        viewModelScope.launch {
            messagingService.unarchiveThread(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to unarchive thread")
                    _uiState.update { it.copy(error = "Failed to unarchive") }
                }
        }
    }

    /**
     * Pin a conversation to the top
     */
    fun pinConversation(threadId: Long) {
        viewModelScope.launch {
            messagingService.pinThread(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to pin thread")
                    _uiState.update { it.copy(error = "Failed to pin") }
                }
        }
    }

    /**
     * Unpin a conversation
     */
    fun unpinConversation(threadId: Long) {
        viewModelScope.launch {
            messagingService.unpinThread(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to unpin thread")
                    _uiState.update { it.copy(error = "Failed to unpin") }
                }
        }
    }

    /**
     * Mute a conversation
     */
    fun muteConversation(threadId: Long) {
        viewModelScope.launch {
            messagingService.muteThread(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to mute thread")
                    _uiState.update { it.copy(error = "Failed to mute") }
                }
        }
    }

    /**
     * Unmute a conversation
     */
    fun unmuteConversation(threadId: Long) {
        viewModelScope.launch {
            messagingService.unmuteThread(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to unmute thread")
                    _uiState.update { it.copy(error = "Failed to unmute") }
                }
        }
    }

    /**
     * Delete a conversation
     */
    fun deleteConversation(threadId: Long) {
        viewModelScope.launch {
            messagingService.deleteThread(threadId)
                .onFailure { e ->
                    Timber.e(e, "Failed to delete thread")
                    _uiState.update { it.copy(error = "Failed to delete") }
                }
        }
    }

    /**
     * Toggle conversation selection
     */
    fun toggleConversationSelection(threadId: Long) {
        _uiState.update { state ->
            val newSelection = if (state.selectedConversations.contains(threadId)) {
                state.selectedConversations - threadId
            } else {
                state.selectedConversations + threadId
            }

            state.copy(
                selectedConversations = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }

    /**
     * Clear all selections
     */
    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedConversations = emptySet(),
                isSelectionMode = false
            )
        }
    }

    /**
     * Archive selected conversations
     */
    fun archiveSelected() {
        viewModelScope.launch {
            _uiState.value.selectedConversations.forEach { threadId ->
                messagingService.archiveThread(threadId)
            }
            clearSelection()
        }
    }

    /**
     * Delete selected conversations
     */
    fun deleteSelected() {
        viewModelScope.launch {
            _uiState.value.selectedConversations.forEach { threadId ->
                messagingService.deleteThread(threadId)
            }
            clearSelection()
        }
    }

    /**
     * Mark selected conversations as read
     */
    fun markSelectedAsRead() {
        viewModelScope.launch {
            _uiState.value.selectedConversations.forEach { threadId ->
                messagingService.markThreadAsRead(threadId)
            }
            clearSelection()
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
