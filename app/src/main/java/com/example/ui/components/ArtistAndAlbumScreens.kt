package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlaybackState
import com.example.model.CatalogAlbum
import com.example.model.CatalogArtist
import com.example.model.TrackUiModel
import com.example.ui.theme.HeartPink
import com.example.ui.theme.LocalAmdvPalette

private data class ArtistSmartPlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val covers: List<String>,
    val tracks: List<TrackUiModel>,
    val shuffleOnPlay: Boolean = false
)

@Composable
fun ArtistDetailScreen(
    artist: CatalogArtist,
    allArtists: List<CatalogArtist>,
    albums: List<CatalogAlbum>,
    tracks: List<TrackUiModel>,
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    onSelectRelatedArtist: (CatalogArtist) -> Unit,
    onAlbumClick: (CatalogAlbum) -> Unit,
    onTrackClick: (TrackUiModel, List<TrackUiModel>) -> Unit,
    onToggleFavorite: (TrackUiModel) -> Unit,
    onOpenLyrics: (TrackUiModel) -> Unit,
    onDownloadTrack: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpandPlayer: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val palette = LocalAmdvPalette.current

    val artistAlbums = remember(artist.id, albums) {
        albums.filter { it.artistId == artist.id }
            .sortedByDescending { it.releaseYear }
    }
    val artistTracks = remember(artist.id, artist.name, tracks) {
        tracks.filter {
            it.artistId == artist.id ||
                (it.artistId.isBlank() && it.artist.equals(artist.name, ignoreCase = true))
        }
    }
    val relatedArtists = remember(artist.id, allArtists) {
        allArtists.filter { it.id != artist.id }
    }

    var showAllTracks by remember(artist.id) { mutableStateOf(false) }
    val displayedTracks = remember(artistTracks, showAllTracks) {
        if (showAllTracks) artistTracks else artistTracks.take(5)
    }

    // Build dynamic real playlists for this artist with 2x2 cover art collages
    val artistPlaylists = remember(artist.id, artist.name, artistAlbums, artistTracks) {
        buildList {
            if (artistTracks.isNotEmpty()) {
                val allCovers = buildCollageCovers(artistTracks, artistAlbums, artist.photoUrl)
                val totalMin = (artistTracks.sumOf { it.durationSec } / 60).coerceAtLeast(1)
                add(
                    ArtistSmartPlaylist(
                        id = "mix_${artist.id}",
                        title = "${artist.name.uppercase()} | MIX",
                        subtitle = "${artistTracks.size} canciones • $totalMin min",
                        covers = allCovers,
                        tracks = artistTracks,
                        shuffleOnPlay = true
                    )
                )
            }
            val favs = artistTracks.filter { it.favorite }
            if (favs.isNotEmpty()) {
                val favMin = (favs.sumOf { it.durationSec } / 60).coerceAtLeast(1)
                add(
                    ArtistSmartPlaylist(
                        id = "fav_${artist.id}",
                        title = "Favoritas de ${artist.name}",
                        subtitle = "${favs.size} canciones • $favMin min",
                        covers = buildCollageCovers(favs, artistAlbums, artist.photoUrl),
                        tracks = favs
                    )
                )
            }
            val offline = artistTracks.filter { it.isDownloaded }
            if (offline.isNotEmpty()) {
                val offMin = (offline.sumOf { it.durationSec } / 60).coerceAtLeast(1)
                add(
                    ArtistSmartPlaylist(
                        id = "off_${artist.id}",
                        title = "Offline • ${artist.name}",
                        subtitle = "${offline.size} canciones • $offMin min",
                        covers = buildCollageCovers(offline, artistAlbums, artist.photoUrl),
                        tracks = offline
                    )
                )
            }
            artistAlbums.take(3).forEach { album ->
                val albumTracks = artistTracks.filter { it.albumId == album.id }
                    .sortedBy { it.trackNumber }
                if (albumTracks.isNotEmpty()) {
                    val albMin = (albumTracks.sumOf { it.durationSec } / 60).coerceAtLeast(1)
                    add(
                        ArtistSmartPlaylist(
                            id = "alb_pl_${album.id}",
                            title = "${album.title} (Álbum)",
                            subtitle = "${albumTracks.size} canciones • $albMin min",
                            covers = buildCollageCovers(albumTracks, listOf(album) + artistAlbums, artist.photoUrl),
                            tracks = albumTracks
                        )
                    )
                }
            }
        }
    }

    // Subtle warm/themed heading tint reminiscent of OpenTune's warm editorial style
    val sectionTitleColor = if (palette.isDark) {
        lerpColor(palette.accent, Color(0xFFF3D59B), 0.55f)
    } else {
        palette.accent
    }
    val warmSubtitleColor = if (palette.isDark) {
        lerpColor(palette.textSecondary, Color(0xFFD9BC82), 0.45f)
    } else {
        palette.textSecondary
    }
    val screenBackdrop = if (palette.isDark) {
        Brush.verticalGradient(
            listOf(
                lerpColor(palette.background, Color(0xFF211A0E), 0.72f),
                lerpColor(palette.background, Color(0xFF141009), 0.65f),
                palette.background
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                palette.accent.copy(alpha = 0.08f),
                palette.background,
                palette.background
            )
        )
    }

    val hasMiniPlayer = playbackState.currentTrack != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackdrop)
            .testTag("artist_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top bar: Back arrow, Artist Name, Link/Copy, Share
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
                        .testTag("artist_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = palette.textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        copyTextToClipboard(
                            context = context,
                            label = artist.name,
                            text = "${artist.name} en AMDV-TUNE (${artistAlbums.size} álbumes, ${artistTracks.size} canciones)"
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("artist_copy_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddLink,
                        contentDescription = "Copiar info del artista",
                        tint = palette.textPrimary
                    )
                }

                IconButton(
                    onClick = {
                        shareText(
                            context = context,
                            text = "Escucha a ${artist.name} en AMDV-TUNE (${artistAlbums.size} álbumes • ${artistTracks.size} canciones)"
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("artist_share_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Compartir artista",
                        tint = palette.textPrimary
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = if (hasMiniPlayer) 118.dp else 36.dp
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Artist visual identity + Full-width pill Radio button
                item(key = "artist_header_${artist.id}") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            TrackCoverArt(
                                coverKey = artist.photoUrl.ifBlank {
                                    "MONO_AMBER_${artist.name.firstOrNull()?.uppercaseChar() ?: 'A'}"
                                },
                                title = artist.name,
                                size = 64.dp,
                                cornerRadius = 32.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${artistAlbums.size} álbumes • ${artistTracks.size} canciones",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = warmSubtitleColor
                                )
                                if (artist.bio.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = artist.bio,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            enabled = artistTracks.isNotEmpty() || tracks.isNotEmpty(),
                            onClick = {
                                val pool = if (artistTracks.isNotEmpty()) {
                                    artistTracks.shuffled() + tracks.filter { it.artistId != artist.id }.shuffled().take(12)
                                } else {
                                    tracks.shuffled()
                                }
                                pool.firstOrNull()?.let { first ->
                                    onTrackClick(first, pool)
                                }
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, palette.textSecondary.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (palette.isDark) Color.White.copy(alpha = 0.04f) else palette.surfaceElevated.copy(alpha = 0.5f),
                                contentColor = palette.textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("artist_radio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sensors,
                                contentDescription = null,
                                tint = palette.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Radio",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.textPrimary
                            )
                        }
                    }
                }

                // Section 1: Canciones más escuchadas + →
                item(key = "top_songs_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 6.dp)
                    ) {
                        Text(
                            text = "Canciones más escuchadas",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp
                            ),
                            color = sectionTitleColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (artistTracks.size > 5) {
                            IconButton(
                                onClick = { showAllTracks = !showAllTracks },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("artist_toggle_all_tracks_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = if (showAllTracks) "Mostrar menos" else "Ver todas las canciones",
                                    tint = sectionTitleColor
                                )
                            }
                        }
                    }
                }

                if (artistTracks.isEmpty()) {
                    item(key = "artist_no_tracks") {
                        Text(
                            text = "Todavía no hay canciones publicadas para este artista.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    item(key = "artist_top_tracks_list") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                        ) {
                            displayedTracks.forEach { track ->
                                OpenTuneTrackRow(
                                    track = track,
                                    isCurrent = playbackState.currentTrack?.id == track.id,
                                    isPlaying = playbackState.isPlaying,
                                    subtitleColor = warmSubtitleColor,
                                    onPlayClick = { onTrackClick(track, artistTracks) },
                                    onToggleFavorite = { onToggleFavorite(track) },
                                    onOpenLyrics = { onOpenLyrics(track) },
                                    onDownloadClick = { onDownloadTrack(track.id) },
                                    onRemoveDownloadClick = { onRemoveDownload(track.id) }
                                )
                            }
                            if (artistTracks.size > 5) {
                                Text(
                                    text = if (showAllTracks) {
                                        "Mostrar menos"
                                    } else {
                                        "Ver todas (${artistTracks.size} canciones)"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = sectionTitleColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showAllTracks = !showAllTracks }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Section 2: Álbumes
                if (artistAlbums.isNotEmpty()) {
                    item(key = "artist_albums_section") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Álbumes",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp
                                ),
                                color = sectionTitleColor,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(artistAlbums, key = { it.id }) { album ->
                                    val albumQueue = artistTracks
                                        .filter { it.albumId == album.id }
                                        .sortedBy { it.trackNumber }
                                    OpenTuneAlbumCard(
                                        album = album,
                                        subtitleColor = warmSubtitleColor,
                                        onCardClick = { onAlbumClick(album) },
                                        onQuickPlayClick = {
                                            val first = albumQueue.firstOrNull()
                                            if (first != null) {
                                                onTrackClick(first, albumQueue)
                                            } else {
                                                onAlbumClick(album)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Listas de reproducción de [Artista] (2x2 cover art collages)
                if (artistPlaylists.isNotEmpty()) {
                    item(key = "artist_playlists_section") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Listas de reproducción de ${artist.name}",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp
                                ),
                                color = sectionTitleColor,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(artistPlaylists, key = { it.id }) { playlist ->
                                    ArtistCollagePlaylistCard(
                                        playlist = playlist,
                                        subtitleColor = warmSubtitleColor,
                                        onClick = {
                                            val queue = if (playlist.shuffleOnPlay) {
                                                playlist.tracks.shuffled()
                                            } else {
                                                playlist.tracks
                                            }
                                            queue.firstOrNull()?.let { first ->
                                                onTrackClick(first, queue)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Puede que también te guste (large circular artists)
                if (relatedArtists.isNotEmpty()) {
                    item(key = "artist_related_section") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Puede que también te guste",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp
                                ),
                                color = sectionTitleColor,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(relatedArtists, key = { it.id }) { other ->
                                    Column(
                                        horizontalAlignment = Alignment.Start,
                                        modifier = Modifier
                                            .width(136.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onSelectRelatedArtist(other) }
                                            .padding(bottom = 4.dp)
                                            .testTag("related_artist_${other.id}")
                                    ) {
                                        TrackCoverArt(
                                            coverKey = other.photoUrl.ifBlank {
                                                "MONO_AMBER_${other.name.firstOrNull()?.uppercaseChar() ?: 'A'}"
                                            },
                                            title = other.name,
                                            size = 136.dp,
                                            cornerRadius = 68.dp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = other.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = palette.textPrimary,
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
        }

        // Floating pill mini-player docked above navigation bar
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
                onToggleFavorite = onToggleFavorite,
                onArtistShortcutClick = { current ->
                    val matched = allArtists.firstOrNull {
                        it.id == current.artistId || it.name.equals(current.artist, ignoreCase = true)
                    }
                    if (matched != null && matched.id != artist.id) {
                        onSelectRelatedArtist(matched)
                    } else {
                        onOpenLyrics(current)
                    }
                }
            )
        }
    }
}

@Composable
fun AlbumDetailScreen(
    album: CatalogAlbum,
    allArtists: List<CatalogArtist>,
    tracks: List<TrackUiModel>,
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    onOpenArtist: (CatalogArtist) -> Unit,
    onDownloadAlbum: (String) -> Unit,
    onTrackClick: (TrackUiModel, List<TrackUiModel>) -> Unit,
    onToggleFavorite: (TrackUiModel) -> Unit,
    onOpenLyrics: (TrackUiModel) -> Unit,
    onDownloadTrack: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpandPlayer: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val palette = LocalAmdvPalette.current
    val albumTracks = remember(album.id, tracks) {
        tracks.filter { it.albumId == album.id }.sortedBy { it.trackNumber }
    }
    val parentArtist = remember(album.artistId, album.artistName, allArtists) {
        allArtists.firstOrNull {
            it.id == album.artistId || it.name.equals(album.artistName, ignoreCase = true)
        }
    }
    val sectionTitleColor = if (palette.isDark) {
        lerpColor(palette.accent, Color(0xFFF3D59B), 0.55f)
    } else {
        palette.accent
    }
    val warmSubtitleColor = if (palette.isDark) {
        lerpColor(palette.textSecondary, Color(0xFFD9BC82), 0.45f)
    } else {
        palette.textSecondary
    }
    val screenBackdrop = if (palette.isDark) {
        Brush.verticalGradient(
            listOf(
                lerpColor(palette.background, Color(0xFF211A0E), 0.72f),
                lerpColor(palette.background, Color(0xFF141009), 0.65f),
                palette.background
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                palette.accent.copy(alpha = 0.08f),
                palette.background,
                palette.background
            )
        )
    }
    val hasMiniPlayer = playbackState.currentTrack != null
    val allOffline = albumTracks.isNotEmpty() && albumTracks.all { it.isDownloaded }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackdrop)
            .testTag("album_detail_screen")
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
                        .testTag("album_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = palette.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        shareText(
                            context = context,
                            text = "Álbum \"${album.title}\" de ${album.artistName} (${album.releaseYear}) en AMDV-TUNE"
                        )
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Compartir álbum",
                        tint = palette.textPrimary
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = if (hasMiniPlayer) 118.dp else 36.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "album_hero_${album.id}") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        TrackCoverArt(
                            coverKey = album.coverUrl.ifBlank {
                                "MONO_AMBER_${album.title.firstOrNull()?.uppercaseChar() ?: 'A'}"
                            },
                            title = album.title,
                            size = 212.dp,
                            cornerRadius = 18.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${album.artistName} • ${album.releaseYear}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                            color = warmSubtitleColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (parentArtist != null) {
                                        Modifier.clickable { onOpenArtist(parentArtist) }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                enabled = albumTracks.isNotEmpty(),
                                onClick = {
                                    albumTracks.firstOrNull()?.let { onTrackClick(it, albumTracks) }
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = sectionTitleColor,
                                    contentColor = if (palette.isDark) Color(0xFF16120B) else Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("album_play_all_btn")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reproducir", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                enabled = albumTracks.isNotEmpty(),
                                onClick = {
                                    val shuffled = albumTracks.shuffled()
                                    shuffled.firstOrNull()?.let { onTrackClick(it, shuffled) }
                                },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, palette.textSecondary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("album_shuffle_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shuffle,
                                    contentDescription = null,
                                    tint = palette.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aleatorio", color = palette.textPrimary)
                            }
                        }

                        if (albumTracks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { onDownloadAlbum(album.id) },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, sectionTitleColor.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (allOffline) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = sectionTitleColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (allOffline) "Álbum disponible offline" else "Descargar álbum offline",
                                    color = sectionTitleColor,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                if (albumTracks.isEmpty()) {
                    item(key = "album_empty") {
                        Text(
                            text = "Este álbum aún no tiene canciones cargadas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                } else {
                    item(key = "album_track_list") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                        ) {
                            albumTracks.forEach { track ->
                                OpenTuneTrackRow(
                                    track = track,
                                    isCurrent = playbackState.currentTrack?.id == track.id,
                                    isPlaying = playbackState.isPlaying,
                                    subtitleColor = warmSubtitleColor,
                                    onPlayClick = { onTrackClick(track, albumTracks) },
                                    onToggleFavorite = { onToggleFavorite(track) },
                                    onOpenLyrics = { onOpenLyrics(track) },
                                    onDownloadClick = { onDownloadTrack(track.id) },
                                    onRemoveDownloadClick = { onRemoveDownload(track.id) }
                                )
                            }
                        }
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
                onToggleFavorite = onToggleFavorite,
                onArtistShortcutClick = { current ->
                    val matched = allArtists.firstOrNull {
                        it.id == current.artistId || it.name.equals(current.artist, ignoreCase = true)
                    }
                    if (matched != null) {
                        onOpenArtist(matched)
                    } else {
                        onOpenLyrics(current)
                    }
                }
            )
        }
    }
}

@Composable
private fun OpenTuneTrackRow(
    track: TrackUiModel,
    isCurrent: Boolean,
    isPlaying: Boolean,
    subtitleColor: Color,
    onPlayClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenLyrics: () -> Unit,
    onDownloadClick: () -> Unit,
    onRemoveDownloadClick: () -> Unit
) {
    val palette = LocalAmdvPalette.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) palette.accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onPlayClick)
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .testTag("opentune_track_row_${track.id}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            TrackCoverArt(
                coverKey = track.coverKey,
                title = track.title,
                size = 52.dp,
                cornerRadius = 10.dp
            )
            if (isCurrent && isPlaying) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    MiniEqualizerBars(color = palette.accent)
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = if (isCurrent) palette.accent else palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.isDownloaded) {
                    Icon(
                        imageVector = Icons.Filled.DownloadDone,
                        contentDescription = "Descargada offline",
                        tint = subtitleColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val progress = track.downloadProgress
        if (progress != null) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0.05f, 1f) },
                color = palette.accent,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(18.dp)
            )
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("track_more_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Más opciones",
                    tint = palette.textPrimary.copy(alpha = 0.85f)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = palette.surfaceElevated
            ) {
                DropdownMenuItem(
                    text = { Text(if (track.favorite) "Quitar de favoritos" else "Añadir a favoritos", color = palette.textPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (track.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (track.favorite) HeartPink else palette.textPrimary
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleFavorite()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Ver letra / Buscar con IA", color = palette.textPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = palette.accent
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onOpenLyrics()
                    }
                )
                if (track.isCloudCatalog) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (track.isDownloaded) "Quitar descarga offline" else "Descargar offline",
                                color = palette.textPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (track.isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                contentDescription = null,
                                tint = palette.accent
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            if (track.isDownloaded) onRemoveDownloadClick() else onDownloadClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenTuneAlbumCard(
    album: CatalogAlbum,
    subtitleColor: Color,
    onCardClick: () -> Unit,
    onQuickPlayClick: () -> Unit
) {
    val palette = LocalAmdvPalette.current

    Column(
        modifier = Modifier
            .width(158.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onCardClick)
            .testTag("artist_album_card_${album.id}")
    ) {
        Box(modifier = Modifier.size(158.dp)) {
            TrackCoverArt(
                coverKey = album.coverUrl.ifBlank {
                    "MONO_AMBER_${album.title.firstOrNull()?.uppercaseChar() ?: 'A'}"
                },
                title = album.title,
                size = 158.dp,
                cornerRadius = 14.dp
            )
            // Translucent dark circular Play badge at bottom-right just like OpenTune
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(onClick = onQuickPlayClick)
                    .testTag("album_quick_play_${album.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Reproducir álbum ${album.title}",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp
            ),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = album.releaseYear.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            color = subtitleColor,
            maxLines = 1
        )
    }
}

@Composable
private fun ArtistCollagePlaylistCard(
    playlist: ArtistSmartPlaylist,
    subtitleColor: Color,
    onClick: () -> Unit
) {
    val palette = LocalAmdvPalette.current

    Column(
        modifier = Modifier
            .width(158.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("artist_playlist_${playlist.id}")
    ) {
        CoverArtCollage2x2(
            covers = playlist.covers,
            title = playlist.title,
            size = 158.dp,
            cornerRadius = 14.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = playlist.subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = subtitleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CoverArtCollage2x2(
    covers: List<String>,
    title: String,
    size: Dp,
    cornerRadius: Dp
) {
    val half = size / 2
    val tiles = remember(covers, title) {
        val clean = covers.filter { it.isNotBlank() }.distinct()
        if (clean.isEmpty()) {
            val ch = title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: 'M'
            listOf("MONO_AMBER_$ch", "MONO_CYAN_$ch", "MONO_INDIGO_$ch", "MONO_SILVER_$ch")
        } else {
            List(4) { idx -> clean[idx % clean.size] }
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f)) {
                TrackCoverArt(coverKey = tiles[0], title = title, size = half, cornerRadius = 0.dp)
                TrackCoverArt(coverKey = tiles[1], title = title, size = half, cornerRadius = 0.dp)
            }
            Row(modifier = Modifier.weight(1f)) {
                TrackCoverArt(coverKey = tiles[2], title = title, size = half, cornerRadius = 0.dp)
                TrackCoverArt(coverKey = tiles[3], title = title, size = half, cornerRadius = 0.dp)
            }
        }
    }
}

private fun buildCollageCovers(
    tracks: List<TrackUiModel>,
    albums: List<CatalogAlbum>,
    artistPhotoUrl: String
): List<String> {
    val distinct = LinkedHashSet<String>()
    albums.forEach { if (it.coverUrl.isNotBlank()) distinct.add(it.coverUrl) }
    tracks.forEach { if (it.coverKey.isNotBlank()) distinct.add(it.coverKey) }
    if (artistPhotoUrl.isNotBlank()) distinct.add(artistPhotoUrl)
    return distinct.take(4)
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir"))
}
