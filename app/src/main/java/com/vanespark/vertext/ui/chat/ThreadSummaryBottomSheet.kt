package com.vanespark.vertext.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vanespark.vertext.domain.service.ExcerptType
import com.vanespark.vertext.domain.service.MessageExcerpt
import com.vanespark.vertext.domain.service.ThreadSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet showing thread summary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadSummaryBottomSheet(
    uiState: ThreadSummaryUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.isVisible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Summarize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Conversation Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Content
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error,
                        onRetry = onRetry
                    )
                }
                uiState.summary != null -> {
                    SummaryContent(summary = uiState.summary)
                }
            }
        }
    }
}

/**
 * Loading state
 */
@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = "Analyzing conversation...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error state
 */
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Failed to generate summary",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(onClick = onRetry) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

/**
 * Summary content
 */
@Composable
private fun SummaryContent(
    summary: ThreadSummary,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Statistics card
        item {
            StatsCard(summary = summary)
        }

        // Description
        item {
            Text(
                text = summary.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Excerpts header
        if (summary.excerpts.isNotEmpty()) {
            item {
                Text(
                    text = "Message Highlights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Excerpts
            items(summary.excerpts) { excerpt ->
                ExcerptItem(excerpt = excerpt)
            }
        }
    }
}

/**
 * Statistics card
 */
@Composable
private fun StatsCard(
    summary: ThreadSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recipient
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = summary.recipient,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.AutoMirrored.Filled.Message,
                    label = "Messages",
                    value = summary.messageCount.toString()
                )
                StatItem(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Sent",
                    value = summary.sentCount.toString()
                )
                StatItem(
                    icon = Icons.Default.Inbox,
                    label = "Received",
                    value = summary.receivedCount.toString()
                )
            }

            // Date range
            if (summary.firstMessageDate != null && summary.lastMessageDate != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))

                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
                Text(
                    text = "From ${dateFormat.format(Date(summary.firstMessageDate))} to ${dateFormat.format(Date(summary.lastMessageDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Single stat item
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

/**
 * Message excerpt item
 */
@Composable
private fun ExcerptItem(
    excerpt: MessageExcerpt,
    modifier: Modifier = Modifier
) {
    if (excerpt.isSeparator) {
        // Separator
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = excerpt.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        // Message excerpt
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (excerpt.type) {
                    ExcerptType.SENT -> MaterialTheme.colorScheme.primaryContainer
                    ExcerptType.RECEIVED -> MaterialTheme.colorScheme.secondaryContainer
                    ExcerptType.SEPARATOR -> MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (excerpt.type) {
                            ExcerptType.SENT -> Icons.AutoMirrored.Filled.Send
                            ExcerptType.RECEIVED -> Icons.Default.Inbox
                            ExcerptType.SEPARATOR -> Icons.Default.MoreHoriz
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = when (excerpt.type) {
                            ExcerptType.SENT -> MaterialTheme.colorScheme.onPrimaryContainer
                            ExcerptType.RECEIVED -> MaterialTheme.colorScheme.onSecondaryContainer
                            ExcerptType.SEPARATOR -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = excerpt.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (excerpt.type) {
                            ExcerptType.SENT -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            ExcerptType.RECEIVED -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            ExcerptType.SEPARATOR -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        }
                    )
                }
                Text(
                    text = excerpt.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (excerpt.type) {
                        ExcerptType.SENT -> MaterialTheme.colorScheme.onPrimaryContainer
                        ExcerptType.RECEIVED -> MaterialTheme.colorScheme.onSecondaryContainer
                        ExcerptType.SEPARATOR -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
