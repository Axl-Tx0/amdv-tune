package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.model.AppReleaseConfig
import com.example.model.CatalogAlbum
import com.example.model.CatalogArtist
import com.example.model.CatalogTrack
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.UUID

class CatalogRepository(
    private val appContext: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val storage = Firebase.storage

    private fun requireAdminUid(): String {
        val user = auth.currentUser
            ?: throw IllegalStateException("Debes iniciar sesión con amdvtune@gmail.com.")
        val email = user.email?.trim().orEmpty()
        check(email.equals("amdvtune@gmail.com", ignoreCase = true)) {
            "Solo la cuenta Admin (amdvtune@gmail.com) puede modificar el catálogo en la nube."
        }
        return user.uid
    }

    private fun safeId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(16)}"

    fun observeArtists(isAdmin: Boolean): Flow<List<CatalogArtist>> {
        val query = if (isAdmin) {
            db.collection("catalog_artists")
        } else {
            db.collection("catalog_artists").whereEqualTo("published", true)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CatalogArtist::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.name.lowercase() }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: error
                if (auth.currentUser != null) {
                    handleFirestoreError(toLog, OperationType.LIST, "catalog_artists")
                }
                throw error
            }
    }

    fun observeAlbums(isAdmin: Boolean): Flow<List<CatalogAlbum>> {
        val query = if (isAdmin) {
            db.collection("catalog_albums")
        } else {
            db.collection("catalog_albums").whereEqualTo("published", true)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CatalogAlbum::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareByDescending<CatalogAlbum> { it.releaseYear }.thenBy { it.title.lowercase() })
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: error
                if (auth.currentUser != null) {
                    handleFirestoreError(toLog, OperationType.LIST, "catalog_albums")
                }
                throw error
            }
    }

    fun observeTracks(isAdmin: Boolean): Flow<List<CatalogTrack>> {
        val query = if (isAdmin) {
            db.collection("catalog_tracks")
        } else {
            db.collection("catalog_tracks").whereEqualTo("published", true)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CatalogTrack::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareBy<CatalogTrack> { it.albumTitle.lowercase() }.thenBy { it.trackNumber }.thenBy { it.title.lowercase() })
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: error
                if (auth.currentUser != null) {
                    handleFirestoreError(toLog, OperationType.LIST, "catalog_tracks")
                }
                throw error
            }
    }

    fun observeAppReleaseConfig(): Flow<AppReleaseConfig?> {
        return db.collection("app_config")
            .document("release")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        AppReleaseConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(null)
            }
    }

    suspend fun publishAppReleaseConfig(
        latestVersionCode: Long,
        latestVersionName: String,
        downloadUrl: String,
        releaseNotes: String,
        mandatory: Boolean
    ): Result<Unit> {
        val docPath = "app_config/release"
        return try {
            val uid = requireAdminUid()
            require(latestVersionCode in 1L..999999L) {
                "Ingresa un código de versión válido (ej. 2)."
            }
            val safeName = latestVersionName.trim().take(40)
            require(safeName.isNotEmpty()) {
                "Ingresa el nombre de versión (ej. 1.1)."
            }
            val safeUrl = downloadUrl.trim()
            require(safeUrl.startsWith("https://", ignoreCase = true) && safeUrl.length in 8..1000) {
                "El enlace de descarga de GitHub Releases debe empezar con https://"
            }
            val safeNotes = releaseNotes.trim().take(1500)

            val payload = mapOf(
                "userId" to uid,
                "latestVersionCode" to latestVersionCode,
                "latestVersionName" to safeName,
                "downloadUrl" to safeUrl,
                "releaseNotes" to safeNotes,
                "mandatory" to mandatory,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("app_config").document("release").set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(Exception(e.localizedMessage ?: "No se pudo publicar la actualización."))
        }
    }

    private fun validateDirectUrl(raw: String, maxLength: Int, fieldLabel: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        require(trimmed.startsWith("https://", ignoreCase = true) || trimmed.startsWith("http://", ignoreCase = true)) {
            "El enlace de $fieldLabel debe empezar con https:// o http://"
        }
        require(trimmed.length <= maxLength) {
            "El enlace de $fieldLabel es demasiado largo (máx. $maxLength caracteres)."
        }
        return trimmed
    }

    suspend fun createArtist(
        name: String,
        bio: String,
        photoUri: Uri?,
        photoUrlInput: String = "",
        onProgress: (Float) -> Unit = {}
    ): Result<CatalogArtist> {
        val artistId = safeId("art")
        val docPath = "catalog_artists/$artistId"
        return try {
            val uid = requireAdminUid()
            val safeName = name.trim().take(100)
            require(safeName.isNotEmpty()) { "Ingresa el nombre del artista." }
            val safeBio = bio.trim().take(500)

            val photoUrl = when {
                photoUri != null -> uploadUriToStorage(
                    uri = photoUri,
                    storagePath = "catalog/artists/$artistId.jpg",
                    contentType = appContext.contentResolver.getType(photoUri) ?: "image/jpeg",
                    onProgress = onProgress
                )
                photoUrlInput.isNotBlank() -> validateDirectUrl(photoUrlInput, 1000, "foto del artista")
                else -> ""
            }

            val payload = mapOf(
                "userId" to uid,
                "name" to safeName,
                "bio" to safeBio,
                "photoUrl" to photoUrl,
                "published" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("catalog_artists").document(artistId).set(payload).await()
            onProgress(1f)
            Result.success(
                CatalogArtist(
                    id = artistId,
                    userId = uid,
                    name = safeName,
                    bio = safeBio,
                    photoUrl = photoUrl,
                    published = true
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(Exception(formatUploadError(e)))
        }
    }

    suspend fun createAlbum(
        artist: CatalogArtist,
        title: String,
        releaseYear: Long,
        coverUri: Uri?,
        coverUrlInput: String = "",
        onProgress: (Float) -> Unit = {}
    ): Result<CatalogAlbum> {
        val albumId = safeId("alb")
        val docPath = "catalog_albums/$albumId"
        return try {
            val uid = requireAdminUid()
            val safeTitle = title.trim().take(120)
            require(safeTitle.isNotEmpty()) { "Ingresa el título del álbum." }
            val safeYear = releaseYear.coerceIn(1900L, 2100L)
            val safeArtistName = artist.name.trim().ifBlank { "Artista" }.take(100)

            val coverUrl = when {
                coverUri != null -> uploadUriToStorage(
                    uri = coverUri,
                    storagePath = "catalog/albums/$albumId.jpg",
                    contentType = appContext.contentResolver.getType(coverUri) ?: "image/jpeg",
                    onProgress = onProgress
                )
                coverUrlInput.isNotBlank() -> validateDirectUrl(coverUrlInput, 1000, "portada del álbum")
                else -> artist.photoUrl.take(1000)
            }

            val payload = mapOf(
                "userId" to uid,
                "artistId" to artist.id,
                "artistName" to safeArtistName,
                "title" to safeTitle,
                "releaseYear" to safeYear,
                "coverUrl" to coverUrl,
                "published" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("catalog_albums").document(albumId).set(payload).await()
            onProgress(1f)
            Result.success(
                CatalogAlbum(
                    id = albumId,
                    userId = uid,
                    artistId = artist.id,
                    artistName = safeArtistName,
                    title = safeTitle,
                    releaseYear = safeYear,
                    coverUrl = coverUrl,
                    published = true
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(Exception(formatUploadError(e)))
        }
    }

    suspend fun uploadAndPublishTrack(
        artist: CatalogArtist,
        album: CatalogAlbum?,
        title: String,
        trackNumber: Long,
        lyrics: String,
        audioUri: Uri?,
        audioUrlInput: String = "",
        customCoverUri: Uri? = null,
        customCoverUrlInput: String = "",
        onProgress: (Float) -> Unit = {}
    ): Result<CatalogTrack> {
        val trackId = safeId("trk")
        val docPath = "catalog_tracks/$trackId"
        return try {
            val uid = requireAdminUid()
            require(audioUri != null || audioUrlInput.isNotBlank()) {
                "Selecciona un archivo de audio o pega el enlace directo del MP3."
            }

            val extracted = if (audioUri != null) {
                extractAudioInfo(audioUri)
            } else {
                ExtractedAudioInfo(null, 0L, null)
            }

            val directAudioUrl = if (audioUri == null) {
                validateDirectUrl(audioUrlInput, 1500, "audio")
            } else {
                ""
            }

            val inferredUrlTitle = if (audioUri == null && title.isBlank()) {
                Uri.parse(directAudioUrl).lastPathSegment
                    ?.substringBefore('?')
                    ?.substringBeforeLast('.')
                    ?.replace('_', ' ')
                    ?.replace('-', ' ')
                    ?.trim()
            } else {
                null
            }

            val safeTitle = title.trim()
                .ifBlank { extracted.title ?: inferredUrlTitle ?: "" }
                .take(120)
            require(safeTitle.isNotEmpty()) { "Ingresa el título de la canción." }

            val safeArtistName = artist.name.trim().ifBlank { "Artista" }.take(100)
            val safeAlbumId = album?.id.orEmpty()
            val safeAlbumTitle = album?.title?.take(120).orEmpty()
            val safeTrackNumber = trackNumber.coerceIn(1L, 999L)
            val manualLyrics = LrcParser.trimToMaxChars(lyrics, 5000)

            val audioUrl: String
            val safeDuration: Long
            if (audioUri != null) {
                val mimeType = appContext.contentResolver.getType(audioUri) ?: "audio/mpeg"
                val ext = when {
                    mimeType.contains("wav", ignoreCase = true) -> "wav"
                    mimeType.contains("mp4", ignoreCase = true) || mimeType.contains("m4a", ignoreCase = true) -> "m4a"
                    mimeType.contains("ogg", ignoreCase = true) -> "ogg"
                    else -> "mp3"
                }
                audioUrl = uploadUriToStorage(
                    uri = audioUri,
                    storagePath = "catalog/tracks/$trackId.$ext",
                    contentType = mimeType,
                    onProgress = { p -> onProgress(p * 0.8f) }
                )
                safeDuration = extracted.durationSec.coerceIn(0L, 86400L)
            } else {
                audioUrl = directAudioUrl
                onProgress(0.6f)
                safeDuration = probeRemoteDurationSec(directAudioUrl).coerceIn(0L, 86400L)
            }

            val coverUrl = when {
                customCoverUri != null -> uploadUriToStorage(
                    uri = customCoverUri,
                    storagePath = "catalog/track_covers/$trackId.jpg",
                    contentType = appContext.contentResolver.getType(customCoverUri) ?: "image/jpeg",
                    onProgress = { p -> onProgress(0.8f + p * 0.15f) }
                )
                customCoverUrlInput.isNotBlank() -> validateDirectUrl(customCoverUrlInput, 1500, "portada")
                extracted.embeddedArt != null -> uploadBytesToStorage(
                    bytes = extracted.embeddedArt,
                    storagePath = "catalog/track_covers/$trackId.jpg",
                    contentType = "image/jpeg"
                )
                !album?.coverUrl.isNullOrBlank() -> album?.coverUrl.orEmpty()
                else -> artist.photoUrl
            }.take(1500)

            val safeLyrics = if (manualLyrics.isNotBlank()) {
                manualLyrics
            } else {
                withTimeoutOrNull(5_500L) {
                    LrcLibService.fetchLyrics(
                        title = safeTitle,
                        artist = safeArtistName,
                        album = safeAlbumTitle,
                        durationSec = safeDuration
                    ).getOrNull()?.lyrics.orEmpty()
                }.orEmpty().let { LrcParser.trimToMaxChars(it, 5000) }
            }

            val payload = mapOf(
                "userId" to uid,
                "artistId" to artist.id,
                "artistName" to safeArtistName,
                "albumId" to safeAlbumId,
                "albumTitle" to safeAlbumTitle,
                "title" to safeTitle,
                "durationSec" to safeDuration,
                "trackNumber" to safeTrackNumber,
                "audioUrl" to audioUrl.take(1500),
                "coverUrl" to coverUrl,
                "lyrics" to safeLyrics,
                "published" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("catalog_tracks").document(trackId).set(payload).await()
            onProgress(1f)
            Result.success(
                CatalogTrack(
                    id = trackId,
                    userId = uid,
                    artistId = artist.id,
                    artistName = safeArtistName,
                    albumId = safeAlbumId,
                    albumTitle = safeAlbumTitle,
                    title = safeTitle,
                    durationSec = safeDuration,
                    trackNumber = safeTrackNumber,
                    audioUrl = audioUrl,
                    coverUrl = coverUrl,
                    lyrics = safeLyrics,
                    published = true
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(Exception(formatUploadError(e)))
        }
    }

    data class FolderUploadSummary(
        val artistId: String,
        val artistName: String,
        val uploadedCount: Int
    )

    suspend fun uploadFolderAlbum(
        existingArtists: List<CatalogArtist>,
        selectedArtistId: String?,
        artistName: String,
        artistPhotoUri: Uri?,
        albumTitle: String,
        releaseYear: Long,
        customCoverUri: Uri?,
        fallbackCoverPath: String,
        tracks: List<ScannedLocalTrack>,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<FolderUploadSummary> {
        return try {
            val uid = requireAdminUid()
            val safeArtistInputName = artistName.trim().ifBlank { "Artista" }.take(100)
            val safeAlbumTitle = albumTitle.trim().ifBlank { "Álbum" }.take(120)
            val safeYear = releaseYear.coerceIn(1900L, 2100L)
            require(tracks.isNotEmpty()) { "Selecciona al menos una canción para subir." }

            val initialStatus = if (CloudinaryUploader.isSignedBackendConfigured()) {
                "Conectando con tu servidor y subiendo portada…"
            } else {
                "Subiendo portada del álbum…"
            }
            onProgress(0.03f, initialStatus)
            val albumId = safeId("alb")
            val albumCoverUrl = when {
                customCoverUri != null -> uploadUriToStorage(
                    uri = customCoverUri,
                    storagePath = "catalog/albums/$albumId.jpg",
                    contentType = appContext.contentResolver.getType(customCoverUri) ?: "image/jpeg",
                    onProgress = {}
                )
                fallbackCoverPath.isNotBlank() && File(fallbackCoverPath).exists() -> {
                    val coverBytes = withContext(Dispatchers.IO) { File(fallbackCoverPath).readBytes() }
                    uploadBytesToStorage(
                        bytes = coverBytes,
                        storagePath = "catalog/albums/$albumId.jpg",
                        contentType = "image/jpeg"
                    )
                }
                else -> ""
            }.take(1000)

            val matchedExistingArtist = when {
                !selectedArtistId.isNullOrBlank() ->
                    existingArtists.firstOrNull { it.id == selectedArtistId }
                else ->
                    existingArtists.firstOrNull { it.name.trim().equals(safeArtistInputName, ignoreCase = true) }
            }

            val artistId: String
            val safeArtistName: String
            val resolvedArtistPhotoUrl: String
            if (matchedExistingArtist != null) {
                artistId = matchedExistingArtist.id
                safeArtistName = matchedExistingArtist.name.trim().ifBlank { safeArtistInputName }.take(100)
                if (artistPhotoUri != null) {
                    onProgress(0.08f, "Actualizando foto de $safeArtistName…")
                    val uploadedPhoto = uploadUriToStorage(
                        uri = artistPhotoUri,
                        storagePath = "catalog/artists/$artistId.jpg",
                        contentType = appContext.contentResolver.getType(artistPhotoUri) ?: "image/jpeg",
                        onProgress = {}
                    ).take(1000)
                    db.collection("catalog_artists").document(artistId)
                        .update(
                            mapOf(
                                "photoUrl" to uploadedPhoto,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        .await()
                    resolvedArtistPhotoUrl = uploadedPhoto
                } else {
                    onProgress(0.08f, "Agregando álbum a $safeArtistName…")
                    resolvedArtistPhotoUrl = matchedExistingArtist.photoUrl
                }
            } else {
                artistId = safeId("art")
                safeArtistName = safeArtistInputName
                onProgress(0.08f, "Creando artista: $safeArtistName…")
                resolvedArtistPhotoUrl = if (artistPhotoUri != null) {
                    uploadUriToStorage(
                        uri = artistPhotoUri,
                        storagePath = "catalog/artists/$artistId.jpg",
                        contentType = appContext.contentResolver.getType(artistPhotoUri) ?: "image/jpeg",
                        onProgress = {}
                    ).take(1000)
                } else {
                    albumCoverUrl
                }
                val artistPayload = mapOf(
                    "userId" to uid,
                    "name" to safeArtistName,
                    "bio" to "",
                    "photoUrl" to resolvedArtistPhotoUrl,
                    "published" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("catalog_artists").document(artistId).set(artistPayload).await()
            }

            onProgress(0.12f, "Creando álbum: $safeAlbumTitle…")
            val albumPayload = mapOf(
                "userId" to uid,
                "artistId" to artistId,
                "artistName" to safeArtistName,
                "title" to safeAlbumTitle,
                "releaseYear" to safeYear,
                "coverUrl" to albumCoverUrl.ifBlank { resolvedArtistPhotoUrl }.take(1000),
                "published" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("catalog_albums").document(albumId).set(albumPayload).await()

            val total = tracks.size
            var uploaded = 0
            tracks.forEachIndexed { idx, item ->
                val baseProgress = 0.14f + (idx.toFloat() / total.toFloat()) * 0.84f
                val stepSpan = 0.84f / total.toFloat()
                onProgress(baseProgress, "Subiendo ${idx + 1}/$total: ${item.title}")

                val trackId = safeId("trk")
                val mimeType = appContext.contentResolver.getType(item.audioUri) ?: "audio/mpeg"
                val ext = when {
                    mimeType.contains("wav", ignoreCase = true) -> "wav"
                    mimeType.contains("flac", ignoreCase = true) -> "flac"
                    mimeType.contains("mp4", ignoreCase = true) || mimeType.contains("m4a", ignoreCase = true) -> "m4a"
                    mimeType.contains("ogg", ignoreCase = true) -> "ogg"
                    else -> item.fileName.substringAfterLast('.', "mp3").lowercase().take(6).ifBlank { "mp3" }
                }

                val audioUrl = uploadUriToStorage(
                    uri = item.audioUri,
                    storagePath = "catalog/tracks/$trackId.$ext",
                    contentType = mimeType,
                    onProgress = { filePct ->
                        onProgress(baseProgress + filePct * stepSpan * 0.72f, "Subiendo ${idx + 1}/$total: ${item.title}")
                    }
                )

                val trackCoverUrl = if (albumCoverUrl.isNotBlank()) {
                    albumCoverUrl
                } else if (item.previewCoverPath.isNotBlank() && File(item.previewCoverPath).exists()) {
                    val bytes = withContext(Dispatchers.IO) { File(item.previewCoverPath).readBytes() }
                    uploadBytesToStorage(
                        bytes = bytes,
                        storagePath = "catalog/track_covers/$trackId.jpg",
                        contentType = "image/jpeg"
                    )
                } else {
                    resolvedArtistPhotoUrl
                }.take(1500)

                val cleanTrackTitle = item.title.trim().ifBlank { "Pista ${idx + 1}" }.take(120)
                val safeTrackDuration = item.durationSec.coerceIn(0L, 86400L)
                onProgress(
                    baseProgress + stepSpan * 0.85f,
                    "Buscando letra (${idx + 1}/$total): $cleanTrackTitle"
                )
                val autoLyrics = withTimeoutOrNull(4_500L) {
                    LrcLibService.fetchLyrics(
                        title = cleanTrackTitle,
                        artist = safeArtistName,
                        album = safeAlbumTitle,
                        durationSec = safeTrackDuration
                    ).getOrNull()?.lyrics.orEmpty()
                }.orEmpty().let { LrcParser.trimToMaxChars(it, 5000) }

                val trackPayload = mapOf(
                    "userId" to uid,
                    "artistId" to artistId,
                    "artistName" to safeArtistName,
                    "albumId" to albumId,
                    "albumTitle" to safeAlbumTitle,
                    "title" to cleanTrackTitle,
                    "durationSec" to safeTrackDuration,
                    "trackNumber" to (idx + 1L).coerceIn(1L, 999L),
                    "audioUrl" to audioUrl.take(1500),
                    "coverUrl" to trackCoverUrl,
                    "lyrics" to autoLyrics,
                    "published" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("catalog_tracks").document(trackId).set(trackPayload).await()
                uploaded++
            }

            val summaryNote = "Álbum publicado en $safeArtistName ($uploaded canciones)"
            onProgress(1f, summaryNote)
            Result.success(
                FolderUploadSummary(
                    artistId = artistId,
                    artistName = safeArtistName,
                    uploadedCount = uploaded
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "catalog_tracks")
            Result.failure(Exception(formatUploadError(e)))
        }
    }

    suspend fun updateCatalogTrackLyrics(trackId: String, lyrics: String): Result<Unit> {
        val docPath = "catalog_tracks/$trackId"
        return try {
            requireAdminUid()
            val safeLyrics = lyrics.trim().take(5000)
            db.collection("catalog_tracks").document(trackId)
                .update(
                    mapOf(
                        "lyrics" to safeLyrics,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCatalogTrack(trackId: String): Result<Unit> {
        val docPath = "catalog_tracks/$trackId"
        return try {
            requireAdminUid()
            db.collection("catalog_tracks").document(trackId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCatalogAlbum(albumId: String): Result<Unit> {
        val docPath = "catalog_albums/$albumId"
        return try {
            requireAdminUid()
            db.collection("catalog_albums").document(albumId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCatalogArtist(artistId: String): Result<Unit> {
        val docPath = "catalog_artists/$artistId"
        return try {
            requireAdminUid()
            db.collection("catalog_artists").document(artistId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private data class CloudinaryTarget(
        val folder: String,
        val publicId: String,
        val fileName: String,
        val isAudio: Boolean
    )

    private fun resolveCloudinaryTarget(storagePath: String, contentType: String): CloudinaryTarget {
        val normalized = storagePath.trimStart('/')
        val fileName = normalized.substringAfterLast('/')
        val parentDir = normalized.substringBeforeLast('/', "").removePrefix("catalog/").trim('/')
        val folder = if (parentDir.isBlank()) "amdvtune" else "amdvtune/$parentDir"
        val publicId = fileName.substringBeforeLast('.')
        val isAudio = contentType.startsWith("audio/", ignoreCase = true) ||
            parentDir.equals("tracks", ignoreCase = true)
        return CloudinaryTarget(
            folder = folder,
            publicId = publicId,
            fileName = fileName,
            isAudio = isAudio
        )
    }

    private suspend fun uploadUriToStorage(
        uri: Uri,
        storagePath: String,
        contentType: String,
        onProgress: (Float) -> Unit
    ): String = withContext(Dispatchers.IO) {
        if (CloudinaryUploader.isConfigured()) {
            val target = resolveCloudinaryTarget(storagePath, contentType)
            return@withContext CloudinaryUploader.uploadUri(
                context = appContext,
                uri = uri,
                folder = target.folder,
                publicId = target.publicId,
                fileName = target.fileName,
                contentType = contentType,
                isAudio = target.isAudio,
                onProgress = onProgress
            )
        }

        val ref = storage.reference.child(storagePath)
        val metadata = StorageMetadata.Builder().setContentType(contentType).build()
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("No se pudo leer el archivo seleccionado.")
        val uploadTask = ref.putBytes(bytes, metadata)
        uploadTask.addOnProgressListener { snap ->
            if (snap.totalByteCount > 0) {
                onProgress((snap.bytesTransferred.toFloat() / snap.totalByteCount.toFloat()).coerceIn(0f, 1f))
            }
        }
        uploadTask.await()
        ref.downloadUrl.await().toString()
    }

    private suspend fun uploadBytesToStorage(
        bytes: ByteArray,
        storagePath: String,
        contentType: String
    ): String = withContext(Dispatchers.IO) {
        if (CloudinaryUploader.isConfigured()) {
            val target = resolveCloudinaryTarget(storagePath, contentType)
            return@withContext CloudinaryUploader.uploadBytes(
                bytes = bytes,
                folder = target.folder,
                publicId = target.publicId,
                fileName = target.fileName,
                contentType = contentType,
                isAudio = target.isAudio
            )
        }

        val ref = storage.reference.child(storagePath)
        val metadata = StorageMetadata.Builder().setContentType(contentType).build()
        ref.putBytes(bytes, metadata).await()
        ref.downloadUrl.await().toString()
    }

    private data class ExtractedAudioInfo(
        val title: String?,
        val durationSec: Long,
        val embeddedArt: ByteArray?
    )

    private suspend fun extractAudioInfo(uri: Uri): ExtractedAudioInfo = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()?.takeIf { it.isNotEmpty() }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val art = retriever.embeddedPicture
            ExtractedAudioInfo(
                title = title,
                durationSec = (durationMs / 1000L).coerceAtLeast(0L),
                embeddedArt = art
            )
        } catch (_: Exception) {
            ExtractedAudioInfo(null, 0L, null)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun probeRemoteDurationSec(audioUrl: String): Long = withTimeoutOrNull(4500L) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(audioUrl, emptyMap())
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                (durationMs / 1000L).coerceAtLeast(0L)
            } catch (_: Exception) {
                0L
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }
    } ?: 0L

    private fun formatUploadError(e: Exception): String {
        return when (e) {
            is StorageException -> {
                if (!CloudinaryUploader.isConfigured()) {
                    "Configura UPLOAD_SIGNER_URL (la URL de tu backend en Render) en el panel Secrets de AI Studio para subir carpetas y MP3 a la nube."
                } else if (e.errorCode == StorageException.ERROR_NOT_AUTHORIZED || e.httpResultCode == 403) {
                    "Permiso denegado en el almacenamiento en la nube."
                } else {
                    "Error al subir archivo a la nube: ${e.localizedMessage ?: "verifica tu conexión"}"
                }
            }
            else -> e.localizedMessage ?: "No se pudo guardar en la nube."
        }
    }
}
