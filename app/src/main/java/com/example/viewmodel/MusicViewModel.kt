package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AudioPickerHelper
import com.example.data.CatalogRepository
import com.example.data.CloudSyncRepository
import com.example.data.DeviceMusicSyncHelper
import com.example.data.LocalFolderScanner
import com.example.data.LrcLibService
import com.example.data.LrcLyricsCache
import com.example.data.LrcParser
import com.example.data.MusicPlayerController
import com.example.data.OfflineDownloadManager
import com.example.data.PlaybackState
import com.example.data.ScannedFolderAlbum
import com.example.data.ScannedLocalTrack
import com.example.data.SeedTracksProvider
import com.example.data.ThemePreferencesRepository
import com.example.model.AppReleaseConfig
import com.example.model.AppThemeMode
import com.example.model.CatalogAlbum
import com.example.model.CatalogArtist
import com.example.model.CatalogTrack
import com.example.model.DownloadedTrackEntity
import com.example.model.LocalTrackEntity
import com.example.model.TrackUiModel
import com.example.model.toOfflineUiModel
import com.example.model.toUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
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
    val cloudFavoriteIds: Set<String> = emptySet(),
    val latestRelease: AppReleaseConfig? = null
)

private data class UiMeta(
    val query: String = "",
    val theme: AppThemeMode = AppThemeMode.NEGRO,
    val syncMessage: String? = null,
    val isImporting: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val downloadProgress: Map<String, Float> = emptyMap()
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

    private val _searchQuery = MutableStateFlow("")
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _isImporting = MutableStateFlow(false)
    private val _isCatalogUploading = MutableStateFlow(false)
    private val _catalogUploadProgress = MutableStateFlow(0f)
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    private val _catalogSnapshot = MutableStateFlow(CatalogSnapshot())
    private val _cachedLyrics = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _folderImportState = MutableStateFlow(FolderImportUiState())
    val folderImportState: StateFlow<FolderImportUiState> = _folderImportState.asStateFlow()

    private val _lyricsLookupState = MutableStateFlow(LyricsLookupUiState())
    val lyricsLookupState: StateFlow<LyricsLookupUiState> = _lyricsLookupState.asStateFlow()

    private val attemptedAutoLyricsIds = mutableSetOf<String>()
    private val inFlightLyricsIds = mutableSetOf<String>()

    private var cloudSyncRepository: CloudSyncRepository? = null
    private var catalogRepository: CatalogRepository? = null
    private var activeUserId: String? = null
    private var isAdminSession: Boolean = false
    private var cloudSyncJob: Job? = null

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    private val uiMetaFlow = combine(
        _searchQuery,
        themeRepo.themeModeFlow,
        _syncMessage,
        _isImporting,
        _isCatalogUploading,
        _catalogUploadProgress,
        _downloadProgress
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        UiMeta(
            query = values[0] as String,
            theme = values[1] as AppThemeMode,
            syncMessage = values[2] as String?,
            isImporting = values[3] as Boolean,
            isUploading = values[4] as Boolean,
            uploadProgress = values[5] as Float,
            downloadProgress = values[6] as Map<String, Float>
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
            playerController.playbackState
                .map { it.currentTrack }
                .distinctUntilChangedBy { it?.id }
                .collect { track ->
                    if (track != null && track.lyrics.isBlank()) {
                        fetchLyricsFromLrcLib(track, forceRefresh = false)
                    }
                }
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

    fun attachAuthenticatedCloud(
        userId: String,
        displayName: String,
        isAdmin: Boolean,
        repo: CloudSyncRepository,
        catalogRepo: CatalogRepository
    ) {
        if (activeUserId == userId && cloudSyncJob?.isActive == true) return
        detachAuthenticatedCloud()
        activeUserId = userId
        isAdminSession = isAdmin
        cloudSyncRepository = repo
        catalogRepository = catalogRepo

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
                catalogRepo.observeAppReleaseConfig()
                    .catch { }
                    .collect { release ->
                        _catalogSnapshot.update { it.copy(latestRelease = release) }
                    }
            }
        }
    }

    fun detachAuthenticatedCloud() {
        cloudSyncJob?.cancel()
        cloudSyncJob = null
        cloudSyncRepository = null
        catalogRepository = null
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
