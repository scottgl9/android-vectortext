package com.vanespark.vectortext.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vanespark.vectortext.domain.service.PermissionManager
import com.vanespark.vectortext.ui.conversations.ConversationListScreen
import com.vanespark.vectortext.ui.permissions.PermissionsScreen
import com.vanespark.vectortext.ui.theme.VectorTextTheme
import dagger.hilt.android.AndroidEntryPoint
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VectorTextTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var hasPermissions by remember {
                        mutableStateOf(permissionManager.isFullySetup())
                    }

                    if (hasPermissions) {
                        ConversationListScreen(
                            onConversationClick = { threadId ->
                                Timber.d("Conversation clicked: $threadId")
                                // TODO: Navigate to chat thread screen
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
                    } else {
                        PermissionsScreen(
                            permissionManager = permissionManager,
                            onPermissionsGranted = {
                                hasPermissions = true
                                Timber.d("All permissions granted")
                            }
                        )
                    }
                }
            }
        }
    }
}
