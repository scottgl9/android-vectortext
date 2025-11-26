package com.vanespark.vertext.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vanespark.vertext.data.model.*

/**
 * Complete rule editor dialog with visual builder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorDialog(
    rule: Rule?,
    onDismiss: () -> Unit,
    onSave: (Rule) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(rule?.name ?: "") }
    var description by remember { mutableStateOf(rule?.description ?: "") }
    var isEnabled by remember { mutableStateOf(rule?.isEnabled ?: true) }
    val triggers = remember { (rule?.triggers ?: emptyList()).toMutableStateList() }
    val conditions = remember { (rule?.conditions ?: emptyList()).toMutableStateList() }
    val actions = remember { (rule?.actions ?: emptyList()).toMutableStateList() }

    var showTriggerPicker by remember { mutableStateOf(false) }
    var showConditionPicker by remember { mutableStateOf(false) }
    var showActionPicker by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank() && triggers.isNotEmpty() && actions.isNotEmpty()

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(0.95f)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = if (rule == null) "Create Rule" else "Edit Rule",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Basic Info
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Rule Name") },
                            placeholder = { Text("e.g., Auto-reply to work messages") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (optional)") },
                            placeholder = { Text("What does this rule do?") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable rule", style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                        }
                    }

                    // Triggers Section
                    item {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "When (Triggers)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "All triggers must match",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(triggers) { trigger ->
                        TriggerChip(
                            trigger = trigger,
                            onRemove = { triggers.remove(trigger) }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { showTriggerPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Trigger")
                        }
                    }

                    // Conditions Section
                    item {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "If (Conditions - Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "All conditions must be true",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(conditions) { condition ->
                        ConditionChip(
                            condition = condition,
                            onRemove = { conditions.remove(condition) }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { showConditionPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Condition")
                        }
                    }

                    // Actions Section
                    item {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Then (Actions)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "All actions will be executed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(actions) { action ->
                        ActionChip(
                            action = action,
                            onRemove = { actions.remove(action) }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { showActionPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Action")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val savedRule = Rule(
                                id = rule?.id ?: 0,
                                name = name,
                                description = description,
                                isEnabled = isEnabled,
                                triggers = triggers.toList(),
                                conditions = conditions.toList(),
                                actions = actions.toList(),
                                triggerCount = rule?.triggerCount ?: 0,
                                lastTriggeredAt = rule?.lastTriggeredAt
                            )
                            onSave(savedRule)
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // Pickers
    if (showTriggerPicker) {
        TriggerPickerDialog(
            onDismiss = { showTriggerPicker = false },
            onSelect = { trigger ->
                triggers.add(trigger)
                showTriggerPicker = false
            }
        )
    }

    if (showConditionPicker) {
        ConditionPickerDialog(
            onDismiss = { showConditionPicker = false },
            onSelect = { condition ->
                conditions.add(condition)
                showConditionPicker = false
            }
        )
    }

    if (showActionPicker) {
        ActionPickerDialog(
            onDismiss = { showActionPicker = false },
            onSelect = { action ->
                actions.add(action)
                showActionPicker = false
            }
        )
    }
}

@Composable
private fun TriggerChip(
    trigger: RuleTrigger,
    onRemove: () -> Unit
) {
    AssistChip(
        onClick = { },
        label = { Text(formatTrigger(trigger)) },
        trailingIcon = {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ConditionChip(
    condition: RuleCondition,
    onRemove: () -> Unit
) {
    AssistChip(
        onClick = { },
        label = { Text(formatCondition(condition)) },
        trailingIcon = {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ActionChip(
    action: RuleAction,
    onRemove: () -> Unit
) {
    AssistChip(
        onClick = { },
        label = { Text(formatAction(action)) },
        trailingIcon = {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun formatTrigger(trigger: RuleTrigger): String {
    return when (trigger) {
        is RuleTrigger.Always -> "Always"
        is RuleTrigger.FromSender -> "From: ${trigger.phoneNumber}"
        is RuleTrigger.ContainsKeyword ->
            "Contains: ${trigger.keywords.joinToString(", ")}"
        is RuleTrigger.TimeRange ->
            "Between ${trigger.startHour}:00 and ${trigger.endHour}:00"
        is RuleTrigger.DaysOfWeek ->
            "On ${trigger.days.joinToString(", ")}"
    }
}

private fun formatCondition(condition: RuleCondition): String {
    return when (condition) {
        is RuleCondition.IsUnread -> "Is unread"
        is RuleCondition.MatchesPattern -> "Matches: ${condition.pattern}"
        is RuleCondition.SenderInContacts -> "Sender is in contacts"
        is RuleCondition.SenderNotInContacts -> "Sender not in contacts"
        is RuleCondition.ThreadCategory -> "Category is ${condition.category}"
    }
}

private fun formatAction(action: RuleAction): String {
    return when (action) {
        is RuleAction.AutoReply -> "Auto-reply: \"${action.message}\""
        is RuleAction.SetCategory -> "Set category: ${action.category}"
        is RuleAction.MarkAsRead -> "Mark as read"
        is RuleAction.Archive -> "Archive"
        is RuleAction.MuteNotifications -> "Mute notifications"
        is RuleAction.PinConversation -> "Pin conversation"
        is RuleAction.BlockSender -> "Block sender"
        is RuleAction.CustomNotification -> "Custom notification"
    }
}
