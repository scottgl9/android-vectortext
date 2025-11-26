package com.vanespark.vertext.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vanespark.vertext.data.model.*

/**
 * Dialog for selecting trigger type and configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (RuleTrigger) -> Unit
) {
    var selectedType by remember { mutableStateOf<TriggerType?>(null) }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Add Trigger",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedType) {
                    TriggerType.ALWAYS -> {
                        AlwaysTriggerConfig(
                            onCancel = { selectedType = null },
                            onCreate = { onSelect(RuleTrigger.Always) }
                        )
                    }
                    TriggerType.FROM_SENDER -> {
                        FromSenderConfig(
                            onCancel = { selectedType = null },
                            onCreate = onSelect
                        )
                    }
                    TriggerType.CONTAINS_KEYWORD -> {
                        ContainsKeywordConfig(
                            onCancel = { selectedType = null },
                            onCreate = onSelect
                        )
                    }
                    TriggerType.TIME_RANGE -> {
                        TimeRangeConfig(
                            onCancel = { selectedType = null },
                            onCreate = onSelect
                        )
                    }
                    TriggerType.DAYS_OF_WEEK -> {
                        DaysOfWeekConfig(
                            onCancel = { selectedType = null },
                            onCreate = onSelect
                        )
                    }
                    null -> {
                        TriggerTypeList(onSelectType = { selectedType = it })
                    }
                }
            }
        }
    }
}

@Composable
private fun TriggerTypeList(onSelectType: (TriggerType) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            TriggerTypeItem(
                icon = Icons.Default.CheckCircle,
                title = "Always",
                description = "Trigger on every message",
                onClick = { onSelectType(TriggerType.ALWAYS) }
            )
        }
        item {
            TriggerTypeItem(
                icon = Icons.Default.Person,
                title = "From Sender",
                description = "Trigger when message is from specific number",
                onClick = { onSelectType(TriggerType.FROM_SENDER) }
            )
        }
        item {
            TriggerTypeItem(
                icon = Icons.Default.Search,
                title = "Contains Keyword",
                description = "Trigger when message contains specific words",
                onClick = { onSelectType(TriggerType.CONTAINS_KEYWORD) }
            )
        }
        item {
            TriggerTypeItem(
                icon = Icons.Default.Schedule,
                title = "Time Range",
                description = "Trigger during specific hours",
                onClick = { onSelectType(TriggerType.TIME_RANGE) }
            )
        }
        item {
            TriggerTypeItem(
                icon = Icons.Default.DateRange,
                title = "Days of Week",
                description = "Trigger on specific days",
                onClick = { onSelectType(TriggerType.DAYS_OF_WEEK) }
            )
        }
    }
}

@Composable
private fun TriggerTypeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AlwaysTriggerConfig(
    onCancel: () -> Unit,
    onCreate: () -> Unit
) {
    Column {
        Text("This will trigger on every message.")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(onClick = onCreate, modifier = Modifier.weight(1f)) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun FromSenderConfig(
    onCancel: () -> Unit,
    onCreate: (RuleTrigger) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            placeholder = { Text("+1234567890") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = { onCreate(RuleTrigger.FromSender(phoneNumber)) },
                enabled = phoneNumber.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun ContainsKeywordConfig(
    onCancel: () -> Unit,
    onCreate: (RuleTrigger) -> Unit
) {
    var keywords by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = keywords,
            onValueChange = { keywords = it },
            label = { Text("Keywords (comma-separated)") },
            placeholder = { Text("urgent, important, asap") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = caseSensitive, onCheckedChange = { caseSensitive = it })
            Text("Case sensitive")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = {
                    val keywordList = keywords.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onCreate(RuleTrigger.ContainsKeyword(keywordList, caseSensitive))
                },
                enabled = keywords.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun TimeRangeConfig(
    onCancel: () -> Unit,
    onCreate: (RuleTrigger) -> Unit
) {
    var startHour by remember { mutableStateOf(9) }
    var endHour by remember { mutableStateOf(17) }

    Column {
        Text("Start Hour: $startHour:00")
        Slider(
            value = startHour.toFloat(),
            onValueChange = { startHour = it.toInt() },
            valueRange = 0f..23f,
            steps = 22
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("End Hour: $endHour:00")
        Slider(
            value = endHour.toFloat(),
            onValueChange = { endHour = it.toInt() },
            valueRange = 0f..23f,
            steps = 22
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = { onCreate(RuleTrigger.TimeRange(startHour, endHour)) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun DaysOfWeekConfig(
    onCancel: () -> Unit,
    onCreate: (RuleTrigger) -> Unit
) {
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }

    Column {
        DayOfWeek.entries.forEach { day ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedDays = if (selectedDays.contains(day)) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedDays.contains(day),
                    onCheckedChange = {
                        selectedDays = if (it) selectedDays + day else selectedDays - day
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(day.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = { onCreate(RuleTrigger.DaysOfWeek(selectedDays)) },
                enabled = selectedDays.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Add")
            }
        }
    }
}

/**
 * Dialog for selecting condition type
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (RuleCondition) -> Unit
) {
    var selectedType by remember { mutableStateOf<ConditionType?>(null) }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Add Condition",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedType) {
                    ConditionType.IS_UNREAD -> {
                        SimpleConditionConfig(
                            description = "Message must be unread",
                            onCancel = { selectedType = null },
                            onCreate = { onSelect(RuleCondition.IsUnread) }
                        )
                    }
                    ConditionType.MATCHES_PATTERN -> {
                        PatternConfig(
                            onCancel = { selectedType = null },
                            onCreate = onSelect
                        )
                    }
                    ConditionType.SENDER_IN_CONTACTS -> {
                        SimpleConditionConfig(
                            description = "Sender must be in contacts",
                            onCancel = { selectedType = null },
                            onCreate = { onSelect(RuleCondition.SenderInContacts) }
                        )
                    }
                    ConditionType.SENDER_NOT_IN_CONTACTS -> {
                        SimpleConditionConfig(
                            description = "Sender must not be in contacts",
                            onCancel = { selectedType = null },
                            onCreate = { onSelect(RuleCondition.SenderNotInContacts) }
                        )
                    }
                    ConditionType.THREAD_CATEGORY -> {
                        CategoryConfig(
                            onCancel = { selectedType = null },
                            onCreate = onSelect
                        )
                    }
                    null -> {
                        ConditionTypeList(onSelectType = { selectedType = it })
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionTypeList(onSelectType: (ConditionType) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ConditionTypeItem(
                icon = Icons.Default.MarkEmailUnread,
                title = "Is Unread",
                description = "Message has not been read",
                onClick = { onSelectType(ConditionType.IS_UNREAD) }
            )
        }
        item {
            ConditionTypeItem(
                icon = Icons.Default.Pattern,
                title = "Matches Pattern",
                description = "Message matches regex pattern",
                onClick = { onSelectType(ConditionType.MATCHES_PATTERN) }
            )
        }
        item {
            ConditionTypeItem(
                icon = Icons.Default.Contacts,
                title = "Sender In Contacts",
                description = "Sender is saved in contacts",
                onClick = { onSelectType(ConditionType.SENDER_IN_CONTACTS) }
            )
        }
        item {
            ConditionTypeItem(
                icon = Icons.Default.PersonOff,
                title = "Sender Not In Contacts",
                description = "Sender is not in contacts",
                onClick = { onSelectType(ConditionType.SENDER_NOT_IN_CONTACTS) }
            )
        }
        item {
            ConditionTypeItem(
                icon = Icons.Default.Category,
                title = "Thread Category",
                description = "Thread belongs to specific category",
                onClick = { onSelectType(ConditionType.THREAD_CATEGORY) }
            )
        }
    }
}

@Composable
private fun ConditionTypeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SimpleConditionConfig(
    description: String,
    onCancel: () -> Unit,
    onCreate: () -> Unit
) {
    Column {
        Text(description)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(onClick = onCreate, modifier = Modifier.weight(1f)) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun PatternConfig(
    onCancel: () -> Unit,
    onCreate: (RuleCondition) -> Unit
) {
    var pattern by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = pattern,
            onValueChange = { pattern = it },
            label = { Text("Regex Pattern") },
            placeholder = { Text("\\d{6}") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = { onCreate(RuleCondition.MatchesPattern(pattern)) },
                enabled = pattern.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun CategoryConfig(
    onCancel: () -> Unit,
    onCreate: (RuleCondition) -> Unit
) {
    var category by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            placeholder = { Text("Work, Personal, etc.") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = { onCreate(RuleCondition.ThreadCategory(category)) },
                enabled = category.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Add")
            }
        }
    }
}

/**
 * Dialog for selecting action type
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (RuleAction) -> Unit
) {
    var selectedType by remember { mutableStateOf<ActionType?>(null) }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Add Action",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedType) {
                    ActionType.AUTO_REPLY -> {
                        AutoReplyConfig(
                            onCancel = { selectedType = null },
                            onCreate = onSelect
                        )
                    }
                    ActionType.SET_CATEGORY -> {
                        SetCategoryConfig(
                            onCancel = { selectedType = null },
                            onCreate = onSelect
                        )
                    }
                    ActionType.MARK_AS_READ -> {
                        SimpleActionConfig(
                            description = "Mark message as read",
                            onCancel = { selectedType = null },
                            onCreate = { onSelect(RuleAction.MarkAsRead) }
                        )
                    }
                    ActionType.ARCHIVE -> {
                        SimpleActionConfig(
                            description = "Archive the conversation",
                            onCancel = { selectedType = null },
                            onCreate = { onSelect(RuleAction.Archive) }
                        )
                    }
                    ActionType.MUTE_NOTIFICATIONS -> {
                        SimpleActionConfig(
                            description = "Mute notifications for this thread",
                            onCancel = { selectedType = null },
                            onCreate = { onSelect(RuleAction.MuteNotifications) }
                        )
                    }
                    ActionType.PIN_CONVERSATION -> {
                        SimpleActionConfig(
                            description = "Pin the conversation",
                            onCancel = { selectedType = null },
                            onCreate = { onSelect(RuleAction.PinConversation) }
                        )
                    }
                    ActionType.BLOCK_SENDER -> {
                        SimpleActionConfig(
                            description = "Block the sender",
                            onCancel = { selectedType = null },
                            onCreate = { onSelect(RuleAction.BlockSender) }
                        )
                    }
                    null -> {
                        ActionTypeList(onSelectType = { selectedType = it })
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTypeList(onSelectType: (ActionType) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ActionTypeItem(
                icon = Icons.AutoMirrored.Filled.Reply,
                title = "Auto-Reply",
                description = "Send automatic reply",
                onClick = { onSelectType(ActionType.AUTO_REPLY) }
            )
        }
        item {
            ActionTypeItem(
                icon = Icons.Default.Category,
                title = "Set Category",
                description = "Assign thread category",
                onClick = { onSelectType(ActionType.SET_CATEGORY) }
            )
        }
        item {
            ActionTypeItem(
                icon = Icons.Default.MarkEmailRead,
                title = "Mark as Read",
                description = "Mark message and thread as read",
                onClick = { onSelectType(ActionType.MARK_AS_READ) }
            )
        }
        item {
            ActionTypeItem(
                icon = Icons.Default.Archive,
                title = "Archive",
                description = "Archive the conversation",
                onClick = { onSelectType(ActionType.ARCHIVE) }
            )
        }
        item {
            ActionTypeItem(
                icon = Icons.Default.NotificationsOff,
                title = "Mute Notifications",
                description = "Silence notifications",
                onClick = { onSelectType(ActionType.MUTE_NOTIFICATIONS) }
            )
        }
        item {
            ActionTypeItem(
                icon = Icons.Default.PushPin,
                title = "Pin Conversation",
                description = "Pin to top of list",
                onClick = { onSelectType(ActionType.PIN_CONVERSATION) }
            )
        }
        item {
            ActionTypeItem(
                icon = Icons.Default.Block,
                title = "Block Sender",
                description = "Block this contact",
                onClick = { onSelectType(ActionType.BLOCK_SENDER) }
            )
        }
    }
}

@Composable
private fun ActionTypeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SimpleActionConfig(
    description: String,
    onCancel: () -> Unit,
    onCreate: () -> Unit
) {
    Column {
        Text(description)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(onClick = onCreate, modifier = Modifier.weight(1f)) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun AutoReplyConfig(
    onCancel: () -> Unit,
    onCreate: (RuleAction) -> Unit
) {
    var message by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Reply Message") },
            placeholder = { Text("I'll get back to you soon") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = { onCreate(RuleAction.AutoReply(message)) },
                enabled = message.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun SetCategoryConfig(
    onCancel: () -> Unit,
    onCreate: (RuleAction) -> Unit
) {
    var category by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            placeholder = { Text("Work, Personal, etc.") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = { onCreate(RuleAction.SetCategory(category)) },
                enabled = category.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Add")
            }
        }
    }
}

// Type enums for picker selection
private enum class TriggerType {
    ALWAYS, FROM_SENDER, CONTAINS_KEYWORD, TIME_RANGE, DAYS_OF_WEEK
}

private enum class ConditionType {
    IS_UNREAD, MATCHES_PATTERN, SENDER_IN_CONTACTS, SENDER_NOT_IN_CONTACTS, THREAD_CATEGORY
}

private enum class ActionType {
    AUTO_REPLY, SET_CATEGORY, MARK_AS_READ, ARCHIVE, MUTE_NOTIFICATIONS, PIN_CONVERSATION, BLOCK_SENDER
}
