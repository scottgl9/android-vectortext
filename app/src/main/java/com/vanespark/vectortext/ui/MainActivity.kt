package com.vanespark.vectortext.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.vanespark.vectortext.ui.conversations.ConversationListScreen
import com.vanespark.vectortext.ui.theme.VectorTextTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main Activity for VectorText
 * Entry point for the app's UI
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VectorTextTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                }
            }
        }
    }
}
