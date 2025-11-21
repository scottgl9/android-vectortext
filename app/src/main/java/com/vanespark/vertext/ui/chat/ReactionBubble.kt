package com.vanespark.vertext.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanespark.vertext.data.model.Reaction

/**
 * Displays reactions on a message bubble
 * Shows emojis with optional sender count (for group messages)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionRow(
    reactions: List<Reaction>,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return

    // Group reactions by emoji and count them
    val groupedReactions = reactions.groupBy { it.emoji }
        .map { (emoji, reactionList) ->
            ReactionGroup(
                emoji = emoji,
                count = reactionList.size,
                senders = reactionList.map { it.senderName ?: it.sender }
            )
        }

    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier,
        contentAlignment = alignment
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            groupedReactions.forEach { reactionGroup ->
                ReactionBubble(
                    emoji = reactionGroup.emoji,
                    count = reactionGroup.count,
                    senders = reactionGroup.senders
                )
            }
        }
    }
}

/**
 * Individual reaction bubble showing emoji, count, and sender
 */
@Composable
private fun ReactionBubble(
    emoji: String,
    count: Int,
    senders: List<String>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            Text(
                text = emoji,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 4.dp)
            )

            // Show sender name for single reactions, or count for multiple
            if (count == 1 && senders.isNotEmpty()) {
                // Single reaction - show who reacted
                Text(
                    text = senders.first(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            } else if (count > 1) {
                // Multiple reactions - show count
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Data class for grouped reactions
 */
private data class ReactionGroup(
    val emoji: String,
    val count: Int,
    val senders: List<String>
)
