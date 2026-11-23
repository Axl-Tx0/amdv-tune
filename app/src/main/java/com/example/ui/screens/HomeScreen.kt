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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.JamSession
import com.example.model.TrackUiModel
import com.example.model.UserPlaylist
import com.example.ui.components.PlaylistCoverMosaic
import com.example.ui.components.SearchPillBar
import com.example.ui.components.TrackCoverArt
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.OrbitronFontFamily
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    tracks: List<TrackUiModel>,
    offlineTracks: List<TrackUiModel>,
    recentTracks: List<TrackUiModel> = emptyList(),
    playlists: List<UserPlaylist> = emptyList(),
    artists: List<CatalogArtist>,
    albums: List<CatalogAlbum>,
    latestReleaseAlbum: CatalogAlbum? = null,
    dailyMixTracks: List<TrackUiModel> = emptyList(),
    trendingTracks: List<Pair<TrackUiModel, Long>> = emptyList(),
    activeJam: JamSession? = null,
    playbackState: PlaybackState,
    isAdmin: Boolean,
    hasUnreadReleases: Boolean = false,
    onSearchBarClick: () -> Unit,
    onOpenReleasesInbox: () -> Unit = {},
    onTrackClick: (TrackUiModel) -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit = {},
    onArtistClick: (CatalogArtist) -> Unit,
    onAlbumClick: (CatalogAlbum) -> Unit,
    onPlayAlbum: (CatalogAlbum) -> Unit = {},
    onPlayDailyMix: () -> Unit = {},
    onRegenerateDailyMix: () -> Unit = {},
    onOpenJamSheet: () -> Unit = {},
    onOpenAdminPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current
    val favoriteTracks = remember(tracks) { tracks.filter { it.favorite } }
    val tracksById = remember(tracks) { tracks.associateBy { it.id } }

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
                        onClick = onOpenReleasesInbox,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_releases_bell_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Novedades y estrenos",
                                tint = if (hasUnreadReleases) palette.accent else palette.textPrimary,
                                modifier = Modifier.size(25.dp)
                            )
                            if (hasUnreadReleases) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3B5C))
                                )
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

            if (latestReleaseAlbum != null) {
                item(key = "home_new_release_hero") {
                    NewReleaseHeroCard(
                        album = latestReleaseAlbum,
                        onOpenAlbum = { onAlbumClick(latestReleaseAlbum) },
                        onPlayAlbum = { onPlayAlbum(latestReleaseAlbum) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item(key = "home_daily_mix_and_jam") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Mix Diario Inteligente
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1.25f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = dailyMixTracks.isNotEmpty(), onClick = onPlayDailyMix)
                            .testTag("home_daily_mix_card")
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            palette.accent.copy(alpha = if (palette.isDark) 0.22f else 0.14f),
                                            palette.surface
                                        )
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = palette.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "MIX DIARIO",
                                            fontFamily = OrbitronFontFamily,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.accent
                                        )
                                    }
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(palette.surfaceElevated.copy(alpha = 0.8f))
                                            .clickable(onClick = onRegenerateDailyMix)
                                            .testTag("home_daily_mix_refresh_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Regenerar Mix Diario",
                                            tint = palette.textPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Radio Inteligente",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${dailyMixTracks.size} temas mezclados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Jam en Vivo
                    val jamActive = activeJam != null
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surface,
                        border = BorderStroke(
                            1.dp,
                            if (jamActive) palette.accent else palette.secondaryAccent.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onOpenJamSheet)
                            .testTag("home_jam_card")
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            (if (jamActive) palette.accent else palette.secondaryAccent)
                                                .copy(alpha = if (palette.isDark) 0.22f else 0.14f),
                                            palette.surface
                                        )
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Podcasts,
                                        contentDescription = null,
                                        tint = if (jamActive) palette.accent else palette.secondaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (jamActive) "EN VIVO" else "JAM",
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (jamActive) palette.accent else palette.secondaryAccent
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (jamActive) activeJam!!.roomCode else "Escuchar juntos",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (jamActive) "Sala sincronizada" else "Crear o unirse",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            if (trendingTracks.isNotEmpty()) {
                item(key = "home_community_trending") {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFF6B35),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TENDENCIAS EN AMDV-TUNE",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(
                                trendingTracks.withIndex().toList(),
                                key = { "trend_${it.value.first.id}" }
                            ) { indexed ->
                                val rank = indexed.index + 1
                                val (track, plays) = indexed.value
                                val isCurrent = playbackState.currentTrack?.id == track.id
                                TrendingTrackCard(
                                    rank = rank,
                                    track = track,
                                    plays = plays,
                                    isCurrent = isCurrent,
                                    onClick = { onTrackClick(track) }
                                )
                            }
                        }
                    }
                }
            }

            if (recentTracks.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "ESCUCHADO RECIENTEMENTE",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.accent,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(recentTracks, key = { "rec_${it.id}" }) { track ->
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

            if (playlists.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "TUS PLAYLISTS",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(playlists, key = { "pl_${it.id}" }) { playlist ->
                                val plTracks = remember(playlist.trackIds, tracksById) {
                                    playlist.trackIds.mapNotNull { tracksById[it] }
                                }
                                Column(
                                    modifier = Modifier
                                        .width(148.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onPlaylistClick(playlist) }
                                        .padding(bottom = 4.dp)
                                        .testTag("home_playlist_${playlist.id}")
                                ) {
                                    PlaylistCoverMosaic(
                                        tracks = plTracks,
                                        title = playlist.name,
                                        size = 148.dp,
                                        cornerRadius = 16.dp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playlist.trackIds.size} canciones",
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

@Composable
private fun NewReleaseHeroCard(
    album: CatalogAlbum,
    onOpenAlbum: () -> Unit,
    onPlayAlbum: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAmdvPalette.current

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenAlbum)
            .testTag("home_new_release_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = if (palette.isDark) 0.24f else 0.14f),
                            palette.secondaryAccent.copy(alpha = if (palette.isDark) 0.16f else 0.08f),
                            palette.surface
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TrackCoverArt(
                    coverKey = album.coverUrl.ifBlank {
                        "MONO_VIOLET_${album.title.firstOrNull()?.uppercaseChar() ?: 'N'}"
                    },
                    title = album.title,
                    size = 96.dp,
                    cornerRadius = 16.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.accent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "NUEVO LANZAMIENTO",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 9.sp,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onPlayAlbum,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = if (palette.isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("home_new_release_play_btn")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Escuchar ahora", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingTrackCard(
    rank: Int,
    track: TrackUiModel,
    plays: Long,
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
            .testTag("home_trending_${track.id}")
    ) {
        Box {
            TrackCoverArt(
                coverKey = track.coverKey,
                title = track.title,
                size = 154.dp,
                cornerRadius = 16.dp
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, if (rank <= 3) Color(0xFFFF6B35) else palette.accent),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = "#$rank",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (rank <= 3) Color(0xFFFF9F1C) else palette.accent,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = "🔥 $plays",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (isCurrent) palette.accent else palette.textPrimary,
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
}
