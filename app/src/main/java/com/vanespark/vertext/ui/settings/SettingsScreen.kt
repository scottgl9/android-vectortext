package com.vanespark.vertext.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vanespark.vertext.R

/**
 * Settings screen for app configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle back button press
    BackHandler(onBack = onNavigateBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Theme Settings
            item {
                SettingsSectionHeader(text = "Appearance")
            }
            item {
                ThemeSettingItem(
                    currentTheme = uiState.theme,
                    onThemeChange = { viewModel.updateTheme(it) }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.DarkMode,
                    title = "AMOLED Black",
                    subtitle = "Use pure black background for dark theme",
                    checked = uiState.useAmoledBlack,
                    onCheckedChange = { viewModel.updateAmoledBlack(it) }
                )
            }

            // Notification Settings
            item {
                SettingsSectionHeader(text = "Notifications")
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Enable Notifications",
                    subtitle = "Receive notifications for new messages",
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.updateNotifications(it) }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Vibrate",
                    subtitle = "Vibrate on new messages",
                    checked = uiState.vibrateEnabled,
                    onCheckedChange = { viewModel.updateVibrate(it) },
                    enabled = uiState.notificationsEnabled
                )
            }

            // Message Settings
            item {
                SettingsSectionHeader(text = "Messages")
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Show delivery reports",
                    subtitle = "Display delivery status for sent messages",
                    checked = uiState.showDeliveryReports,
                    onCheckedChange = { viewModel.updateDeliveryReports(it) }
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Person,
                    title = "Show contact names",
                    subtitle = "Display contact names instead of phone numbers",
                    checked = uiState.showContactNames,
                    onCheckedChange = { viewModel.updateShowContactNames(it) }
                )
            }

            // Storage & Backup
            item {
                SettingsSectionHeader(text = "Storage & Backup")
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.Backup,
                    title = "Backup messages",
                    subtitle = "Export messages to local storage",
                    onClick = { viewModel.backupMessages() }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.RestorePage,
                    title = "Restore messages",
                    subtitle = "Import messages from backup",
                    onClick = { viewModel.restoreMessages() }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.CleaningServices,
                    title = "Clear cache",
                    subtitle = "Free up storage space",
                    onClick = { viewModel.clearCache() }
                )
            }

            // About
            item {
                SettingsSectionHeader(text = "About")
            }
            item {
                SettingsInfoItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = uiState.appVersion
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.Description,
                    title = "Privacy Policy",
                    subtitle = "View privacy policy",
                    onClick = { viewModel.openPrivacyPolicy() }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Default.Gavel,
                    title = "Terms of Service",
                    subtitle = "View terms of service",
                    onClick = { viewModel.openTermsOfService() }
                )
            }
        }
    }

    // Show dialogs based on state
    if (uiState.showBackupDialog) {
        BackupDialog(
            onDismiss = { viewModel.dismissBackupDialog() },
            onConfirm = { viewModel.confirmBackup() }
        )
    }

    if (uiState.showRestoreDialog) {
        RestoreDialog(
            onDismiss = { viewModel.dismissRestoreDialog() },
            onConfirm = { viewModel.confirmRestore() }
        )
    }
}

/**
 * Section header for settings groups
 */
@Composable
private fun SettingsSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/**
 * Theme selection setting item
 */
@Composable
private fun ThemeSettingItem(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        modifier = modifier.clickable { expanded = true },
        headlineContent = { Text("Theme") },
        supportingContent = { Text(currentTheme.displayName) },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        ThemeMode.entries.forEach { theme ->
            DropdownMenuItem(
                text = { Text(theme.displayName) },
                onClick = {
                    onThemeChange(theme)
                    expanded = false
                },
                leadingIcon = if (theme == currentTheme) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

/**
 * Switch setting item
 */
@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

/**
 * Clickable action setting item
 */
@Composable
private fun SettingsActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    )
}

/**
 * Information display setting item (non-clickable)
 */
@Composable
private fun SettingsInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    )
}

/**
 * Backup confirmation dialog
 */
@Composable
private fun BackupDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Backup, contentDescription = null) },
        title = { Text("Backup Messages") },
        text = { Text("This will export all your messages to local storage. Continue?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Restore confirmation dialog
 */
@Composable
private fun RestoreDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.RestorePage, contentDescription = null) },
        title = { Text("Restore Messages") },
        text = { Text("This will import messages from a backup file. Existing messages will not be affected. Continue?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Theme mode options
 */
enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}
