package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LrcParser
import com.example.data.PlaybackState
import com.example.model.TrackUiModel
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.OrbitronFontFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun ReactiveAudioVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    secondaryColor: Color,
    bandLevelsMb: List<Int> = emptyList(),
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "audio_visualizer_phase")
    val phaseA by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "viz_phase_a"
    )
    val phaseB by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "viz_phase_b"
    )
    val energyScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.14f,
        animationSpec = tween(durationMillis = 320),
        label = "viz_energy"
    )

    val eqBoosts = remember(bandLevelsMb) {
        if (bandLevelsMb.size >= 5) {
            bandLevelsMb.map { (1f + (it / 1200f) * 0.35f).coerceIn(0.65f, 1.38f) }
        } else {
            listOf(1f, 1f, 1f, 1f, 1f)
        }
    }

    Canvas(modifier = modifier) {
        val barCount = 28
        val totalWidth = size.width
        val totalHeight = size.height
        if (totalWidth <= 0f || totalHeight <= 0f) return@Canvas

        val gap = totalWidth * 0.012f
        val barWidth = ((totalWidth - gap * (barCount - 1)) / barCount).coerceAtLeast(3f)
        val centerIdx = (barCount - 1) / 2f

        for (i in 0 until barCount) {
            val distFromCenter = abs(i - centerIdx) / centerIdx
            val envelope = (1f - distFromCenter * 0.55f).coerceIn(0.35f, 1f)

            val eqBucket = ((i.toFloat() / barCount) * eqBoosts.size).toInt().coerceIn(0, eqBoosts.lastIndex)
            val eqGain = eqBoosts[eqBucket]

            val wave1 = sin(phaseA + i * 0.58f)
            val wave2 = sin(phaseB * 1.4f - i * 0.92f) * 0.65f
            val wave3 = sin(phaseA * 2.1f + i * 1.35f) * 0.35f
            val combined = ((wave1 + wave2 + wave3) / 2f + 0.5f).coerceIn(0.08f, 1f)

            val barHeight = (totalHeight * (0.14f + 0.86f * combined * envelope * eqGain * energyScale))
                .coerceIn(4f, totalHeight)
            val x = i * (barWidth + gap)
            val topY = (totalHeight - barHeight) / 2f

            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.92f),
                    secondaryColor.copy(alpha = 0.72f),
                    accentColor.copy(alpha = 0.92f)
                ),
                startY = topY,
                endY = topY + barHeight
            )

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, topY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

fun formatLyricsOffsetSeconds(offsetMs: Long): String {
    if (offsetMs == 0L) return "0.0s"
    val sec = offsetMs / 1000.0
    val sign = if (offsetMs > 0L) "+" else ""
    return String.format("%s%.1fs", sign, sec)
}

@Composable
fun LyricsSyncOffsetBar(
    offsetMs: Long,
    onAdjustOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.surfaceElevated.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onAdjustOffset(-500L) }
                .testTag("lyrics_offset_minus_btn")
        ) {
            Text(
                text = "-0.5s",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (offsetMs != 0L) palette.accent.copy(alpha = 0.16f) else palette.surfaceElevated.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, if (offsetMs != 0L) palette.accent else palette.border),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onResetOffset)
                .testTag("lyrics_offset_reset_btn")
        ) {
            Text(
                text = "Sync ${formatLyricsOffsetSeconds(offsetMs)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (offsetMs != 0L) palette.accent else palette.textSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.surfaceElevated.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onAdjustOffset(500L) }
                .testTag("lyrics_offset_plus_btn")
        ) {
            Text(
                text = "+0.5s",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun FullScreenKaraokeOverlay(
    visible: Boolean,
    track: TrackUiModel?,
    playbackState: PlaybackState,
    dynamicAccent: Color,
    lyricsOffsetMs: Long,
    onAdjustLyricsOffset: (Long) -> Unit,
    onResetLyricsOffset: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = visible && track != null) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible && track != null,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(180))
    ) {
        if (track != null) {
            val palette = LocalAmdvPalette.current
            val syncedLines = remember(track.lyrics) {
                LrcParser.parseSyncedLyrics(track.lyrics)
            }
            val effectivePositionMs = (playbackState.positionMs + lyricsOffsetMs).coerceAtLeast(0L)
            val activeLineIndex = remember(syncedLines, effectivePositionMs) {
                LrcParser.findActiveLineIndex(syncedLines, effectivePositionMs)
            }
            val listState = rememberLazyListState()

            LaunchedEffect(activeLineIndex, track.id, syncedLines.size) {
                if (syncedLines.isNotEmpty() && activeLineIndex >= 0) {
                    val target = (activeLineIndex - 2).coerceAtLeast(0)
                    listState.animateScrollToItem(target)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dynamicAccent.copy(alpha = if (palette.isDark) 0.45f else 0.25f),
                                palette.background,
                                dynamicAccent.copy(alpha = if (palette.isDark) 0.22f else 0.12f)
                            )
                        )
                    )
                    .clickable(enabled = true, onClick = {})
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("karaoke_fullscreen_overlay")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TrackCoverArt(
                            coverKey = track.coverKey,
                            title = track.title,
                            size = 44.dp,
                            cornerRadius = 10.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MODO KARAOKE",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 10.sp,
                                letterSpacing = 1.6.sp,
                                color = dynamicAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("karaoke_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cerrar modo Karaoke",
                                tint = palette.textPrimary
                            )
                        }
                    }

                    if (syncedLines.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LyricsSyncOffsetBar(
                                offsetMs = lyricsOffsetMs,
                                onAdjustOffset = onAdjustLyricsOffset,
                                onResetOffset = onResetLyricsOffset
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Immersive Lyrics Body
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when {
                            syncedLines.isNotEmpty() -> {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(top = 48.dp, bottom = 96.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(syncedLines) { idx, line ->
                                        val isActive = idx == activeLineIndex
                                        val isPast = activeLineIndex >= 0 && idx < activeLineIndex
                                        val color = when {
                                            isActive -> if (palette.isDark) Color.White else palette.textPrimary
                                            isPast -> palette.textSecondary.copy(alpha = 0.38f)
                                            else -> palette.textPrimary.copy(alpha = 0.56f)
                                        }
                                        Text(
                                            text = line.text,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontSize = if (isActive) 28.sp else 20.sp,
                                                lineHeight = if (isActive) 36.sp else 28.sp,
                                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold
                                            ),
                                            color = color,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isActive) dynamicAccent.copy(alpha = 0.16f) else Color.Transparent
                                                )
                                                .clickable {
                                                    onSeekTo((line.timeMs - lyricsOffsetMs).coerceAtLeast(0L))
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                            track.lyrics.isNotBlank() -> {
                                val plain = remember(track.lyrics) {
                                    LrcParser.stripTimestamps(track.lyrics).ifBlank { track.lyrics }
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 16.dp)
                                ) {
                                    Text(
                                        text = plain,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontSize = 22.sp,
                                            lineHeight = 34.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = palette.textPrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "Sin letra disponible para esta canción.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = palette.textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Mini Transport + Visualizer
                    ReactiveAudioVisualizer(
                        isPlaying = playbackState.isPlaying,
                        accentColor = dynamicAccent,
                        secondaryColor = palette.secondaryAccent,
                        bandLevelsMb = playbackState.audioEffects.bands.map { it.levelMb },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = dynamicAccent,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onPlayPause)
                                .testTag("karaoke_play_pause_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                                    tint = Color.Black,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Siguiente",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
