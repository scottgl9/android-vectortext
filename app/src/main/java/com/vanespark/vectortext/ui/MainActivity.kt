package com.vanespark.vectortext.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.vanespark.vectortext.domain.service.PermissionManager
import com.vanespark.vectortext.domain.service.SmsSyncService
import com.vanespark.vectortext.ui.chat.ChatThreadScreen
import com.vanespark.vectortext.ui.chat.ChatThreadViewModel
import com.vanespark.vectortext.ui.conversations.ConversationListScreen
import com.vanespark.vectortext.ui.permissions.PermissionsScreen
import com.vanespark.vectortext.ui.sync.SyncScreen
import com.vanespark.vectortext.ui.theme.VectorTextTheme
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
                        hasPermissions && hasSynced && currentThreadId != null -> {
                            // Chat thread screen
                            ChatThreadScreen(
                                threadId = currentThreadId!!,
                                onNavigateBack = {
                                    currentThreadId = null
                                },
                                viewModelFactory = chatThreadViewModelFactory
                            )
                        }
                        hasPermissions && hasSynced -> {
                            // Conversation list screen
                            ConversationListScreen(
                                onConversationClick = { threadId ->
                                    Timber.d("Conversation clicked: $threadId")
                                    currentThreadId = threadId
                                },
                                onNewMessageClick = {
                                    Timber.d("New message clicked")
                                    // TODO: Navigate to new message screen
                                },
                                onSearchClick = {
                                    Timber.d("Search clicked")
                                    // TODO: Navigate to search screen
                                },
                                onMenuClick = {
                                    Timber.d("Menu clicked")
                                    // TODO: Show navigation drawer or menu
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
