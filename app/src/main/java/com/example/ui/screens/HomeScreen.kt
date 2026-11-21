package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlaybackState
import com.example.model.AppThemeMode
import com.example.model.CatalogAlbum
import com.example.model.CatalogArtist
import com.example.model.TrackUiModel
import com.example.ui.components.SearchPillBar
import com.example.ui.components.TrackCoverArt
import com.example.ui.theme.LocalAmdvPalette

@Composable
fun HomeScreen(
    tracks: List<TrackUiModel>,
    offlineTracks: List<TrackUiModel>,
    artists: List<CatalogArtist>,
    albums: List<CatalogAlbum>,
    playbackState: PlaybackState,
    isAdmin: Boolean,
    onSearchBarClick: () -> Unit,
    onTrackClick: (TrackUiModel) -> Unit,
    onArtistClick: (CatalogArtist) -> Unit,
    onAlbumClick: (CatalogAlbum) -> Unit,
    onOpenAdminPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current
    val favoriteTracks = tracks.filter { it.favorite }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (palette.mode == AppThemeMode.NEON) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                palette.accent.copy(alpha = 0.08f),
                                palette.background,
                                palette.secondaryAccent.copy(alpha = 0.05f)
                            )
                        )
                    )
                } else {
                    Modifier.background(palette.background)
                }
            )
    ) {
        LazyColumn(
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 2.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.amdv_tune_logo),
                        contentDescription = stringResource(R.string.cd_app_logo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.brand_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.accent.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.7f)),
                                modifier = Modifier.testTag("home_admin_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AdminPanelSettings,
                                        contentDescription = stringResource(R.string.role_admin_badge),
                                        tint = palette.accent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = stringResource(R.string.role_admin_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accent,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = onOpenAdminPanel,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_admin_upload_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = palette.surfaceElevated,
                                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.6f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudUpload,
                                        contentDescription = "Subir al catálogo",
                                        tint = palette.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onSearchBarClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_search_shortcut")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.cd_search),
                            tint = palette.textPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            if (artists.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "ARTISTAS",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(artists, key = { it.id }) { artist ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(96.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onArtistClick(artist) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    TrackCoverArt(
                                        coverKey = artist.photoUrl.ifBlank {
                                            "MONO_CYAN_${artist.name.firstOrNull()?.uppercaseChar() ?: 'A'}"
                                        },
                                        title = artist.name,
                                        size = 84.dp,
                                        cornerRadius = 42.dp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.textPrimary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (albums.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "ÁLBUMES",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(albums, key = { it.id }) { album ->
                                Column(
                                    modifier = Modifier
                                        .width(148.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onAlbumClick(album) }
                                        .padding(bottom = 4.dp)
                                ) {
                                    TrackCoverArt(
                                        coverKey = album.coverUrl.ifBlank {
                                            "MONO_VIOLET_${album.title.firstOrNull()?.uppercaseChar() ?: 'A'}"
                                        },
                                        title = album.title,
                                        size = 148.dp,
                                        cornerRadius = 16.dp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
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

            if (offlineTracks.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "DESCARGADAS OFFLINE",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.accent,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(offlineTracks, key = { "off_${it.id}" }) { track ->
                                val isCurrent = playbackState.currentTrack?.id == track.id
                                HomeMusicCard(
                                    track = track,
                                    isCurrent = isCurrent,
                                    onClick = { onTrackClick(track) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = stringResource(R.string.section_my_music),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (tracks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.empty_library_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(tracks, key = { it.id }) { track ->
                                val isCurrent = playbackState.currentTrack?.id == track.id
                                HomeMusicCard(
                                    track = track,
                                    isCurrent = isCurrent,
                                    onClick = { onTrackClick(track) }
                                )
                            }
                        }
                    }
                }
            }

            if (favoriteTracks.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.stat_favorites).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(favoriteTracks, key = { "fav_${it.id}" }) { track ->
                                val isCurrent = playbackState.currentTrack?.id == track.id
                                HomeMusicCard(
                                    track = track,
                                    isCurrent = isCurrent,
                                    onClick = { onTrackClick(track) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMusicCard(
    track: TrackUiModel,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAmdvPalette.current

    Column(
        modifier = Modifier
            .width(154.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp)
            .testTag("home_card_${track.id}")
    ) {
        Box {
            TrackCoverArt(
                coverKey = track.coverKey,
                title = track.title,
                size = 154.dp,
                cornerRadius = 16.dp
            )
            if (track.isDownloaded) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.72f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.DownloadDone,
                        contentDescription = "Disponible offline",
                        tint = palette.accent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (isCurrent) palette.accent else palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
