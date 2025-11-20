package com.vanespark.vertext.ui.archived

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vanespark.vertext.R
import com.vanespark.vertext.ui.conversations.ConversationCard

/**
 * Screen for viewing archived conversations
 * Shows list of conversations that have been archived
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedConversationsScreen(
    onNavigateBack: () -> Unit,
    onConversationClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchivedConversationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle back button press
    BackHandler(onBack = onNavigateBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archived)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (uiState.conversations.isNotEmpty() && uiState.selectedConversations.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.unarchiveSelected() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Unarchive,
                            contentDescription = null
                        )
                    },
                    text = { Text("Unarchive (${uiState.selectedConversations.size})") }
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.conversations.isEmpty() -> {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No archived conversations",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Conversations you archive will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.conversations) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            isSelected = uiState.selectedConversations.contains(conversation.threadId),
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleSelection(conversation.threadId)
                                } else {
                                    onConversationClick(conversation.threadId)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSelection(conversation.threadId)
                            },
                            onArchive = { },
                            onDelete = {
                                viewModel.deleteConversation(conversation.threadId)
                            },
                            onPin = { }
                        )
                    }
                }
            }
        }
    }
}
