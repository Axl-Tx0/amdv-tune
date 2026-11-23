package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.PlaybackState
import com.example.model.TrackUiModel
import com.example.model.UserPlaylist
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.TrashRose

@Composable
fun PlaylistCoverMosaic(
    tracks: List<TrackUiModel>,
    title: String,
    size: Dp,
    cornerRadius: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    val distinctCovers = remember(tracks) {
        tracks.map { it.coverKey }.filter { it.isNotBlank() }.distinct().take(4)
    }
    if (distinctCovers.size >= 4) {
        val half = size / 2
        Column(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
        ) {
            Row {
                TrackCoverArt(coverKey = distinctCovers[0], title = title, size = half, cornerRadius = 0.dp)
                TrackCoverArt(coverKey = distinctCovers[1], title = title, size = half, cornerRadius = 0.dp)
            }
            Row {
                TrackCoverArt(coverKey = distinctCovers[2], title = title, size = half, cornerRadius = 0.dp)
                TrackCoverArt(coverKey = distinctCovers[3], title = title, size = half, cornerRadius = 0.dp)
            }
        }
    } else {
        val singleCover = distinctCovers.firstOrNull()
            ?: "MONO_VIOLET_${title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: 'P'}"
        TrackCoverArt(
            coverKey = singleCover,
            title = title,
            size = size,
            cornerRadius = cornerRadius,
            modifier = modifier
        )
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: UserPlaylist,
    allTracks: List<TrackUiModel>,
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDownloadPlaylist: () -> Unit,
    onTrackClick: (TrackUiModel, List<TrackUiModel>) -> Unit,
    onToggleFavorite: (TrackUiModel) -> Unit,
    onRemoveFromPlaylist: (String) -> Unit,
    onDownloadTrack: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onPlayNextInQueue: (TrackUiModel) -> Unit,
    onAddToQueueEnd: (TrackUiModel) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpandPlayer: () -> Unit,
    onShareClick: () -> Unit = {}
) {
    BackHandler(onBack = onDismiss)

    val palette = LocalAmdvPalette.current
    val tracksById = remember(allTracks) { allTracks.associateBy { it.id } }
    val playlistTracks = remember(playlist.trackIds, tracksById) {
        playlist.trackIds.mapNotNull { tracksById[it] }
    }
    val cloudInPlaylist = remember(playlistTracks) { playlistTracks.filter { it.isCloudCatalog } }
    val allOffline = cloudInPlaylist.isNotEmpty() && cloudInPlaylist.all { it.isDownloaded }
    val hasMiniPlayer = playbackState.currentTrack != null
    var confirmDelete by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.accent.copy(alpha = if (palette.isDark) 0.18f else 0.10f),
                        palette.background,
                        palette.background
                    )
                )
            )
            .testTag("playlist_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("playlist_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = palette.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("playlist_share_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Compartir playlist por código",
                        tint = palette.accent
                    )
                }
                IconButton(
                    onClick = onRenameClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar playlist",
                        tint = palette.textPrimary
                    )
                }
                IconButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Eliminar playlist",
                        tint = TrashRose
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = if (hasMiniPlayer) 118.dp else 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "playlist_hero") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        PlaylistCoverMosaic(
                            tracks = playlistTracks,
                            title = playlist.name,
                            size = 188.dp,
                            cornerRadius = 20.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        if (playlist.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val totalMins = (playlistTracks.sumOf { it.durationSec } / 60L).coerceAtLeast(0L)
                        Text(
                            text = "${playlistTracks.size} canciones${if (totalMins > 0) " • $totalMins min" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accent
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                enabled = playlistTracks.isNotEmpty(),
                                onClick = {
                                    playlistTracks.firstOrNull()?.let { onTrackClick(it, playlistTracks) }
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accent,
                                    contentColor = if (palette.isDark) Color.Black else Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("playlist_play_all_btn")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reproducir", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                enabled = playlistTracks.isNotEmpty(),
                                onClick = {
                                    val shuffled = playlistTracks.shuffled()
                                    shuffled.firstOrNull()?.let { onTrackClick(it, shuffled) }
                                },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aleatorio", color = palette.textPrimary)
                            }
                        }

                        if (cloudInPlaylist.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onDownloadPlaylist,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("playlist_download_all_btn")
                            ) {
                                Icon(
                                    imageVector = if (allOffline) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (allOffline) "Playlist disponible offline" else "Descargar playlist offline",
                                    color = palette.accent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                if (playlistTracks.isEmpty()) {
                    item(key = "playlist_empty") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = palette.textSecondary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Esta playlist está vacía",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toca el menú de cualquier canción o el botón + en el reproductor para añadir temas aquí.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    itemsIndexed(playlistTracks, key = { idx, tr -> "${tr.id}_$idx" }) { index, track ->
                        TrackNumberedRow(
                            index = index + 1,
                            track = track,
                            isCurrent = playbackState.currentTrack?.id == track.id,
                            isPlaying = playbackState.isPlaying,
                            onPlayClick = { onTrackClick(track, playlistTracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            trailingActionType = TrailingActionType.DELETE,
                            onTrailingAction = { onRemoveFromPlaylist(track.id) },
                            onDownloadClick = { onDownloadTrack(track.id) },
                            onRemoveDownloadClick = { onRemoveDownload(track.id) },
                            onPlayNextClick = { onPlayNextInQueue(track) },
                            onAddToQueueClick = { onAddToQueueEnd(track) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 6.dp)
        ) {
            MiniPlayerBanner(
                playbackState = playbackState,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onBannerClick = onExpandPlayer,
                onToggleFavorite = onToggleFavorite
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("¿Eliminar playlist?") },
            text = { Text("Se eliminará \"${playlist.name}\" de tu cuenta. Tus canciones y descargas no se borrarán.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteClick()
                    }
                ) {
                    Text("Eliminar", color = TrashRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancelar", color = palette.textSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    track: TrackUiModel,
    playlists: List<UserPlaylist>,
    allTracks: List<TrackUiModel>,
    isSignedIn: Boolean,
    onDismiss: () -> Unit,
    onToggleTrackInPlaylist: (UserPlaylist) -> Unit,
    onCreateNewPlaylist: (String, String) -> Unit
) {
    val palette = LocalAmdvPalette.current
    var showCreateInline by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    val tracksById = remember(allTracks) { allTracks.associateBy { it.id } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
                .testTag("add_to_playlist_sheet")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackCoverArt(
                    coverKey = track.coverKey,
                    title = track.title,
                    size = 46.dp,
                    cornerRadius = 10.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Añadir a playlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "${track.title} • ${track.artist}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isSignedIn) {
                Text(
                    text = "Inicia sesión con Google desde la pestaña Perfil para crear y sincronizar tus playlists en la nube.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            } else {
                if (showCreateInline) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surfaceElevated.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it.take(80) },
                                label = { Text("Nombre de la playlist") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newDesc,
                                onValueChange = { newDesc = it.take(200) },
                                label = { Text("Descripción (opcional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { showCreateInline = false }) {
                                    Text("Cancelar", color = palette.textSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    enabled = newName.isNotBlank(),
                                    onClick = {
                                        onCreateNewPlaylist(newName.trim(), newDesc.trim())
                                        newName = ""
                                        newDesc = ""
                                        showCreateInline = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.accent,
                                        contentColor = if (palette.isDark) Color.Black else Color.White
                                    )
                                ) {
                                    Text("Crear y añadir", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    OutlinedButton(
                        onClick = { showCreateInline = true },
                        border = BorderStroke(1.dp, palette.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sheet_new_playlist_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear nueva playlist", color = palette.accent, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (playlists.isEmpty()) {
                    Text(
                        text = "Todavía no tienes playlists. Crea la primera arriba.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            val isMember = pl.trackIds.contains(track.id)
                            val plTracks = pl.trackIds.mapNotNull { tracksById[it] }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isMember) palette.accent.copy(alpha = 0.12f) else palette.surfaceElevated.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, if (isMember) palette.accent else palette.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onToggleTrackInPlaylist(pl) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    PlaylistCoverMosaic(
                                        tracks = plTracks,
                                        title = pl.name,
                                        size = 44.dp,
                                        cornerRadius = 10.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = palette.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pl.trackIds.size} canciones",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isMember) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = if (isMember) "Incluida" else "No incluida",
                                        tint = if (isMember) palette.accent else palette.textSecondary
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

@Composable
fun CreateOrRenamePlaylistDialog(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    confirmLabel: String = "Guardar",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val palette = LocalAmdvPalette.current
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = palette.accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Nombre de la playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(200) },
                    label = { Text("Descripción (opcional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim(), description.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = if (palette.isDark) Color.Black else Color.White
                )
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = palette.textSecondary)
            }
        }
    )
}
