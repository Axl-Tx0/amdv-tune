package com.example.ui.screens

import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlaybackState
import com.example.model.AppThemeMode
import com.example.model.TrackUiModel
import com.example.ui.components.TrackNumberedRow
import com.example.ui.components.TrailingActionType
import com.example.ui.theme.LocalAmdvPalette

@Composable
fun LibraryScreen(
    deviceTracks: List<TrackUiModel>,
    offlineTracks: List<TrackUiModel>,
    playbackState: PlaybackState,
    isAdmin: Boolean,
    hasAudioPermission: Boolean,
    isSyncingDeviceMusic: Boolean,
    onOpenAdminPanel: () -> Unit,
    onSyncDeviceMusicClick: () -> Unit,
    onGoToSearchClick: () -> Unit,
    onTrackClick: (TrackUiModel, List<TrackUiModel>) -> Unit,
    onToggleFavorite: (TrackUiModel) -> Unit,
    onDeleteTrack: (TrackUiModel) -> Unit,
    onOpenLyrics: (TrackUiModel) -> Unit,
    onDownloadTrack: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current
    val isAndroid13OrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: Todas, 1: Descargadas, 2: Dispositivo

    val allPersonalTracks = remember(offlineTracks, deviceTracks) {
        offlineTracks + deviceTracks
    }

    val displayedTracks = remember(allPersonalTracks, offlineTracks, deviceTracks, selectedFilter) {
        when (selectedFilter) {
            1 -> offlineTracks
            2 -> deviceTracks
            else -> allPersonalTracks
        }
    }

    val cardBorder = if (palette.mode == AppThemeMode.NEON) {
        Brush.horizontalGradient(
            listOf(
                palette.accent.copy(alpha = 0.55f),
                palette.secondaryAccent.copy(alpha = 0.45f)
            )
        )
    } else {
        SolidColor(palette.border)
    }

    val syncTitle = if (isSyncingDeviceMusic) {
        stringResource(R.string.sync_device_music_syncing_title)
    } else {
        stringResource(R.string.sync_device_music_title)
    }

    val syncSubtitle = when {
        isSyncingDeviceMusic -> stringResource(R.string.sync_device_music_syncing_subtitle)
        hasAudioPermission -> "${deviceTracks.size} canciones en el dispositivo • Toca para actualizar"
        isAndroid13OrHigher -> stringResource(R.string.sync_device_music_permission_subtitle)
        deviceTracks.isNotEmpty() -> "${deviceTracks.size} canciones sincronizadas • Toca para elegir otra carpeta"
        else -> stringResource(R.string.sync_device_music_folder_subtitle)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(top = 12.dp)
    ) {
        if (isAdmin) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenAdminPanel)
                    .testTag("admin_catalog_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.accent.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = "Subir música a la nube",
                            tint = palette.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Panel Admin • Subir a la nube",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.accent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Agrega artistas, álbumes y MP3 para todos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(enabled = !isSyncingDeviceMusic, onClick = onSyncDeviceMusicClick)
                .testTag("sync_device_music_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (hasAudioPermission) {
                                Brush.linearGradient(
                                    listOf(
                                        palette.accent.copy(alpha = 0.28f),
                                        palette.accent.copy(alpha = 0.14f)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        palette.metallicStart.copy(alpha = 0.7f),
                                        palette.metallicEnd.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        )
                ) {
                    if (isSyncingDeviceMusic) {
                        CircularProgressIndicator(
                            color = if (hasAudioPermission) palette.accent else Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (hasAudioPermission) Icons.Filled.Refresh else Icons.Filled.Sync,
                            contentDescription = stringResource(R.string.cd_sync_device_music),
                            tint = if (hasAudioPermission) palette.accent else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = syncTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = syncSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                if (!hasAudioPermission && !isSyncingDeviceMusic) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.accent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = if (isAndroid13OrHigher) "Permitir" else "Elegir",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                "Todas (${allPersonalTracks.size})",
                "Descargadas (${offlineTracks.size})",
                "Dispositivo (${deviceTracks.size})"
            )
            itemsIndexed(filters) { idx, label ->
                FilterChip(
                    selected = selectedFilter == idx,
                    onClick = { selectedFilter = idx },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                        selectedLabelColor = palette.accent
                    ),
                    modifier = Modifier.testTag("library_filter_chip_$idx")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (displayedTracks.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LibraryMusic,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when (selectedFilter) {
                        1 -> "Sin canciones descargadas"
                        2 -> "Sin canciones del dispositivo"
                        else -> stringResource(R.string.empty_library_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (selectedFilter) {
                        1 -> "Busca una canción y pulsa el botón de descarga para guardarla aquí y escucharla sin internet"
                        2 -> "Pulsa arriba para conceder permiso de audio y sincronizar toda la música de tu dispositivo"
                        else -> stringResource(R.string.empty_library_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                if (selectedFilter == 2) {
                    Button(
                        onClick = onSyncDeviceMusicClick,
                        enabled = !isSyncingDeviceMusic,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("library_empty_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Sincronizar ahora", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onGoToSearchClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("library_go_to_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Buscar música", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(displayedTracks, key = { _, track -> track.id }) { index, track ->
                    val isCurrent = playbackState.currentTrack?.id == track.id
                    val trailingType = if (track.isCloudCatalog) {
                        TrailingActionType.LYRICS
                    } else {
                        TrailingActionType.DELETE
                    }
                    TrackNumberedRow(
                        index = index + 1,
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = playbackState.isPlaying,
                        onPlayClick = { onTrackClick(track, displayedTracks) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        trailingActionType = trailingType,
                        onTrailingAction = {
                            if (trailingType == TrailingActionType.LYRICS) {
                                onOpenLyrics(track)
                            } else {
                                onDeleteTrack(track)
                            }
                        },
                        onDownloadClick = { onDownloadTrack(track.id) },
                        onRemoveDownloadClick = { onRemoveDownload(track.id) }
                    )
                }
            }
        }
    }
}
