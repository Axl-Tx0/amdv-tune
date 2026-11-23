package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CatalogAlbum
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.OrbitronFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleasesInboxSheet(
    recentAlbums: List<CatalogAlbum>,
    followedArtistIds: Set<String>,
    onDismiss: () -> Unit,
    onOpenAlbum: (CatalogAlbum) -> Unit,
    onPlayAlbum: (CatalogAlbum) -> Unit
) {
    val palette = LocalAmdvPalette.current
    val followedReleases = remember(recentAlbums, followedArtistIds) {
        recentAlbums.filter { followedArtistIds.contains(it.artistId) }
    }
    val otherReleases = remember(recentAlbums, followedArtistIds) {
        recentAlbums.filter { !followedArtistIds.contains(it.artistId) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
                .testTag("releases_inbox_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ESTRENOS Y NOVEDADES",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = palette.accent
                    )
                    Text(
                        text = "Nuevos lanzamientos del catálogo y de tus artistas seguidos",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (recentAlbums.isEmpty()) {
                Text(
                    text = "Todavía no hay álbumes publicados en el catálogo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (followedReleases.isNotEmpty()) {
                        item(key = "followed_header") {
                            Text(
                                text = "DE ARTISTAS QUE SIGUES",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent
                            )
                        }
                        items(followedReleases, key = { "f_${it.id}" }) { album ->
                            ReleaseInboxRow(
                                album = album,
                                isFollowedArtist = true,
                                onOpenAlbum = {
                                    onDismiss()
                                    onOpenAlbum(album)
                                },
                                onPlayAlbum = {
                                    onDismiss()
                                    onPlayAlbum(album)
                                }
                            )
                        }
                    }

                    if (otherReleases.isNotEmpty()) {
                        item(key = "all_releases_header") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ÚLTIMOS LANZAMIENTOS",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondary
                            )
                        }
                        items(otherReleases, key = { "a_${it.id}" }) { album ->
                            ReleaseInboxRow(
                                album = album,
                                isFollowedArtist = false,
                                onOpenAlbum = {
                                    onDismiss()
                                    onOpenAlbum(album)
                                },
                                onPlayAlbum = {
                                    onDismiss()
                                    onPlayAlbum(album)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseInboxRow(
    album: CatalogAlbum,
    isFollowedArtist: Boolean,
    onOpenAlbum: () -> Unit,
    onPlayAlbum: () -> Unit
) {
    val palette = LocalAmdvPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isFollowedArtist) palette.accent.copy(alpha = 0.10f) else palette.surfaceElevated.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (isFollowedArtist) palette.accent.copy(alpha = 0.55f) else palette.border
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenAlbum)
            .testTag("release_inbox_item_${album.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            TrackCoverArt(
                coverKey = album.coverUrl.ifBlank {
                    "MONO_CYAN_${album.title.firstOrNull()?.uppercaseChar() ?: 'A'}"
                },
                title = album.title,
                size = 56.dp,
                cornerRadius = 12.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (isFollowedArtist) {
                    Text(
                        text = "SIGUIENDO • ESTRENO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                }
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
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
            Surface(
                onClick = onPlayAlbum,
                shape = CircleShape,
                color = palette.accent,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Escuchar álbum",
                    tint = if (palette.isDark) Color.Black else Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
