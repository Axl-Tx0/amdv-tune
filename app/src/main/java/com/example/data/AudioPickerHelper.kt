package com.example.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.LocalTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object AudioPickerHelper {

    suspend fun extractTrackFromUri(
        context: Context,
        uri: Uri,
        nextSortOrder: Int
    ): LocalTrackEntity = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some document providers only grant temporary read permission
        }

        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var durationSec = 0L
        var coverPath: String? = null

        val trackId = "local_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            durationSec = (durationMs / 1000L).coerceAtLeast(0L)

            val artBytes = retriever.embeddedPicture
            if (artBytes != null && artBytes.isNotEmpty()) {
                val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
                val outFile = File(coversDir, "$trackId.jpg")
                outFile.writeBytes(artBytes)
                coverPath = outFile.absolutePath
            }
        } catch (_: Exception) {
            // Fallback to display name from ContentResolver
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        if (title.isNullOrBlank()) {
            title = queryDisplayName(context, uri)
                ?.substringBeforeLast('.')
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "Canción local"
        }

        val safeTitle = title.take(120)
        val safeArtist = (artist ?: "Desconocido").take(100)
        val monogramChar = safeTitle.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: 'M'
        val finalCoverKey = coverPath ?: "MONO_VIOLET_$monogramChar"

        LocalTrackEntity(
            id = trackId,
            title = safeTitle,
            artist = safeArtist,
            durationSec = durationSec,
            sourceType = "LOCAL",
            audioUri = uri.toString(),
            coverKey = finalCoverKey,
            favorite = false,
            lyrics = "Archivo importado desde el dispositivo.\n\nArtista: $safeArtist\nTítulo: $safeTitle",
            sortOrder = nextSortOrder,
            deleted = false
        )
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }
}
