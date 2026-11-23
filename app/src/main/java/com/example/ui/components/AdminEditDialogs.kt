package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CatalogAlbum
import com.example.model.CatalogArtist
import com.example.model.CatalogTrack
import com.example.ui.theme.LocalAmdvPalette

@Composable
fun EditArtistDialog(
    artist: CatalogArtist,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, photoUri: Uri?, photoUrl: String) -> Unit
) {
    val palette = LocalAmdvPalette.current
    var name by remember(artist.id) { mutableStateOf(artist.name) }
    var bio by remember(artist.id) { mutableStateOf(artist.bio) }
    var photoUrl by remember(artist.id) { mutableStateOf(artist.photoUrl) }
    var photoUri by remember(artist.id) { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photoUri = uri
            photoUrl = ""
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        title = { Text("Editar artista") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(100) },
                    label = { Text("Nombre del artista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(500) },
                    label = { Text("Biografía o género") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (photoUri != null) "Nueva foto seleccionada ✓" else "Cambiar foto desde galería")
                }
                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = {
                        photoUrl = it
                        if (it.isNotBlank()) photoUri = null
                    },
                    label = { Text("URL de foto") },
                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && name.isNotBlank(),
                onClick = { onSave(name, bio, photoUri, photoUrl) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = if (palette.isDark) Color.Black else Color.White
                )
            ) {
                Text(if (isSaving) "Guardando…" else "Guardar cambios", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Cancelar", color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun EditAlbumDialog(
    album: CatalogAlbum,
    artists: List<CatalogArtist>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (artist: CatalogArtist, title: String, year: Long, coverUri: Uri?, coverUrl: String) -> Unit
) {
    val palette = LocalAmdvPalette.current
    var selectedArtist by remember(album.id) {
        mutableStateOf(artists.firstOrNull { it.id == album.artistId } ?: artists.firstOrNull())
    }
    var title by remember(album.id) { mutableStateOf(album.title) }
    var yearText by remember(album.id) { mutableStateOf(album.releaseYear.toString()) }
    var coverUrl by remember(album.id) { mutableStateOf(album.coverUrl) }
    var coverUri by remember(album.id) { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            coverUri = uri
            coverUrl = ""
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        title = { Text("Editar álbum") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (artists.isNotEmpty()) {
                    Text("Artista:", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(artists, key = { it.id }) { art ->
                            FilterChip(
                                selected = selectedArtist?.id == art.id,
                                onClick = { selectedArtist = art },
                                label = { Text(art.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                                    selectedLabelColor = palette.accent
                                )
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Título del álbum") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Año de lanzamiento") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (coverUri != null) "Nueva portada seleccionada ✓" else "Cambiar portada desde galería")
                }
                OutlinedTextField(
                    value = coverUrl,
                    onValueChange = {
                        coverUrl = it
                        if (it.isNotBlank()) coverUri = null
                    },
                    label = { Text("URL de portada") },
                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && selectedArtist != null && title.isNotBlank(),
                onClick = {
                    val artist = selectedArtist ?: return@Button
                    val year = yearText.toLongOrNull() ?: album.releaseYear
                    onSave(artist, title, year, coverUri, coverUrl)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = if (palette.isDark) Color.Black else Color.White
                )
            ) {
                Text(if (isSaving) "Guardando…" else "Guardar cambios", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Cancelar", color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun EditTrackDialog(
    track: CatalogTrack,
    artists: List<CatalogArtist>,
    albums: List<CatalogAlbum>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        artist: CatalogArtist,
        album: CatalogAlbum?,
        title: String,
        trackNumber: Long,
        lyrics: String,
        coverUri: Uri?,
        coverUrl: String
    ) -> Unit
) {
    val palette = LocalAmdvPalette.current
    var selectedArtist by remember(track.id) {
        mutableStateOf(artists.firstOrNull { it.id == track.artistId } ?: artists.firstOrNull())
    }
    var selectedAlbum by remember(track.id) {
        mutableStateOf(albums.firstOrNull { it.id == track.albumId })
    }
    var title by remember(track.id) { mutableStateOf(track.title) }
    var trackNumberText by remember(track.id) { mutableStateOf(track.trackNumber.toString()) }
    var lyrics by remember(track.id) { mutableStateOf(track.lyrics) }
    var coverUrl by remember(track.id) { mutableStateOf(track.coverUrl) }
    var coverUri by remember(track.id) { mutableStateOf<Uri?>(null) }

    val artistAlbums = remember(selectedArtist, albums) {
        albums.filter { it.artistId == selectedArtist?.id }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            coverUri = uri
            coverUrl = ""
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        title = { Text("Editar canción") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (artists.isNotEmpty()) {
                    Text("Artista:", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(artists, key = { it.id }) { art ->
                            FilterChip(
                                selected = selectedArtist?.id == art.id,
                                onClick = {
                                    selectedArtist = art
                                    if (selectedAlbum?.artistId != art.id) selectedAlbum = null
                                },
                                label = { Text(art.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                                    selectedLabelColor = palette.accent
                                )
                            )
                        }
                    }
                }

                if (artistAlbums.isNotEmpty()) {
                    Text("Álbum:", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedAlbum == null,
                                onClick = { selectedAlbum = null },
                                label = { Text("Sencillo") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                                    selectedLabelColor = palette.accent
                                )
                            )
                        }
                        items(artistAlbums, key = { it.id }) { alb ->
                            FilterChip(
                                selected = selectedAlbum?.id == alb.id,
                                onClick = { selectedAlbum = alb },
                                label = { Text(alb.title) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                                    selectedLabelColor = palette.accent
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Título de la canción") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = trackNumberText,
                    onValueChange = { trackNumberText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Número de pista") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (coverUri != null) "Nueva portada seleccionada ✓" else "Cambiar portada desde galería")
                }
                OutlinedTextField(
                    value = coverUrl,
                    onValueChange = {
                        coverUrl = it
                        if (it.isNotBlank()) coverUri = null
                    },
                    label = { Text("URL de portada") },
                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it.take(5000) },
                    label = { Text("Letra (admite formato sincronizado LRC)") },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && selectedArtist != null && title.isNotBlank(),
                onClick = {
                    val artist = selectedArtist ?: return@Button
                    val num = trackNumberText.toLongOrNull() ?: track.trackNumber
                    onSave(artist, selectedAlbum, title, num, lyrics, coverUri, coverUrl)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = if (palette.isDark) Color.Black else Color.White
                )
            ) {
                Text(if (isSaving) "Guardando…" else "Guardar cambios", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Cancelar", color = palette.textSecondary)
            }
        }
    )
}
