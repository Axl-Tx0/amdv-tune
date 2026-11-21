package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlaybackState
import com.example.model.TrackUiModel
import com.example.ui.theme.HeartPink
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.OrbitronFontFamily

@Composable
fun FullScreenPlayerOverlay(
    visible: Boolean,
    playbackState: PlaybackState,
    isAiLyricsLoading: Boolean,
    aiLyricsError: String?,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleMuteOnNotifications: () -> Unit,
    onToggleFavorite: (TrackUiModel) -> Unit,
    onDownloadTrack: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onFetchAiLyrics: (TrackUiModel) -> Unit,
    onEditLyrics: (TrackUiModel) -> Unit
) {
    val track = playbackState.currentTrack

    BackHandler(enabled = visible && track != null) {
        onCollapse()
    }

    AnimatedVisibility(
        visible = visible && track != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(240)
        ) + fadeOut(tween(180))
    ) {
        if (track != null) {
            FullScreenPlayerContent(
                track = track,
                playbackState = playbackState,
                isAiLyricsLoading = isAiLyricsLoading,
                aiLyricsError = aiLyricsError,
                onCollapse = onCollapse,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeekTo = onSeekTo,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onToggleMuteOnNotifications = onToggleMuteOnNotifications,
                onToggleFavorite = { onToggleFavorite(track) },
                onDownloadTrack = { onDownloadTrack(track.id) },
                onRemoveDownload = { onRemoveDownload(track.id) },
                onFetchAiLyrics = { onFetchAiLyrics(track) },
                onEditLyrics = { onEditLyrics(track) }
            )
        }
    }
}

