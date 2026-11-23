package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.LrcParser
import com.example.data.PlaybackState
import com.example.model.CommunityTrackStats
import com.example.model.JamSession
import com.example.model.TrackComment
import com.example.model.TrackUiModel
import com.example.model.UserTrackReaction
import com.example.ui.theme.HeartPink
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.OrbitronFontFamily
import kotlinx.coroutines.delay

@Composable
fun FullScreenPlayerOverlay(
    visible: Boolean,
    playbackState: PlaybackState,
    isLyricsLoading: Boolean = false,
    lyricsStatusMessage: String? = null,
    lyricsOffsetMs: Long = 0L,
    communityStats: CommunityTrackStats? = null,
    userReaction: UserTrackReaction? = null,
    activeJam: JamSession? = null,
    trackComments: List<TrackComment> = emptyList(),
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
    onEditLyrics: (TrackUiModel) -> Unit,
    onRequestAutoLyrics: (TrackUiModel, Boolean) -> Unit = { _, _ -> },
    onAdjustLyricsOffset: (String, Long) -> Unit = { _, _ -> },
    onResetLyricsOffset: (String) -> Unit = {},
    onToggleReaction: (String, String) -> Unit = { _, _ -> },
    onOpenJamSheet: () -> Unit = {},
    onOpenComments: () -> Unit = {},
    onOpenDrivingMode: () -> Unit = {},
    onOpenArtist: (TrackUiModel) -> Unit = {},
    onAddToPlaylist: (TrackUiModel) -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {}
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
                isLyricsLoading = isLyricsLoading,
                lyricsStatusMessage = lyricsStatusMessage,
                lyricsOffsetMs = lyricsOffsetMs,
                communityStats = communityStats,
                userReaction = userReaction,
                activeJam = activeJam,
                trackComments = trackComments,
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
                onEditLyrics = { onEditLyrics(track) },
                onRequestAutoLyrics = { force -> onRequestAutoLyrics(track, force) },
                onAdjustLyricsOffset = { delta -> onAdjustLyricsOffset(track.id, delta) },
                onResetLyricsOffset = { onResetLyricsOffset(track.id) },
                onToggleReaction = { type -> onToggleReaction(track.id, type) },
                onOpenJamSheet = onOpenJamSheet,
                onOpenComments = onOpenComments,
                onOpenDrivingMode = onOpenDrivingMode,
                onOpenArtist = { onOpenArtist(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
                onOpenQueue = onOpenQueue,
                onOpenEqualizer = onOpenEqualizer,
                onOpenSleepTimer = onOpenSleepTimer
            )
        }
    }
}

