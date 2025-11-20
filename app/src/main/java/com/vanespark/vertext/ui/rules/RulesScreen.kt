package com.vanespark.vertext.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vanespark.vertext.data.model.Rule
import java.text.SimpleDateFormat
import java.util.*

/**
 * Rules management screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Automation Rules") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
            ExtendedFloatingActionButton(
                onClick = { viewModel.showNewRuleEditor() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Rule") }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.rules.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    RulesList(
                        rules = uiState.rules,
                        onToggleEnabled = { ruleId, enabled ->
                            viewModel.toggleRuleEnabled(ruleId, enabled)
                        },
                        onEditRule = { rule ->
                            viewModel.showEditRuleEditor(rule)
                        },
                        onDeleteRule = { ruleId ->
                            showDeleteConfirmation = ruleId
                        }
                    )
                }
            }

            // Success/Error messages
            uiState.successMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearSuccessMessage() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(message)
                }
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteConfirmation != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                title = { Text("Delete Rule?") },
                text = { Text("This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteRule(showDeleteConfirmation!!)
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

        // Rule editor dialog (placeholder)
        if (uiState.showEditor) {
            AlertDialog(
                onDismissRequest = { viewModel.hideEditor() },
                title = { Text(if (uiState.selectedRule == null) "New Rule" else "Edit Rule") },
                text = {
                    Text("Rule builder UI coming soon!\n\nFor now, rules can be created programmatically through the RuleRepository.")
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.hideEditor() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

/**
 * Loading state
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Empty state
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No Rules Yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create automation rules to automatically manage your messages",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Rules list
 */
@Composable
private fun RulesList(
    rules: List<Rule>,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onEditRule: (Rule) -> Unit,
    onDeleteRule: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(rules) { rule ->
            RuleCard(
                rule = rule,
                onToggleEnabled = { enabled -> onToggleEnabled(rule.id, enabled) },
                onEdit = { onEditRule(rule) },
                onDelete = { onDeleteRule(rule.id) }
            )
        }
    }
}

/**
 * Individual rule card
 */
@Composable
private fun RuleCard(
    rule: Rule,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (rule.description.isNotEmpty()) {
                        Text(
                            text = rule.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${rule.triggerCount} times",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (rule.lastTriggeredAt != null) {
                    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    Text(
                        text = "Last: ${dateFormat.format(Date(rule.lastTriggeredAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expand/Collapse
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (expanded) "Show Less" else "Show Details")
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            // Details (expanded)
            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Triggers
                Text(
                    text = "Triggers (${rule.triggers.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                rule.triggers.forEach { trigger ->
                    Text(
                        text = "• ${formatTrigger(trigger)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Conditions
                if (rule.conditions.isNotEmpty()) {
                    Text(
                        text = "Conditions (${rule.conditions.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    rule.conditions.forEach { condition ->
                        Text(
                            text = "• ${formatCondition(condition)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Actions
                Text(
                    text = "Actions (${rule.actions.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                rule.actions.forEach { action ->
                    Text(
                        text = "• ${formatAction(action)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
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
}

/**
 * Format trigger for display
 */
private fun formatTrigger(trigger: com.vanespark.vertext.data.model.RuleTrigger): String {
    return when (trigger) {
        is com.vanespark.vertext.data.model.RuleTrigger.Always -> "Always"
        is com.vanespark.vertext.data.model.RuleTrigger.FromSender -> "From ${trigger.phoneNumber}"
        is com.vanespark.vertext.data.model.RuleTrigger.ContainsKeyword ->
            "Contains: ${trigger.keywords.joinToString(", ")}"
        is com.vanespark.vertext.data.model.RuleTrigger.TimeRange ->
            "Between ${trigger.startHour}:00 and ${trigger.endHour}:00"
        is com.vanespark.vertext.data.model.RuleTrigger.DaysOfWeek ->
            "On ${trigger.days.joinToString(", ")}"
    }
}

/**
 * Format condition for display
 */
private fun formatCondition(condition: com.vanespark.vertext.data.model.RuleCondition): String {
    return when (condition) {
        is com.vanespark.vertext.data.model.RuleCondition.IsUnread -> "Is unread"
        is com.vanespark.vertext.data.model.RuleCondition.MatchesPattern -> "Matches pattern: ${condition.pattern}"
        is com.vanespark.vertext.data.model.RuleCondition.SenderInContacts -> "Sender is in contacts"
        is com.vanespark.vertext.data.model.RuleCondition.SenderNotInContacts -> "Sender not in contacts"
        is com.vanespark.vertext.data.model.RuleCondition.ThreadCategory -> "Category is ${condition.category}"
    }
}

/**
 * Format action for display
 */
private fun formatAction(action: com.vanespark.vertext.data.model.RuleAction): String {
    return when (action) {
        is com.vanespark.vertext.data.model.RuleAction.AutoReply -> "Auto-reply: \"${action.message}\""
        is com.vanespark.vertext.data.model.RuleAction.SetCategory -> "Set category: ${action.category}"
        is com.vanespark.vertext.data.model.RuleAction.MarkAsRead -> "Mark as read"
        is com.vanespark.vertext.data.model.RuleAction.Archive -> "Archive conversation"
        is com.vanespark.vertext.data.model.RuleAction.MuteNotifications -> "Mute notifications"
        is com.vanespark.vertext.data.model.RuleAction.PinConversation -> "Pin conversation"
        is com.vanespark.vertext.data.model.RuleAction.BlockSender -> "Block sender"
        is com.vanespark.vertext.data.model.RuleAction.CustomNotification -> "Custom notification"
    }
}
