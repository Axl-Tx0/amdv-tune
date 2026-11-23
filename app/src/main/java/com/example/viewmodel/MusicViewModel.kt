package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AudioPickerHelper
import com.example.data.CatalogRepository
import com.example.data.CloudSyncRepository
import com.example.data.CommunityAndJamRepository
import com.example.data.DeviceMusicSyncHelper
import com.example.data.ListeningStatsRepository
import com.example.data.LocalFolderScanner
import com.example.data.LrcLibService
import com.example.data.LrcLyricsCache
import com.example.data.LrcParser
import com.example.data.MusicPlayerController
import com.example.data.OfflineDownloadManager
import com.example.data.PersistedEqualizerConfig
import com.example.data.PlaybackState
import com.example.data.ScannedFolderAlbum
import com.example.data.ScannedLocalTrack
import com.example.data.SeedTracksProvider
import com.example.data.ThemePreferencesRepository
import com.example.model.AppReleaseConfig
import com.example.model.AppThemeMode
import com.example.model.ArtistStats
import com.example.model.AudioEffectsState
import com.example.model.CatalogAlbum
import com.example.model.CatalogArtist
import com.example.model.CatalogTrack
import com.example.model.CommunityTrackStats
import com.example.model.DownloadedTrackEntity
import com.example.model.EqualizerPreset
import com.example.model.JamSession
import com.example.model.ListeningStats
import com.example.model.LocalTrackEntity
import com.example.model.TrackComment
import com.example.model.TrackUiModel
import com.example.model.UserPlaylist
import com.example.model.UserTrackReaction
import com.example.model.toOfflineUiModel
import com.example.model.toUiModel
import java.time.LocalDate
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class LyricsLookupUiState(
    val loadingTrackId: String? = null,
    val statusTrackId: String? = null,
    val statusMessage: String? = null
)

data class FolderImportUiState(
    val isScanning: Boolean = false,
    val preview: ScannedFolderAlbum? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

data class AppUiState(
    val tracks: List<TrackUiModel> = emptyList(),
    val cloudTracks: List<TrackUiModel> = emptyList(),
    val offlineTracks: List<TrackUiModel> = emptyList(),
    val localTracks: List<TrackUiModel> = emptyList(),
    val deviceTracks: List<TrackUiModel> = emptyList(),
    val artists: List<CatalogArtist> = emptyList(),
    val albums: List<CatalogAlbum> = emptyList(),
    val rawCatalogTracks: List<CatalogTrack> = emptyList(),
    val playlists: List<UserPlaylist> = emptyList(),
    val recentTracks: List<TrackUiModel> = emptyList(),
    val topTracks: List<Pair<TrackUiModel, Int>> = emptyList(),
    val topArtists: List<Pair<String, Int>> = emptyList(),
    val totalPlaysCount: Int = 0,
    val totalListeningMinutes: Int = 0,
    val listeningStreakDays: Int = 0,
    val lyricsOffsetsMs: Map<String, Long> = emptyMap(),
    val trendingTracks: List<Pair<TrackUiModel, Long>> = emptyList(),
    val communityStats: Map<String, CommunityTrackStats> = emptyMap(),
    val userReactions: Map<String, UserTrackReaction> = emptyMap(),
    val latestReleaseAlbum: CatalogAlbum? = null,
    val recentReleaseAlbums: List<CatalogAlbum> = emptyList(),
    val hasUnreadReleases: Boolean = false,
    val dailyMixTracks: List<TrackUiModel> = emptyList(),
    val activeJam: JamSession? = null,
    val isJamHost: Boolean = false,
    val artistStats: Map<String, ArtistStats> = emptyMap(),
    val followedArtistIds: Set<String> = emptySet(),
    val currentTrackComments: List<TrackComment> = emptyList(),
    val moderationComments: List<TrackComment> = emptyList(),
    val latestRelease: AppReleaseConfig? = null,
    val downloadProgress: Map<String, Float> = emptyMap(),
    val searchQuery: String = "",
    val themeMode: AppThemeMode = AppThemeMode.NEGRO,
    val syncMessage: String? = null,
    val isImporting: Boolean = false,
    val isCatalogUploading: Boolean = false,
    val catalogUploadProgress: Float = 0f
) {
    val favoriteCount: Int
        get() = tracks.count { it.favorite }

    val offlineCount: Int
        get() = offlineTracks.size

    val filteredTracks: List<TrackUiModel>
        get() {
            val q = searchQuery.trim()
            if (q.isEmpty()) return tracks
            return tracks.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.artist.contains(q, ignoreCase = true) ||
                    it.albumTitle.contains(q, ignoreCase = true)
            }
        }
}

private data class CatalogSnapshot(
    val artists: List<CatalogArtist> = emptyList(),
    val albums: List<CatalogAlbum> = emptyList(),
    val tracks: List<CatalogTrack> = emptyList(),
    val playlists: List<UserPlaylist> = emptyList(),
    val cloudFavoriteIds: Set<String> = emptySet(),
    val latestRelease: AppReleaseConfig? = null,
    val communityStats: Map<String, CommunityTrackStats> = emptyMap(),
    val userReactions: Map<String, UserTrackReaction> = emptyMap(),
    val activeJam: JamSession? = null,
    val artistStats: Map<String, ArtistStats> = emptyMap(),
    val followedArtistIds: Set<String> = emptySet(),
    val currentTrackComments: List<TrackComment> = emptyList(),
    val moderationComments: List<TrackComment> = emptyList()
)

private data class UiMeta(
    val query: String = "",
    val theme: AppThemeMode = AppThemeMode.NEGRO,
    val syncMessage: String? = null,
    val isImporting: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val downloadProgress: Map<String, Float> = emptyMap(),
    val listeningStats: ListeningStats = ListeningStats(),
    val dailyMixSeed: Long = 0L,
    val lastSeenReleaseMs: Long = 0L
)

