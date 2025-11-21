package com.vanespark.vertext.ui.conversations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vanespark.vertext.R
import com.vanespark.vertext.data.model.ThreadCategory
import com.vanespark.vertext.ui.assistant.AIAssistantBottomSheet
import com.vanespark.vertext.ui.assistant.AIAssistantViewModel

/**
 * Main conversation list screen
 * Displays all SMS conversations with Material You design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onConversationClick: (Long) -> Unit,
    onNewMessageClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationListViewModel = hiltViewModel(),
    aiAssistantViewModel: AIAssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiAssistantUiState by aiAssistantViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ConversationListTopBar(
                title = stringResource(R.string.app_name),
                unreadCount = uiState.unreadCount,
                isSelectionMode = uiState.isSelectionMode,
                selectedCount = uiState.selectedConversations.size,
                totalCount = uiState.conversations.size,
                onMenuClick = onMenuClick,
                onSearchClick = onSearchClick,
                onCancelSelection = { viewModel.clearSelection() },
                onDeleteSelected = { showDeleteDialog = true },
                onArchiveSelected = { viewModel.archiveSelected() },
                onMarkSelectedAsRead = { viewModel.markSelectedAsRead() },
                onSelectAll = { viewModel.selectAll() },
                onDeselectAll = { viewModel.clearSelection() },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // AI Assistant FAB
                    ExtendedFloatingActionButton(
                        onClick = { aiAssistantViewModel.show() },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "AI Assistant"
                            )
                        },
                        text = { Text("Ask AI") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    // New Message FAB
                    FloatingActionButton(
                        onClick = onNewMessageClick,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.new_message)
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        ConversationListContent(
            uiState = uiState,
            onConversationClick = { threadId ->
                // In selection mode, toggle selection. Otherwise, open conversation
                if (uiState.isSelectionMode) {
                    viewModel.toggleConversationSelection(threadId)
                } else {
                    onConversationClick(threadId)
                }
            },
            onConversationLongClick = { viewModel.toggleConversationSelection(it) },
            onArchiveConversation = { viewModel.archiveConversation(it) },
            onDeleteConversation = { viewModel.deleteConversation(it) },
            onPinConversation = { viewModel.pinConversation(it) },
            onCategorySelected = { viewModel.selectCategory(it) },
            modifier = Modifier.padding(paddingValues)
        )

        // AI Assistant Bottom Sheet
        AIAssistantBottomSheet(
            uiState = aiAssistantUiState,
            onDismiss = { aiAssistantViewModel.dismiss() },
            onSendMessage = { aiAssistantViewModel.sendMessage(it) },
            onUpdateInput = { aiAssistantViewModel.updateInputText(it) },
            onClearHistory = { aiAssistantViewModel.clearHistory() }
        )

        // Delete confirmation dialog
        if (showDeleteDialog) {
            DeleteSelectedDialog(
                selectedCount = uiState.selectedConversations.size,
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    viewModel.deleteSelected()
                    showDeleteDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListTopBar(
    title: String,
    unreadCount: Int,
    isSelectionMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCancelSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onArchiveSelected: () -> Unit,
    onMarkSelectedAsRead: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    modifier: Modifier = Modifier
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = if (isSelectionMode) {
                    "$selectedCount selected"
                } else {
                    title
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = if (isSelectionMode) onCancelSelection else onMenuClick) {
                Icon(
                    imageVector = if (isSelectionMode) {
                        Icons.Default.Close
                    } else {
                        Icons.Default.Menu
                    },
                    contentDescription = if (isSelectionMode) {
                        stringResource(R.string.cancel)
                    } else {
                        stringResource(R.string.menu)
                    }
                )
            }
        },
        actions = {
            if (isSelectionMode) {
                // Archive button
                IconButton(
                    onClick = onArchiveSelected,
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archive selected"
                    )
                }

                // Mark as read button
                IconButton(
                    onClick = onMarkSelectedAsRead,
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Mark as read"
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDeleteSelected,
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete selected"
                    )
                }

                // More options (Select all/Deselect all)
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    if (selectedCount < totalCount) {
                        DropdownMenuItem(
                            text = { Text("Select all") },
                            onClick = {
                                onSelectAll()
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                    if (selectedCount > 0) {
                        DropdownMenuItem(
                            text = { Text("Deselect all") },
                            onClick = {
                                onDeselectAll()
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            } else {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search)
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun ConversationListContent(
    uiState: ConversationListUiState,
    onConversationClick: (Long) -> Unit,
    onConversationLongClick: (Long) -> Unit,
    onArchiveConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onPinConversation: (Long) -> Unit,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Category filter chips
        if (uiState.availableCategories.isNotEmpty()) {
            CategoryFilterChips(
                availableCategories = uiState.availableCategories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = onCategorySelected
            )
        }

        // Conversation list content
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.weight(1f))
            }
            uiState.conversations.isEmpty() -> {
                EmptyState(modifier = Modifier.weight(1f))
            }
            else -> {
                ConversationList(
                    conversations = uiState.conversations,
                    selectedConversations = uiState.selectedConversations,
                    onConversationClick = onConversationClick,
                    onConversationLongClick = onConversationLongClick,
                    onArchiveConversation = onArchiveConversation,
                    onDeleteConversation = onDeleteConversation,
                    onPinConversation = onPinConversation,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConversationList(
    conversations: List<ConversationUiItem>,
    selectedConversations: Set<Long>,
    onConversationClick: (Long) -> Unit,
    onConversationLongClick: (Long) -> Unit,
    onArchiveConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onPinConversation: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            items = conversations,
            key = { it.threadId }
        ) { conversation ->
            ConversationCard(
                conversation = conversation,
                isSelected = selectedConversations.contains(conversation.threadId),
                onClick = { onConversationClick(conversation.threadId) },
                onLongClick = { onConversationLongClick(conversation.threadId) },
                onArchive = { onArchiveConversation(conversation.threadId) },
                onDelete = { onDeleteConversation(conversation.threadId) },
                onPin = { onPinConversation(conversation.threadId) }
            )
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.no_conversations),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.no_conversations_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

/**
 * Horizontal scrollable row of category filter chips
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterChips(
    availableCategories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip (always first)
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        // Individual category chips
        items(availableCategories) { categoryStr ->
            val category = ThreadCategory.fromString(categoryStr)
            FilterChip(
                selected = selectedCategory == categoryStr,
                onClick = { onCategorySelected(categoryStr) },
                label = {
                    Text("${category.icon} ${category.displayName}")
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * Delete confirmation dialog for selected conversations
 */
@Composable
private fun DeleteSelectedDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = if (selectedCount == 1) {
                    "Delete conversation?"
                } else {
                    "Delete $selectedCount conversations?"
                }
            )
        },
        text = {
            Text(
                text = if (selectedCount == 1) {
                    "This conversation will be permanently deleted. This action cannot be undone."
                } else {
                    "These $selectedCount conversations will be permanently deleted. This action cannot be undone."
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
