package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ScannedLocalTrack
import com.example.model.CatalogArtist
import com.example.ui.theme.LocalAmdvPalette
import com.example.viewmodel.FolderImportUiState

@Composable
fun FolderUploadTabContent(
    artists: List<CatalogArtist>,
    importState: FolderImportUiState,
    isUploading: Boolean,
    onScanFolderTree: (Uri) -> Unit,
    onScanAudioUris: (List<Uri>) -> Unit,
    onClearPreview: () -> Unit,
    onPublishFolderAlbum: (
        selectedArtistId: String?,
        artistName: String,
        artistPhotoUri: Uri?,
        albumTitle: String,
        releaseYear: Long,
        customCoverUri: Uri?,
        fallbackCoverPath: String,
        selectedTracks: List<ScannedLocalTrack>,
        autoFindLyricsWithAi: Boolean,
        onDone: (Boolean, String?) -> Unit
    ) -> Unit
) {
    val palette = LocalAmdvPalette.current

    // null = "+ Nuevo artista"; non-null = existing artist ID
    var selectedArtistId by remember { mutableStateOf<String?>(null) }
    var artistManuallyChosen by remember { mutableStateOf(false) }
    var editableArtist by remember { mutableStateOf("") }
    var artistPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var editableAlbum by remember { mutableStateOf("") }
    var editableYear by remember { mutableStateOf("2026") }
    var customCoverUri by remember { mutableStateOf<Uri?>(null) }
    var autoFindLyricsWithAi by remember { mutableStateOf(true) }
    val selectedMap = remember { mutableStateMapOf<String, Boolean>() }

    val selectedExistingArtist = remember(artists, selectedArtistId) {
        artists.firstOrNull { it.id == selectedArtistId }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                customCoverUri = null
                onScanFolderTree(uri)
            }
        }
    )

    val multiAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                customCoverUri = null
                onScanAudioUris(uris)
            }
        }
    )

    val artistPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) artistPhotoUri = uri
        }
    )

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) customCoverUri = uri
        }
    )

    val preview = importState.preview
    LaunchedEffect(preview) {
        if (preview != null) {
            if (!artistManuallyChosen) {
                val autoMatch = artists.firstOrNull {
                    it.name.trim().equals(preview.artistName.trim(), ignoreCase = true)
                }
                if (autoMatch != null) {
                    selectedArtistId = autoMatch.id
                    editableArtist = autoMatch.name
                } else {
                    selectedArtistId = null
                    editableArtist = preview.artistName
                }
            } else if (selectedArtistId == null && editableArtist.isBlank()) {
                editableArtist = preview.artistName
            }
            editableAlbum = preview.albumTitle
            editableYear = preview.releaseYear.toString()
            customCoverUri = preview.coverUri
            selectedMap.clear()
            preview.tracks.forEach { t -> selectedMap[t.id] = true }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Paso 1: Artista (seleccionar existente o crear nuevo con nombre y foto)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.surfaceElevated.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1. Artista",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "new_artist_chip") {
                        FilterChip(
                            selected = selectedExistingArtist == null,
                            onClick = {
                                selectedArtistId = null
                                artistManuallyChosen = true
                                artistPhotoUri = null
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Nuevo artista") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.accent.copy(alpha = 0.22f),
                                selectedLabelColor = palette.accent,
                                selectedLeadingIconColor = palette.accent
                            ),
                            modifier = Modifier.testTag("folder_new_artist_chip")
                        )
                    }
                    items(artists, key = { it.id }) { art ->
                        FilterChip(
                            selected = selectedExistingArtist?.id == art.id,
                            onClick = {
                                selectedArtistId = art.id
                                editableArtist = art.name
                                artistPhotoUri = null
                                artistManuallyChosen = true
                            },
                            leadingIcon = {
                                TrackCoverArt(
                                    coverKey = art.photoUrl.ifBlank {
                                        "MONO_CYAN_${art.name.firstOrNull()?.uppercaseChar() ?: 'A'}"
                                    },
                                    title = art.name,
                                    size = 20.dp,
                                    cornerRadius = 10.dp
                                )
                            },
                            label = { Text(art.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.accent.copy(alpha = 0.22f),
                                selectedLabelColor = palette.accent
                            ),
                            modifier = Modifier.testTag("folder_artist_chip_${art.id}")
                        )
                    }
                }

                if (selectedExistingArtist != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(palette.surface)
                        ) {
                            if (artistPhotoUri != null) {
                                AsyncImage(
                                    model = artistPhotoUri,
                                    contentDescription = selectedExistingArtist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                TrackCoverArt(
                                    coverKey = selectedExistingArtist.photoUrl.ifBlank {
                                        "MONO_CYAN_${selectedExistingArtist.name.firstOrNull()?.uppercaseChar() ?: 'A'}"
                                    },
                                    title = selectedExistingArtist.name,
                                    size = 54.dp,
                                    cornerRadius = 27.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedExistingArtist.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Sube todas las carpetas/álbumes que quieras para este artista.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        TextButton(
                            enabled = !isUploading,
                            onClick = {
                                artistPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("folder_change_existing_artist_photo_btn")
                        ) {
                            Text(
                                text = if (artistPhotoUri != null) "Foto ✓" else "Foto",
                                color = palette.accent
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(palette.surface)
                                .clickable(enabled = !isUploading) {
                                    artistPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            if (artistPhotoUri != null) {
                                AsyncImage(
                                    model = artistPhotoUri,
                                    contentDescription = "Foto del artista",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Elegir foto del artista",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedButton(
                            enabled = !isUploading,
                            onClick = {
                                artistPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("folder_pick_artist_photo_btn")
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (artistPhotoUri != null) "Foto del artista ✓" else "Elegir foto del artista",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editableArtist,
                        onValueChange = {
                            editableArtist = it
                            artistManuallyChosen = true
                        },
                        label = { Text("Nombre del artista") },
                        placeholder = { Text("Ej: Bad Bunny, Daft Punk…") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_artist_name_input")
                    )
                }
            }
        }

        // Paso 2: Elegir carpeta o varios MP3 del álbum
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LibraryMusic,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "2. Selecciona carpeta o MP3 del álbum",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                enabled = !importState.isScanning && !isUploading,
                onClick = { folderPickerLauncher.launch(null) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = if (palette.isDark) Color.Black else Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("admin_pick_folder_btn")
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Elegir carpeta",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                enabled = !importState.isScanning && !isUploading,
                onClick = { multiAudioPickerLauncher.launch(arrayOf("audio/*")) },
                border = BorderStroke(1.dp, palette.accent),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("admin_pick_multi_mp3_btn")
            ) {
                Icon(
                    Icons.Filled.AudioFile,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Varios MP3",
                    color = palette.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (importState.isScanning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                CircularProgressIndicator(
                    color = palette.accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Leyendo canciones y portadas…",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.accent
                )
            }
        }

        if (!importState.statusMessage.isNullOrBlank()) {
            Text(
                text = importState.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = palette.accent
            )
        }
        if (!importState.errorMessage.isNullOrBlank()) {
            Text(
                text = importState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryAccent
            )
        }

        if (preview != null) {
            HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (customCoverUri != null) {
                    AsyncImage(
                        model = customCoverUri,
                        contentDescription = editableAlbum,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    val previewCoverKey = when {
                        preview.previewCoverPath.isNotBlank() -> preview.previewCoverPath
                        else -> "MONO_VIOLET_${editableAlbum.firstOrNull()?.uppercaseChar() ?: 'A'}"
                    }
                    TrackCoverArt(
                        coverKey = previewCoverKey,
                        title = editableAlbum,
                        size = 68.dp,
                        cornerRadius = 12.dp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${preview.tracks.size} canciones listas",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.accent
                    )
                    Text(
                        text = if (customCoverUri != null) {
                            "Portada del álbum seleccionada ✓"
                        } else if (preview.previewCoverPath.isNotBlank()) {
                            "Portada detectada automáticamente ✓"
                        } else {
                            "Sin portada detectada (opcional)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                TextButton(
                    enabled = !isUploading,
                    onClick = onClearPreview
                ) {
                    Text("Limpiar", color = palette.textSecondary)
                }
            }

            OutlinedButton(
                enabled = !isUploading,
                onClick = {
                    coverPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (customCoverUri != null) "Cambiar portada del álbum ✓" else "Elegir portada del álbum (opcional)")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = editableAlbum,
                    onValueChange = { editableAlbum = it },
                    label = { Text("Nombre del Álbum / Carpeta") },
                    singleLine = true,
                    modifier = Modifier.weight(0.7f)
                )
                OutlinedTextField(
                    value = editableYear,
                    onValueChange = { editableYear = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Año") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.3f)
                )
            }

            val selectedTracks = preview.tracks.filter { selectedMap[it.id] == true }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Canciones a subir (${selectedTracks.size}/${preview.tracks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
                val allSelected = selectedTracks.size == preview.tracks.size
                TextButton(
                    onClick = {
                        val next = !allSelected
                        preview.tracks.forEach { t -> selectedMap[t.id] = next }
                    }
                ) {
                    Text(
                        text = if (allSelected) "Desmarcar todas" else "Marcar todas",
                        color = palette.accent
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.surfaceElevated.copy(alpha = 0.65f),
                border = BorderStroke(
                    1.dp,
                    if (autoFindLyricsWithAi) palette.accent.copy(alpha = 0.5f) else palette.border
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Buscar letras automáticamente con IA",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Usa el nombre del artista y cada canción al publicar el álbum.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoFindLyricsWithAi,
                        onCheckedChange = { autoFindLyricsWithAi = it },
                        enabled = !isUploading,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (palette.isDark) Color.Black else Color.White,
                            checkedTrackColor = palette.accent
                        ),
                        modifier = Modifier.testTag("folder_auto_ai_lyrics_switch")
                    )
                }
            }

            val resolvedArtistName = selectedExistingArtist?.name ?: editableArtist
            Button(
                enabled = !isUploading && selectedTracks.isNotEmpty() && resolvedArtistName.isNotBlank() && editableAlbum.isNotBlank(),
                onClick = {
                    val year = editableYear.toLongOrNull() ?: 2026L
                    onPublishFolderAlbum(
                        selectedExistingArtist?.id,
                        resolvedArtistName,
                        artistPhotoUri,
                        editableAlbum,
                        year,
                        customCoverUri,
                        preview.previewCoverPath,
                        selectedTracks,
                        autoFindLyricsWithAi
                    ) { ok, savedArtistId ->
                        if (ok) {
                            customCoverUri = null
                            artistPhotoUri = null
                            if (!savedArtistId.isNullOrBlank()) {
                                // Mantiene el artista seleccionado para subir más carpetas/álbumes fácilmente
                                selectedArtistId = savedArtistId
                                artistManuallyChosen = true
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = if (palette.isDark) Color.Black else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admin_publish_folder_btn")
            ) {
                Text(
                    text = "Subir Álbum a ${resolvedArtistName.ifBlank { "la Nube" }} (${selectedTracks.size})",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            }

            preview.tracks.forEachIndexed { idx, track ->
                val checked = selectedMap[track.id] == true
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated.copy(alpha = if (checked) 0.6f else 0.25f),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedMap[track.id] = !checked }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { selectedMap[track.id] = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = palette.accent,
                                checkmarkColor = if (palette.isDark) Color.Black else Color.White
                            )
                        )
                        TrackCoverArt(
                            coverKey = track.previewCoverPath.ifBlank {
                                preview.previewCoverPath.ifBlank {
                                    "MONO_CYAN_${track.title.firstOrNull()?.uppercaseChar() ?: 'M'}"
                                }
                            },
                            title = track.title,
                            size = 42.dp,
                            cornerRadius = 8.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${idx + 1}. ${track.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append(track.artistName.ifBlank { resolvedArtistName })
                                    if (track.durationSec > 0) {
                                        val m = track.durationSec / 60L
                                        val s = track.durationSec % 60L
                                        append(" • $m:${s.toString().padStart(2, '0')}")
                                    }
                                },
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
