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

@Keep
@IgnoreExtraProperties
data class UserPlaylist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val trackIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class EqualizerPreset(val key: String, val label: String, val bandLevelsMb: List<Int>, val defaultBass: Int) {
    NORMAL("NORMAL", "Normal", listOf(0, 0, 0, 0, 0), 0),
    BASS_BOOST("BASS_BOOST", "Graves+", listOf(600, 400, 0, 100, 200), 650),
    ELECTRONICA("ELECTRONICA", "Electrónica", listOf(500, 200, -100, 300, 500), 500),
    ROCK("ROCK", "Rock", listOf(450, 150, -150, 250, 450), 350),
    POP("POP", "Pop", listOf(-100, 250, 400, 200, -100), 200),
    VOCAL("VOCAL", "Vocal", listOf(-250, 100, 450, 350, 100), 0),
    CUSTOM("CUSTOM", "Manual", listOf(0, 0, 0, 0, 0), 0);

    companion object {
        fun fromKey(raw: String?): EqualizerPreset {
            return entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: NORMAL
        }
    }
}

data class EqualizerBandUi(
    val index: Int,
    val label: String,
    val levelMb: Int
)

data class AudioEffectsState(
    val enabled: Boolean = false,
    val preset: EqualizerPreset = EqualizerPreset.NORMAL,
    val bands: List<EqualizerBandUi> = defaultBands(),
    val minLevelMb: Int = -1200,
    val maxLevelMb: Int = 1200,
    val bassBoostStrength: Int = 0, // 0..1000
    val hardwareAttached: Boolean = false
) {
    companion object {
        fun defaultBands(levels: List<Int> = listOf(0, 0, 0, 0, 0)): List<EqualizerBandUi> {
            val labels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
            return labels.mapIndexed { idx, label ->
                EqualizerBandUi(index = idx, label = label, levelMb = levels.getOrElse(idx) { 0 })
            }
        }
    }
}

data class ListeningStats(
    val recentTrackIds: List<String> = emptyList(),
    val playCounts: Map<String, Int> = emptyMap(),
    val totalListeningSec: Long = 0L,
    val activeDays: List<String> = emptyList(), // ISO YYYY-MM-DD sorted ascending
    val lyricsOffsetsMs: Map<String, Long> = emptyMap()
) {
    fun currentStreakDays(todayIso: String): Int {
        if (activeDays.isEmpty()) return 0
        val daySet = activeDays.toSet()
        return runCatching {
            var cursor = java.time.LocalDate.parse(todayIso)
            if (!daySet.contains(cursor.toString())) {
                cursor = cursor.minusDays(1)
            }
            var streak = 0
            while (daySet.contains(cursor.toString())) {
                streak++
                cursor = cursor.minusDays(1)
            }
            streak
        }.getOrElse { activeDays.size }
    }
}

@Keep
@IgnoreExtraProperties
data class CommunityTrackStats(
    @DocumentId val id: String = "",
    val trackId: String = "",
    val playCount: Long = 0L,
    val fireCount: Long = 0L,
    val heartCount: Long = 0L,
    val headphonesCount: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserTrackReaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val fire: Boolean = false,
    val heart: Boolean = false,
    val headphones: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class JamSession(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostUserId: String = "",
    val hostName: String = "",
    val currentTrackId: String = "",
    val playing: Boolean = false,
    val positionMs: Long = 0L,
    val queueTrackIds: List<String> = emptyList(),
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SharedPlaylist(
    @DocumentId val id: String = "",
    val shareCode: String = "",
    val ownerUserId: String = "",
    val ownerName: String = "",
    val name: String = "",
    val description: String = "",
    val trackIds: List<String> = emptyList(),
    val collaborative: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserArtistFollow(
    @DocumentId val id: String = "",
    val userId: String = "",
    val artistId: String = "",
    val artistName: String = "",
    @ServerTimestamp val followedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ArtistStats(
    @DocumentId val id: String = "",
    val artistId: String = "",
    val followerCount: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TrackComment(
    @DocumentId val id: String = "",
    val trackId: String = "",
    val trackTitle: String = "",
    val userId: String = "",
    val authorName: String = "",
    val timestampMs: Long = 0L,
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
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