@Composable
private fun FullScreenPlayerContent(
    track: TrackUiModel,
    playbackState: PlaybackState,
    isAiLyricsLoading: Boolean,
    aiLyricsError: String?,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleMuteOnNotifications: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownloadTrack: () -> Unit,
    onRemoveDownload: () -> Unit,
    onFetchAiLyrics: () -> Unit,
    onEditLyrics: () -> Unit
) {
    val palette = LocalAmdvPalette.current
    var showLyricsView by remember(track.id) { mutableStateOf(false) }

    val durationMs = playbackState.durationMs.coerceAtLeast(track.durationSec * 1000L).coerceAtLeast(1000L)
    var isDragging by remember(track.id) { mutableStateOf(false) }
    var dragFraction by remember(track.id) { mutableFloatStateOf(0f) }

    val liveFraction = (playbackState.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val sliderValue = if (isDragging) dragFraction else liveFraction
    val displayedPositionMs = if (isDragging) (dragFraction * durationMs).toLong() else playbackState.positionMs

    LaunchedEffect(track.id) {
        isDragging = false
        dragFraction = 0f
    }

    val artScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying) 1f else 0.93f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "art_scale"
    )

    val topGlowColor = if (palette.isDark) {
        palette.accent.copy(alpha = 0.22f)
    } else {
        palette.accent.copy(alpha = 0.14f)
    }
    val secondaryGlowColor = if (palette.isDark) {
        palette.secondaryAccent.copy(alpha = 0.16f)
    } else {
        palette.secondaryAccent.copy(alpha = 0.08f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            topGlowColor,
                            palette.background.copy(alpha = 0.92f),
                            palette.background
                        )
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryGlowColor, Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.22f),
                        radius = size.width * 0.85f
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.22f),
                    radius = size.width * 0.85f
                )
            }
            .clickable(enabled = true, onClick = {}) // Consumes clicks so background list cannot be tapped through
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.TopCenter
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            val coverSize = min(
                maxWidth - 24.dp,
                (maxHeight * 0.39f).coerceIn(180.dp, 330.dp)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_collapse_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Minimizar reproductor",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "REPRODUCIENDO AHORA",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (track.albumTitle.isNotBlank()) {
                            Text(
                                text = track.albumTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { showLyricsView = !showLyricsView },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_toggle_lyrics_top_btn")
                    ) {
                        Icon(
                            imageVector = if (showLyricsView) Icons.Filled.MusicNote else Icons.Filled.Mic,
                            contentDescription = if (showLyricsView) "Ver portada" else stringResource(R.string.cd_lyrics),
                            tint = if (showLyricsView) palette.accent else palette.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Hero Artwork OR Scrollable Lyrics Area
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = showLyricsView,
                        transitionSpec = {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                        },
                        label = "player_center_content"
                    ) { showingLyrics ->
                        if (!showingLyrics) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = palette.surfaceElevated,
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        color = palette.accent.copy(alpha = if (playbackState.isPlaying) 0.55f else 0.25f)
                                    ),
                                    shadowElevation = 18.dp,
                                    modifier = Modifier
                                        .size(coverSize)
                                        .scale(artScale)
                                ) {
                                    TrackCoverArt(
                                        coverKey = track.coverKey,
                                        title = track.title,
                                        size = coverSize,
                                        cornerRadius = 24.dp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Source / Offline status pill
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = palette.surfaceElevated.copy(alpha = 0.72f),
                                    border = BorderStroke(1.dp, palette.border)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        val badgeIcon = when {
                                            playbackState.isSilencedForNotification -> Icons.Filled.NotificationsOff
                                            track.isDownloaded -> Icons.Filled.DownloadDone
                                            track.isCloudCatalog -> Icons.Filled.CloudDone
                                            else -> Icons.Filled.MusicNote
                                        }
                                        val badgeLabel = when {
                                            playbackState.isSilencedForNotification -> "Silenciado por notificación"
                                            track.isDownloaded -> "Disponible offline"
                                            track.isCloudCatalog -> "Catálogo en la nube"
                                            else -> "Biblioteca local"
                                        }
                                        Icon(
                                            imageVector = badgeIcon,
                                            contentDescription = null,
                                            tint = if (playbackState.isSilencedForNotification) palette.secondaryAccent else palette.accent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = badgeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (playbackState.isSilencedForNotification) palette.secondaryAccent else palette.textSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            // Full Lyrics Panel inside Full-Screen Player
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = palette.surface.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(18.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "LETRA",
                                            fontFamily = OrbitronFontFamily,
                                            fontSize = 12.sp,
                                            letterSpacing = 1.4.sp,
                                            color = palette.accent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(
                                                enabled = !isAiLyricsLoading,
                                                onClick = onFetchAiLyrics,
                                                modifier = Modifier.testTag("player_ai_lyrics_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = palette.accent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Buscar con IA", color = palette.accent)
                                            }
                                            IconButton(
                                                onClick = onEditLyrics,
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Editar letra",
                                                    tint = palette.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isAiLyricsLoading) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(
                                                    color = palette.accent,
                                                    modifier = Modifier.size(34.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "Buscando letra con IA para\n\"${track.title}\"…",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = palette.textSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else if (track.lyrics.isNotBlank()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            if (!aiLyricsError.isNullOrBlank()) {
                                                Text(
                                                    text = aiLyricsError,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = palette.secondaryAccent,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                            }
                                            Text(
                                                text = track.lyrics,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    lineHeight = 27.sp
                                                ),
                                                color = palette.textPrimary
                                            )
                                        }
                                    } else {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = aiLyricsError
                                                        ?: "Esta canción aún no tiene letra guardada.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (!aiLyricsError.isNullOrBlank()) palette.secondaryAccent else palette.textSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                                Button(
                                                    onClick = onFetchAiLyrics,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = palette.accent,
                                                        contentColor = if (palette.isDark) Color.Black else Color.White
                                                    ),
                                                    shape = RoundedCornerShape(24.dp),
                                                    modifier = Modifier.height(48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.AutoAwesome,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Buscar letra con IA",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Track Title, Artist, Favorite & Offline Download
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (track.isCloudCatalog) {
                        val dlProgress = track.downloadProgress
                        if (dlProgress != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                CircularProgressIndicator(
                                    progress = { dlProgress.coerceIn(0.05f, 1f) },
                                    color = palette.accent,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (track.isDownloaded) onRemoveDownload() else onDownloadTrack()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_download_btn")
                            ) {
                                Icon(
                                    imageVector = if (track.isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                    contentDescription = if (track.isDownloaded) "Descargada offline" else "Descargar offline",
                                    tint = if (track.isDownloaded) palette.accent else palette.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (track.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.cd_favorite),
                            tint = if (track.favorite) HeartPink else palette.textSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Interactive Seek Bar & Timestamps
                Slider(
                    value = sliderValue,
                    onValueChange = { value ->
                        isDragging = true
                        dragFraction = value
                    },
                    onValueChangeFinished = {
                        val targetMs = (dragFraction * durationMs).toLong()
                        onSeekTo(targetMs)
                        isDragging = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = palette.accent,
                        activeTrackColor = palette.accent,
                        inactiveTrackColor = palette.border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_seek_slider")
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = formatPlaybackMs(displayedPositionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    Text(
                        text = formatPlaybackMs(durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Transport Controls (Shuffle, Previous, Play/Pause, Next, Repeat)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Shuffle
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_shuffle_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Reproducción aleatoria",
                                tint = if (playbackState.shuffleEnabled) palette.accent else palette.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (playbackState.shuffleEnabled) palette.accent else Color.Transparent)
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("player_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Canción anterior",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Play / Pause Hero Button
                    Surface(
                        onClick = onPlayPause,
                        shape = CircleShape,
                        color = palette.accent,
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("player_play_pause_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_pause),
                                tint = if (palette.isDark) Color.Black else Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // Next
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("player_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = stringResource(R.string.cd_next),
                            tint = palette.textPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Repeat
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onToggleRepeat,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_repeat_btn")
                        ) {
                            Icon(
                                imageVector = if (playbackState.repeatOneEnabled) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                contentDescription = "Repetir canción",
                                tint = if (playbackState.repeatOneEnabled) palette.accent else palette.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (playbackState.repeatOneEnabled) palette.accent else Color.Transparent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Quick Bottom Pills: Lyrics Toggle & Silence on Notifications Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        onClick = { showLyricsView = !showLyricsView },
                        shape = RoundedCornerShape(24.dp),
                        color = palette.surfaceElevated.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, if (showLyricsView) palette.accent else palette.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("player_lyrics_pill_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (showLyricsView) Icons.Filled.MusicNote else Icons.Filled.Mic,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showLyricsView) {
                                    "Portada"
                                } else if (track.lyrics.isNotBlank()) {
                                    "Ver letra"
                                } else {
                                    "Letra IA"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val muteNotifOn = playbackState.muteOnNotificationsEnabled
                    Surface(
                        onClick = onToggleMuteOnNotifications,
                        shape = RoundedCornerShape(24.dp),
                        color = if (muteNotifOn) {
                            palette.accent.copy(alpha = 0.16f)
                        } else {
                            palette.surfaceElevated.copy(alpha = 0.85f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (muteNotifOn) palette.accent else palette.border
                        ),
                        modifier = Modifier
                            .weight(1.15f)
                            .height(48.dp)
                            .testTag("player_mute_notif_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (muteNotifOn) Icons.Filled.NotificationsOff else Icons.Filled.Notifications,
                                contentDescription = "Silenciar al recibir o ver notificaciones",
                                tint = if (muteNotifOn) palette.accent else palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (muteNotifOn) "Silenciar notif." else "Notif. normal",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (muteNotifOn) palette.accent else palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

private fun formatPlaybackMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "$mins:${secs.toString().padStart(2, '0')}"
}
