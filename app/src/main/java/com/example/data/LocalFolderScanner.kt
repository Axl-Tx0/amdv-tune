package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.UUID

data class ScannedLocalTrack(
    val id: String,
    val audioUri: Uri,
    val fileName: String,
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val durationSec: Long,
    val trackNumber: Long,
    val previewCoverPath: String = ""
)

data class ScannedFolderAlbum(
    val folderName: String,
    val artistName: String,
    val albumTitle: String,
    val releaseYear: Long,
    val coverUri: Uri? = null,
    val previewCoverPath: String = "",
    val tracks: List<ScannedLocalTrack>
)

object LocalFolderScanner {

    private val audioExtensions = setOf("mp3", "m4a", "wav", "flac", "ogg", "aac", "opus", "mp4")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
    private val yearRegex = Regex("\\b(19[5-9]\\d|20[0-3]\\d)\\b")

    suspend fun scanFolderTree(context: Context, treeUri: Uri): Result<ScannedFolderAlbum> =
        withContext(Dispatchers.IO) {
            try {
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootName = queryDocumentDisplayName(context, DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId))
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: rootDocId.substringAfterLast('/').substringAfterLast(':').ifBlank { "Álbum" }

                val audioEntries = mutableListOf<Pair<Uri, String>>()
                val imageEntries = mutableListOf<Pair<Uri, String>>()

                collectTreeDocuments(
                    context = context,
                    treeUri = treeUri,
                    parentDocId = rootDocId,
                    depth = 0,
                    audioOut = audioEntries,
                    imageOut = imageEntries
                )

                if (audioEntries.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            "No se encontraron archivos de música (MP3, M4A, WAV, FLAC, OGG) dentro de la carpeta \"$rootName\"."
                        )
                    )
                }

                val preferredFolderCover = imageEntries
                    .sortedBy { (_, name) ->
                        val lower = name.lowercase()
                        when {
                            lower.startsWith("cover") || lower.startsWith("folder") || lower.startsWith("front") -> 0
                            lower.startsWith("album") -> 1
                            else -> 2
                        }
                    }
                    .firstOrNull()
                    ?.first

                val album = buildScannedAlbum(
                    context = context,
                    fallbackFolderName = rootName,
                    audioEntries = audioEntries,
                    folderCoverUri = preferredFolderCover
                )
                Result.success(album)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(Exception("No se pudo leer la carpeta: ${e.localizedMessage ?: "error"}"))
            }
        }

    suspend fun scanAudioUris(context: Context, uris: List<Uri>): Result<ScannedFolderAlbum> =
        withContext(Dispatchers.IO) {
            try {
                if (uris.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("No se seleccionaron archivos de audio.")
                    )
                }
                val entries = uris.map { uri ->
                    val name = queryOpenableDisplayName(context, uri) ?: "Cancion.mp3"
                    uri to name
                }
                val album = buildScannedAlbum(
                    context = context,
                    fallbackFolderName = "Mi Álbum",
                    audioEntries = entries,
                    folderCoverUri = null
                )
                Result.success(album)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(Exception("No se pudieron leer los audios: ${e.localizedMessage ?: "error"}"))
            }
        }

    private fun collectTreeDocuments(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        depth: Int,
        audioOut: MutableList<Pair<Uri, String>>,
        imageOut: MutableList<Pair<Uri, String>>
    ) {
        if (depth > 3 || audioOut.size >= 120) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext() && audioOut.size < 120) {
                val docId = if (idIdx >= 0) cursor.getString(idIdx).orEmpty() else continue
                val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty() else ""
                if (docId.isEmpty()) continue

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    collectTreeDocuments(context, treeUri, docId, depth + 1, audioOut, imageOut)
                } else {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    when {
                        mime.startsWith("audio/", ignoreCase = true) || ext in audioExtensions -> {
                            audioOut.add(docUri to name.ifBlank { "Pista.mp3" })
                        }
                        mime.startsWith("image/", ignoreCase = true) || ext in imageExtensions -> {
                            imageOut.add(docUri to name)
                        }
                    }
                }
            }
        }
    }

    private fun buildScannedAlbum(
        context: Context,
        fallbackFolderName: String,
        audioEntries: List<Pair<Uri, String>>,
        folderCoverUri: Uri?
    ): ScannedFolderAlbum {
        val previewDir = File(context.cacheDir, "folder_import_preview").apply {
            if (exists()) listFiles()?.forEach { it.delete() }
            mkdirs()
        }

        val sortedEntries = audioEntries.sortedBy { (_, name) -> name.lowercase() }
        var firstEmbeddedCoverPath = ""
        val detectedArtists = mutableListOf<String>()
        val detectedAlbums = mutableListOf<String>()
        var detectedYear: Long? = null

        val rawTracks = sortedEntries.mapIndexed { idx, (uri, fileName) ->
            val meta = extractTrackMeta(context, uri, fileName, previewDir)
            if (meta.artist.isNotBlank()) detectedArtists.add(meta.artist)
            if (meta.album.isNotBlank()) detectedAlbums.add(meta.album)
            if (detectedYear == null && meta.year != null) detectedYear = meta.year
            if (firstEmbeddedCoverPath.isBlank() && meta.previewCoverPath.isNotBlank()) {
                firstEmbeddedCoverPath = meta.previewCoverPath
            }
            ScannedLocalTrack(
                id = "scan_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                audioUri = uri,
                fileName = fileName,
                title = meta.title,
                artistName = meta.artist,
                albumTitle = meta.album,
                durationSec = meta.durationSec,
                trackNumber = meta.trackNumber ?: (idx + 1L),
                previewCoverPath = meta.previewCoverPath
            )
        }

        val sortedTracks = rawTracks
            .sortedWith(compareBy<ScannedLocalTrack> { it.trackNumber }.thenBy { it.fileName.lowercase() })
            .mapIndexed { idx, item -> item.copy(trackNumber = idx + 1L) }

        val dominantArtist = detectedArtists
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: "Artista"

        val dominantAlbum = detectedAlbums
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: fallbackFolderName

        val resolvedYear = detectedYear
            ?: yearRegex.find(fallbackFolderName)?.value?.toLongOrNull()
            ?: Calendar.getInstance().get(Calendar.YEAR).toLong().coerceIn(2000L, 2100L)

        val folderCoverPreviewPath = if (folderCoverUri != null) {
            copyUriToPreviewFile(context, folderCoverUri, File(previewDir, "album_cover.jpg"))
                ?: firstEmbeddedCoverPath
        } else {
            firstEmbeddedCoverPath
        }

        return ScannedFolderAlbum(
            folderName = fallbackFolderName.take(120),
            artistName = dominantArtist.take(100),
            albumTitle = dominantAlbum.take(120),
            releaseYear = resolvedYear.coerceIn(1900L, 2100L),
            coverUri = folderCoverUri,
            previewCoverPath = folderCoverPreviewPath,
            tracks = sortedTracks.map { t ->
                if (t.artistName.isBlank()) t.copy(artistName = dominantArtist) else t
            }
        )
    }

    private data class RawTrackMeta(
        val title: String,
        val artist: String,
        val album: String,
        val year: Long?,
        val durationSec: Long,
        val trackNumber: Long?,
        val previewCoverPath: String
    )

    private fun extractTrackMeta(
        context: Context,
        uri: Uri,
        fileName: String,
        previewDir: File
    ): RawTrackMeta {
        val fallbackTitle = fileName
            .substringBeforeLast('.')
            .replace(Regex("^\\d+[\\s.\\-_]+"), "")
            .replace('_', ' ')
            .trim()
            .ifBlank { fileName.substringBeforeLast('.').ifBlank { "Sin título" } }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackTitle
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?.trim()?.takeIf { it.isNotEmpty() }
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()?.takeIf { it.isNotEmpty() } ?: albumArtist.orEmpty()
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim().orEmpty()
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.let { yearRegex.find(it)?.value?.toLongOrNull() }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val trackNumRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.trim()
                ?.substringBefore('/')
                ?.toLongOrNull()
                ?.takeIf { it in 1L..999L }

            var previewPath = ""
            val artBytes = retriever.embeddedPicture
            if (artBytes != null && artBytes.isNotEmpty()) {
                val outFile = File(previewDir, "art_${UUID.randomUUID().toString().take(8)}.jpg")
                runCatching {
                    outFile.writeBytes(artBytes)
                    previewPath = outFile.absolutePath
                }
            }

            RawTrackMeta(
                title = title.take(120),
                artist = artist.take(100),
                album = album.take(120),
                year = year,
                durationSec = (durationMs / 1000L).coerceIn(0L, 86400L),
                trackNumber = trackNumRaw,
                previewCoverPath = previewPath
            )
        } catch (_: Exception) {
            RawTrackMeta(
                title = fallbackTitle.take(120),
                artist = "",
                album = "",
                year = null,
                durationSec = 0L,
                trackNumber = null,
                previewCoverPath = ""
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun copyUriToPreviewFile(context: Context, uri: Uri, dest: File): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            if (dest.exists() && dest.length() > 0L) dest.absolutePath else null
        }.getOrNull()
    }

    private fun queryDocumentDisplayName(context: Context, docUri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    private fun queryOpenableDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }
}
