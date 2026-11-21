package com.example.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.model.LocalTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DeviceMusicSyncHelper {

    private val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "wma")

    suspend fun scanMediaStoreAudio(
        context: Context,
        existingById: Map<String, LocalTrackEntity>,
        startSortOrder: Int
    ): List<LocalTrackEntity> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        // Filter out very short notification/UI clips (< 10 seconds)
        val selection = "${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val results = mutableListOf<LocalTrackEntity>()
        var orderCounter = startSortOrder

        resolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idCol)
                val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                val displayName = cursor.getString(displayNameCol)?.trim().orEmpty()
                val safeTitle = rawTitle.ifBlank {
                    displayName.substringBeforeLast('.').ifBlank { "Canción del dispositivo" }
                }.take(120)

                val rawArtist = cursor.getString(artistCol)?.trim().orEmpty()
                val safeArtist = if (rawArtist.isBlank() || rawArtist.equals("<unknown>", ignoreCase = true)) {
                    "Artista desconocido"
                } else {
                    rawArtist.take(100)
                }

                val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                val durationSec = (durationMs / 1000L).coerceAtLeast(1L)
                val albumId = cursor.getLong(albumIdCol)

                val trackId = "device_$mediaId"
                val contentUri = ContentUris.withAppendedId(collection, mediaId)
                val coverKey = if (albumId > 0L) {
                    ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()
                } else {
                    val ch = safeTitle.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: 'M'
                    "MONO_CYAN_$ch"
                }

                val previous = existingById[trackId]
                orderCounter++
                results.add(
                    LocalTrackEntity(
                        id = trackId,
                        title = safeTitle,
                        artist = safeArtist,
                        durationSec = durationSec,
                        sourceType = "LOCAL",
                        audioUri = contentUri.toString(),
                        coverKey = coverKey,
                        favorite = previous?.favorite ?: false,
                        lyrics = previous?.lyrics.orEmpty(),
                        sortOrder = previous?.sortOrder ?: orderCounter,
                        deleted = false
                    )
                )
            }
        }
        results
    }

    suspend fun scanFolderTreeAudio(
        context: Context,
        treeUri: Uri,
        existingById: Map<String, LocalTrackEntity>,
        startSortOrder: Int
    ): List<LocalTrackEntity> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val audioUris = mutableListOf<Pair<Uri, String>>()
        collectAudioDocuments(context, treeUri, rootDocId, audioUris, depth = 0)

        val results = mutableListOf<LocalTrackEntity>()
        var orderCounter = startSortOrder
        audioUris.forEach { (docUri, displayName) ->
            val trackId = "device_saf_${docUri.toString().hashCode().toUInt()}"
            val previous = existingById[trackId]
            val extracted = AudioPickerHelper.extractTrackFromUri(context, docUri, ++orderCounter)
            val fallbackTitle = displayName.substringBeforeLast('.').ifBlank { extracted.title }
            results.add(
                extracted.copy(
                    id = trackId,
                    title = extracted.title.ifBlank { fallbackTitle }.take(120),
                    favorite = previous?.favorite ?: false,
                    lyrics = previous?.lyrics ?: "",
                    sortOrder = previous?.sortOrder ?: orderCounter,
                    deleted = false
                )
            )
        }
        results
    }

    private fun collectAudioDocuments(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        out: MutableList<Pair<Uri, String>>,
        depth: Int
    ) {
        if (depth > 5 || out.size >= 500) return
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

            while (cursor.moveToNext() && out.size < 500) {
                val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty() else ""

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    collectAudioDocuments(context, treeUri, docId, out, depth + 1)
                } else {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (mime.startsWith("audio/") || ext in audioExtensions) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        out.add(docUri to name)
                    }
                }
            }
        }
    }
}
