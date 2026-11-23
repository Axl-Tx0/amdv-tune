package com.example.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState as PlatformPlaybackState
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.EXTRA_OPEN_FULL_SCREEN_PLAYER
import com.example.MainActivity
import com.example.R
import com.example.model.TrackUiModel
import com.example.widget.AmdvMusicWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MusicPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var playerController: MusicPlayerController
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSession

    private var stateCollectorJob: Job? = null
    private var artworkJob: Job? = null

    private var isInForeground = false
    private var lastNotificationTrackId: String? = null
    private var lastNotificationPlaying: Boolean? = null
    private var lastNotificationDuration: Long = -1L
    private var cachedCoverKey: String? = null
    private var cachedArtworkBitmap: Bitmap? = null

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                playerController.pause()
            }
        }
    }
    private var noisyReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        playerController = PlaybackControllerHolder.getInstance(applicationContext)
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        initMediaSession()
        registerNoisyReceiver()
        observePlaybackState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> playerController.togglePlayPause()
            ACTION_PREV -> playerController.playPrevious()
            ACTION_NEXT -> playerController.playNext()
            ACTION_STOP -> {
                playerController.pause()
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
        }

        val state = playerController.playbackState.value
        val track = state.currentTrack
        if (track == null) {
            // Cumplir el contrato de startForeground antes de detener si no hay pista activa
            val fallback = buildPlaceholderNotification()
            promoteForeground(fallback)
            stopForegroundAndSelf()
            return START_NOT_STICKY
        }

        updateSessionAndNotification(state, forceNotification = true)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!playerController.playbackState.value.isPlaying) {
            stopForegroundAndSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stateCollectorJob?.cancel()
        artworkJob?.cancel()
        serviceScope.cancel()
        if (noisyReceiverRegistered) {
            try {
                unregisterReceiver(noisyReceiver)
            } catch (_: Exception) {
            }
            noisyReceiverRegistered = false
        }
        try {
            mediaSession.isActive = false
            mediaSession.release()
        } catch (_: Exception) {
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "AMDV_TUNE_MEDIA_SESSION").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    playerController.resumeOrPlay()
                }

                override fun onPause() {
                    playerController.pause()
                }

                override fun onSkipToNext() {
                    playerController.playNext()
                }

                override fun onSkipToPrevious() {
                    playerController.playPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    playerController.seekTo(pos)
                    updateSessionPlaybackState(playerController.playbackState.value)
                }

                override fun onStop() {
                    playerController.pause()
                    stopForegroundAndSelf()
                }
            })
            isActive = true
        }
    }

    private fun registerNoisyReceiver() {
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(noisyReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(noisyReceiver, filter)
            }
            noisyReceiverRegistered = true
        } catch (_: Exception) {
        }
    }

    private fun observePlaybackState() {
        stateCollectorJob?.cancel()
        stateCollectorJob = serviceScope.launch {
            playerController.playbackState.collectLatest { state ->
                if (state.currentTrack == null) {
                    stopForegroundAndSelf()
                    return@collectLatest
                }
                updateSessionAndNotification(state, forceNotification = false)
            }
        }
    }

    private fun updateSessionAndNotification(state: PlaybackState, forceNotification: Boolean) {
        val track = state.currentTrack ?: return
        val durationMs = state.durationMs.coerceAtLeast(track.durationSec * 1000L)

        // Si cambió la portada, cargarla en IO y refrescar notificación + MediaSession al terminar
        if (cachedCoverKey != track.coverKey) {
            cachedCoverKey = track.coverKey
            cachedArtworkBitmap = createMonogramArtwork(track.title, track.coverKey)
            loadTrackArtworkAsync(track)
        }

        val activeForeground = state.isPlaying || state.isTransitioning
        val trackChanged = lastNotificationTrackId != track.id
        val playChanged = lastNotificationPlaying != activeForeground
        val durationChanged = kotlin.math.abs(durationMs - lastNotificationDuration) >= 1000L

        updateSessionPlaybackState(state)

        if (trackChanged || durationChanged || forceNotification) {
            updateSessionMetadata(track, durationMs, cachedArtworkBitmap)
        }

        if (trackChanged || playChanged || forceNotification) {
            lastNotificationTrackId = track.id
            lastNotificationPlaying = activeForeground
            lastNotificationDuration = durationMs
            postNotification(track, activeForeground, cachedArtworkBitmap)
            AmdvMusicWidgetProvider.updateAllWidgets(
                context = applicationContext,
                track = track,
                isPlaying = state.isPlaying,
                artwork = cachedArtworkBitmap
            )
        }
    }

    private fun updateSessionPlaybackState(state: PlaybackState) {
        val actions = PlatformPlaybackState.ACTION_PLAY or
            PlatformPlaybackState.ACTION_PAUSE or
            PlatformPlaybackState.ACTION_PLAY_PAUSE or
            PlatformPlaybackState.ACTION_SKIP_TO_NEXT or
            PlatformPlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlatformPlaybackState.ACTION_SEEK_TO or
            PlatformPlaybackState.ACTION_STOP

        val platformState = if (state.isPlaying) {
            PlatformPlaybackState.STATE_PLAYING
        } else {
            PlatformPlaybackState.STATE_PAUSED
        }
        val speed = if (state.isPlaying) 1.0f else 0.0f

        val pbState = PlatformPlaybackState.Builder()
            .setActions(actions)
            .setState(platformState, state.positionMs.coerceAtLeast(0L), speed)
            .build()
        mediaSession.setPlaybackState(pbState)
    }

    private fun updateSessionMetadata(track: TrackUiModel, durationMs: Long, artwork: Bitmap?) {
        val builder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, track.id)
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track.albumTitle.ifBlank { "AMDV-TUNE" })
            .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs.coerceAtLeast(0L))

        if (artwork != null) {
            builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
            builder.putBitmap(MediaMetadata.METADATA_KEY_ART, artwork)
        }
        mediaSession.setMetadata(builder.build())
    }

    private fun loadTrackArtworkAsync(track: TrackUiModel) {
        artworkJob?.cancel()
        val keyAtRequest = track.coverKey
        artworkJob = serviceScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeCoverBitmap(keyAtRequest)
            }
            if (bitmap != null && cachedCoverKey == keyAtRequest) {
                cachedArtworkBitmap = bitmap
                val current = playerController.playbackState.value
                val activeTrack = current.currentTrack
                if (activeTrack != null && activeTrack.id == track.id) {
                    val dur = current.durationMs.coerceAtLeast(activeTrack.durationSec * 1000L)
                    updateSessionMetadata(activeTrack, dur, bitmap)
                    postNotification(activeTrack, current.isPlaying, bitmap)
                    AmdvMusicWidgetProvider.updateAllWidgets(
                        context = applicationContext,
                        track = activeTrack,
                        isPlaying = current.isPlaying,
                        artwork = bitmap
                    )
                }
            }
        }
    }

    private fun decodeCoverBitmap(coverKey: String): Bitmap? {
        return try {
            when {
                coverKey == "COVER_CRIMSON" ->
                    BitmapFactory.decodeResource(resources, R.drawable.img_cover_crimson_1794973232429)
                coverKey == "COVER_SUNSET" ->
                    BitmapFactory.decodeResource(resources, R.drawable.img_cover_sunset_1794973233484)
                coverKey == "COVER_CYAN_DRIP" ->
                    BitmapFactory.decodeResource(resources, R.drawable.img_cover_cyan_drip_1794973233181)
                coverKey == "COVER_EMBER" ->
                    BitmapFactory.decodeResource(resources, R.drawable.img_cover_ember_1794973233687)
                coverKey.startsWith("/") -> {
                    val file = File(coverKey)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
                coverKey.startsWith("content://") -> {
                    contentResolver.openInputStream(Uri.parse(coverKey))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                coverKey.startsWith("http://") || coverKey.startsWith("https://") -> {
                    val conn = (URL(coverKey).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        doInput = true
                    }
                    try {
                        if (conn.responseCode in 200..299) {
                            conn.inputStream.use { BitmapFactory.decodeStream(it) }
                        } else {
                            null
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
                else -> null
            }?.let { bmp ->
                scaleBitmapMax(bmp, 512)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmapMax(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        return Bitmap.createScaledBitmap(
            src,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun createMonogramArtwork(title: String, coverKey: String): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val (startColor, endColor) = when {
            coverKey.contains("CYAN") -> 0xFF0E7490.toInt() to 0xFF0F172A.toInt()
            coverKey.contains("AMBER") -> 0xFF92400E.toInt() to 0xFF1C1917.toInt()
            coverKey.contains("EMERALD") -> 0xFF065F46.toInt() to 0xFF06120E.toInt()
            coverKey.contains("INDIGO") -> 0xFF3730A3.toInt() to 0xFF090A1A.toInt()
            else -> 0xFF7E22CE.toInt() to 0xFF1E3A8A.toInt()
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), startColor, endColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        val letter = title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "A"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 112f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val y = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(letter, size / 2f, y, textPaint)
        return bmp
    }

    private fun postNotification(track: TrackUiModel, isPlaying: Boolean, artwork: Bitmap?) {
        val notification = buildMediaNotification(track, isPlaying, artwork)
        if (isPlaying) {
            promoteForeground(notification)
        } else {
            if (isInForeground) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                isInForeground = false
            }
            if (hasNotificationPermission()) {
                try {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to update paused notification", e)
                }
            }
        }
    }

    private fun promoteForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isInForeground = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground media service", e)
        }
    }

    private fun stopForegroundAndSelf() {
        if (isInForeground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            isInForeground = false
        }
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
        }
        stopSelf()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun buildMediaNotification(
        track: TrackUiModel,
        isPlaying: Boolean,
        artwork: Bitmap?
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_FULL_SCREEN_PLAYER, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notif_prev),
            "Anterior",
            servicePendingIntent(ACTION_PREV, 101)
        ).build()

        val playPauseAction = Notification.Action.Builder(
            Icon.createWithResource(
                this,
                if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
            ),
            if (isPlaying) "Pausar" else "Reproducir",
            servicePendingIntent(ACTION_PLAY_PAUSE, 102)
        ).build()

        val nextAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notif_next),
            "Siguiente",
            servicePendingIntent(ACTION_NEXT, 103)
        ).build()

        val deletePendingIntent = servicePendingIntent(ACTION_STOP, 104)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val subtitle = if (track.albumTitle.isNotBlank()) {
            "${track.artist} • ${track.albumTitle}"
        } else {
            track.artist
        }

        return builder
            .setSmallIcon(R.drawable.ic_notification_music)
            .setLargeIcon(artwork)
            .setContentTitle(track.title)
            .setContentText(subtitle)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun buildPlaceholderNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentTitle("AMDV-TUNE")
            .setContentText("Preparando reproductor…")
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción de música",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción en barra de notificaciones y pantalla de bloqueo"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "MusicPlaybackService"
        private const val CHANNEL_ID = "amdv_playback_channel"
        private const val NOTIFICATION_ID = 4107

        const val ACTION_SYNC = "com.example.action.SYNC_PLAYBACK"
        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_STOP = "com.example.action.STOP"

        fun sync(context: Context, isPlaying: Boolean) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, MusicPlaybackService::class.java).apply {
                action = ACTION_SYNC
            }
            try {
                if (isPlaying) {
                    ContextCompat.startForegroundService(appCtx, intent)
                } else {
                    appCtx.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to sync MusicPlaybackService", e)
            }
        }

        fun stop(context: Context) {
            val appCtx = context.applicationContext
            try {
                appCtx.stopService(Intent(appCtx, MusicPlaybackService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
