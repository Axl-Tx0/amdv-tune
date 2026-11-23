package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.LrcParser
import com.example.data.PlaybackState
import com.example.model.AppThemeMode
import com.example.model.TrackUiModel
import com.example.ui.theme.HeartPink
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.TrashRose
import java.io.File

@Composable
fun TrackCoverArt(
    coverKey: String,
    title: String,
    size: Dp,
    cornerRadius: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(cornerRadius)
    val drawableRes = when (coverKey) {
        "COVER_CRIMSON" -> R.drawable.img_cover_crimson_1794973232429
        "COVER_SUNSET" -> R.drawable.img_cover_sunset_1794973233484
        "COVER_CYAN_DRIP" -> R.drawable.img_cover_cyan_drip_1794973233181
        "COVER_EMBER" -> R.drawable.img_cover_ember_1794973233687
        else -> null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            drawableRes != null -> {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            coverKey.startsWith("/") -> {
                AsyncImage(
                    model = File(coverKey),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            coverKey.startsWith("http://") || coverKey.startsWith("https://") -> {
                AsyncImage(
                    model = coverKey,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            coverKey.startsWith("content://") -> {
                val letter = title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "M"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF0E7490), Color(0xFF0F172A)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.44f).sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    AsyncImage(
                        model = Uri.parse(coverKey),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            else -> {
                val letter = coverKey.substringAfterLast('_').firstOrNull()?.toString()
                    ?: title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
                    ?: "M"
                val gradientColors = when {
                    coverKey.contains("CYAN") -> listOf(Color(0xFF0E7490), Color(0xFF0F172A))
                    coverKey.contains("AMBER") -> listOf(Color(0xFF92400E), Color(0xFF1C1917))
                    coverKey.contains("SILVER") -> listOf(Color(0xFF475569), Color(0xFF090D16))
                    coverKey.contains("EMERALD") -> listOf(Color(0xFF065F46), Color(0xFF06120E))
                    coverKey.contains("INDIGO") -> listOf(Color(0xFF3730A3), Color(0xFF090A1A))
                    else -> listOf(Color(0xFF7E22CE), Color(0xFF2563EB))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle equalizer lines behind monogram
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = size * 0.14f)
                    ) {
                        listOf(0.28f, 0.45f, 0.62f, 0.38f, 0.52f).forEach { ratio ->
                            Box(
                                modifier = Modifier
                                    .width((size * 0.06f).coerceAtLeast(2.dp))
                                    .height(size * ratio * 0.55f)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.22f))
                            )
                        }
                    }
                    Text(
                        text = letter,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.44f).sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchPillBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalAmdvPalette.current
    val borderBrush = if (palette.mode == AppThemeMode.NEON) {
        Brush.horizontalGradient(listOf(palette.accent.copy(alpha = 0.55f), palette.secondaryAccent.copy(alpha = 0.45f)))
    } else {
        SolidColor(palette.border)
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, borderBrush),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = stringResource(R.string.cd_search),
                tint = palette.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!readOnly) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input")
                    )
                }
            }
            if (!readOnly && query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Limpiar búsqueda",
                        tint = palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun MiniEqualizerBars(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bar1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(290), RepeatMode.Reverse),
        label = "b3"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.height(14.dp)
    ) {
        listOf(bar1, bar2, bar3).forEach { scale ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((14 * scale).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun TrackNumberedRow(
    index: Int,
    track: TrackUiModel,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    trailingActionType: TrailingActionType,
    onTrailingAction: () -> Unit,
    modifier: Modifier = Modifier,
    onDownloadClick: (() -> Unit)? = null,
    onRemoveDownloadClick: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onPlayNextClick: (() -> Unit)? = null,
    onAddToQueueClick: (() -> Unit)? = null
) {
    val palette = LocalAmdvPalette.current
    var menuExpanded by remember { mutableStateOf(false) }
    val hasOverflowMenu = onAddToPlaylistClick != null || onPlayNextClick != null || onAddToQueueClick != null
    val subtitle = if (track.albumTitle.isNotBlank()) {
        "${track.artist} • ${track.albumTitle}"
    } else {
        track.artist
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurrent) palette.surfaceElevated.copy(alpha = 0.72f) else Color.Transparent
            )
            .clickable(onClick = onPlayClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("track_row_${track.id}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(26.dp)
        ) {
            if (isCurrent && isPlaying) {
                MiniEqualizerBars(color = palette.accent)
            } else {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) palette.accent else palette.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        TrackCoverArt(
            coverKey = track.coverKey,
            title = track.title,
            size = 48.dp,
            cornerRadius = 10.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) palette.accent else palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = track.formattedDuration,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )

        if (track.isCloudCatalog) {
            val progress = track.downloadProgress
            if (progress != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0.05f, 1f) },
                        color = palette.accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (track.isDownloaded && onRemoveDownloadClick != null) {
                IconButton(
                    onClick = onRemoveDownloadClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("offline_done_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DownloadDone,
                        contentDescription = "Descargada offline",
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (!track.isDownloaded && onDownloadClick != null) {
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("download_btn_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Descargar offline",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Icon(
                imageVector = if (track.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(R.string.cd_favorite),
                tint = if (track.favorite) HeartPink else palette.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onTrailingAction,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            when (trailingActionType) {
                TrailingActionType.LYRICS -> {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.cd_lyrics),
                        tint = palette.textPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                TrailingActionType.DELETE -> {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete),
                        tint = TrashRose,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }

        if (hasOverflowMenu) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("track_overflow_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Más opciones",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = palette.surfaceElevated
                ) {
                    if (onPlayNextClick != null) {
                        DropdownMenuItem(
                            text = { Text("Reproducir siguiente", color = palette.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = null,
                                    tint = palette.accent
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onPlayNextClick()
                            }
                        )
                    }
                    if (onAddToQueueClick != null) {
                        DropdownMenuItem(
                            text = { Text("Añadir a la cola", color = palette.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = palette.accent
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onAddToQueueClick()
                            }
                        )
                    }
                    if (onAddToPlaylistClick != null) {
                        DropdownMenuItem(
                            text = { Text("Añadir a playlist", color = palette.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    tint = palette.accent
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onAddToPlaylistClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class TrailingActionType {
    LYRICS,
    DELETE
}

fun scallopedCookieShape(
    lobes: Int = 12,
    depthFraction: Float = 0.085f
): androidx.compose.foundation.shape.GenericShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val maxRadius = minOf(cx, cy)
    val baseRadius = maxRadius * (1f - depthFraction)
    val amplitude = maxRadius * depthFraction
    val steps = 144
    for (i in 0..steps) {
        val theta = (2.0 * Math.PI * i) / steps
        val r = baseRadius + amplitude * kotlin.math.cos(lobes * theta).toFloat()
        val x = cx + r * kotlin.math.cos(theta).toFloat()
        val y = cy + r * kotlin.math.sin(theta).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
fun MiniPlayerBanner(
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onBannerClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: ((TrackUiModel) -> Unit)? = null,
    onArtistShortcutClick: ((TrackUiModel) -> Unit)? = null
) {
    val palette = LocalAmdvPalette.current
    val track = playbackState.currentTrack
    val playCookieShape = remember { scallopedCookieShape(lobes = 12, depthFraction = 0.085f) }
    val artCookieShape = remember { scallopedCookieShape(lobes = 10, depthFraction = 0.07f) }
    val pillContainer = if (palette.isDark) {
        palette.surfaceElevated.copy(alpha = 0.96f)
    } else {
        palette.surface.copy(alpha = 0.98f)
    }

    AnimatedVisibility(visible = track != null, modifier = modifier) {
        if (track != null) {
            Surface(
                shape = RoundedCornerShape(38.dp),
                color = pillContainer,
                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.30f)),
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable(onClick = onBannerClick)
                    .testTag("mini_player_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(artCookieShape)
                            .background(palette.accent.copy(alpha = 0.55f))
                            .padding(2.dp)
                            .clip(artCookieShape)
                    ) {
                        TrackCoverArt(
                            coverKey = track.coverKey,
                            title = track.title,
                            size = 44.dp,
                            cornerRadius = 22.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onArtistShortcutClick != null) {
                        IconButton(
                            onClick = { onArtistShortcutClick(track) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .testTag("mini_player_artist_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Ver artista",
                                tint = palette.textPrimary.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (onToggleFavorite != null) {
                        IconButton(
                            onClick = { onToggleFavorite(track) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .testTag("mini_player_fav_btn")
                        ) {
                            Icon(
                                imageVector = if (track.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = stringResource(R.string.cd_favorite),
                                tint = if (track.favorite) HeartPink else palette.textPrimary.copy(alpha = 0.85f),
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = stringResource(R.string.cd_next),
                                tint = palette.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(playCookieShape)
                            .background(palette.accent)
                            .clickable(onClick = onPlayPause)
                            .testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = if (palette.isDark) Color(0xFF14110B) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsDetailSheet(
    track: TrackUiModel,
    isLyricsLoading: Boolean = false,
    lyricsStatusMessage: String? = null,
    onDismiss: () -> Unit,
    onSaveLyrics: (String) -> Unit,
    onRequestAutoLyrics: (Boolean) -> Unit = {}
) {
    val palette = LocalAmdvPalette.current
    var isEditing by remember(track.id) { mutableStateOf(false) }
    var draftText by remember(track.id, track.lyrics) { mutableStateOf(track.lyrics) }
    val isSynced = remember(track.lyrics) { LrcParser.hasSyncedTimestamps(track.lyrics) }
    val displayLyrics = remember(track.lyrics) {
        LrcParser.stripTimestamps(track.lyrics).ifBlank { track.lyrics }
    }

    LaunchedEffect(track.id) {
        if (track.lyrics.isBlank() && !isLyricsLoading) {
            onRequestAutoLyrics(false)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackCoverArt(
                    coverKey = track.coverKey,
                    title = track.title,
                    size = 56.dp,
                    cornerRadius = 12.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artist} • ${track.formattedDuration}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                TextButton(onClick = {
                    if (isEditing) {
                        onSaveLyrics(draftText)
                        isEditing = false
                    } else {
                        isEditing = true
                    }
                }) {
                    Text(
                        text = if (isEditing) "Guardar" else "Editar",
                        color = palette.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
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
                        text = stringResource(R.string.lyrics_sheet_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.accent
                    )
                    if (isSynced && !isEditing) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.accent.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "Sincronizada • LRCLIB",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (!isEditing) {
                    TextButton(
                        onClick = { onRequestAutoLyrics(true) },
                        enabled = !isLyricsLoading,
                        modifier = Modifier.testTag("sheet_fetch_lrclib_btn")
                    ) {
                        if (isLyricsLoading) {
                            CircularProgressIndicator(
                                color = palette.accent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buscando…", color = palette.accent)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Buscar letra en LRCLIB",
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LRCLIB", color = palette.accent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    minLines = 6,
                    maxLines = 14,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLyricsLoading && displayLyrics.isBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = palette.accent,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Buscando letra en LRCLIB…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    } else {
                        Text(
                            text = displayLyrics.ifBlank {
                                lyricsStatusMessage
                                    ?: "Sin letra registrada para este tema. Toca \"LRCLIB\" para buscarla o \"Editar\" para agregarla."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
