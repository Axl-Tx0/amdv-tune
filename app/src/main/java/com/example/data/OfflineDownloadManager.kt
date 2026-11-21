package com.example.data

import android.content.Context
import com.example.model.CatalogTrack
import com.example.model.DownloadedTrackEntity
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class OfflineDownloadManager(
    context: Context,
    private val downloadedDao: DownloadedTrackDao
) {
    private val appContext = context.applicationContext
    private val audioDir: File by lazy {
        File(appContext.filesDir, "offline_audio").apply { mkdirs() }
    }
    private val coverDir: File by lazy {
        File(appContext.filesDir, "offline_covers").apply { mkdirs() }
    }

    suspend fun downloadTrack(
        track: CatalogTrack,
        isFavorite: Boolean,
        onProgress: (Float) -> Unit
    ): Result<DownloadedTrackEntity> = withContext(Dispatchers.IO) {
        try {
            val safeName = track.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val audioFile = File(audioDir, "$safeName.audio")
            val tempAudio = File(audioDir, "$safeName.audio.tmp")

            downloadUrlToFile(
                url = track.audioUrl,
                destFile = tempAudio,
                onProgress = { p -> onProgress(p * 0.9f) }
            )
            if (audioFile.exists()) audioFile.delete()
            if (!tempAudio.renameTo(audioFile)) {
                tempAudio.copyTo(audioFile, overwrite = true)
                tempAudio.delete()
            }

            var localCoverPath = ""
            if (track.coverUrl.isNotBlank()) {
                val coverFile = File(coverDir, "$safeName.jpg")
                val tempCover = File(coverDir, "$safeName.jpg.tmp")
                try {
                    downloadUrlToFile(
                        url = track.coverUrl,
                        destFile = tempCover,
                        onProgress = {}
                    )
                    if (coverFile.exists()) coverFile.delete()
                    if (!tempCover.renameTo(coverFile)) {
                        tempCover.copyTo(coverFile, overwrite = true)
                        tempCover.delete()
                    }
                    localCoverPath = coverFile.absolutePath
                } catch (_: Exception) {
                    tempCover.delete()
                }
            }

            val entity = DownloadedTrackEntity(
                trackId = track.id,
                artistId = track.artistId,
                artistName = track.artistName,
                albumId = track.albumId,
                albumTitle = track.albumTitle,
                title = track.title,
                durationSec = track.durationSec,
                trackNumber = track.trackNumber,
                localAudioPath = audioFile.absolutePath,
                localCoverPath = localCoverPath,
                lyrics = track.lyrics,
                favorite = isFavorite,
                downloadedAtMs = System.currentTimeMillis()
            )
            downloadedDao.upsert(entity)
            onProgress(1f)
            Result.success(entity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo descargar offline: ${e.localizedMessage ?: "error de red"}"))
        }
    }

    suspend fun removeDownload(trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = downloadedDao.getByTrackId(trackId)
            if (existing != null) {
                File(existing.localAudioPath).takeIf { it.exists() }?.delete()
                if (existing.localCoverPath.isNotBlank()) {
                    File(existing.localCoverPath).takeIf { it.exists() }?.delete()
                }
            }
            downloadedDao.deleteByTrackId(trackId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadUrlToFile(
        url: String,
        destFile: File,
        onProgress: (Float) -> Unit
    ) {
        if (url.startsWith("gs://")) {
            val ref = Firebase.storage.getReferenceFromUrl(url)
            val task = ref.getFile(destFile)
            task.addOnProgressListener { snap ->
                if (snap.totalByteCount > 0) {
                    onProgress((snap.bytesTransferred.toFloat() / snap.totalByteCount.toFloat()).coerceIn(0f, 1f))
                }
            }
            task.await()
            return
        }

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
        }
        try {
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buf = ByteArray(8192)
                    var readTotal = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        output.write(buf, 0, read)
                        readTotal += read
                        if (total > 0) {
                            onProgress((readTotal.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
