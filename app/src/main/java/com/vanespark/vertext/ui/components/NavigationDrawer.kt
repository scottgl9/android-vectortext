package com.vanespark.vertext.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vanespark.vertext.R
import kotlinx.coroutines.launch

/**
 * Navigation drawer for Vertext
 * Provides access to main app sections
 */
@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    currentRoute: String,
    onNavigateToConversations: () -> Unit,
    onNavigateToArchived: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))

                // App title
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Main navigation items
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.conversations)) },
                    selected = currentRoute == "conversations",
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToConversations()
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.archived)) },
                    selected = currentRoute == "archived",
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToArchived()
                        }
                    },
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.blocked)) },
                    selected = currentRoute == "blocked",
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToBlocked()
                        }
                    },
                    icon = { Icon(Icons.Default.Block, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Insights") },
                    selected = currentRoute == "insights",
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToInsights()
                        }
                    },
                    icon = { Icon(Icons.Default.Insights, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Settings and About
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings)) },
                    selected = currentRoute == "settings",
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToSettings()
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.about)) },
                    selected = currentRoute == "about",
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToAbout()
                        }
                    },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        content = content
    )
}