class MusicViewModel(
    private val appContext: Context,
    private val database: AppDatabase,
    private val themeRepo: ThemePreferencesRepository,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val trackDao = database.trackDao()
    private val downloadedDao = database.downloadedTrackDao()
    private val offlineManager = OfflineDownloadManager(appContext, downloadedDao)
    private val lyricsCache = LrcLyricsCache(appContext)
    private val listeningStatsRepo = ListeningStatsRepository(appContext)

    private val _searchQuery = MutableStateFlow("")
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _isImporting = MutableStateFlow(false)
    private val _isCatalogUploading = MutableStateFlow(false)
    private val _catalogUploadProgress = MutableStateFlow(0f)
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    private val _catalogSnapshot = MutableStateFlow(CatalogSnapshot())
    private val _cachedLyrics = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _listeningStats = MutableStateFlow(ListeningStats())
    private val _dailyMixSeed = MutableStateFlow(runCatching { LocalDate.now().toEpochDay() }.getOrDefault(System.currentTimeMillis() / 86_400_000L))
    private val _folderImportState = MutableStateFlow(FolderImportUiState())
    val folderImportState: StateFlow<FolderImportUiState> = _folderImportState.asStateFlow()

    private val _lyricsLookupState = MutableStateFlow(LyricsLookupUiState())
    val lyricsLookupState: StateFlow<LyricsLookupUiState> = _lyricsLookupState.asStateFlow()

    private val attemptedAutoLyricsIds = mutableSetOf<String>()
    private val inFlightLyricsIds = mutableSetOf<String>()
    private var lastRecordedPlayTrackId: String? = null

    private var cloudSyncRepository: CloudSyncRepository? = null
    private var catalogRepository: CatalogRepository? = null
    private var communityRepository: CommunityAndJamRepository? = null
    private var activeUserId: String? = null
    private var activeDisplayName: String = "Usuario AMDV"
    private var isAdminSession: Boolean = false
    private var cloudSyncJob: Job? = null
    private var jamObserverJob: Job? = null
    private var trackCommentsJob: Job? = null
    private var lastPushedJamTrackId: String? = null
    private var lastPushedJamPlaying: Boolean? = null
    private var lastPushedJamPosMs: Long = 0L

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    private val uiMetaFlow = combine(
        _searchQuery,
        themeRepo.themeModeFlow,
        _syncMessage,
        _isImporting,
        _isCatalogUploading,
        _catalogUploadProgress,
        _downloadProgress,
        _listeningStats,
        _dailyMixSeed,
        themeRepo.lastSeenReleaseMsFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        UiMeta(
            query = values[0] as String,
            theme = values[1] as AppThemeMode,
            syncMessage = values[2] as String?,
            isImporting = values[3] as Boolean,
            isUploading = values[4] as Boolean,
            uploadProgress = values[5] as Float,
            downloadProgress = values[6] as Map<String, Float>,
            listeningStats = values[7] as ListeningStats,
            dailyMixSeed = values[8] as Long,
            lastSeenReleaseMs = values[9] as Long
        )
    }

    val uiState: StateFlow<AppUiState> = combine(
        trackDao.observeActiveTracks(),
        downloadedDao.observeDownloadedTracks(),
        _catalogSnapshot,
        _cachedLyrics,
        uiMetaFlow
    ) { localEntities, downloadedEntities, catalog, cachedLyrics, meta ->
        buildAppUiState(localEntities, downloadedEntities, catalog, cachedLyrics, meta)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AppUiState()
    )

    init {
        viewModelScope.launch {
            if (trackDao.countAll() == 0) {
                trackDao.insertAll(SeedTracksProvider.initialTracks())
            }
        }
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { lyricsCache.loadAll() }
            if (loaded.isNotEmpty()) {
                _cachedLyrics.update { loaded + it }
            }
        }
        viewModelScope.launch {
            _listeningStats.value = listeningStatsRepo.load()
        }
        viewModelScope.launch {
            val savedEq = themeRepo.equalizerConfigFlow.first()
            playerController.applyPersistedAudioEffects(
                enabled = savedEq.enabled,
                preset = savedEq.preset,
                bandLevelsMb = savedEq.bandLevelsMb,
                bassBoostStrength = savedEq.bassBoostStrength
            )
        }
        viewModelScope.launch {
            themeRepo.autoContinueFlow.collect { enabled ->
                playerController.setAutoContinueEnabled(enabled)
            }
        }
        viewModelScope.launch {
            themeRepo.muteOnNotificationsFlow.collect { enabled ->
                playerController.setMuteOnNotificationsEnabled(enabled)
            }
        }
        viewModelScope.launch {
            themeRepo.crossfadeSecondsFlow.collect { sec ->
                playerController.setCrossfadeSeconds(sec)
            }
        }
        viewModelScope.launch {
            playerController.playbackState
                .map { it.currentTrack }
                .distinctUntilChangedBy { it?.id }
                .collect { track ->
                    if (track != null && track.lyrics.isBlank()) {
                        fetchLyricsFromLrcLib(track, forceRefresh = false)
                    }
                    observeCommentsForCurrentTrack(track?.id)
                }
        }
        viewModelScope.launch {
            playerController.playbackState
                .map { Pair(it.currentTrack?.id, it.isPlaying) }
                .collect { (trackId, isPlaying) ->
                    if (trackId != null && isPlaying && trackId != lastRecordedPlayTrackId) {
                        lastRecordedPlayTrackId = trackId
                        recordTrackPlay(trackId)
                    }
                }
        }
        viewModelScope.launch {
            playerController.playbackState.collect { pb ->
                val jam = _catalogSnapshot.value.activeJam ?: return@collect
                val uid = activeUserId ?: return@collect
                if (!jam.active || jam.hostUserId != uid) return@collect
                val trackId = pb.currentTrack?.id.orEmpty()
                val playing = pb.isPlaying
                val pos = pb.positionMs
                val changed = trackId != lastPushedJamTrackId ||
                    playing != lastPushedJamPlaying ||
                    abs(pos - lastPushedJamPosMs) > 4000L
                if (changed && trackId.isNotBlank()) {
                    lastPushedJamTrackId = trackId
                    lastPushedJamPlaying = playing
                    lastPushedJamPosMs = pos
                    communityRepository?.updateJamPlayback(
                        roomCode = jam.roomCode,
                        currentTrackId = trackId,
                        playing = playing,
                        positionMs = pos
                    )
                }
            }
        }
    }

    private fun recordTrackPlay(trackId: String) {
        val current = _listeningStats.value
        val nextRecent = (listOf(trackId) + current.recentTrackIds.filter { it != trackId }).take(25)
        val nextCounts = current.playCounts.toMutableMap().apply {
            this[trackId] = (this[trackId] ?: 0) + 1
        }
        val trackDuration = uiState.value.tracks.firstOrNull { it.id == trackId }?.durationSec?.takeIf { it > 0L } ?: 180L
        val todayIso = runCatching { LocalDate.now().toString() }.getOrDefault("")
        val nextDays = if (todayIso.isNotBlank() && !current.activeDays.contains(todayIso)) {
            (current.activeDays + todayIso).sorted().takeLast(365)
        } else {
            current.activeDays
        }
        val updated = current.copy(
            recentTrackIds = nextRecent,
            playCounts = nextCounts,
            totalListeningSec = current.totalListeningSec + trackDuration,
            activeDays = nextDays
        )
        _listeningStats.value = updated
        viewModelScope.launch {
            listeningStatsRepo.save(updated)
            communityRepository?.recordCommunityPlay(trackId)
        }
    }

    private fun buildAppUiState(
        localEntities: List<LocalTrackEntity>,
        downloadedEntities: List<DownloadedTrackEntity>,
        catalog: CatalogSnapshot,
        cachedLyrics: Map<String, String>,
        meta: UiMeta
    ): AppUiState {
        val localUi = localEntities.map { entity ->
            val model = entity.toUiModel()
            val resolvedLyrics = model.lyrics.ifBlank { cachedLyrics[model.id].orEmpty() }
            if (resolvedLyrics != model.lyrics) model.copy(lyrics = resolvedLyrics) else model
        }
        val downloadedById = downloadedEntities.associateBy { it.trackId }

        val cloudUi = catalog.tracks.map { ct ->
            val downloaded = downloadedById[ct.id]
            val hasFile = downloaded != null && File(downloaded.localAudioPath).exists()
            val isFav = catalog.cloudFavoriteIds.contains(ct.id) || (downloaded?.favorite == true)
            val fallbackMonogram = "MONO_CYAN_${ct.title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: 'M'}"
            val cover = when {
                hasFile && downloaded!!.localCoverPath.isNotBlank() && File(downloaded.localCoverPath).exists() ->
                    downloaded.localCoverPath
                ct.coverUrl.isNotBlank() -> ct.coverUrl
                else -> fallbackMonogram
            }
            val resolvedLyrics = ct.lyrics
                .ifBlank { downloaded?.lyrics.orEmpty() }
                .ifBlank { cachedLyrics[ct.id].orEmpty() }
            TrackUiModel(
                id = ct.id,
                title = ct.title,
                artist = ct.artistName,
                durationSec = ct.durationSec,
                sourceType = "CLOUD",
                audioUri = ct.audioUrl,
                coverKey = cover,
                favorite = isFav,
                lyrics = resolvedLyrics,
                albumTitle = ct.albumTitle,
                artistId = ct.artistId,
                albumId = ct.albumId,
                trackNumber = ct.trackNumber,
                isCloudCatalog = true,
                isDownloaded = hasFile,
                localAudioPath = if (hasFile) downloaded!!.localAudioPath else null,
                downloadProgress = meta.downloadProgress[ct.id]
            )
        }

        val cloudIds = cloudUi.map { it.id }.toSet()
        val offlineOnlyUi = downloadedEntities
            .filter { File(it.localAudioPath).exists() && !cloudIds.contains(it.trackId) }
            .map { dl ->
                val isFav = catalog.cloudFavoriteIds.contains(dl.trackId) || dl.favorite
                val model = dl.toOfflineUiModel(isFav)
                val resolvedLyrics = model.lyrics.ifBlank { cachedLyrics[model.id].orEmpty() }
                if (resolvedLyrics != model.lyrics) model.copy(lyrics = resolvedLyrics) else model
            }

        val offlineUi = cloudUi.filter { it.isDownloaded } + offlineOnlyUi
        val deviceUi = localUi.filter { it.sourceType == "LOCAL" }
        val combinedTracks = cloudUi + offlineOnlyUi + localUi
        val tracksById = combinedTracks.associateBy { it.id }

        val recentTracks = meta.listeningStats.recentTrackIds.mapNotNull { tracksById[it] }
        val topTracks = meta.listeningStats.playCounts.entries
            .mapNotNull { (id, count) -> tracksById[id]?.let { it to count } }
            .sortedByDescending { it.second }
            .take(5)
        val artistPlayCounts = mutableMapOf<String, Int>()
        meta.listeningStats.playCounts.forEach { (id, count) ->
            val artistName = tracksById[id]?.artist?.trim()
            if (!artistName.isNullOrBlank()) {
                artistPlayCounts[artistName] = (artistPlayCounts[artistName] ?: 0) + count
            }
        }
        val topArtists = artistPlayCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }
        val totalPlays = meta.listeningStats.playCounts.values.sum()
        val totalListeningMinutes = (meta.listeningStats.totalListeningSec / 60L).toInt()
        val todayIso = runCatching { LocalDate.now().toString() }.getOrDefault("")
        val streakDays = meta.listeningStats.currentStreakDays(todayIso)

        val trendingTracks = catalog.communityStats.values
            .mapNotNull { stat ->
                val tr = tracksById[stat.trackId.ifBlank { stat.id }] ?: return@mapNotNull null
                val score = stat.playCount + (stat.fireCount + stat.heartCount + stat.headphonesCount) * 2L
                if (score <= 0L) null else Triple(tr, stat.playCount.coerceAtLeast(1L), score)
            }
            .sortedByDescending { it.third }
            .take(10)
            .map { it.first to it.second }

        val sortedReleaseAlbums = catalog.albums
            .sortedWith(
                compareByDescending<CatalogAlbum> { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.releaseYear }
            )
        val latestReleaseAlbum = sortedReleaseAlbums.firstOrNull()
        val newestReleaseTimestampMs = latestReleaseAlbum?.createdAt?.toDate()?.time ?: 0L
        val hasUnreadReleases = sortedReleaseAlbums.isNotEmpty() && (
            newestReleaseTimestampMs > meta.lastSeenReleaseMs ||
                (meta.lastSeenReleaseMs == 0L && sortedReleaseAlbums.isNotEmpty())
            )

        val dailyMixTracks = buildDailyMix(
            tracks = combinedTracks,
            playCounts = meta.listeningStats.playCounts,
            seed = meta.dailyMixSeed
        )

        playerController.updateCatalog(combinedTracks)

        return AppUiState(
            tracks = combinedTracks,
            cloudTracks = cloudUi + offlineOnlyUi,
            offlineTracks = offlineUi,
            localTracks = localUi,
            deviceTracks = deviceUi,
            artists = catalog.artists,
            albums = catalog.albums,
            rawCatalogTracks = catalog.tracks,
            playlists = catalog.playlists,
            recentTracks = recentTracks,
            topTracks = topTracks,
            topArtists = topArtists,
            totalPlaysCount = totalPlays,
            totalListeningMinutes = totalListeningMinutes,
            listeningStreakDays = streakDays,
            lyricsOffsetsMs = meta.listeningStats.lyricsOffsetsMs,
            trendingTracks = trendingTracks,
            communityStats = catalog.communityStats,
            userReactions = catalog.userReactions,
            latestReleaseAlbum = latestReleaseAlbum,
            recentReleaseAlbums = sortedReleaseAlbums.take(20),
            hasUnreadReleases = hasUnreadReleases,
            dailyMixTracks = dailyMixTracks,
            activeJam = catalog.activeJam,
            isJamHost = catalog.activeJam != null && catalog.activeJam.hostUserId == activeUserId,
            artistStats = catalog.artistStats,
            followedArtistIds = catalog.followedArtistIds,
            currentTrackComments = catalog.currentTrackComments,
            moderationComments = catalog.moderationComments,
            latestRelease = catalog.latestRelease,
            downloadProgress = meta.downloadProgress,
            searchQuery = meta.query,
            themeMode = meta.theme,
            syncMessage = meta.syncMessage,
            isImporting = meta.isImporting,
            isCatalogUploading = meta.isUploading,
            catalogUploadProgress = meta.uploadProgress
        )
    }

    private fun buildDailyMix(
        tracks: List<TrackUiModel>,
        playCounts: Map<String, Int>,
        seed: Long
    ): List<TrackUiModel> {
        if (tracks.isEmpty()) return emptyList()
        val rng = Random(seed)
        val favorites = tracks.filter { it.favorite }.shuffled(rng)
        val unheard = tracks.filter { !it.favorite && (playCounts[it.id] ?: 0) == 0 }.shuffled(rng)
        val others = tracks.filter { !it.favorite && (playCounts[it.id] ?: 0) > 0 }.shuffled(rng)

        val picked = mutableListOf<TrackUiModel>()
        val artistUsage = mutableMapOf<String, Int>()
        fun addDiverse(source: List<TrackUiModel>, limit: Int) {
            for (tr in source) {
                if (picked.size >= limit) break
                if (picked.any { it.id == tr.id }) continue
                val count = artistUsage[tr.artist.lowercase()] ?: 0
                if (count < 2 || tracks.size <= 8) {
                    picked.add(tr)
                    artistUsage[tr.artist.lowercase()] = count + 1
                }
            }
        }
        addDiverse(favorites, 6)
        addDiverse(unheard, 12)
        addDiverse(others, 15)
        if (picked.size < 15) {
            tracks.shuffled(rng).forEach { tr ->
                if (picked.size < 15 && picked.none { it.id == tr.id }) picked.add(tr)
            }
        }
        return picked
    }

    fun attachAuthenticatedCloud(
        userId: String,
        displayName: String,
        isAdmin: Boolean,
        repo: CloudSyncRepository,
        catalogRepo: CatalogRepository,
        communityAndJamRepo: CommunityAndJamRepository
    ) {
        if (activeUserId == userId && cloudSyncJob?.isActive == true) return
        detachAuthenticatedCloud()
        activeUserId = userId
        activeDisplayName = displayName
        isAdminSession = isAdmin
        cloudSyncRepository = repo
        catalogRepository = catalogRepo
        communityRepository = communityAndJamRepo

        cloudSyncJob = viewModelScope.launch {
            val currentTheme = uiState.value.themeMode
            repo.ensureUserProfile(userId, displayName, currentTheme)
                .onFailure { err ->
                    _syncMessage.value = "Error de sincronización: ${err.localizedMessage ?: "sin conexión"}"
                }

            launch {
                repo.observeUserProfile(userId)
                    .catch { err ->
                        _syncMessage.value = "Perfil en nube no disponible: ${err.localizedMessage ?: "error"}"
                    }
                    .collect { cloudProfile ->
                        if (cloudProfile != null) {
                            themeRepo.setThemeMode(AppThemeMode.fromKey(cloudProfile.themeMode))
                        }
                    }
            }

            launch {
                repo.observeUserTracks(userId)
                    .catch { err ->
                        _syncMessage.value = "Biblioteca en nube: ${err.localizedMessage ?: "error"}"
                    }
                    .collect { cloudTracks ->
                        val favIds = cloudTracks.filter { it.favorite }.map { it.id }.toSet()
                        _catalogSnapshot.update { it.copy(cloudFavoriteIds = favIds) }
                        cloudTracks.forEach { remote ->
                            val local = trackDao.getById(remote.id)
                            if (local != null && local.favorite != remote.favorite) {
                                trackDao.setFavorite(remote.id, remote.favorite)
                            }
                        }
                    }
            }

            launch {
                catalogRepo.observeArtists(isAdmin)
                    .catch { err ->
                        _syncMessage.value = "Artistas en nube: ${err.localizedMessage ?: "sin conexión"}"
                    }
                    .collect { list ->
                        _catalogSnapshot.update { it.copy(artists = list) }
                    }
            }

            launch {
                catalogRepo.observeAlbums(isAdmin)
                    .catch { err ->
                        _syncMessage.value = "Álbumes en nube: ${err.localizedMessage ?: "sin conexión"}"
                    }
                    .collect { list ->
                        _catalogSnapshot.update { it.copy(albums = list) }
                    }
            }

            launch {
                catalogRepo.observeTracks(isAdmin)
                    .catch { err ->
                        _syncMessage.value = "Catálogo en nube: ${err.localizedMessage ?: "sin conexión"}"
                    }
                    .collect { list ->
                        _catalogSnapshot.update { it.copy(tracks = list) }
                    }
            }

            launch {
                repo.observeUserPlaylists(userId)
                    .catch { err ->
                        _syncMessage.value = "Playlists en nube: ${err.localizedMessage ?: "sin conexión"}"
                    }
                    .collect { playlists ->
                        _catalogSnapshot.update { it.copy(playlists = playlists) }
                    }
            }

            launch {
                catalogRepo.observeAppReleaseConfig()
                    .catch { }
                    .collect { release ->
                        _catalogSnapshot.update { it.copy(latestRelease = release) }
                    }
            }

            launch {
                communityAndJamRepo.observeCommunityStats()
                    .catch { }
                    .collect { stats ->
                        _catalogSnapshot.update { it.copy(communityStats = stats) }
                    }
            }

            launch {
                communityAndJamRepo.observeUserReactions(userId)
                    .catch { }
                    .collect { reactions ->
                        _catalogSnapshot.update { it.copy(userReactions = reactions) }
                    }
            }

            launch {
                communityAndJamRepo.observeArtistStats()
                    .catch { }
                    .collect { stats ->
                        _catalogSnapshot.update { it.copy(artistStats = stats) }
                    }
            }

            launch {
                communityAndJamRepo.observeFollowedArtistIds(userId)
                    .catch { }
                    .collect { followed ->
                        _catalogSnapshot.update { it.copy(followedArtistIds = followed) }
                    }
            }

            if (isAdmin) {
                launch {
                    communityAndJamRepo.observeAllCommentsForModeration()
                        .catch { }
                        .collect { allComments ->
                            _catalogSnapshot.update { it.copy(moderationComments = allComments) }
                        }
                }
            }
        }

        observeCommentsForCurrentTrack(playbackState.value.currentTrack?.id)
    }

    private fun observeCommentsForCurrentTrack(trackId: String?) {
        trackCommentsJob?.cancel()
        trackCommentsJob = null
        val repo = communityRepository
        if (trackId.isNullOrBlank() || repo == null || activeUserId == null) {
            _catalogSnapshot.update { it.copy(currentTrackComments = emptyList()) }
            return
        }
        trackCommentsJob = viewModelScope.launch {
            repo.observeCommentsForTrack(trackId)
                .catch { }
                .collect { comments ->
                    _catalogSnapshot.update { it.copy(currentTrackComments = comments) }
                }
        }
    }

    fun detachAuthenticatedCloud() {
        jamObserverJob?.cancel()
        jamObserverJob = null
        trackCommentsJob?.cancel()
        trackCommentsJob = null
        cloudSyncJob?.cancel()
        cloudSyncJob = null
        cloudSyncRepository = null
        catalogRepository = null
        communityRepository = null
        activeUserId = null
        isAdminSession = false
        _catalogSnapshot.value = CatalogSnapshot()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            themeRepo.setThemeMode(mode)
            val uid = activeUserId
            val repo = cloudSyncRepository
            if (uid != null && repo != null) {
                repo.updateThemeMode(uid, mode)
                    .onFailure { err ->
                        _syncMessage.value = "Tema guardado localmente (${err.localizedMessage ?: "sin red"})"
                    }
            }
        }
    }

    fun playTrack(track: TrackUiModel) {
        playerController.playTrack(track, uiState.value.tracks)
    }

    fun playTracksQueue(track: TrackUiModel, queue: List<TrackUiModel>) {
        playerController.playTrack(track, queue.ifEmpty { uiState.value.tracks })
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun playNext() = playerController.playNext()
    fun playPrevious() = playerController.playPrevious()
    fun toggleShuffle() = playerController.toggleShuffle()
    fun toggleRepeat() = playerController.toggleRepeat()

    fun toggleAutoContinue() {
        val next = !playbackState.value.autoContinueEnabled
        playerController.setAutoContinueEnabled(next)
        viewModelScope.launch {
            themeRepo.setAutoContinue(next)
        }
        _syncMessage.value = if (next) {
            "Reproducción continua activada: pasará automáticamente a la siguiente canción"
        } else {
            "Reproducción continua desactivada"
        }
    }

    fun toggleMuteOnNotifications() {
        val next = !playbackState.value.muteOnNotificationsEnabled
        playerController.setMuteOnNotificationsEnabled(next)
        viewModelScope.launch {
            themeRepo.setMuteOnNotifications(next)
        }
        _syncMessage.value = if (next) {
            "Silencio en notificaciones activado: se silenciará brevemente al llegar una notificación"
        } else {
            "Música sin cortes activada: no se silenciará al llegar notificaciones"
        }
    }

    // --- Queue Controls ---
    fun playNextInQueue(track: TrackUiModel) {
        playerController.playNextInQueue(track)
        _syncMessage.value = "\"${track.title}\" sonará a continuación"
    }

    fun addToQueueEnd(track: TrackUiModel) {
        playerController.addToQueueEnd(track)
        _syncMessage.value = "\"${track.title}\" añadida a la cola"
    }

    fun removeFromQueue(index: Int) = playerController.removeFromQueue(index)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playerController.moveQueueItem(fromIndex, toIndex)
    fun playQueueIndex(index: Int) = playerController.playQueueIndex(index)

    // --- Sleep Timer ---
    fun setSleepTimerMinutes(minutes: Int) {
        playerController.setSleepTimerMinutes(minutes)
        _syncMessage.value = if (minutes > 0) {
            "Temporizador activado: se pausará en $minutes min"
        } else {
            "Temporizador desactivado"
        }
    }

    fun setSleepAfterCurrentTrack() {
        playerController.setSleepAfterCurrentTrack()
        _syncMessage.value = "La música se pausará al terminar esta canción"
    }

    fun cancelSleepTimer() {
        playerController.cancelSleepTimer()
        _syncMessage.value = "Temporizador cancelado"
    }

    // --- Equalizer & Bass Boost ---
    private fun persistEffects(state: AudioEffectsState) {
        viewModelScope.launch {
            themeRepo.saveEqualizerConfig(
                PersistedEqualizerConfig(
                    enabled = state.enabled,
                    preset = state.preset,
                    bandLevelsMb = state.bands.map { it.levelMb },
                    bassBoostStrength = state.bassBoostStrength
                )
            )
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        val updated = playerController.setEqualizerEnabled(enabled)
        persistEffects(updated)
    }

    fun selectEqualizerPreset(preset: EqualizerPreset) {
        val updated = playerController.selectEqualizerPreset(preset)
        persistEffects(updated)
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMb: Int) {
        val updated = playerController.setEqualizerBandLevel(bandIndex, levelMb)
        persistEffects(updated)
    }

    fun setBassBoostStrength(strength: Int) {
        val updated = playerController.setBassBoostStrength(strength)
        persistEffects(updated)
    }

    // --- Custom Playlists (Cloud Synced + Offline Download) ---
    fun createPlaylist(
        name: String,
        description: String = "",
        initialTrackId: String? = null,
        onDone: (Boolean) -> Unit = {}
    ) {
        val uid = activeUserId
        val repo = cloudSyncRepository
        if (uid == null || repo == null) {
            _syncMessage.value = "Inicia sesión con Google en Perfil para crear y sincronizar playlists."
            onDone(false)
            return
        }
        viewModelScope.launch {
            val initialIds = if (initialTrackId.isNullOrBlank()) emptyList() else listOf(initialTrackId)
            repo.createPlaylist(uid, name, description, initialIds)
                .onSuccess { created ->
                    _syncMessage.value = "Playlist \"${created.name}\" creada"
                    onDone(true)
                }
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo crear la playlist"
                    onDone(false)
                }
        }
    }

    fun renamePlaylist(
        playlistId: String,
        newName: String,
        newDescription: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val uid = activeUserId
        val repo = cloudSyncRepository
        if (uid == null || repo == null) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            repo.updatePlaylist(uid, playlistId, name = newName, description = newDescription)
                .onSuccess {
                    _syncMessage.value = "Playlist actualizada"
                    onDone(true)
                }
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo actualizar la playlist"
                    onDone(false)
                }
        }
    }

    fun deletePlaylist(playlistId: String, onDone: () -> Unit = {}) {
        val uid = activeUserId
        val repo = cloudSyncRepository ?: return
        if (uid == null) return
        viewModelScope.launch {
            repo.deletePlaylist(uid, playlistId)
                .onSuccess {
                    _syncMessage.value = "Playlist eliminada"
                    onDone()
                }
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo eliminar la playlist"
                }
        }
    }

    fun toggleTrackInPlaylist(playlist: UserPlaylist, track: TrackUiModel) {
        val uid = activeUserId
        val repo = cloudSyncRepository
        if (uid == null || repo == null) {
            _syncMessage.value = "Inicia sesión con Google para editar playlists."
            return
        }
        val alreadyIn = playlist.trackIds.contains(track.id)
        val nextIds = if (alreadyIn) {
            playlist.trackIds.filter { it != track.id }
        } else {
            playlist.trackIds + track.id
        }
        viewModelScope.launch {
            repo.updatePlaylist(uid, playlist.id, trackIds = nextIds)
                .onSuccess {
                    _syncMessage.value = if (alreadyIn) {
                        "Quitada de \"${playlist.name}\""
                    } else {
                        "Añadida a \"${playlist.name}\""
                    }
                }
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo actualizar la playlist"
                }
        }
    }

    fun removeTrackFromPlaylist(playlist: UserPlaylist, trackId: String) {
        val uid = activeUserId
        val repo = cloudSyncRepository ?: return
        if (uid == null) return
        val nextIds = playlist.trackIds.filter { it != trackId }
        viewModelScope.launch {
            repo.updatePlaylist(uid, playlist.id, trackIds = nextIds)
                .onSuccess {
                    _syncMessage.value = "Canción quitada de \"${playlist.name}\""
                }
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo quitar la canción"
                }
        }
    }

    fun downloadPlaylistOffline(playlist: UserPlaylist) {
        val catalogById = _catalogSnapshot.value.tracks.associateBy { it.id }
        val toDownload = playlist.trackIds.mapNotNull { catalogById[it] }
        if (toDownload.isEmpty()) {
            _syncMessage.value = "No hay canciones de la nube pendientes por descargar en esta playlist."
            return
        }
        viewModelScope.launch {
            var count = 0
            toDownload.forEach { ct ->
                val already = downloadedDao.getByTrackId(ct.id)
                if (already == null || !File(already.localAudioPath).exists()) {
                    _downloadProgress.update { it + (ct.id to 0.05f) }
                    val isFav = _catalogSnapshot.value.cloudFavoriteIds.contains(ct.id)
                    val res = offlineManager.downloadTrack(ct, isFav) { p ->
                        _downloadProgress.update { it + (ct.id to p) }
                    }
                    _downloadProgress.update { it - ct.id }
                    if (res.isSuccess) count++
                }
            }
            _syncMessage.value = if (count > 0) {
                "Se descargaron $count canciones de \"${playlist.name}\" para escuchar offline"
            } else {
                "Toda la playlist \"${playlist.name}\" ya está disponible offline"
            }
        }
    }

    fun toggleFavorite(track: TrackUiModel) {
        val nextFav = !track.favorite
        viewModelScope.launch {
            if (track.isCloudCatalog) {
                _catalogSnapshot.update { snap ->
                    val updated = if (nextFav) snap.cloudFavoriteIds + track.id else snap.cloudFavoriteIds - track.id
                    snap.copy(cloudFavoriteIds = updated)
                }
                downloadedDao.setFavorite(track.id, nextFav)
            } else {
                trackDao.setFavorite(track.id, nextFav)
            }
            val uid = activeUserId
            val repo = cloudSyncRepository
            if (uid != null && repo != null) {
                repo.setTrackFavorite(uid, track, nextFav)
                    .onFailure { err ->
                        _syncMessage.value = "Favorito guardado localmente (${err.localizedMessage ?: "sin red"})"
                    }
            }
        }
    }

    fun updateLyrics(track: TrackUiModel, newLyrics: String) {
        val cleaned = LrcParser.trimToMaxChars(newLyrics, 5000)
        viewModelScope.launch {
            persistLyricsForTrack(track = track, lyrics = cleaned, showCloudError = true)
        }
    }

    fun fetchLyricsFromLrcLib(track: TrackUiModel, forceRefresh: Boolean = false) {
        val trackId = track.id
        if (!forceRefresh) {
            if (track.lyrics.isNotBlank() || !_cachedLyrics.value[trackId].isNullOrBlank()) return
            if (attemptedAutoLyricsIds.contains(trackId)) return
        }
        if (inFlightLyricsIds.contains(trackId)) return

        attemptedAutoLyricsIds.add(trackId)
        inFlightLyricsIds.add(trackId)

        viewModelScope.launch {
            _lyricsLookupState.value = LyricsLookupUiState(
                loadingTrackId = trackId,
                statusTrackId = null,
                statusMessage = null
            )
            try {
                val realDurationSec = if (
                    playbackState.value.currentTrack?.id == trackId &&
                    playbackState.value.durationMs > 1000L
                ) {
                    playbackState.value.durationMs / 1000L
                } else {
                    track.durationSec
                }

                val result = LrcLibService.fetchLyrics(
                    title = track.title,
                    artist = track.artist,
                    album = track.albumTitle,
                    durationSec = realDurationSec
                )

                result.onSuccess { match ->
                    when {
                        match != null && match.lyrics.isNotBlank() -> {
                            persistLyricsForTrack(
                                track = track,
                                lyrics = match.lyrics,
                                showCloudError = false
                            )
                            _lyricsLookupState.value = LyricsLookupUiState(
                                loadingTrackId = null,
                                statusTrackId = trackId,
                                statusMessage = if (forceRefresh) {
                                    if (match.isSynced) "Letra sincronizada cargada desde LRCLIB"
                                    else "Letra cargada desde LRCLIB"
                                } else null
                            )
                        }
                        match?.isInstrumental == true -> {
                            _lyricsLookupState.value = LyricsLookupUiState(
                                loadingTrackId = null,
                                statusTrackId = trackId,
                                statusMessage = "Esta pista está marcada como instrumental en LRCLIB."
                            )
                        }
                        else -> {
                            _lyricsLookupState.value = LyricsLookupUiState(
                                loadingTrackId = null,
                                statusTrackId = trackId,
                                statusMessage = "No se encontró letra en LRCLIB para esta canción."
                            )
                        }
                    }
                }.onFailure { err ->
                    _lyricsLookupState.value = LyricsLookupUiState(
                        loadingTrackId = null,
                        statusTrackId = trackId,
                        statusMessage = err.localizedMessage ?: "No se pudo consultar LRCLIB."
                    )
                }
            } finally {
                inFlightLyricsIds.remove(trackId)
                if (_lyricsLookupState.value.loadingTrackId == trackId) {
                    _lyricsLookupState.update { it.copy(loadingTrackId = null) }
                }
            }
        }
    }

    private suspend fun persistLyricsForTrack(
        track: TrackUiModel,
        lyrics: String,
        showCloudError: Boolean
    ) {
        withContext(Dispatchers.IO) {
            lyricsCache.save(track.id, lyrics)
        }
        _cachedLyrics.update { current ->
            if (lyrics.isBlank()) current - track.id else current + (track.id to lyrics)
        }

        if (track.isCloudCatalog) {
            downloadedDao.updateLyrics(track.id, lyrics)
            val repo = catalogRepository
            if (isAdminSession && repo != null) {
                repo.updateCatalogTrackLyrics(track.id, lyrics)
                    .onFailure { err ->
                        if (showCloudError) {
                            _syncMessage.value = "No se pudo guardar la letra en la nube: ${err.localizedMessage ?: "error"}"
                        }
                    }
            }
            _catalogSnapshot.update { snap ->
                snap.copy(tracks = snap.tracks.map { ct ->
                    if (ct.id == track.id) ct.copy(lyrics = lyrics) else ct
                })
            }
        } else {
            trackDao.updateLyrics(track.id, lyrics)
        }
    }

    fun deleteTrack(track: TrackUiModel) {
        viewModelScope.launch {
            if (track.isCloudCatalog) {
                if (track.isDownloaded) {
                    removeOfflineDownload(track.id)
                }
                return@launch
            }
            playerController.clearIfDeleted(track.id)
            trackDao.markDeleted(track.id)
            val uid = activeUserId
            val repo = cloudSyncRepository
            if (uid != null && repo != null) {
                repo.deleteTrack(uid, track.id)
            }
            _syncMessage.value = "Eliminada: ${track.title}"
        }
    }

    fun syncDeviceAudioLibrary(silent: Boolean = false) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val existingList = trackDao.getDeviceTracksOnce()
                val existingById = existingList.associateBy { it.id }
                val scanned = DeviceMusicSyncHelper.scanMediaStoreAudio(
                    context = appContext,
                    existingById = existingById,
                    startSortOrder = trackDao.maxSortOrder()
                )
                val tracksToUpsert = if (silent) {
                    scanned.filter { existingById[it.id]?.deleted != true }
                } else {
                    scanned
                }
                if (tracksToUpsert.isNotEmpty()) {
                    trackDao.insertAll(tracksToUpsert)
                }
                val scannedIds = scanned.map { it.id }.toSet()
                existingList
                    .filter {
                        it.id.startsWith("device_") &&
                            !it.id.startsWith("device_saf_") &&
                            it.id !in scannedIds &&
                            !it.deleted
                    }
                    .forEach { missing ->
                        trackDao.markDeleted(missing.id)
                    }
                if (!silent) {
                    _syncMessage.value = if (tracksToUpsert.isNotEmpty()) {
                        "${tracksToUpsert.size} canciones sincronizadas desde tu dispositivo"
                    } else {
                        "No se encontraron canciones de audio en tu dispositivo"
                    }
                }
            } catch (_: SecurityException) {
                if (!silent) {
                    _syncMessage.value = "Concede el permiso de audio para sincronizar la música de tu dispositivo"
                }
            } catch (e: Exception) {
                if (!silent) {
                    _syncMessage.value = "No se pudo sincronizar la música: ${e.localizedMessage ?: "error"}"
                }
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun syncDeviceAudioFromFolder(treeUri: Uri) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val existingById = trackDao.getDeviceTracksOnce().associateBy { it.id }
                val scanned = DeviceMusicSyncHelper.scanFolderTreeAudio(
                    context = appContext,
                    treeUri = treeUri,
                    existingById = existingById,
                    startSortOrder = trackDao.maxSortOrder()
                )
                if (scanned.isNotEmpty()) {
                    trackDao.insertAll(scanned)
                    _syncMessage.value = "${scanned.size} canciones sincronizadas desde tu dispositivo"
                } else {
                    _syncMessage.value = "No se encontraron archivos de audio en esa carpeta"
                }
            } catch (e: Exception) {
                _syncMessage.value = "No se pudo sincronizar la carpeta: ${e.localizedMessage ?: "error"}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun showMessage(message: String) {
        _syncMessage.value = message
    }

    fun downloadTrackOffline(trackId: String) {
        val catalogTrack = _catalogSnapshot.value.tracks.firstOrNull { it.id == trackId } ?: return
        if (_downloadProgress.value.containsKey(trackId)) return

        viewModelScope.launch {
            _downloadProgress.update { it + (trackId to 0.02f) }
            val isFav = _catalogSnapshot.value.cloudFavoriteIds.contains(trackId)
            val result = offlineManager.downloadTrack(catalogTrack, isFav) { progress ->
                _downloadProgress.update { it + (trackId to progress) }
            }
            _downloadProgress.update { it - trackId }
            result.onSuccess {
                _syncMessage.value = "Descargada offline: ${catalogTrack.title}"
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "Error al descargar offline"
            }
        }
    }

    fun downloadAlbumOffline(albumId: String) {
        val albumTracks = _catalogSnapshot.value.tracks.filter { it.albumId == albumId }
        if (albumTracks.isEmpty()) return
        viewModelScope.launch {
            var count = 0
            albumTracks.forEach { ct ->
                val already = downloadedDao.getByTrackId(ct.id)
                if (already == null || !File(already.localAudioPath).exists()) {
                    _downloadProgress.update { it + (ct.id to 0.05f) }
                    val isFav = _catalogSnapshot.value.cloudFavoriteIds.contains(ct.id)
                    val res = offlineManager.downloadTrack(ct, isFav) { p ->
                        _downloadProgress.update { it + (ct.id to p) }
                    }
                    _downloadProgress.update { it - ct.id }
                    if (res.isSuccess) count++
                }
            }
            _syncMessage.value = if (count > 0) {
                "Se descargaron $count canciones del álbum para escuchar sin conexión"
            } else {
                "El álbum ya está disponible offline"
            }
        }
    }

    fun removeOfflineDownload(trackId: String) {
        viewModelScope.launch {
            offlineManager.removeDownload(trackId)
                .onSuccess {
                    _syncMessage.value = "Descarga offline eliminada"
                }
        }
    }

    fun adminCreateArtist(
        name: String,
        bio: String,
        photoUri: Uri?,
        photoUrlInput: String = "",
        onDone: (Boolean) -> Unit
    ) {
        val repo = catalogRepository
        if (repo == null) {
            _syncMessage.value = "Inicia sesión con amdvtune@gmail.com primero."
            onDone(false)
            return
        }
        viewModelScope.launch {
            _isCatalogUploading.value = true
            _catalogUploadProgress.value = 0f
            val res = repo.createArtist(name, bio, photoUri, photoUrlInput) { p ->
                _catalogUploadProgress.value = p
            }
            _isCatalogUploading.value = false
            res.onSuccess { artist ->
                _syncMessage.value = "Artista publicado en la nube: ${artist.name}"
                onDone(true)
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "Error al crear artista"
                onDone(false)
            }
        }
    }

    fun adminCreateAlbum(
        artist: CatalogArtist,
        title: String,
        releaseYear: Long,
        coverUri: Uri?,
        coverUrlInput: String = "",
        onDone: (Boolean) -> Unit
    ) {
        val repo = catalogRepository
        if (repo == null) {
            _syncMessage.value = "Inicia sesión con amdvtune@gmail.com primero."
            onDone(false)
            return
        }
        viewModelScope.launch {
            _isCatalogUploading.value = true
            _catalogUploadProgress.value = 0f
            val res = repo.createAlbum(artist, title, releaseYear, coverUri, coverUrlInput) { p ->
                _catalogUploadProgress.value = p
            }
            _isCatalogUploading.value = false
            res.onSuccess { album ->
                _syncMessage.value = "Álbum publicado en la nube: ${album.title}"
                onDone(true)
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "Error al crear álbum"
                onDone(false)
            }
        }
    }

    fun adminPublishTrack(
        artist: CatalogArtist,
        album: CatalogAlbum?,
        title: String,
        trackNumber: Long,
        lyrics: String,
        audioUri: Uri?,
        audioUrlInput: String = "",
        customCoverUri: Uri? = null,
        customCoverUrlInput: String = "",
        onDone: (Boolean) -> Unit
    ) {
        val repo = catalogRepository
        if (repo == null) {
            _syncMessage.value = "Inicia sesión con amdvtune@gmail.com primero."
            onDone(false)
            return
        }
        viewModelScope.launch {
            _isCatalogUploading.value = true
            _catalogUploadProgress.value = 0f
            val res = repo.uploadAndPublishTrack(
                artist = artist,
                album = album,
                title = title,
                trackNumber = trackNumber,
                lyrics = lyrics,
                audioUri = audioUri,
                audioUrlInput = audioUrlInput,
                customCoverUri = customCoverUri,
                customCoverUrlInput = customCoverUrlInput
            ) { p ->
                _catalogUploadProgress.value = p
            }
            _isCatalogUploading.value = false
            res.onSuccess { track ->
                val lyricsTag = if (track.lyrics.isNotBlank()) " (con letra)" else ""
                _syncMessage.value = "Canción publicada en la nube: ${track.title}$lyricsTag"
                onDone(true)
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "Error al subir canción"
                onDone(false)
            }
        }
    }

    fun scanLocalFolderTree(treeUri: Uri) {
        viewModelScope.launch {
            _folderImportState.value = FolderImportUiState(
                isScanning = true,
                statusMessage = "Escaneando carpeta de música…"
            )
            val res = LocalFolderScanner.scanFolderTree(appContext, treeUri)
            res.onSuccess { album ->
                _folderImportState.value = FolderImportUiState(
                    isScanning = false,
                    preview = album,
                    statusMessage = "Se detectaron ${album.tracks.size} canciones en \"${album.folderName}\"."
                )
            }.onFailure { err ->
                _folderImportState.value = FolderImportUiState(
                    isScanning = false,
                    errorMessage = err.localizedMessage ?: "No se pudo leer la carpeta."
                )
            }
        }
    }

    fun scanMultipleLocalAudios(uris: List<Uri>) {
        viewModelScope.launch {
            _folderImportState.value = FolderImportUiState(
                isScanning = true,
                statusMessage = "Leyendo archivos de audio…"
            )
            val res = LocalFolderScanner.scanAudioUris(appContext, uris)
            res.onSuccess { album ->
                _folderImportState.value = FolderImportUiState(
                    isScanning = false,
                    preview = album,
                    statusMessage = "Se cargaron ${album.tracks.size} canciones listas para subir."
                )
            }.onFailure { err ->
                _folderImportState.value = FolderImportUiState(
                    isScanning = false,
                    errorMessage = err.localizedMessage ?: "No se pudieron leer los audios."
                )
            }
        }
    }

    fun clearFolderPreview() {
        _folderImportState.value = FolderImportUiState()
    }

    fun adminUploadFolderAlbum(
        selectedArtistId: String?,
        artistName: String,
        artistPhotoUri: Uri?,
        albumTitle: String,
        releaseYear: Long,
        customCoverUri: Uri?,
        fallbackCoverPath: String,
        selectedTracks: List<ScannedLocalTrack>,
        onDone: (Boolean, String?) -> Unit
    ) {
        val repo = catalogRepository
        if (repo == null) {
            _syncMessage.value = "Inicia sesión con amdvtune@gmail.com primero."
            onDone(false, null)
            return
        }
        viewModelScope.launch {
            _isCatalogUploading.value = true
            _catalogUploadProgress.value = 0.03f
            val res = repo.uploadFolderAlbum(
                existingArtists = _catalogSnapshot.value.artists,
                selectedArtistId = selectedArtistId,
                artistName = artistName,
                artistPhotoUri = artistPhotoUri,
                albumTitle = albumTitle,
                releaseYear = releaseYear,
                customCoverUri = customCoverUri,
                fallbackCoverPath = fallbackCoverPath,
                tracks = selectedTracks
            ) { progress, status ->
                _catalogUploadProgress.value = progress
                _folderImportState.update { it.copy(statusMessage = status, errorMessage = null) }
            }
            _isCatalogUploading.value = false
            res.onSuccess { summary ->
                _folderImportState.value = FolderImportUiState()
                _syncMessage.value = "Álbum publicado en ${summary.artistName} (${summary.uploadedCount} canciones)"
                onDone(true, summary.artistId)
            }.onFailure { err ->
                _folderImportState.update { it.copy(errorMessage = err.localizedMessage) }
                _syncMessage.value = err.localizedMessage ?: "Error al subir el álbum"
                onDone(false, null)
            }
        }
    }

    fun adminDeleteCatalogTrack(trackId: String) {
        val repo = catalogRepository ?: return
        viewModelScope.launch {
            repo.deleteCatalogTrack(trackId).onSuccess {
                _syncMessage.value = "Canción eliminada del catálogo en la nube"
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "No se pudo eliminar"
            }
        }
    }

    fun adminDeleteCatalogAlbum(albumId: String) {
        val repo = catalogRepository ?: return
        viewModelScope.launch {
            repo.deleteCatalogAlbum(albumId).onSuccess {
                _syncMessage.value = "Álbum eliminado del catálogo en la nube"
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "No se pudo eliminar"
            }
        }
    }

    fun adminUpdateArtist(
        artist: CatalogArtist,
        newName: String,
        newBio: String,
        newPhotoUri: Uri?,
        newPhotoUrlInput: String,
        onDone: (Boolean) -> Unit
    ) {
        val repo = catalogRepository ?: run {
            onDone(false)
            return
        }
        viewModelScope.launch {
            _isCatalogUploading.value = true
            _catalogUploadProgress.value = 0.2f
            val res = repo.updateArtist(
                artist = artist,
                newName = newName,
                newBio = newBio,
                newPhotoUri = newPhotoUri,
                newPhotoUrlInput = newPhotoUrlInput,
                relatedAlbums = _catalogSnapshot.value.albums,
                relatedTracks = _catalogSnapshot.value.tracks
            ) { p -> _catalogUploadProgress.value = p }
            _isCatalogUploading.value = false
            _catalogUploadProgress.value = 0f
            res.onSuccess {
                _syncMessage.value = "Artista actualizado: ${newName.trim()}"
                onDone(true)
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "No se pudo editar el artista"
                onDone(false)
            }
        }
    }

    fun adminUpdateAlbum(
        album: CatalogAlbum,
        newArtist: CatalogArtist,
        newTitle: String,
        newReleaseYear: Long,
        newCoverUri: Uri?,
        newCoverUrlInput: String,
        onDone: (Boolean) -> Unit
    ) {
        val repo = catalogRepository ?: run {
            onDone(false)
            return
        }
        viewModelScope.launch {
            _isCatalogUploading.value = true
            _catalogUploadProgress.value = 0.2f
            val res = repo.updateAlbum(
                album = album,
                newArtist = newArtist,
                newTitle = newTitle,
                newReleaseYear = newReleaseYear,
                newCoverUri = newCoverUri,
                newCoverUrlInput = newCoverUrlInput,
                relatedTracks = _catalogSnapshot.value.tracks
            ) { p -> _catalogUploadProgress.value = p }
            _isCatalogUploading.value = false
            _catalogUploadProgress.value = 0f
            res.onSuccess {
                _syncMessage.value = "Álbum actualizado: ${newTitle.trim()}"
                onDone(true)
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "No se pudo editar el álbum"
                onDone(false)
            }
        }
    }

    fun adminUpdateTrack(
        track: CatalogTrack,
        newArtist: CatalogArtist,
        newAlbum: CatalogAlbum?,
        newTitle: String,
        newTrackNumber: Long,
        newLyrics: String,
        newCoverUri: Uri?,
        newCoverUrlInput: String,
        onDone: (Boolean) -> Unit
    ) {
        val repo = catalogRepository ?: run {
            onDone(false)
            return
        }
        viewModelScope.launch {
            _isCatalogUploading.value = true
            _catalogUploadProgress.value = 0.2f
            val res = repo.updateTrack(
                track = track,
                newArtist = newArtist,
                newAlbum = newAlbum,
                newTitle = newTitle,
                newTrackNumber = newTrackNumber,
                newLyrics = newLyrics,
                newCoverUri = newCoverUri,
                newCoverUrlInput = newCoverUrlInput
            ) { p -> _catalogUploadProgress.value = p }
            _isCatalogUploading.value = false
            _catalogUploadProgress.value = 0f
            res.onSuccess {
                _syncMessage.value = "Canción actualizada: ${newTitle.trim()}"
                onDone(true)
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "No se pudo editar la canción"
                onDone(false)
            }
        }
    }

    fun adminDeleteCatalogArtist(artistId: String) {
        val repo = catalogRepository ?: return
        viewModelScope.launch {
            repo.deleteCatalogArtist(artistId).onSuccess {
                _syncMessage.value = "Artista eliminado del catálogo en la nube"
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "No se pudo eliminar"
            }
        }
    }

    fun adminPublishAppUpdate(
        versionCode: Long,
        versionName: String,
        downloadUrl: String,
        releaseNotes: String,
        mandatory: Boolean,
        onDone: (Boolean) -> Unit
    ) {
        val repo = catalogRepository ?: return
        viewModelScope.launch {
            _isCatalogUploading.value = true
            _catalogUploadProgress.value = 0.5f
            val res = repo.publishAppReleaseConfig(
                latestVersionCode = versionCode,
                latestVersionName = versionName,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes,
                mandatory = mandatory
            )
            _isCatalogUploading.value = false
            _catalogUploadProgress.value = 0f
            res.onSuccess {
                _syncMessage.value = "Actualización v$versionName publicada. Todos los usuarios recibirán el aviso al abrir la app."
                onDone(true)
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "No se pudo publicar la actualización"
                onDone(false)
            }
        }
    }

    // --- v1.8 Lyrics Offset, Daily Mix, Reactions, Live Jam & Shared Playlists ---
    fun adjustLyricsOffset(trackId: String, deltaMs: Long) {
        val current = _listeningStats.value
        val prev = current.lyricsOffsetsMs[trackId] ?: 0L
        val next = (prev + deltaMs).coerceIn(-15_000L, 15_000L)
        val updatedMap = current.lyricsOffsetsMs.toMutableMap().apply {
            if (next == 0L) remove(trackId) else put(trackId, next)
        }
        val updated = current.copy(lyricsOffsetsMs = updatedMap)
        _listeningStats.value = updated
        viewModelScope.launch { listeningStatsRepo.save(updated) }
    }

    fun resetLyricsOffset(trackId: String) {
        val current = _listeningStats.value
        if (!current.lyricsOffsetsMs.containsKey(trackId)) return
        val updated = current.copy(lyricsOffsetsMs = current.lyricsOffsetsMs - trackId)
        _listeningStats.value = updated
        viewModelScope.launch { listeningStatsRepo.save(updated) }
    }

    fun regenerateDailyMix() {
        _dailyMixSeed.value = System.currentTimeMillis()
        _syncMessage.value = "Nuevo Mix Diario generado"
    }

    fun playDailyMix() {
        val mix = uiState.value.dailyMixTracks
        val first = mix.firstOrNull() ?: return
        playerController.playTrack(first, mix)
    }

    fun playAlbumTracks(album: CatalogAlbum) {
        val albumTracks = uiState.value.cloudTracks
            .filter { it.albumId == album.id }
            .sortedBy { it.trackNumber }
        val first = albumTracks.firstOrNull() ?: return
        playerController.playTrack(first, albumTracks)
    }

    fun toggleTrackReaction(trackId: String, reactionType: String) {
        val uid = activeUserId
        val repo = communityRepository
        if (uid == null || repo == null) {
            _syncMessage.value = "Inicia sesión con Google en Perfil para reaccionar."
            return
        }
        val current = _catalogSnapshot.value.userReactions[trackId]
        viewModelScope.launch {
            repo.toggleReaction(uid, trackId, reactionType, current)
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo guardar tu reacción"
                }
        }
    }

    fun createJamSession() {
        val uid = activeUserId
        val repo = communityRepository
        if (uid == null || repo == null) {
            _syncMessage.value = "Inicia sesión con Google para crear una sala Jam."
            return
        }
        val pb = playbackState.value
        val currentTrackId = pb.currentTrack?.id ?: uiState.value.cloudTracks.firstOrNull()?.id.orEmpty()
        val queueIds = pb.queue.filter { it.isCloudCatalog }.map { it.id }.take(30)
        viewModelScope.launch {
            repo.createJamSession(
                hostUserId = uid,
                hostName = activeDisplayName,
                currentTrackId = currentTrackId,
                playing = pb.isPlaying,
                positionMs = pb.positionMs,
                queueTrackIds = queueIds
            ).onSuccess { session ->
                lastPushedJamTrackId = session.currentTrackId
                lastPushedJamPlaying = session.playing
                lastPushedJamPosMs = session.positionMs
                startObservingJam(session.roomCode)
                _syncMessage.value = "Sala Jam ${session.roomCode} creada. Comparte el código con tus amigos."
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "No se pudo crear la sala Jam"
            }
        }
    }

    fun joinJamSession(roomCode: String) {
        val repo = communityRepository
        if (activeUserId == null || repo == null) {
            _syncMessage.value = "Inicia sesión con Google para unirte a una sala Jam."
            return
        }
        val normalized = roomCode.trim().uppercase()
        viewModelScope.launch {
            repo.fetchJamSession(normalized)
                .onSuccess { session ->
                    if (session == null || !session.active) {
                        _syncMessage.value = "No se encontró ninguna sala Jam activa con el código $normalized"
                    } else {
                        startObservingJam(session.roomCode)
                        _syncMessage.value = "Conectado al Jam de ${session.hostName} (${session.roomCode})"
                    }
                }
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo unir a la sala Jam"
                }
        }
    }

    private fun startObservingJam(roomCode: String) {
        val repo = communityRepository ?: return
        jamObserverJob?.cancel()
        jamObserverJob = viewModelScope.launch {
            repo.observeJamSession(roomCode)
                .catch { err ->
                    _syncMessage.value = "Jam desconectado: ${err.localizedMessage ?: "sin red"}"
                }
                .collect { session ->
                    if (session == null || !session.active) {
                        val wasInRoom = _catalogSnapshot.value.activeJam != null
                        _catalogSnapshot.update { it.copy(activeJam = null) }
                        if (wasInRoom) {
                            _syncMessage.value = "La sala Jam ha finalizado"
                        }
                        jamObserverJob?.cancel()
                        return@collect
                    }
                    _catalogSnapshot.update { it.copy(activeJam = session) }
                    if (session.hostUserId != activeUserId) {
                        applyRemoteJamPlayback(session)
                    }
                }
        }
    }

    private fun applyRemoteJamPlayback(session: JamSession) {
        val allById = uiState.value.tracks.associateBy { it.id }
        val targetTrack = allById[session.currentTrackId] ?: return
        val pb = playbackState.value
        val jamQueue = session.queueTrackIds.mapNotNull { allById[it] }.ifEmpty { uiState.value.cloudTracks }

        if (pb.currentTrack?.id != targetTrack.id) {
            playerController.playTrack(targetTrack, jamQueue)
            if (session.positionMs > 2500L) {
                playerController.seekTo(session.positionMs)
            }
            if (!session.playing) {
                playerController.pause()
            }
        } else {
            if (session.playing && !pb.isPlaying) {
                playerController.resumeOrPlay()
            } else if (!session.playing && pb.isPlaying) {
                playerController.pause()
            }
            if (abs(pb.positionMs - session.positionMs) > 3500L) {
                playerController.seekTo(session.positionMs)
            }
        }
    }

    fun addTrackToActiveJam(track: TrackUiModel) {
        val jam = _catalogSnapshot.value.activeJam ?: return
        val repo = communityRepository ?: return
        viewModelScope.launch {
            repo.addTrackToJamQueue(jam.roomCode, track.id)
                .onSuccess {
                    _syncMessage.value = "\"${track.title}\" añadida a la cola del Jam"
                }
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo añadir al Jam"
                }
        }
    }

    fun leaveJamSession() {
        val jam = _catalogSnapshot.value.activeJam
        val uid = activeUserId
        val repo = communityRepository
        jamObserverJob?.cancel()
        jamObserverJob = null
        _catalogSnapshot.update { it.copy(activeJam = null) }
        if (jam != null && uid != null && jam.hostUserId == uid && repo != null) {
            viewModelScope.launch {
                repo.closeJamSession(jam.roomCode)
            }
            _syncMessage.value = "Sala Jam ${jam.roomCode} finalizada"
        } else if (jam != null) {
            _syncMessage.value = "Saliste de la sala Jam"
        }
    }

    fun sharePlaylistWithCode(
        playlist: UserPlaylist,
        collaborative: Boolean,
        onCodeReady: (String?) -> Unit
    ) {
        val uid = activeUserId
        val repo = communityRepository
        if (uid == null || repo == null) {
            _syncMessage.value = "Inicia sesión con Google para compartir playlists."
            onCodeReady(null)
            return
        }
        viewModelScope.launch {
            repo.publishSharedPlaylist(uid, activeDisplayName, playlist, collaborative)
                .onSuccess { shared ->
                    _syncMessage.value = "Código generado: ${shared.shareCode}"
                    onCodeReady(shared.shareCode)
                }
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo compartir la playlist"
                    onCodeReady(null)
                }
        }
    }

    fun joinSharedPlaylistByCode(shareCode: String, onDone: (Boolean) -> Unit = {}) {
        val uid = activeUserId
        val repo = communityRepository
        val syncRepo = cloudSyncRepository
        if (uid == null || repo == null || syncRepo == null) {
            _syncMessage.value = "Inicia sesión con Google para importar playlists compartidas."
            onDone(false)
            return
        }
        val normalized = shareCode.trim().uppercase()
        viewModelScope.launch {
            repo.fetchSharedPlaylist(normalized)
                .onSuccess { shared ->
                    if (shared == null) {
                        _syncMessage.value = "No existe ninguna playlist con el código $normalized"
                        onDone(false)
                    } else {
                        val importedDesc = buildString {
                            if (shared.description.isNotBlank()) append("${shared.description} • ")
                            append("De ${shared.ownerName} (Código $normalized)")
                        }.take(200)
                        syncRepo.createPlaylist(
                            userId = uid,
                            name = shared.name,
                            description = importedDesc,
                            initialTrackIds = shared.trackIds
                        ).onSuccess {
                            _syncMessage.value = "Playlist \"${shared.name}\" importada con ${shared.trackIds.size} canciones"
                            onDone(true)
                        }.onFailure { err ->
                            _syncMessage.value = err.localizedMessage ?: "No se pudo guardar la playlist importada"
                            onDone(false)
                        }
                    }
                }
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo buscar el código"
                    onDone(false)
                }
        }
    }

    // --- v1.9: Crossfade, Artist Follows, Releases Inbox, Timed Comments ---
    fun setCrossfadeSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 12)
        playerController.setCrossfadeSeconds(clamped)
        viewModelScope.launch {
            themeRepo.setCrossfadeSeconds(clamped)
        }
    }

    fun markReleasesInboxSeen() {
        val newestMs = uiState.value.latestReleaseAlbum?.createdAt?.toDate()?.time
            ?: System.currentTimeMillis()
        val markMs = maxOf(newestMs, System.currentTimeMillis())
        viewModelScope.launch {
            themeRepo.setLastSeenReleaseMs(markMs)
        }
    }

    fun toggleFollowArtist(artist: CatalogArtist) {
        val uid = activeUserId
        val repo = communityRepository
        if (uid == null || repo == null) {
            _syncMessage.value = "Inicia sesión con Google para seguir artistas."
            return
        }
        val currentlyFollowing = _catalogSnapshot.value.followedArtistIds.contains(artist.id)
        viewModelScope.launch {
            repo.toggleFollowArtist(uid, artist.id, artist.name, currentlyFollowing)
                .onSuccess { nowFollowing ->
                    _syncMessage.value = if (nowFollowing) {
                        "Ahora sigues a ${artist.name}"
                    } else {
                        "Dejaste de seguir a ${artist.name}"
                    }
                }
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo actualizar el seguimiento"
                }
        }
    }

    fun addTrackComment(
        track: TrackUiModel,
        timestampMs: Long,
        text: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val uid = activeUserId
        val repo = communityRepository
        if (uid == null || repo == null) {
            _syncMessage.value = "Inicia sesión con Google para comentar en canciones."
            onDone(false)
            return
        }
        viewModelScope.launch {
            repo.addTrackComment(
                userId = uid,
                authorName = activeDisplayName,
                trackId = track.id,
                trackTitle = track.title,
                timestampMs = timestampMs,
                text = text
            ).onSuccess {
                _syncMessage.value = "Comentario publicado en el segundo ${formatMsShort(timestampMs)}"
                onDone(true)
            }.onFailure { err ->
                _syncMessage.value = err.localizedMessage ?: "No se pudo publicar el comentario"
                onDone(false)
            }
        }
    }

    fun deleteTrackComment(commentId: String) {
        val repo = communityRepository ?: return
        viewModelScope.launch {
            repo.deleteTrackComment(commentId)
                .onSuccess {
                    _syncMessage.value = "Comentario eliminado"
                }
                .onFailure { err ->
                    _syncMessage.value = err.localizedMessage ?: "No se pudo eliminar el comentario"
                }
        }
    }

    private fun formatMsShort(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        return "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}"
    }

    fun clearMessage() {
        _syncMessage.value = null
        playerController.clearError()
    }

    override fun onCleared() {
        super.onCleared()
        detachAuthenticatedCloud()
        if (!playerController.playbackState.value.isPlaying) {
            playerController.release()
        }
    }
}
