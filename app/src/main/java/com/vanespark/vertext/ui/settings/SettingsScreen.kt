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
import androidx.compose.ui.text.font.FontWeight
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
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backupUiState by backupViewModel.uiState.collectAsState()

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

            // Search & Indexing
            item {
                SettingsSectionHeader(text = "Search & Indexing")
            }
            item {
                IndexingStatusItem(
                    embeddedCount = uiState.embeddedMessageCount,
                    totalCount = uiState.totalMessageCount,
                    indexingProgress = uiState.indexingProgress,
                    lastIndexed = uiState.lastIndexedTimestamp,
                    onRefresh = { viewModel.refreshIndexingStats() },
                    onReindex = { viewModel.triggerReindexing() }
                )
            }
            item {
                CategorizationItem(
                    categorizedCount = uiState.categorizedThreadCount,
                    totalCount = uiState.totalThreadCount,
                    isCategorizing = uiState.isCategorizing,
                    onRefresh = { viewModel.refreshCategoryStats() },
                    onCategorize = { viewModel.triggerCategorization() }
                )
            }

            // Storage & Backup
            item {
                SettingsSectionHeader(text = "Storage & Backup")
            }
            item {
                BackupManagementItem(
                    backupUiState = backupUiState,
                    onCreateBackup = { backupViewModel.createBackup() },
                    onRestoreBackup = { backupPath -> backupViewModel.restoreBackup(backupPath) },
                    onDeleteBackup = { backupPath -> backupViewModel.deleteBackup(backupPath) },
                    onRefresh = { backupViewModel.loadBackups() },
                    onDismissError = { backupViewModel.clearError() },
                    onDismissSuccess = { backupViewModel.clearSuccessMessage() }
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
 * Indexing status item showing embedding statistics
 */
@Composable
private fun IndexingStatusItem(
    embeddedCount: Int,
    totalCount: Int,
    indexingProgress: Float?,
    lastIndexed: Long?,
    onRefresh: () -> Unit,
    onReindex: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Message Indexing",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh stats"
                    )
                }
            }

            // Stats
            val percentage = if (totalCount > 0) {
                (embeddedCount.toFloat() / totalCount * 100).toInt()
            } else 0

            Text(
                text = "$embeddedCount of $totalCount messages indexed ($percentage%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Progress bar if indexing
            if (indexingProgress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { indexingProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Indexing... ${(indexingProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Last indexed timestamp
            if (lastIndexed != null && indexingProgress == null) {
                val timeAgo = formatTimeAgo(lastIndexed)
                Text(
                    text = "Last indexed: $timeAgo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reindex button
            FilledTonalButton(
                onClick = onReindex,
                modifier = Modifier.fillMaxWidth(),
                enabled = indexingProgress == null
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (indexingProgress != null) "Indexing..." else "Reindex Messages")
            }
        }
    }
}

/**
 * Format timestamp as relative time
 */
private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 604800_000 -> "${diff / 86400_000} days ago"
        else -> {
            val date = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
                .format(java.util.Date(timestamp))
            date
        }
    }
}

/**
 * Categorization status item showing thread categorization statistics
 */
@Composable
private fun CategorizationItem(
    categorizedCount: Int,
    totalCount: Int,
    isCategorizing: Boolean,
    onRefresh: () -> Unit,
    onCategorize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Thread Categorization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }

            // Progress indicator
            if (isCategorizing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Categorizing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "$categorizedCount / $totalCount",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Threads Categorized",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action button
            Button(
                onClick = onCategorize,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCategorizing
            ) {
                Icon(
                    imageVector = if (isCategorizing) Icons.Default.HourglassEmpty else Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isCategorizing) "Categorizing..." else "Categorize Threads")
            }
        }
    }
}

/**
 * Backup management card with create, restore, and delete functionality
 */
@Composable
private fun BackupManagementItem(
    backupUiState: BackupUiState,
    onCreateBackup: () -> Unit,
    onRestoreBackup: (String) -> Unit,
    onDeleteBackup: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onDismissSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRestoreConfirmation by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Encrypted Backups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider()

            // Backup progress
            if (backupUiState.backupProgress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = backupUiState.backupProgress.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { backupUiState.backupProgress.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${backupUiState.backupProgress.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Divider()
            }

            // Restore progress
            if (backupUiState.restoreProgress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = backupUiState.restoreProgress.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { backupUiState.restoreProgress.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${backupUiState.restoreProgress.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Divider()
            }

            // Error message
            if (backupUiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = backupUiState.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissError) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Success message
            if (backupUiState.successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = backupUiState.successMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissSuccess) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Create backup button
            Button(
                onClick = onCreateBackup,
                modifier = Modifier.fillMaxWidth(),
                enabled = backupUiState.backupProgress == null && backupUiState.restoreProgress == null
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Backup")
            }

            // Backup list
            if (backupUiState.backups.isNotEmpty()) {
                Divider()
                Text(
                    text = "Available Backups (${backupUiState.backups.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                backupUiState.backups.forEach { backup ->
                    BackupListItem(
                        backup = backup,
                        onRestore = { showRestoreConfirmation = backup.path },
                        onDelete = { showDeleteConfirmation = backup.path },
                        enabled = backupUiState.backupProgress == null && backupUiState.restoreProgress == null
                    )
                }
            } else if (!backupUiState.isLoading) {
                Text(
                    text = "No backups found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }

    // Restore confirmation dialog
    if (showRestoreConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = null },
            title = { Text("Restore Backup?") },
            text = { Text("This will replace all current messages with the backup. The app will need to restart after restore completes.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackup(showRestoreConfirmation!!)
                        showRestoreConfirmation = null
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Backup?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBackup(showDeleteConfirmation!!)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Individual backup list item
 */
@Composable
private fun BackupListItem(
    backup: com.vanespark.vertext.domain.service.BackupMetadata,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.US)
                            .format(java.util.Date(backup.timestamp)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatBackupSize(backup.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.RestorePage,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

/**
 * Format backup file size
 */
private fun formatBackupSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}

/**
 * Theme mode options
 */
enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}
