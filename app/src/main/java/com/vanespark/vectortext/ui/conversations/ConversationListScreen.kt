package com.vanespark.vectortext.ui.conversations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vanespark.vectortext.R

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
    viewModel: ConversationListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
                onMenuClick = onMenuClick,
                onSearchClick = onSearchClick,
                onCancelSelection = { viewModel.clearSelection() },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        ConversationListContent(
            uiState = uiState,
            onConversationClick = onConversationClick,
            onConversationLongClick = { viewModel.toggleConversationSelection(it) },
            onArchiveConversation = { viewModel.archiveConversation(it) },
            onDeleteConversation = { viewModel.deleteConversation(it) },
            onPinConversation = { viewModel.pinConversation(it) },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListTopBar(
    title: String,
    unreadCount: Int,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCancelSelection: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    modifier: Modifier = Modifier
) {
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
                        Icons.Default.MoreVert // TODO: Use Close icon
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
            if (!isSelectionMode) {
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
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            LoadingState(modifier = modifier)
        }
        uiState.conversations.isEmpty() -> {
            EmptyState(modifier = modifier)
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
                modifier = modifier
            )
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
