package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import com.example.model.AudioEffectsState
import com.example.model.EqualizerPreset
import com.example.model.TrackUiModel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackState(
    val currentTrack: TrackUiModel? = null,
    val isPlaying: Boolean = false,
    val isTransitioning: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatOneEnabled: Boolean = false,
    val autoContinueEnabled: Boolean = true,
    val muteOnNotificationsEnabled: Boolean = false,
    val isSilencedForNotification: Boolean = false,
    val queue: List<TrackUiModel> = emptyList(),
    val currentQueueIndex: Int = -1,
    val sleepTimerRemainingMs: Long? = null,
    val sleepAfterCurrentTrack: Boolean = false,
    val crossfadeSeconds: Int = 0,
    val audioEffects: AudioEffectsState = AudioEffectsState(),
    val errorMessage: String? = null
)

object PlaybackControllerHolder {
    @Volatile
    private var instance: MusicPlayerController? = null

    fun getInstance(context: Context): MusicPlayerController {
        return instance ?: synchronized(this) {
            instance ?: MusicPlayerController(context.applicationContext).also { instance = it }
        }
    }
}

class MusicPlayerController(private val context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private var mediaPlayer: MediaPlayer? = null
    private var fadingOutPlayer: MediaPlayer? = null
    private var crossfadeJob: Job? = null
    private var crossfadeTriggeredForTrackId: String? = null
    private var tickerJob: Job? = null
    private var focusRestoreJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var transitionWakeLock: PowerManager.WakeLock? = null
    private val audioEffectsManager = AudioEffectsManager()

    private var catalogQueue: List<TrackUiModel> = emptyList()
    private var activeQueue: List<TrackUiModel> = emptyList()
    private val recentlyPlayedIds = LinkedHashSet<String>()
    private var consecutiveAutoAdvanceErrors = 0

    // AudioFocus & Notification Silence state
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var resumeOnFocusGain = false
    private var mutedByNotificationFocus = false

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        scope.launch {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    if (_playbackState.value.muteOnNotificationsEnabled) {
                        mutedByNotificationFocus = true
                        applyEffectiveVolume()
                        scheduleFocusSilenceFallback()
                    } else {
                        // Mantener el volumen completo sin cortar ni bajar la música al llegar notificaciones
                        mutedByNotificationFocus = false
                        applyEffectiveVolume()
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    val wasPlaying = _playbackState.value.isPlaying || (mediaPlayer?.isPlaying == true)
                    resumeOnFocusGain = wasPlaying
                    if (wasPlaying) {
                        pauseInternal(abandonFocus = false, markSilenced = _playbackState.value.muteOnNotificationsEnabled)
                    }
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    focusRestoreJob?.cancel()
                    focusRestoreJob = null
                    mutedByNotificationFocus = false
                    applyEffectiveVolume()
                    if (resumeOnFocusGain) {
                        resumeOnFocusGain = false
                        resumeOrPlay()
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    focusRestoreJob?.cancel()
                    focusRestoreJob = null
                    resumeOnFocusGain = false
                    mutedByNotificationFocus = false
                    hasAudioFocus = false
                    pauseInternal(abandonFocus = true, markSilenced = false)
                }
            }
        }
    }

    fun updateCatalog(tracks: List<TrackUiModel>) {
        catalogQueue = tracks
        val byId = tracks.associateBy { it.id }

        if (activeQueue.isEmpty()) {
            activeQueue = tracks
        } else {
            // Preserve the user's current Album / Artist / Playlist queue order while refreshing metadata
            val refreshed = activeQueue.mapNotNull { existing -> byId[existing.id] ?: existing }
            activeQueue = refreshed
        }

        val active = _playbackState.value.currentTrack
        val updatedActive = active?.let { byId[it.id] ?: it }
        val resolvedIdx = updatedActive?.let { cur -> activeQueue.indexOfFirst { it.id == cur.id } } ?: -1
        _playbackState.update {
            it.copy(
                currentTrack = updatedActive ?: it.currentTrack,
                queue = activeQueue,
                currentQueueIndex = resolvedIdx
            )
        }
    }

    fun playTrack(track: TrackUiModel, queue: List<TrackUiModel>) {
        activeQueue = queue.ifEmpty { catalogQueue.ifEmpty { listOf(track) } }
        if (activeQueue.none { it.id == track.id }) {
            activeQueue = listOf(track) + activeQueue
        }
        syncQueueState(track)
        consecutiveAutoAdvanceErrors = 0
        if (_playbackState.value.currentTrack?.id == track.id && mediaPlayer != null) {
            togglePlayPause()
            return
        }
        startMediaPlayer(track, autoAdvance = false)
    }

    private fun syncQueueState(activeTrack: TrackUiModel? = _playbackState.value.currentTrack) {
        val idx = activeTrack?.let { cur -> activeQueue.indexOfFirst { it.id == cur.id } } ?: -1
        _playbackState.update {
            it.copy(
                queue = activeQueue.toList(),
                currentQueueIndex = idx
            )
        }
    }

    fun playNextInQueue(track: TrackUiModel) {
        val current = _playbackState.value.currentTrack
        if (current == null) {
            playTrack(track, listOf(track))
            return
        }
        if (current.id == track.id) return
        val mutable = activeQueue.toMutableList()
        mutable.removeAll { it.id == track.id }
        val currentIdx = mutable.indexOfFirst { it.id == current.id }
        val insertIdx = if (currentIdx >= 0) (currentIdx + 1).coerceAtMost(mutable.size) else 0
        mutable.add(insertIdx, track)
        activeQueue = mutable
        syncQueueState(current)
    }

    fun addToQueueEnd(track: TrackUiModel) {
        val current = _playbackState.value.currentTrack
        if (current == null) {
            playTrack(track, listOf(track))
            return
        }
        val mutable = activeQueue.toMutableList()
        if (mutable.none { it.id == track.id }) {
            mutable.add(track)
            activeQueue = mutable
            syncQueueState(current)
        }
    }

    fun removeFromQueue(index: Int) {
        if (index !in activeQueue.indices) return
        val target = activeQueue[index]
        if (target.id == _playbackState.value.currentTrack?.id) return
        val mutable = activeQueue.toMutableList()
        mutable.removeAt(index)
        activeQueue = mutable
        syncQueueState()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in activeQueue.indices || toIndex !in activeQueue.indices || fromIndex == toIndex) return
        val mutable = activeQueue.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        activeQueue = mutable
        syncQueueState()
    }

    fun playQueueIndex(index: Int) {
        if (index !in activeQueue.indices) return
        val target = activeQueue[index]
        consecutiveAutoAdvanceErrors = 0
        startMediaPlayer(target, autoAdvance = false)
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        if (minutes <= 0) {
            _playbackState.update { it.copy(sleepTimerRemainingMs = null, sleepAfterCurrentTrack = false) }
            return
        }
        val totalMs = minutes * 60_000L
        _playbackState.update {
            it.copy(
                sleepTimerRemainingMs = totalMs,
                sleepAfterCurrentTrack = false
            )
        }
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0L) {
                delay(1000L)
                remaining = (remaining - 1000L).coerceAtLeast(0L)
                _playbackState.update { it.copy(sleepTimerRemainingMs = if (remaining > 0L) remaining else null) }
            }
            if (isActive) {
                pause()
                _playbackState.update {
                    it.copy(
                        sleepTimerRemainingMs = null,
                        sleepAfterCurrentTrack = false
                    )
                }
            }
        }
    }

    fun setSleepAfterCurrentTrack() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _playbackState.update {
            it.copy(
                sleepTimerRemainingMs = null,
                sleepAfterCurrentTrack = true
            )
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _playbackState.update {
            it.copy(
                sleepTimerRemainingMs = null,
                sleepAfterCurrentTrack = false
            )
        }
    }

    fun applyPersistedAudioEffects(
        enabled: Boolean,
        preset: EqualizerPreset,
        bandLevelsMb: List<Int>,
        bassBoostStrength: Int
    ) {
        val next = audioEffectsManager.applyConfig(enabled, preset, bandLevelsMb, bassBoostStrength)
        _playbackState.update { it.copy(audioEffects = next) }
    }

    fun setEqualizerEnabled(enabled: Boolean): AudioEffectsState {
        val next = audioEffectsManager.setEnabled(enabled)
        _playbackState.update { it.copy(audioEffects = next) }
        return next
    }

    fun selectEqualizerPreset(preset: EqualizerPreset): AudioEffectsState {
        val next = audioEffectsManager.selectPreset(preset)
        _playbackState.update { it.copy(audioEffects = next) }
        return next
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMb: Int): AudioEffectsState {
        val next = audioEffectsManager.setBandLevel(bandIndex, levelMb)
        _playbackState.update { it.copy(audioEffects = next) }
        return next
    }

    fun setBassBoostStrength(strength: Int): AudioEffectsState {
        val next = audioEffectsManager.setBassBoost(strength)
        _playbackState.update { it.copy(audioEffects = next) }
        return next
    }

    private fun startMediaPlayer(
        track: TrackUiModel,
        autoAdvance: Boolean,
        crossfadeMs: Long = 0L
    ) {
        acquireTransitionWakeLock()
        val outgoingPlayer = if (crossfadeMs > 0L && mediaPlayer != null) {
            cancelFadingOutPlayer()
            stopTicker()
            audioEffectsManager.release()
            val prev = mediaPlayer
            mediaPlayer = null
            prev
        } else {
            releasePlayerOnly()
            null
        }
        crossfadeTriggeredForTrackId = null
        recentlyPlayedIds.add(track.id)
        if (recentlyPlayedIds.size > activeQueue.size.coerceAtLeast(1)) {
            recentlyPlayedIds.clear()
            recentlyPlayedIds.add(track.id)
        }

        _playbackState.update {
            it.copy(
                currentTrack = track,
                isPlaying = false,
                isTransitioning = true,
                positionMs = 0L,
                durationMs = track.durationSec * 1000L,
                errorMessage = null
            )
        }

        val player = MediaPlayer().apply {
            setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }

        try {
            val offlinePath = track.localAudioPath
            val offlineFile = if (!offlinePath.isNullOrBlank()) File(offlinePath) else null
            when {
                offlineFile != null && offlineFile.exists() -> {
                    player.setDataSource(offlineFile.absolutePath)
                }
                track.sourceType == "BUNDLED" -> {
                    val resId = appContext.resources.getIdentifier(
                        track.audioUri,
                        "raw",
                        appContext.packageName
                    )
                    if (resId == 0) {
                        throw IllegalStateException("Recurso de audio no encontrado: ${track.audioUri}")
                    }
                    val afd = appContext.resources.openRawResourceFd(resId)
                    afd.use {
                        player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    }
                }
                else -> {
                    player.setDataSource(appContext, Uri.parse(track.audioUri))
                }
            }

            player.setOnPreparedListener { mp ->
                if (mediaPlayer !== mp) return@setOnPreparedListener
                consecutiveAutoAdvanceErrors = 0
                requestAudioFocusIfNeeded()
                val fxState = runCatching { audioEffectsManager.attachToSession(mp.audioSessionId) }
                    .getOrElse { audioEffectsManager.getState() }

                if (outgoingPlayer != null && crossfadeMs > 0L && !mutedByNotificationFocus) {
                    fadingOutPlayer = outgoingPlayer
                    runCatching { mp.setVolume(0.02f, 0.02f) }
                    mp.start()
                    runCrossfadeRamp(outgoingPlayer, mp, crossfadeMs)
                } else {
                    if (outgoingPlayer != null) {
                        runCatching { outgoingPlayer.stop() }
                        runCatching { outgoingPlayer.release() }
                    }
                    applyEffectiveVolumeTo(mp)
                    mp.start()
                }

                val realDuration = mp.duration.toLong().coerceAtLeast(0L)
                val queueIdx = activeQueue.indexOfFirst { it.id == track.id }
                _playbackState.update {
                    it.copy(
                        isPlaying = true,
                        isTransitioning = false,
                        durationMs = if (realDuration > 0) realDuration else track.durationSec * 1000L,
                        positionMs = 0L,
                        queue = activeQueue.toList(),
                        currentQueueIndex = queueIdx,
                        audioEffects = fxState,
                        errorMessage = null
                    )
                }
                releaseTransitionWakeLock()
                MusicPlaybackService.sync(appContext, isPlaying = true)
                startProgressTicker()
            }

            player.setOnCompletionListener { mp ->
                if (mediaPlayer !== mp) return@setOnCompletionListener
                handleTrackCompleted(mp)
            }

            player.setOnErrorListener { mp, _, _ ->
                if (mediaPlayer !== mp) return@setOnErrorListener true
                releaseTransitionWakeLock()
                stopTicker()
                if (autoAdvance && consecutiveAutoAdvanceErrors < 2 && resolveAllPlayableTracks().size > 1) {
                    consecutiveAutoAdvanceErrors++
                    playNext(autoAdvance = true)
                } else {
                    consecutiveAutoAdvanceErrors = 0
                    _playbackState.update {
                        it.copy(
                            isPlaying = false,
                            isTransitioning = false,
                            errorMessage = "No se pudo reproducir este archivo de audio."
                        )
                    }
                    MusicPlaybackService.sync(appContext, isPlaying = false)
                }
                true
            }

            mediaPlayer = player
            MusicPlaybackService.sync(appContext, isPlaying = true)
            player.prepareAsync()
        } catch (e: Exception) {
            releaseTransitionWakeLock()
            if (outgoingPlayer != null) {
                runCatching { outgoingPlayer.stop() }
                runCatching { outgoingPlayer.release() }
            }
            player.release()
            if (autoAdvance && consecutiveAutoAdvanceErrors < 2 && resolveAllPlayableTracks().size > 1) {
                consecutiveAutoAdvanceErrors++
                playNext(autoAdvance = true)
            } else {
                consecutiveAutoAdvanceErrors = 0
                _playbackState.update {
                    it.copy(
                        isPlaying = false,
                        isTransitioning = false,
                        errorMessage = "Audio no disponible: ${e.localizedMessage ?: "error de lectura"}"
                    )
                }
            }
        }
    }

    private fun runCrossfadeRamp(outgoing: MediaPlayer, incoming: MediaPlayer, durationMs: Long) {
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            val steps = (durationMs / 50L).coerceIn(10L, 240L).toInt()
            val stepDelay = (durationMs / steps).coerceAtLeast(30L)
            for (step in 1..steps) {
                if (!isActive || mediaPlayer !== incoming) break
                val fraction = step.toFloat() / steps.toFloat()
                val master = if (mutedByNotificationFocus) 0f else 1f
                runCatching { outgoing.setVolume((1f - fraction) * master, (1f - fraction) * master) }
                runCatching { incoming.setVolume(fraction * master, fraction * master) }
                delay(stepDelay)
            }
            runCatching { outgoing.stop() }
            runCatching { outgoing.release() }
            if (fadingOutPlayer === outgoing) {
                fadingOutPlayer = null
            }
            if (mediaPlayer === incoming) {
                applyEffectiveVolumeTo(incoming)
            }
        }
    }

    private fun cancelFadingOutPlayer() {
        crossfadeJob?.cancel()
        crossfadeJob = null
        fadingOutPlayer?.let { old ->
            runCatching { old.stop() }
            runCatching { old.release() }
        }
        fadingOutPlayer = null
    }

    private fun handleTrackCompleted(mp: MediaPlayer) {
        val state = _playbackState.value
        if (state.sleepAfterCurrentTrack) {
            stopTicker()
            abandonAudioFocus()
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    isTransitioning = false,
                    positionMs = it.durationMs,
                    sleepAfterCurrentTrack = false
                )
            }
            MusicPlaybackService.sync(appContext, isPlaying = false)
            return
        }
        if (state.repeatOneEnabled) {
            try {
                mp.seekTo(0)
                applyEffectiveVolumeTo(mp)
                mp.start()
                _playbackState.update { it.copy(isPlaying = true, positionMs = 0L) }
                MusicPlaybackService.sync(appContext, isPlaying = true)
                startProgressTicker()
            } catch (_: Exception) {
                state.currentTrack?.let { startMediaPlayer(it, autoAdvance = true) }
            }
        } else if (state.autoContinueEnabled) {
            playNext(autoAdvance = true)
        } else {
            stopTicker()
            _playbackState.update {
                it.copy(isPlaying = false, isTransitioning = false, positionMs = it.durationMs)
            }
            MusicPlaybackService.sync(appContext, isPlaying = false)
        }
    }

    fun pause() {
        resumeOnFocusGain = false
        pauseInternal(abandonFocus = true, markSilenced = false)
    }

    private fun pauseInternal(abandonFocus: Boolean, markSilenced: Boolean) {
        cancelFadingOutPlayer()
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            }
            stopTicker()
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    isTransitioning = false,
                    isSilencedForNotification = markSilenced,
                    positionMs = runCatching { player.currentPosition.toLong() }.getOrDefault(it.positionMs)
                )
            }
            if (abandonFocus) {
                abandonAudioFocus()
            }
            MusicPlaybackService.sync(appContext, isPlaying = false)
        } catch (_: Exception) {
        }
    }

    fun resumeOrPlay() {
        val player = mediaPlayer
        val active = _playbackState.value.currentTrack ?: return
        if (player == null) {
            startMediaPlayer(active, autoAdvance = false)
            return
        }
        try {
            requestAudioFocusIfNeeded()
            mutedByNotificationFocus = false
            applyEffectiveVolumeTo(player)
            if (!player.isPlaying) {
                player.start()
                _playbackState.update {
                    it.copy(
                        isPlaying = true,
                        isTransitioning = false,
                        isSilencedForNotification = false,
                        errorMessage = null
                    )
                }
                MusicPlaybackService.sync(appContext, isPlaying = true)
                startProgressTicker()
            }
        } catch (_: Exception) {
            startMediaPlayer(active, autoAdvance = false)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        val active = _playbackState.value.currentTrack ?: return
        if (player == null) {
            startMediaPlayer(active, autoAdvance = false)
            return
        }
        try {
            if (player.isPlaying) {
                pause()
            } else {
                resumeOrPlay()
            }
        } catch (_: Exception) {
            startMediaPlayer(active, autoAdvance = false)
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        try {
            val target = positionMs.coerceIn(0L, _playbackState.value.durationMs.coerceAtLeast(1L))
            player.seekTo(target.toInt())
            _playbackState.update { it.copy(positionMs = target) }
        } catch (_: Exception) {
        }
    }

    fun playNext(autoAdvance: Boolean = false, crossfadeMs: Long = 0L) {
        val queue = resolvePlayableQueueForNext()
        if (queue.isEmpty()) return
        val current = _playbackState.value.currentTrack

        val nextTrack = if (_playbackState.value.shuffleEnabled && queue.size > 1) {
            val unplayed = queue.filter { it.id != current?.id && !recentlyPlayedIds.contains(it.id) }
            val candidates = unplayed.ifEmpty { queue.filter { it.id != current?.id } }
            candidates.random()
        } else {
            val index = queue.indexOfFirst { it.id == current?.id }
            when {
                index == -1 -> queue.first()
                index + 1 < queue.size -> queue[index + 1]
                else -> {
                    // Finished current album/playlist queue -> continue seamlessly into remaining catalog tracks if any
                    val activeIds = queue.map { it.id }.toSet()
                    val continuation = catalogQueue.filter { !activeIds.contains(it.id) }
                    if (continuation.isNotEmpty()) {
                        activeQueue = queue + continuation
                        continuation.first()
                    } else {
                        queue.first()
                    }
                }
            }
        }

        if (!autoAdvance) {
            consecutiveAutoAdvanceErrors = 0
        }
        startMediaPlayer(nextTrack, autoAdvance = autoAdvance, crossfadeMs = crossfadeMs)
    }

    private fun resolvePlayableQueueForNext(): List<TrackUiModel> {
        return when {
            activeQueue.size > 1 -> activeQueue
            catalogQueue.size > 1 -> {
                activeQueue = catalogQueue
                catalogQueue
            }
            activeQueue.isNotEmpty() -> activeQueue
            else -> catalogQueue
        }
    }

    private fun resolveAllPlayableTracks(): List<TrackUiModel> {
        return if (catalogQueue.size >= activeQueue.size) catalogQueue else activeQueue
    }

    fun playPrevious() {
        val queue = resolvePlayableQueueForNext()
        if (queue.isEmpty()) return
        if (_playbackState.value.positionMs > 3000L) {
            seekTo(0L)
            return
        }
        val current = _playbackState.value.currentTrack
        val index = queue.indexOfFirst { it.id == current?.id }
        val prevTrack = if (index <= 0) queue.last() else queue[index - 1]
        consecutiveAutoAdvanceErrors = 0
        startMediaPlayer(prevTrack, autoAdvance = false)
    }

    fun toggleShuffle() {
        _playbackState.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    fun toggleRepeat() {
        _playbackState.update { it.copy(repeatOneEnabled = !it.repeatOneEnabled) }
    }

    fun setAutoContinueEnabled(enabled: Boolean) {
        _playbackState.update { it.copy(autoContinueEnabled = enabled) }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        _playbackState.update { it.copy(crossfadeSeconds = seconds.coerceIn(0, 12)) }
    }

    fun setMuteOnNotificationsEnabled(enabled: Boolean) {
        _playbackState.update { it.copy(muteOnNotificationsEnabled = enabled) }
        if (!enabled) {
            focusRestoreJob?.cancel()
            focusRestoreJob = null
            mutedByNotificationFocus = false
            applyEffectiveVolume()
        }
    }

    private fun scheduleFocusSilenceFallback() {
        focusRestoreJob?.cancel()
        focusRestoreJob = scope.launch {
            delay(4_500L)
            if (mutedByNotificationFocus) {
                mutedByNotificationFocus = false
                applyEffectiveVolume()
            }
        }
    }

    private fun applyEffectiveVolume() {
        val player = mediaPlayer
        if (player != null) {
            applyEffectiveVolumeTo(player)
        } else {
            _playbackState.update { it.copy(isSilencedForNotification = mutedByNotificationFocus) }
        }
    }

    private fun applyEffectiveVolumeTo(player: MediaPlayer) {
        val silenced = mutedByNotificationFocus
        val vol = if (silenced) 0f else 1f
        try {
            player.setVolume(vol, vol)
        } catch (_: Exception) {
        }
        _playbackState.update { it.copy(isSilencedForNotification = silenced) }
    }

    private fun requestAudioFocusIfNeeded() {
        if (hasAudioFocus) return
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(true)
                // Evita que Android baje/corte el volumen automáticamente en notificaciones;
                // nuestro listener mantiene volumen 1.0 salvo que el usuario active "Silenciar notif."
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus && audioFocusRequest == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusListener)
            }
        } catch (_: Exception) {
        }
        hasAudioFocus = false
    }

    private fun acquireTransitionWakeLock() {
        try {
            if (transitionWakeLock == null) {
                transitionWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "AMDV_TUNE::TrackTransition"
                ).apply { setReferenceCounted(false) }
            }
            transitionWakeLock?.acquire(15_000L)
        } catch (_: Exception) {
        }
    }

    private fun releaseTransitionWakeLock() {
        try {
            if (transitionWakeLock?.isHeld == true) {
                transitionWakeLock?.release()
            }
        } catch (_: Exception) {
        }
    }

    fun clearIfDeleted(trackId: String) {
        if (_playbackState.value.currentTrack?.id == trackId) {
            releasePlayerOnly()
            abandonAudioFocus()
            _playbackState.update {
                PlaybackState(
                    shuffleEnabled = it.shuffleEnabled,
                    repeatOneEnabled = it.repeatOneEnabled,
                    autoContinueEnabled = it.autoContinueEnabled,
                    muteOnNotificationsEnabled = it.muteOnNotificationsEnabled
                )
            }
            MusicPlaybackService.stop(appContext)
        }
    }

    fun clearError() {
        _playbackState.update { it.copy(errorMessage = null) }
    }

    private fun startProgressTicker() {
        stopTicker()
        tickerJob = scope.launch {
            while (isActive) {
                val player = mediaPlayer
                if (player != null) {
                    val playing = runCatching { player.isPlaying }.getOrDefault(false)
                    if (playing) {
                        val pos = runCatching { player.currentPosition.toLong() }.getOrDefault(0L).coerceAtLeast(0L)
                        val dur = runCatching { player.duration.toLong() }
                            .getOrDefault(_playbackState.value.durationMs)
                            .coerceAtLeast(_playbackState.value.durationMs)
                        _playbackState.update { it.copy(positionMs = pos, durationMs = dur) }

                        val st = _playbackState.value
                        val cfMs = st.crossfadeSeconds * 1000L
                        val curTrackId = st.currentTrack?.id
                        if (cfMs > 0L &&
                            curTrackId != null &&
                            crossfadeTriggeredForTrackId != curTrackId &&
                            st.autoContinueEnabled &&
                            !st.repeatOneEnabled &&
                            !st.sleepAfterCurrentTrack &&
                            dur > cfMs + 3_000L &&
                            (dur - pos) in 400L..cfMs &&
                            resolvePlayableQueueForNext().size > 1
                        ) {
                            crossfadeTriggeredForTrackId = curTrackId
                            playNext(autoAdvance = true, crossfadeMs = (dur - pos).coerceIn(1_000L, cfMs))
                            break
                        }
                    } else if (_playbackState.value.isPlaying && !_playbackState.value.isTransitioning) {
                        // Safeguard for HTTP streams that hit EOF without firing OnCompletionListener
                        val pos = runCatching { player.currentPosition.toLong() }.getOrDefault(0L)
                        val dur = _playbackState.value.durationMs
                        if (dur > 3_000L && pos >= dur - 650L) {
                            handleTrackCompleted(player)
                            break
                        }
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun releasePlayerOnly() {
        cancelFadingOutPlayer()
        stopTicker()
        audioEffectsManager.release()
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    fun release() {
        focusRestoreJob?.cancel()
        sleepTimerJob?.cancel()
        releaseTransitionWakeLock()
        releasePlayerOnly()
        abandonAudioFocus()
    }
}
