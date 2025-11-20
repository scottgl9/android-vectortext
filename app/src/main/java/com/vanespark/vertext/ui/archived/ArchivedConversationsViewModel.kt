package com.vanespark.vertext.ui.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanespark.vertext.data.repository.ThreadRepository
import com.vanespark.vertext.ui.conversations.ConversationUiItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for archived conversations screen
 */
data class ArchivedConversationsUiState(
    val conversations: List<ConversationUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedConversations: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

/**
 * ViewModel for archived conversations screen
 * Manages archived conversations list and selection state
 */
@HiltViewModel
class ArchivedConversationsViewModel @Inject constructor(
    private val threadRepository: ThreadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchivedConversationsUiState())
    val uiState: StateFlow<ArchivedConversationsUiState> = _uiState.asStateFlow()

    init {
        loadArchivedConversations()
    }

    /**
     * Load archived conversations from repository
     */
    private fun loadArchivedConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                threadRepository.getArchivedThreads().collect { threads ->
                    val conversations = threads.map { thread ->
                        ConversationUiItem.fromThread(thread)
                    }

                    _uiState.update {
                        it.copy(
                            conversations = conversations,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load archived conversations")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        conversations = emptyList()
                    )
                }
            }
        }
    }

    /**
     * Toggle selection for a conversation
     * Enables selection mode if first item selected
     */
    fun toggleSelection(threadId: Long) {
        _uiState.update { state ->
            val selectedConversations = state.selectedConversations.toMutableSet()

            if (selectedConversations.contains(threadId)) {
                selectedConversations.remove(threadId)
            } else {
                selectedConversations.add(threadId)
            }

            state.copy(
                selectedConversations = selectedConversations,
                isSelectionMode = selectedConversations.isNotEmpty()
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
     * Unarchive selected conversations
     */
    fun unarchiveSelected() {
        viewModelScope.launch {
            val threadIds = _uiState.value.selectedConversations.toList()

            try {
                threadIds.forEach { threadId ->
                    threadRepository.unarchiveThread(threadId)
                    Timber.d("Unarchived thread: $threadId")
                }

                // Clear selection after unarchiving
                clearSelection()
            } catch (e: Exception) {
                Timber.e(e, "Failed to unarchive conversations")
            }
        }
    }

    /**
     * Delete a conversation permanently
     */
    fun deleteConversation(threadId: Long) {
        viewModelScope.launch {
            try {
                threadRepository.deleteThread(threadId)
                Timber.d("Deleted thread: $threadId")

                // Remove from selection if it was selected
                _uiState.update { state ->
                    val selectedConversations = state.selectedConversations.toMutableSet()
                    selectedConversations.remove(threadId)

                    state.copy(
                        selectedConversations = selectedConversations,
                        isSelectionMode = selectedConversations.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete conversation")
            }
        }
    }
}
