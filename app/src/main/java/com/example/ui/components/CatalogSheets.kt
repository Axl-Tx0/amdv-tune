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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CloudinaryUploader
import com.example.data.PlaybackState
import com.example.data.ScannedLocalTrack
import com.example.model.AppReleaseConfig
import com.example.model.CatalogAlbum
import com.example.model.CatalogArtist
import com.example.model.CatalogTrack
import com.example.model.TrackComment
import com.example.model.TrackUiModel
import com.example.ui.theme.LocalAmdvPalette
import com.example.ui.theme.TrashRose
import com.example.viewmodel.FolderImportUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCatalogSheet(
    artists: List<CatalogArtist>,
    albums: List<CatalogAlbum>,
    cloudTracks: List<TrackUiModel>,
    rawCatalogTracks: List<CatalogTrack>,
    moderationComments: List<TrackComment> = emptyList(),
    latestRelease: AppReleaseConfig?,
    folderImportState: FolderImportUiState,
    isUploading: Boolean,
    uploadProgress: Float,
    onDismiss: () -> Unit,
    onScanFolderTree: (Uri) -> Unit,
    onScanAudioUris: (List<Uri>) -> Unit,
    onClearFolderPreview: () -> Unit,
    onPublishFolderAlbum: (
        selectedArtistId: String?,
        artistName: String,
        artistPhotoUri: Uri?,
        albumTitle: String,
        releaseYear: Long,
        customCoverUri: Uri?,
        fallbackCoverPath: String,
        selectedTracks: List<ScannedLocalTrack>,
        onDone: (Boolean, String?) -> Unit
    ) -> Unit,
    onCreateArtist: (
        name: String,
        bio: String,
        photoUri: Uri?,
        photoUrlInput: String,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onCreateAlbum: (
        artist: CatalogArtist,
        title: String,
        year: Long,
        coverUri: Uri?,
        coverUrlInput: String,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onPublishTrack: (
        artist: CatalogArtist,
        album: CatalogAlbum?,
        title: String,
        trackNumber: Long,
        lyrics: String,
        audioUri: Uri?,
        audioUrlInput: String,
        coverUri: Uri?,
        coverUrlInput: String,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onDeleteArtist: (String) -> Unit,
    onDeleteAlbum: (String) -> Unit,
    onDeleteTrack: (String) -> Unit,
    onUpdateArtist: (
        artist: CatalogArtist,
        newName: String,
        newBio: String,
        newPhotoUri: Uri?,
        newPhotoUrlInput: String,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onUpdateAlbum: (
        album: CatalogAlbum,
        newArtist: CatalogArtist,
        newTitle: String,
        newReleaseYear: Long,
        newCoverUri: Uri?,
        newCoverUrlInput: String,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onUpdateTrack: (
        track: CatalogTrack,
        newArtist: CatalogArtist,
        newAlbum: CatalogAlbum?,
        newTitle: String,
        newTrackNumber: Long,
        newLyrics: String,
        newCoverUri: Uri?,
        newCoverUrlInput: String,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onPublishAppUpdate: (
        versionCode: Long,
        versionName: String,
        downloadUrl: String,
        releaseNotes: String,
        mandatory: Boolean,
        onDone: (Boolean) -> Unit
    ) -> Unit,
    onDeleteComment: (String) -> Unit = {}
) {
    val palette = LocalAmdvPalette.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingArtist by remember { mutableStateOf<CatalogArtist?>(null) }
    var editingAlbum by remember { mutableStateOf<CatalogAlbum?>(null) }
    var editingTrack by remember { mutableStateOf<CatalogTrack?>(null) }
    val rawTracksById = remember(rawCatalogTracks) { rawCatalogTracks.associateBy { it.id } }

    // Artist form state
    var artistName by remember { mutableStateOf("") }
    var artistBio by remember { mutableStateOf("") }
    var artistPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var artistPhotoUrl by remember { mutableStateOf("") }

    // Album form state
    var selectedAlbumArtist by remember(artists) { mutableStateOf(artists.firstOrNull()) }
    var albumTitle by remember { mutableStateOf("") }
    var albumYear by remember { mutableStateOf("2026") }
    var albumCoverUri by remember { mutableStateOf<Uri?>(null) }
    var albumCoverUrl by remember { mutableStateOf("") }

    // Track form state
    var useDirectUrlMode by remember { mutableStateOf(false) }
    var selectedTrackArtist by remember(artists) { mutableStateOf(artists.firstOrNull()) }
    var selectedTrackAlbum by remember(albums, selectedTrackArtist) {
        mutableStateOf(albums.firstOrNull { it.artistId == selectedTrackArtist?.id })
    }
    var trackTitle by remember { mutableStateOf("") }
    var trackNumberText by remember { mutableStateOf("1") }
    var trackLyrics by remember { mutableStateOf("") }
    var trackAudioUri by remember { mutableStateOf<Uri?>(null) }
    var trackAudioUrl by remember { mutableStateOf("") }
    var trackCoverUri by remember { mutableStateOf<Uri?>(null) }
    var trackCoverUrl by remember { mutableStateOf("") }

    val artistPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                artistPhotoUri = uri
                artistPhotoUrl = ""
            }
        }
    )
    val albumCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                albumCoverUri = uri
                albumCoverUrl = ""
            }
        }
    )
    val trackCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                trackCoverUri = uri
                trackCoverUrl = ""
            }
        }
    )
    val trackAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                trackAudioUri = uri
                trackAudioUrl = ""
            }
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CloudUpload,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Panel Admin • Catálogo en la nube",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            val signedBackendReady = remember { CloudinaryUploader.isSignedBackendConfigured() }
            val unsignedReady = remember { CloudinaryUploader.isUnsignedConfigured() }
            val cloudReady = signedBackendReady || unsignedReady
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (cloudReady) {
                    palette.accent.copy(alpha = 0.14f)
                } else {
                    palette.surfaceElevated.copy(alpha = 0.7f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (cloudReady) palette.accent.copy(alpha = 0.45f) else palette.border
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = if (cloudReady) Icons.Filled.CloudDone else Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (cloudReady) palette.accent else palette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            signedBackendReady -> "Backend propio conectado ✓ (subidas firmadas a Cloudinary)"
                            unsignedReady -> "Nube gratis conectada: Cloudinary ✓ (carpetas, MP3 y portadas)"
                            else -> "Pega la URL de tu backend de Render en UPLOAD_SIGNER_URL dentro del panel Secrets de AI Studio."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (cloudReady) palette.accent else palette.textSecondary
                    )
                }
            }

            if (isUploading) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { uploadProgress.coerceIn(0.05f, 1f) },
                    color = palette.accent,
                    trackColor = palette.border,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Subiendo a la nube… ${(uploadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = palette.surface,
                contentColor = palette.accent,
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Carpeta", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Artista", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Álbum", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Canción", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Actualizar App", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = {
                        Text(
                            text = if (moderationComments.isNotEmpty()) {
                                "Comentarios (${moderationComments.size})"
                            } else {
                                "Comentarios"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        FolderUploadTabContent(
                            artists = artists,
                            importState = folderImportState,
                            isUploading = isUploading,
                            onScanFolderTree = onScanFolderTree,
                            onScanAudioUris = onScanAudioUris,
                            onClearPreview = onClearFolderPreview,
                            onPublishFolderAlbum = onPublishFolderAlbum
                        )
                    }

                    1 -> {
                        OutlinedTextField(
                            value = artistName,
                            onValueChange = { artistName = it },
                            label = { Text("Nombre del artista") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = artistBio,
                            onValueChange = { artistBio = it },
                            label = { Text("Biografía o género (opcional)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = {
                                artistPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (artistPhotoUri != null) "Foto seleccionada ✓" else "Elegir foto desde el celular")
                        }
                        OutlinedTextField(
                            value = artistPhotoUrl,
                            onValueChange = {
                                artistPhotoUrl = it
                                if (it.isNotBlank()) artistPhotoUri = null
                            },
                            label = { Text("O pegar URL de foto (opcional)") },
                            placeholder = { Text("https://.../artista.jpg") },
                            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            enabled = !isUploading && artistName.isNotBlank(),
                            onClick = {
                                onCreateArtist(artistName, artistBio, artistPhotoUri, artistPhotoUrl) { ok ->
                                    if (ok) {
                                        artistName = ""
                                        artistBio = ""
                                        artistPhotoUri = null
                                        artistPhotoUrl = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accent,
                                contentColor = if (palette.isDark) Color.Black else Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_publish_artist_btn")
                        ) {
                            Text("Publicar Artista en la Nube", fontWeight = FontWeight.Bold)
                        }

                        if (artists.isNotEmpty()) {
                            HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 6.dp))
                            Text("Artistas publicados (${artists.size})", style = MaterialTheme.typography.titleSmall)
                            artists.forEach { art ->
                                AdminExistingItemRow(
                                    title = art.name,
                                    subtitle = art.bio.ifBlank { "Sin biografía" },
                                    onEdit = { editingArtist = art },
                                    onDelete = { onDeleteArtist(art.id) }
                                )
                            }
                        }
                    }

                    2 -> {
                        if (artists.isEmpty()) {
                            Text(
                                text = "Primero crea al menos un Artista en la pestaña Artista.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        } else {
                            Text("Selecciona el artista:", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(artists, key = { it.id }) { art ->
                                    FilterChip(
                                        selected = selectedAlbumArtist?.id == art.id,
                                        onClick = { selectedAlbumArtist = art },
                                        label = { Text(art.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                                            selectedLabelColor = palette.accent
                                        )
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = albumTitle,
                                onValueChange = { albumTitle = it },
                                label = { Text("Título del álbum") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = albumYear,
                                onValueChange = { albumYear = it.filter { ch -> ch.isDigit() }.take(4) },
                                label = { Text("Año de lanzamiento") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedButton(
                                onClick = {
                                    albumCoverPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Album, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (albumCoverUri != null) "Portada seleccionada ✓" else "Elegir portada desde el celular")
                            }
                            OutlinedTextField(
                                value = albumCoverUrl,
                                onValueChange = {
                                    albumCoverUrl = it
                                    if (it.isNotBlank()) albumCoverUri = null
                                },
                                label = { Text("O pegar URL de portada (opcional)") },
                                placeholder = { Text("https://.../portada.jpg") },
                                leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                enabled = !isUploading && selectedAlbumArtist != null && albumTitle.isNotBlank(),
                                onClick = {
                                    val artist = selectedAlbumArtist ?: return@Button
                                    val year = albumYear.toLongOrNull() ?: 2026L
                                    onCreateAlbum(artist, albumTitle, year, albumCoverUri, albumCoverUrl) { ok ->
                                        if (ok) {
                                            albumTitle = ""
                                            albumCoverUri = null
                                            albumCoverUrl = ""
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accent,
                                    contentColor = if (palette.isDark) Color.Black else Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_publish_album_btn")
                            ) {
                                Text("Publicar Álbum en la Nube", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (albums.isNotEmpty()) {
                            HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 6.dp))
                            Text("Álbumes publicados (${albums.size})", style = MaterialTheme.typography.titleSmall)
                            albums.forEach { alb ->
                                AdminExistingItemRow(
                                    title = alb.title,
                                    subtitle = "${alb.artistName} • ${alb.releaseYear}",
                                    onEdit = { editingAlbum = alb },
                                    onDelete = { onDeleteAlbum(alb.id) }
                                )
                            }
                        }
                    }

                    3 -> {
                        if (artists.isEmpty()) {
                            Text(
                                text = "Primero crea al menos un Artista en la pestaña Artista.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        } else {
                            Text("Artista:", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(artists, key = { it.id }) { art ->
                                    FilterChip(
                                        selected = selectedTrackArtist?.id == art.id,
                                        onClick = {
                                            selectedTrackArtist = art
                                            selectedTrackAlbum = albums.firstOrNull { it.artistId == art.id }
                                        },
                                        label = { Text(art.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                                            selectedLabelColor = palette.accent
                                        )
                                    )
                                }
                            }

                            val artistAlbums = albums.filter { it.artistId == selectedTrackArtist?.id }
                            if (artistAlbums.isNotEmpty()) {
                                Text("Álbum (opcional):", style = MaterialTheme.typography.labelLarge)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        FilterChip(
                                            selected = selectedTrackAlbum == null,
                                            onClick = { selectedTrackAlbum = null },
                                            label = { Text("Sencillo") }
                                        )
                                    }
                                    items(artistAlbums, key = { it.id }) { alb ->
                                        FilterChip(
                                            selected = selectedTrackAlbum?.id == alb.id,
                                            onClick = { selectedTrackAlbum = alb },
                                            label = { Text(alb.title) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                                                selectedLabelColor = palette.accent
                                            )
                                        )
                                    }
                                }
                            }

                            Text("Origen del audio:", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !useDirectUrlMode,
                                    onClick = {
                                        useDirectUrlMode = false
                                        trackAudioUrl = ""
                                    },
                                    label = { Text("Subir desde el celular") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                                        selectedLabelColor = palette.accent
                                    )
                                )
                                FilterChip(
                                    selected = useDirectUrlMode,
                                    onClick = {
                                        useDirectUrlMode = true
                                        trackAudioUri = null
                                        trackCoverUri = null
                                    },
                                    label = { Text("Enlace directo (URL)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                                        selectedLabelColor = palette.accent
                                    )
                                )
                            }

                            if (useDirectUrlMode) {
                                OutlinedTextField(
                                    value = trackAudioUrl,
                                    onValueChange = { trackAudioUrl = it },
                                    label = { Text("URL directa del MP3 / Audio") },
                                    placeholder = { Text("https://.../cancion.mp3") },
                                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedButton(
                                    onClick = { trackAudioPicker.launch(arrayOf("audio/*")) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.AudioFile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (trackAudioUri != null) "Archivo de audio seleccionado ✓" else "Elegir archivo MP3 / Audio")
                                }
                            }

                            OutlinedTextField(
                                value = trackTitle,
                                onValueChange = { trackTitle = it },
                                label = {
                                    Text(
                                        if (useDirectUrlMode) {
                                            "Título de la canción"
                                        } else {
                                            "Título de la canción (opcional si tiene ID3)"
                                        }
                                    )
                                },
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
                            OutlinedTextField(
                                value = trackLyrics,
                                onValueChange = { trackLyrics = it },
                                label = { Text("Letra de la canción (opcional)") },
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (useDirectUrlMode) {
                                OutlinedTextField(
                                    value = trackCoverUrl,
                                    onValueChange = { trackCoverUrl = it },
                                    label = { Text("URL de portada personalizada (opcional)") },
                                    placeholder = { Text("https://.../portada.jpg") },
                                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        trackCoverPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Image, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (trackCoverUri != null) "Portada personalizada ✓" else "Portada personalizada (opcional)")
                                }
                            }

                            val hasAudioSource = if (useDirectUrlMode) {
                                trackAudioUrl.isNotBlank() && trackTitle.isNotBlank()
                            } else {
                                trackAudioUri != null
                            }
                            Button(
                                enabled = !isUploading && selectedTrackArtist != null && hasAudioSource,
                                onClick = {
                                    val artist = selectedTrackArtist ?: return@Button
                                    val num = trackNumberText.toLongOrNull() ?: 1L
                                    onPublishTrack(
                                        artist,
                                        selectedTrackAlbum,
                                        trackTitle,
                                        num,
                                        trackLyrics,
                                        if (useDirectUrlMode) null else trackAudioUri,
                                        if (useDirectUrlMode) trackAudioUrl else "",
                                        if (useDirectUrlMode) null else trackCoverUri,
                                        trackCoverUrl
                                    ) { ok ->
                                        if (ok) {
                                            trackTitle = ""
                                            trackLyrics = ""
                                            trackAudioUri = null
                                            trackAudioUrl = ""
                                            trackCoverUri = null
                                            trackCoverUrl = ""
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accent,
                                    contentColor = if (palette.isDark) Color.Black else Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_publish_track_btn")
                            ) {
                                Text("Publicar Canción en la Nube", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (cloudTracks.isNotEmpty()) {
                            HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 6.dp))
                            Text("Canciones en la nube (${cloudTracks.size})", style = MaterialTheme.typography.titleSmall)
                            cloudTracks.forEach { tr ->
                                val raw = rawTracksById[tr.id]
                                AdminExistingItemRow(
                                    title = tr.title,
                                    subtitle = "${tr.artist}${if (tr.albumTitle.isNotBlank()) " • ${tr.albumTitle}" else ""}",
                                    onEdit = raw?.let { { editingTrack = it } },
                                    onDelete = { onDeleteTrack(tr.id) }
                                )
                            }
                        }
                    }

                    4 -> {
                        AdminAppUpdateTabContent(
                            currentCloudRelease = latestRelease,
                            isUploading = isUploading,
                            onPublishAppUpdate = onPublishAppUpdate
                        )
                    }

                    5 -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChatBubbleOutline,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Moderación de comentarios (${moderationComments.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                        Text(
                            text = "Revisa o elimina comentarios con marca de tiempo publicados por usuarios en las canciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        if (moderationComments.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surfaceElevated.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Aún no hay comentarios publicados en ninguna canción.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            moderationComments.forEach { comment ->
                                AdminExistingItemRow(
                                    title = "${comment.trackTitle.ifBlank { "Canción" }} • ${formatCommentTimestamp(comment.timestampMs)}",
                                    subtitle = "${comment.authorName}: ${comment.text}",
                                    onEdit = null,
                                    onDelete = { onDeleteComment(comment.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val targetArtist = editingArtist
    if (targetArtist != null) {
        EditArtistDialog(
            artist = targetArtist,
            isSaving = isUploading,
            onDismiss = { editingArtist = null },
            onSave = { name, bio, photoUri, photoUrl ->
                onUpdateArtist(targetArtist, name, bio, photoUri, photoUrl) { ok ->
                    if (ok) editingArtist = null
                }
            }
        )
    }

    val targetAlbum = editingAlbum
    if (targetAlbum != null) {
        EditAlbumDialog(
            album = targetAlbum,
            artists = artists,
            isSaving = isUploading,
            onDismiss = { editingAlbum = null },
            onSave = { artist, title, year, coverUri, coverUrl ->
                onUpdateAlbum(targetAlbum, artist, title, year, coverUri, coverUrl) { ok ->
                    if (ok) editingAlbum = null
                }
            }
        )
    }

    val targetTrack = editingTrack
    if (targetTrack != null) {
        EditTrackDialog(
            track = targetTrack,
            artists = artists,
            albums = albums,
            isSaving = isUploading,
            onDismiss = { editingTrack = null },
            onSave = { artist, album, title, num, lyrics, coverUri, coverUrl ->
                onUpdateTrack(targetTrack, artist, album, title, num, lyrics, coverUri, coverUrl) { ok ->
                    if (ok) editingTrack = null
                }
            }
        )
    }
}

@Composable
private fun AdminExistingItemRow(
    title: String,
    subtitle: String,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    val palette = LocalAmdvPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceElevated.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onEdit != null) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar elemento",
                        tint = palette.accent
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Eliminar de la nube",
                    tint = TrashRose
                )
            }
        }
    }
}
