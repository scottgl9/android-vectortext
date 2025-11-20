package com.vanespark.vertext.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.vanespark.vertext.data.repository.ThreadRepository
import com.vanespark.vertext.domain.service.PermissionManager
import com.vanespark.vertext.domain.service.SmsSyncService
import com.vanespark.vertext.ui.archived.ArchivedConversationsScreen
import com.vanespark.vertext.ui.blocked.BlockedContactsScreen
import com.vanespark.vertext.ui.chat.ChatThreadScreen
import com.vanespark.vertext.ui.chat.ChatThreadViewModel
import com.vanespark.vertext.ui.components.AppNavigationDrawer
import com.vanespark.vertext.ui.compose.NewChatScreen
import com.vanespark.vertext.ui.conversations.ConversationListScreen
import com.vanespark.vertext.ui.insights.InsightsScreen
import com.vanespark.vertext.ui.permissions.PermissionsScreen
import com.vanespark.vertext.ui.rules.RulesScreen
import com.vanespark.vertext.ui.search.SearchScreen
import com.vanespark.vertext.ui.settings.SettingsScreen
import com.vanespark.vertext.ui.sync.SyncScreen
import com.vanespark.vertext.ui.theme.VectorTextTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main Activity for VectorText
 * Entry point for the app's UI
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var smsSyncService: SmsSyncService

    @Inject
    lateinit var chatThreadViewModelFactory: ChatThreadViewModel.Factory

    @Inject
    lateinit var threadRepository: ThreadRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VectorTextTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    var hasPermissions by remember {
                        mutableStateOf(permissionManager.isFullySetup())
                    }
                    var hasSynced by remember { mutableStateOf(false) }
                    var isCheckingSync by remember { mutableStateOf(true) }
                    var currentThreadId by remember { mutableStateOf<Long?>(null) }
                    var showNewChat by remember { mutableStateOf(false) }
                    var showSearch by remember { mutableStateOf(false) }
                    var showArchived by remember { mutableStateOf(false) }
                    var showBlocked by remember { mutableStateOf(false) }
                    var showInsights by remember { mutableStateOf(false) }
                    var showRules by remember { mutableStateOf(false) }
                    var showSettings by remember { mutableStateOf(false) }
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                    // Check if initial sync is complete
                    LaunchedEffect(hasPermissions) {
                        if (hasPermissions) {
                            scope.launch {
                                hasSynced = smsSyncService.hasCompletedInitialSync()
                                isCheckingSync = false
                            }
                        } else {
                            isCheckingSync = false
                        }
                    }

                    when {
                        !hasPermissions -> {
                            PermissionsScreen(
                                permissionManager = permissionManager,
                                onPermissionsGranted = {
                                    hasPermissions = true
                                    Timber.d("All permissions granted")
                                }
                            )
                        }
                        hasPermissions && !hasSynced && !isCheckingSync -> {
                            SyncScreen(
                                onSyncComplete = {
                                    hasSynced = true
                                    Timber.d("Initial sync completed")
                                }
                            )
                        }
                        hasPermissions && hasSynced -> {
                            // Main app with navigation drawer
                            AppNavigationDrawer(
                                drawerState = drawerState,
                                currentRoute = "conversations",
                                onNavigateToConversations = {
                                    Timber.d("Navigate to conversations")
                                    currentThreadId = null
                                    showNewChat = false
                                    showSearch = false
                                    showArchived = false
                                    showBlocked = false
                                    showInsights = false
                                    showSettings = false
                                },
                                onNavigateToArchived = {
                                    Timber.d("Navigate to archived")
                                    currentThreadId = null
                                    showNewChat = false
                                    showSearch = false
                                    showArchived = true
                                    showBlocked = false
                                    showInsights = false
                                    showSettings = false
                                },
                                onNavigateToBlocked = {
                                    Timber.d("Navigate to blocked")
                                    currentThreadId = null
                                    showNewChat = false
                                    showSearch = false
                                    showArchived = false
                                    showBlocked = true
                                    showInsights = false
                                    showSettings = false
                                },
                                onNavigateToInsights = {
                                    Timber.d("Navigate to insights")
                                    currentThreadId = null
                                    showNewChat = false
                                    showSearch = false
                                    showArchived = false
                                    showBlocked = false
                                    showInsights = true
                                    showSettings = false
                                },
                                onNavigateToSettings = {
                                    Timber.d("Navigate to settings")
                                    currentThreadId = null
                                    showNewChat = false
                                    showSearch = false
                                    showArchived = false
                                    showBlocked = false
                                    showInsights = false
                                    showSettings = true
                                },
                                onNavigateToAbout = {
                                    Timber.d("Navigate to about")
                                    // TODO: Implement about screen
                                }
                            ) {
                                when {
                                    showRules -> {
                                        // Rules screen
                                        RulesScreen(
                                            onNavigateBack = {
                                                showRules = false
                                            }
                                        )
                                    }
                                    showSettings -> {
                                        // Settings screen
                                        SettingsScreen(
                                            onNavigateBack = {
                                                showSettings = false
                                            },
                                            onNavigateToRules = {
                                                showSettings = false
                                                showRules = true
                                            }
                                        )
                                    }
                                    showInsights -> {
                                        // Insights screen
                                        InsightsScreen(
                                            onNavigateBack = {
                                                showInsights = false
                                            }
                                        )
                                    }
                                    showBlocked -> {
                                        // Blocked contacts screen
                                        BlockedContactsScreen(
                                            onNavigateBack = {
                                                showBlocked = false
                                            }
                                        )
                                    }
                                    showArchived -> {
                                        // Archived conversations screen
                                        ArchivedConversationsScreen(
                                            onNavigateBack = {
                                                showArchived = false
                                            },
                                            onConversationClick = { threadId ->
                                                showArchived = false
                                                currentThreadId = threadId
                                            }
                                        )
                                    }
                                    showSearch -> {
                                        // Search screen
                                        SearchScreen(
                                            onNavigateBack = {
                                                showSearch = false
                                            },
                                            onConversationClick = { threadId ->
                                                showSearch = false
                                                currentThreadId = threadId
                                            }
                                        )
                                    }
                                    showNewChat -> {
                                        // New chat screen
                                        NewChatScreen(
                                            onNavigateBack = {
                                                showNewChat = false
                                            },
                                            onContactSelected = { phoneNumber, contactName ->
                                                scope.launch {
                                                    // Get or create thread for this recipient
                                                    val thread = threadRepository.getOrCreateThread(
                                                        recipient = phoneNumber,
                                                        recipientName = contactName
                                                    )
                                                    showNewChat = false
                                                    currentThreadId = thread.id
                                                }
                                            }
                                        )
                                    }
                                    currentThreadId != null -> {
                                        // Chat thread screen
                                        ChatThreadScreen(
                                            threadId = currentThreadId!!,
                                            onNavigateBack = {
                                                currentThreadId = null
                                            },
                                            viewModelFactory = chatThreadViewModelFactory
                                        )
                                    }
                                    else -> {
                                        // Conversation list screen
                                        ConversationListScreen(
                                            onConversationClick = { threadId ->
                                                Timber.d("Conversation clicked: $threadId")
                                                currentThreadId = threadId
                                            },
                                            onNewMessageClick = {
                                                Timber.d("New message clicked")
                                                showNewChat = true
                                            },
                                            onSearchClick = {
                                                Timber.d("Search clicked")
                                                showSearch = true
                                            },
                                            onMenuClick = {
                                                Timber.d("Menu clicked")
                                                scope.launch {
                                                    drawerState.open()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
