package com.example.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Entity(tableName = "tracks")
data class LocalTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val sourceType: String, // "BUNDLED" or "LOCAL"
    val audioUri: String, // raw resource name for BUNDLED, content:// URI for LOCAL
    val coverKey: String, // drawable key or cached image path or "MONOGRAM"
    val favorite: Boolean,
    val lyrics: String,
    val sortOrder: Int,
    val deleted: Boolean = false
)

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrackEntity(
    @PrimaryKey val trackId: String,
    val artistId: String,
    val artistName: String,
    val albumId: String,
    val albumTitle: String,
    val title: String,
    val durationSec: Long,
    val trackNumber: Long,
    val localAudioPath: String,
    val localCoverPath: String,
    val lyrics: String,
    val favorite: Boolean = false,
    val downloadedAtMs: Long = System.currentTimeMillis()
)

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val themeMode: String = "NEGRO",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudTrack(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val artist: String = "",
    val durationSec: Long = 0L,
    val favorite: Boolean = false,
    val sourceType: String = "BUNDLED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CatalogArtist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CatalogAlbum(
    @DocumentId val id: String = "",
    val userId: String = "",
    val artistId: String = "",
    val artistName: String = "",
    val title: String = "",
    val releaseYear: Long = 2026L,
    val coverUrl: String = "",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CatalogTrack(
    @DocumentId val id: String = "",
    val userId: String = "",
    val artistId: String = "",
    val artistName: String = "",
    val albumId: String = "",
    val albumTitle: String = "",
    val title: String = "",
    val durationSec: Long = 0L,
    val trackNumber: Long = 1L,
    val audioUrl: String = "",
    val coverUrl: String = "",
    val lyrics: String = "",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppReleaseConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val latestVersionCode: Long = 0L,
    val latestVersionName: String = "",
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val mandatory: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun hasNewerVersion(currentVersionCode: Int): Boolean {
        return latestVersionCode > currentVersionCode.toLong() &&
            downloadUrl.trim().startsWith("https://", ignoreCase = true)
    }
}

data class TrackUiModel(
    val id: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val sourceType: String, // "BUNDLED", "LOCAL", or "CLOUD"
    val audioUri: String,
    val coverKey: String,
    val favorite: Boolean,
    val lyrics: String,
    val albumTitle: String = "",
    val artistId: String = "",
    val albumId: String = "",
    val trackNumber: Long = 1L,
    val isCloudCatalog: Boolean = false,
    val isDownloaded: Boolean = false,
    val localAudioPath: String? = null,
    val downloadProgress: Float? = null
) {
    val formattedDuration: String
        get() {
            val total = durationSec.coerceAtLeast(0L)
            val mins = total / 60L
            val secs = total % 60L
            return "$mins:${secs.toString().padStart(2, '0')}"
        }
}

fun LocalTrackEntity.toUiModel(): TrackUiModel = TrackUiModel(
    id = id,
    title = title,
    artist = artist,
    durationSec = durationSec,
    sourceType = sourceType,
    audioUri = audioUri,
    coverKey = coverKey,
    favorite = favorite,
    lyrics = lyrics
)

fun DownloadedTrackEntity.toOfflineUiModel(isFavorite: Boolean): TrackUiModel = TrackUiModel(
    id = trackId,
    title = title,
    artist = artistName,
    durationSec = durationSec,
    sourceType = "CLOUD",
    audioUri = localAudioPath,
    coverKey = localCoverPath.ifBlank { "MONO_EMERALD_${title.firstOrNull()?.uppercaseChar() ?: 'M'}" },
    favorite = isFavorite,
    lyrics = lyrics,
    albumTitle = albumTitle,
    artistId = artistId,
    albumId = albumId,
    trackNumber = trackNumber,
    isCloudCatalog = true,
    isDownloaded = true,
    localAudioPath = localAudioPath
)
