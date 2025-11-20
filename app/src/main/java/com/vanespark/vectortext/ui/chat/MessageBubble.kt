package com.vanespark.vectortext.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Message bubble component
 * Displays a single message with appropriate styling for incoming/outgoing messages
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageUiItem,
    onLongPress: (MessageUiItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine bubble alignment and color
    val alignment = if (message.isIncoming) Alignment.CenterStart else Alignment.CenterEnd
    val bubbleColor = if (message.isIncoming) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val textColor = if (message.isIncoming) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    // Determine bubble shape based on grouping
    val bubbleShape = when {
        message.isFirstInGroup && message.isLastInGroup -> RoundedCornerShape(20.dp)
        message.isFirstInGroup && !message.isIncoming -> RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 4.dp
        )
        message.isFirstInGroup && message.isIncoming -> RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 4.dp,
            bottomEnd = 20.dp
        )
        message.isLastInGroup && !message.isIncoming -> RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 4.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp
        )
        message.isLastInGroup && message.isIncoming -> RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp
        )
        !message.isIncoming -> RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 4.dp,
            bottomStart = 20.dp,
            bottomEnd = 4.dp
        )
        else -> RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 20.dp,
            bottomStart = 4.dp,
            bottomEnd = 20.dp
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Date divider (shown on first message of a new day)
        if (message.formattedDate.isNotEmpty() && message.isFirstInGroup) {
            DateDivider(date = message.formattedDate)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Message bubble
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = alignment
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = { /* Single click - could show timestamp */ },
                        onLongClick = { onLongPress(message) }
                    ),
                color = bubbleColor,
                shape = bubbleShape
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
                ) {
                    // Message text
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Timestamp and status row
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Status indicators for outgoing messages
                        if (!message.isIncoming) {
                            when {
                                message.isFailed -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Failed",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                message.isSending -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = textColor.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }
                        }

                        // Timestamp
                        Text(
                            text = message.formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Spacing between messages
        Spacer(modifier = Modifier.height(if (message.isLastInGroup) 12.dp else 2.dp))
    }
}

/**
 * Date divider component
 * Shows a date label centered with lines on both sides
 */
@Composable
private fun DateDivider(
    date: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
