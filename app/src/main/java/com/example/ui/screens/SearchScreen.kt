package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlaybackState
import com.example.model.CatalogAlbum
import com.example.model.CatalogArtist
import com.example.model.TrackUiModel
import com.example.ui.components.SearchPillBar
import com.example.ui.components.TrackCoverArt
import com.example.ui.components.TrackNumberedRow
import com.example.ui.components.TrailingActionType
import com.example.ui.theme.LocalAmdvPalette

@Composable
fun SearchScreen(
    query: String,
    tracks: List<TrackUiModel>,
    artists: List<CatalogArtist>,
    albums: List<CatalogAlbum>,
    playbackState: PlaybackState,
    onQueryChange: (String) -> Unit,
    onTrackClick: (TrackUiModel) -> Unit,
    onArtistClick: (CatalogArtist) -> Unit,
    onAlbumClick: (CatalogAlbum) -> Unit,
    onToggleFavorite: (TrackUiModel) -> Unit,
    onOpenLyrics: (TrackUiModel) -> Unit,
    onDownloadTrack: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onAddToPlaylist: (TrackUiModel) -> Unit = {},
    onPlayNextInQueue: (TrackUiModel) -> Unit = {},
    onAddToQueueEnd: (TrackUiModel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current
    // 0: Canciones, 1: Offline, 2: Álbumes, 3: Artistas
    var selectedFilter by remember { mutableIntStateOf(0) }
    val q = query.trim()

    val filteredArtists = remember(artists, q) {
        if (q.isEmpty()) artists else artists.filter { it.name.contains(q, ignoreCase = true) }
    }
    val filteredAlbums = remember(albums, q) {
        if (q.isEmpty()) albums else albums.filter {
            it.title.contains(q, ignoreCase = true) || it.artistName.contains(q, ignoreCase = true)
        }
    }
    val displayedTracks = remember(tracks, selectedFilter) {
        if (selectedFilter == 1) tracks.filter { it.isDownloaded } else tracks
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(top = 12.dp)
    ) {
        SearchPillBar(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("Canciones", "Offline", "Álbumes", "Artistas")
            itemsIndexed(filters) { idx, label ->
                FilterChip(
                    selected = selectedFilter == idx,
                    onClick = { selectedFilter = idx },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                        selectedLabelColor = palette.accent
                    ),
                    modifier = Modifier.testTag("search_filter_chip_$idx")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedFilter) {
            2 -> {
                Text(
                    text = "ÁLBUMES (${filteredAlbums.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (filteredAlbums.isEmpty()) {
                    SearchEmptyState(palette.textPrimary, palette.textSecondary)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredAlbums, key = { it.id }) { album ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surface,
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onAlbumClick(album) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    TrackCoverArt(
                                        coverKey = album.coverUrl.ifBlank {
                                            "MONO_VIOLET_${album.title.firstOrNull()?.uppercaseChar() ?: 'A'}"
                                        },
                                        title = album.title,
                                        size = 54.dp,
                                        cornerRadius = 10.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = palette.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${album.artistName} • ${album.releaseYear}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                Text(
                    text = "ARTISTAS (${filteredArtists.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (filteredArtists.isEmpty()) {
                    SearchEmptyState(palette.textPrimary, palette.textSecondary)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredArtists, key = { it.id }) { artist ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surface,
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onArtistClick(artist) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    TrackCoverArt(
                                        coverKey = artist.photoUrl.ifBlank {
                                            "MONO_CYAN_${artist.name.firstOrNull()?.uppercaseChar() ?: 'A'}"
                                        },
                                        title = artist.name,
                                        size = 54.dp,
                                        cornerRadius = 27.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artist.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = palette.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = artist.bio.ifBlank { "Artista del catálogo" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                Text(
                    text = if (selectedFilter == 1) {
                        "DESCARGADAS OFFLINE (${displayedTracks.size})"
                    } else if (q.isBlank()) {
                        stringResource(R.string.section_all_music)
                    } else {
                        "RESULTADOS (${displayedTracks.size})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (displayedTracks.isEmpty()) {
                    SearchEmptyState(palette.textPrimary, palette.textSecondary)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(displayedTracks, key = { _, track -> track.id }) { index, track ->
                            val isCurrent = playbackState.currentTrack?.id == track.id
                            TrackNumberedRow(
                                index = index + 1,
                                track = track,
                                isCurrent = isCurrent,
                                isPlaying = playbackState.isPlaying,
                                onPlayClick = { onTrackClick(track) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                trailingActionType = TrailingActionType.LYRICS,
                                onTrailingAction = { onOpenLyrics(track) },
                                onDownloadClick = { onDownloadTrack(track.id) },
                                onRemoveDownloadClick = { onRemoveDownload(track.id) },
                                onAddToPlaylistClick = { onAddToPlaylist(track) },
                                onPlayNextClick = { onPlayNextInQueue(track) },
                                onAddToQueueClick = { onAddToQueueEnd(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    primary: androidx.compose.ui.graphics.Color,
    secondary: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            tint = secondary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.no_results_title),
            style = MaterialTheme.typography.titleMedium,
            color = primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.no_results_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = secondary,
            textAlign = TextAlign.Center
        )
    }
}