@Composable
private fun FullScreenPlayerContent(
    track: TrackUiModel,
    playbackState: PlaybackState,
    isLyricsLoading: Boolean,
    lyricsStatusMessage: String?,
    lyricsOffsetMs: Long,
    communityStats: CommunityTrackStats?,
    userReaction: UserTrackReaction?,
    activeJam: JamSession?,
    trackComments: List<TrackComment>,
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
    onEditLyrics: () -> Unit,
    onRequestAutoLyrics: (Boolean) -> Unit,
    onAdjustLyricsOffset: (Long) -> Unit,
    onResetLyricsOffset: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onOpenJamSheet: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenDrivingMode: () -> Unit,
    onOpenArtist: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalAmdvPalette.current
    var showLyricsView by remember(track.id) { mutableStateOf(false) }
    var showKaraokeMode by remember(track.id) { mutableStateOf(false) }
    var showLyricCardStudio by remember(track.id) { mutableStateOf(false) }
    var showVisualizer by remember { mutableStateOf(true) }
    var showHeartBurst by remember { mutableStateOf(false) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(750L)
            showHeartBurst = false
        }
    }

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

    LaunchedEffect(showLyricsView, track.id) {
        if (showLyricsView && track.lyrics.isBlank() && !isLyricsLoading) {
            onRequestAutoLyrics(false)
        }
    }

    val artScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying) 1f else 0.93f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "art_scale"
    )

    var rawCoverAccent by remember { mutableStateOf(palette.accent) }
    LaunchedEffect(track.coverKey, palette.accent) {
        rawCoverAccent = CoverColorExtractor.extractAccentColor(
            context = context,
            coverKey = track.coverKey,
            fallback = palette.accent
        )
    }
    val dynamicCoverAccent by animateColorAsState(
        targetValue = rawCoverAccent,
        animationSpec = tween(durationMillis = 550),
        label = "dynamic_cover_accent"
    )

    val topGlowColor = if (palette.isDark) {
        dynamicCoverAccent.copy(alpha = 0.42f)
    } else {
        dynamicCoverAccent.copy(alpha = 0.22f)
    }
    val midGlowColor = if (palette.isDark) {
        dynamicCoverAccent.copy(alpha = 0.16f)
    } else {
        dynamicCoverAccent.copy(alpha = 0.08f)
    }
    val secondaryGlowColor = if (palette.isDark) {
        dynamicCoverAccent.copy(alpha = 0.28f)
    } else {
        dynamicCoverAccent.copy(alpha = 0.12f)
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
                            midGlowColor,
                            palette.background.copy(alpha = 0.96f),
                            palette.background
                        )
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryGlowColor, Color.Transparent),
                        center = Offset(size.width * 0.80f, size.height * 0.24f),
                        radius = size.width * 0.90f
                    ),
                    center = Offset(size.width * 0.80f, size.height * 0.24f),
                    radius = size.width * 0.90f
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
                // 1. Top Header Bar (drag down to collapse)
                var headerDragTotal by remember { mutableFloatStateOf(0f) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { headerDragTotal = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    headerDragTotal += dragAmount
                                },
                                onDragEnd = {
                                    if (headerDragTotal > 110f) {
                                        onCollapse()
                                    }
                                    headerDragTotal = 0f
                                }
                            )
                        }
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
                            text = if (activeJam != null) "JAM • ${activeJam.roomCode}" else "REPRODUCIENDO AHORA",
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
                        onClick = onOpenDrivingMode,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_driving_mode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = "Modo conducción",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenJamSheet,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_jam_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Podcasts,
                            contentDescription = "Jam en vivo",
                            tint = if (activeJam != null) palette.accent else palette.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
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

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Hero Artwork (Swipe L/R next/prev, double-tap favorite, audio visualizer) OR Scrollable Lyrics Area
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
                            var horizontalDragAccum by remember(track.id) { mutableFloatStateOf(0f) }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(coverSize)
                                        .scale(artScale)
                                        .pointerInput(track.id) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { horizontalDragAccum = 0f },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    horizontalDragAccum += dragAmount
                                                },
                                                onDragEnd = {
                                                    if (horizontalDragAccum < -90f) {
                                                        onNext()
                                                    } else if (horizontalDragAccum > 90f) {
                                                        onPrevious()
                                                    }
                                                    horizontalDragAccum = 0f
                                                }
                                            )
                                        }
                                        .pointerInput(track.id) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    showHeartBurst = true
                                                    onToggleFavorite()
                                                }
                                            )
                                        }
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = palette.surfaceElevated,
                                        border = BorderStroke(
                                            width = 1.5.dp,
                                            color = dynamicCoverAccent.copy(alpha = if (playbackState.isPlaying) 0.72f else 0.35f)
                                        ),
                                        shadowElevation = 18.dp,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        TrackCoverArt(
                                            coverKey = track.coverKey,
                                            title = track.title,
                                            size = coverSize,
                                            cornerRadius = 24.dp
                                        )
                                    }

                                    // Double-tap heart burst animation
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showHeartBurst,
                                        enter = fadeIn(tween(120)),
                                        exit = fadeOut(tween(350))
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(86.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.55f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                contentDescription = null,
                                                tint = HeartPink,
                                                modifier = Modifier.size(50.dp)
                                            )
                                        }
                                    }

                                    // Floating SoundCloud-style timed comment pill over cover art
                                    FloatingTimedCommentBanner(
                                        comments = trackComments,
                                        positionMs = playbackState.positionMs,
                                        onClick = onOpenComments,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(horizontal = 10.dp, vertical = 10.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Reactive Audio Spectrum Visualizer
                                if (showVisualizer) {
                                    ReactiveAudioVisualizer(
                                        isPlaying = playbackState.isPlaying,
                                        accentColor = dynamicCoverAccent,
                                        secondaryColor = palette.secondaryAccent,
                                        bandLevelsMb = playbackState.audioEffects.bands.map { it.levelMb },
                                        modifier = Modifier
                                            .fillMaxWidth(0.84f)
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { showVisualizer = false }
                                            .testTag("player_audio_visualizer")
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                // Source / Offline status pill + visualizer toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
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

                                    Surface(
                                        onClick = { showVisualizer = !showVisualizer },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (showVisualizer) dynamicCoverAccent.copy(alpha = 0.16f) else palette.surfaceElevated.copy(alpha = 0.72f),
                                        border = BorderStroke(1.dp, if (showVisualizer) dynamicCoverAccent else palette.border),
                                        modifier = Modifier.testTag("player_visualizer_toggle_btn")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.GraphicEq,
                                                contentDescription = "Visualizador de audio",
                                                tint = if (showVisualizer) dynamicCoverAccent else palette.textSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Full Lyrics Panel inside Full-Screen Player (LRCLIB synced karaoke + fine sync offset + plain fallback)
                            val syncedLines = remember(track.lyrics) {
                                LrcParser.parseSyncedLyrics(track.lyrics)
                            }
                            val effectivePositionMs = (displayedPositionMs + lyricsOffsetMs).coerceAtLeast(0L)
                            val activeLineIndex = remember(syncedLines, effectivePositionMs) {
                                LrcParser.findActiveLineIndex(syncedLines, effectivePositionMs)
                            }
                            val lyricsListState = rememberLazyListState()

                            LaunchedEffect(activeLineIndex, track.id, syncedLines.size) {
                                if (syncedLines.isNotEmpty() && activeLineIndex >= 0) {
                                    val targetIndex = (activeLineIndex - 2).coerceAtLeast(0)
                                    lyricsListState.animateScrollToItem(targetIndex)
                                }
                            }

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
                                        .padding(horizontal = 18.dp, vertical = 14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "LETRA",
                                                fontFamily = OrbitronFontFamily,
                                                fontSize = 12.sp,
                                                letterSpacing = 1.4.sp,
                                                color = palette.accent,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (syncedLines.isNotEmpty()) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = palette.accent.copy(alpha = 0.14f),
                                                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f))
                                                ) {
                                                    Text(
                                                        text = "SINCRONIZADA",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.accent,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (track.lyrics.isNotBlank()) {
                                                IconButton(
                                                    onClick = { showLyricCardStudio = true },
                                                    modifier = Modifier
                                                        .minimumInteractiveComponentSize()
                                                        .testTag("player_lyric_card_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.AutoAwesome,
                                                        contentDescription = "Crear tarjeta de letra para compartir",
                                                        tint = palette.accent,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { showKaraokeMode = true },
                                                    modifier = Modifier
                                                        .minimumInteractiveComponentSize()
                                                        .testTag("player_open_karaoke_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Fullscreen,
                                                        contentDescription = "Modo Karaoke pantalla completa",
                                                        tint = palette.accent,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { onRequestAutoLyrics(true) },
                                                enabled = !isLyricsLoading,
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("player_refresh_lrclib_btn")
                                            ) {
                                                if (isLyricsLoading) {
                                                    CircularProgressIndicator(
                                                        color = palette.accent,
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Filled.Refresh,
                                                        contentDescription = "Buscar letra en LRCLIB",
                                                        tint = palette.accent,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
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

                                    if (syncedLines.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LyricsSyncOffsetBar(
                                            offsetMs = lyricsOffsetMs,
                                            onAdjustOffset = onAdjustLyricsOffset,
                                            onResetOffset = onResetLyricsOffset
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    when {
                                        isLyricsLoading && track.lyrics.isBlank() -> {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        color = palette.accent,
                                                        strokeWidth = 2.8.dp,
                                                        modifier = Modifier.size(34.dp)
                                                    )
                                                    Text(
                                                        text = "Buscando letra en LRCLIB…",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = palette.textSecondary,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }

                                        syncedLines.isNotEmpty() -> {
                                            LazyColumn(
                                                state = lyricsListState,
                                                contentPadding = PaddingValues(top = 18.dp, bottom = 48.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .testTag("player_synced_lyrics_list")
                                            ) {
                                                itemsIndexed(syncedLines) { index, line ->
                                                    val isActive = index == activeLineIndex
                                                    val isPast = activeLineIndex >= 0 && index < activeLineIndex
                                                    val lineBg = if (isActive) {
                                                        palette.accent.copy(alpha = if (palette.isDark) 0.16f else 0.12f)
                                                    } else {
                                                        Color.Transparent
                                                    }
                                                    val lineTextColor = when {
                                                        isActive -> if (palette.isDark) palette.textPrimary else palette.accent
                                                        isPast -> palette.textSecondary.copy(alpha = 0.45f)
                                                        else -> palette.textPrimary.copy(alpha = 0.72f)
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = lineBg,
                                                        border = if (isActive) {
                                                            BorderStroke(1.dp, palette.accent.copy(alpha = 0.4f))
                                                        } else null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .clickable {
                                                                onSeekTo((line.timeMs - lyricsOffsetMs).coerceAtLeast(0L))
                                                            }
                                                    ) {
                                                        Text(
                                                            text = line.text,
                                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                                fontSize = if (isActive) 20.sp else 16.sp,
                                                                lineHeight = if (isActive) 28.sp else 23.sp,
                                                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium
                                                            ),
                                                            color = lineTextColor,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        track.lyrics.isNotBlank() -> {
                                            val plainText = remember(track.lyrics) {
                                                LrcParser.stripTimestamps(track.lyrics).ifBlank { track.lyrics }
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(rememberScrollState())
                                            ) {
                                                Text(
                                                    text = plainText,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        lineHeight = 27.sp
                                                    ),
                                                    color = palette.textPrimary
                                                )
                                            }
                                        }

                                        else -> {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Text(
                                                        text = lyricsStatusMessage
                                                            ?: "Esta canción aún no tiene letra guardada.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = palette.textSecondary,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Button(
                                                        onClick = { onRequestAutoLyrics(true) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = palette.accent,
                                                            contentColor = if (palette.isDark) Color.Black else Color.White
                                                        ),
                                                        shape = RoundedCornerShape(24.dp),
                                                        modifier = Modifier
                                                            .height(48.dp)
                                                            .testTag("player_fetch_lrclib_btn")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Refresh,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Buscar en LRCLIB",
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    OutlinedButton(
                                                        onClick = onEditLyrics,
                                                        shape = RoundedCornerShape(24.dp),
                                                        border = BorderStroke(1.dp, palette.border),
                                                        modifier = Modifier.height(48.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Edit,
                                                            contentDescription = null,
                                                            tint = palette.textPrimary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Agregar letra manual",
                                                            color = palette.textPrimary
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
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onOpenArtist)
                                .testTag("player_artist_link")
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
                        onClick = onAddToPlaylist,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_add_playlist_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Añadir a playlist",
                            tint = palette.textPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(25.dp)
                        )
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

                // 3b. Community Quick Reactions Row (🔥 ❤️ 🎧)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ReactionPill(
                        emoji = "🔥",
                        count = communityStats?.fireCount ?: 0L,
                        active = userReaction?.fire == true,
                        onClick = { onToggleReaction("fire") },
                        testTag = "player_react_fire_btn"
                    )
                    ReactionPill(
                        emoji = "❤️",
                        count = communityStats?.heartCount ?: 0L,
                        active = userReaction?.heart == true,
                        onClick = { onToggleReaction("heart") },
                        testTag = "player_react_heart_btn"
                    )
                    ReactionPill(
                        emoji = "🎧",
                        count = communityStats?.headphonesCount ?: 0L,
                        active = userReaction?.headphones == true,
                        onClick = { onToggleReaction("headphones") },
                        testTag = "player_react_headphones_btn"
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        onClick = onOpenComments,
                        shape = RoundedCornerShape(16.dp),
                        color = if (trackComments.isNotEmpty()) palette.accent.copy(alpha = 0.16f) else palette.surfaceElevated.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, if (trackComments.isNotEmpty()) palette.accent.copy(alpha = 0.7f) else palette.border),
                        modifier = Modifier.testTag("player_comments_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChatBubbleOutline,
                                contentDescription = "Comentarios con marca de tiempo",
                                tint = if (trackComments.isNotEmpty()) palette.accent else palette.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (trackComments.isNotEmpty()) "${trackComments.size}" else "Comentar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (trackComments.isNotEmpty()) palette.accent else palette.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

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

                Spacer(modifier = Modifier.height(10.dp))

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
                            .height(44.dp)
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
                                text = if (showLyricsView) "Portada" else "Ver letra",
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
                            .weight(1.1f)
                            .height(44.dp)
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
                                contentDescription = if (muteNotifOn) "Silenciar al llegar notificaciones" else "No silenciar con notificaciones",
                                tint = if (muteNotifOn) palette.accent else palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (muteNotifOn) "Silenciar notif." else "Sin cortes",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (muteNotifOn) palette.accent else palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 7. Extra Controls Bar: Queue, Equalizer & Sleep Timer
                val eqActive = playbackState.audioEffects.enabled
                val timerActive = playbackState.sleepTimerRemainingMs != null || playbackState.sleepAfterCurrentTrack
                val timerText = when {
                    playbackState.sleepAfterCurrentTrack -> "Fin pista"
                    playbackState.sleepTimerRemainingMs != null -> formatSleepCountdown(playbackState.sleepTimerRemainingMs)
                    else -> "Timer"
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        onClick = onOpenQueue,
                        shape = RoundedCornerShape(20.dp),
                        color = palette.surfaceElevated.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("player_queue_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Cola de reproducción",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Cola (${playbackState.queue.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenEqualizer,
                        shape = RoundedCornerShape(20.dp),
                        color = if (eqActive) palette.accent.copy(alpha = 0.16f) else palette.surfaceElevated.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, if (eqActive) palette.accent else palette.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("player_eq_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Equalizer,
                                contentDescription = "Ecualizador",
                                tint = if (eqActive) palette.accent else palette.textPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (eqActive) playbackState.audioEffects.preset.label else "EQ",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (eqActive) palette.accent else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenSleepTimer,
                        shape = RoundedCornerShape(20.dp),
                        color = if (timerActive) palette.accent.copy(alpha = 0.16f) else palette.surfaceElevated.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, if (timerActive) palette.accent else palette.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("player_timer_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Temporizador de apagado",
                                tint = if (timerActive) palette.accent else palette.textPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = timerText,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (timerActive) palette.accent else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        FullScreenKaraokeOverlay(
            visible = showKaraokeMode,
            track = track,
            playbackState = playbackState,
            dynamicAccent = dynamicCoverAccent,
            lyricsOffsetMs = lyricsOffsetMs,
            onAdjustLyricsOffset = onAdjustLyricsOffset,
            onResetLyricsOffset = onResetLyricsOffset,
            onSeekTo = onSeekTo,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onDismiss = { showKaraokeMode = false }
        )

        if (showLyricCardStudio && track.lyrics.isNotBlank()) {
            LyricCardStudioSheet(
                track = track,
                accentColor = dynamicCoverAccent,
                initialPositionMs = displayedPositionMs,
                onDismiss = { showLyricCardStudio = false }
            )
        }
    }
}

@Composable
private fun ReactionPill(
    emoji: String,
    count: Long,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalAmdvPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (active) palette.accent.copy(alpha = 0.22f) else palette.surfaceElevated.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, if (active) palette.accent else palette.border),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(text = emoji, fontSize = 13.sp)
            Text(
                text = if (count > 0) count.toString() else "+",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = if (active) palette.accent else palette.textSecondary
            )
        }
    }
}

private fun formatPlaybackMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "$mins:${secs.toString().padStart(2, '0')}"
}
