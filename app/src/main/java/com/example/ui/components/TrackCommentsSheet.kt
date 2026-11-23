package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrackComment
import com.example.model.TrackUiModel
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.TrashRose
import kotlin.math.abs

fun formatCommentTimestamp(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

@Composable
fun FloatingTimedCommentBanner(
    comments: List<TrackComment>,
    positionMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current
    val activeComment = remember(comments, positionMs / 1000L) {
        comments.firstOrNull { abs(it.timestampMs - positionMs) <= 2600L }
    }

    AnimatedVisibility(
        visible = activeComment != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        if (activeComment != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.74f),
                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.7f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onClick)
                    .testTag("floating_timed_comment_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = formatCommentTimestamp(activeComment.timestampMs),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.accent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${activeComment.authorName}: ${activeComment.text}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackCommentsSheet(
    track: TrackUiModel,
    currentPositionMs: Long,
    comments: List<TrackComment>,
    currentUserId: String?,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onAddComment: (Long, String, (Boolean) -> Unit) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    val palette = LocalAmdvPalette.current
    var draftText by remember(track.id) { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
                .testTag("track_comments_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.ChatBubbleOutline,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COMENTARIOS EN EL TIEMPO",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = palette.accent
                    )
                    Text(
                        text = "${track.title} • ${comments.size} comentarios",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (comments.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceElevated.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(22.dp)
                    ) {
                        Text(
                            text = "Aún no hay comentarios en esta canción",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sé el primero en dejar tu reacción en el segundo ${formatCommentTimestamp(currentPositionMs)}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(comments, key = { it.id.ifBlank { "${it.userId}_${it.timestampMs}_${it.text.hashCode()}" } }) { comment ->
                        val canDelete = isAdmin || (currentUserId != null && comment.userId == currentUserId)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surfaceElevated.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = palette.accent.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onSeekTo(comment.timestampMs) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Schedule,
                                            contentDescription = "Ir al segundo",
                                            tint = palette.accent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = formatCommentTimestamp(comment.timestampMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.accent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comment.authorName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = comment.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary.copy(alpha = 0.9f)
                                    )
                                }

                                if (canDelete) {
                                    IconButton(
                                        onClick = { onDeleteComment(comment.id) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_comment_${comment.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Eliminar comentario",
                                            tint = TrashRose,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Composer pinned to current playback second
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { if (it.length <= 220) draftText = it },
                    label = { Text("Comentar en ${formatCommentTimestamp(currentPositionMs)}") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("track_comment_input")
                )
                Surface(
                    shape = CircleShape,
                    color = if (draftText.isNotBlank() && !isSending) palette.accent else palette.surfaceElevated,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = draftText.isNotBlank() && !isSending) {
                            val message = draftText.trim()
                            val stamp = currentPositionMs
                            isSending = true
                            onAddComment(stamp, message) { ok ->
                                isSending = false
                                if (ok) draftText = ""
                            }
                        }
                        .testTag("track_comment_send_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar comentario",
                            tint = if (draftText.isNotBlank() && !isSending) {
                                if (palette.isDark) Color.Black else Color.White
                            } else {
                                palette.textSecondary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
