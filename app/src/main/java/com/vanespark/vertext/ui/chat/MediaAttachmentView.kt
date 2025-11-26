package com.vanespark.vertext.ui.chat

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.vanespark.vertext.R
import com.vanespark.vertext.data.model.MediaAttachment
import com.vanespark.vertext.data.model.MediaType

/**
 * Composable for displaying a media attachment (image, video, or audio)
 */
@Composable
fun MediaAttachmentView(
    attachment: MediaAttachment,
    modifier: Modifier = Modifier,
    onImageClick: ((Uri) -> Unit)? = null,
    onVideoClick: ((Uri) -> Unit)? = null
) {
    when (attachment.mediaType) {
        MediaType.IMAGE -> ImageAttachment(
            attachment = attachment,
            modifier = modifier,
            onClick = onImageClick
        )
        MediaType.VIDEO -> VideoAttachment(
            attachment = attachment,
            modifier = modifier,
            onClick = onVideoClick
        )
        MediaType.AUDIO -> AudioAttachment(
            attachment = attachment,
            modifier = modifier
        )
        MediaType.OTHER -> GenericAttachment(
            attachment = attachment,
            modifier = modifier
        )
    }
}

/**
 * Image attachment view using Coil for image loading
 */
@Composable
private fun ImageAttachment(
    attachment: MediaAttachment,
    modifier: Modifier = Modifier,
    onClick: ((Uri) -> Unit)? = null
) {
    val uri = Uri.parse(attachment.uri)

    SubcomposeAsyncImage(
        model = uri,
        contentDescription = stringResource(R.string.view_image),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(uri) }
                } else {
                    Modifier
                }
            ),
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.attachment_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

/**
 * Video attachment view with play icon overlay
 */
@Composable
private fun VideoAttachment(
    attachment: MediaAttachment,
    modifier: Modifier = Modifier,
    onClick: ((Uri) -> Unit)? = null
) {
    val uri = Uri.parse(attachment.uri)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(uri) }
                } else {
                    Modifier
                }
            )
    ) {
        // Video thumbnail (using first frame if available, or placeholder)
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = stringResource(R.string.video_thumbnail),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            },
            error = {
                // Show video icon placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        )

        // Play button overlay
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.play_video),
                tint = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/**
 * Audio attachment view with playback controls using ExoPlayer
 */
@Composable
private fun AudioAttachment(
    attachment: MediaAttachment,
    modifier: Modifier = Modifier
) {
    val uri = Uri.parse(attachment.uri)

    AudioPlayer(
        audioUri = uri,
        fileName = attachment.fileName,
        fileSize = attachment.fileSize,
        modifier = modifier
    )
}

/**
 * Generic attachment view for unsupported file types
 */
@Composable
private fun GenericAttachment(
    attachment: MediaAttachment,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = attachment.fileName ?: "Attachment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = attachment.mimeType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Format file size in human-readable format
 */
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0

    return when {
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}
