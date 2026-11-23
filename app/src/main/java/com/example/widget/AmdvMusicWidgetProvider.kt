package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.widget.RemoteViews
import com.example.EXTRA_OPEN_FULL_SCREEN_PLAYER
import com.example.MainActivity
import com.example.R
import com.example.data.MusicPlaybackService
import com.example.data.PlaybackControllerHolder
import com.example.model.TrackUiModel

class AmdvMusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val state = PlaybackControllerHolder.getInstance(context).playbackState.value
        val views = buildRemoteViews(
            context = context,
            title = state.currentTrack?.title,
            artist = state.currentTrack?.artist,
            isPlaying = state.isPlaying,
            artwork = lastArtwork
        )
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val controller = PlaybackControllerHolder.getInstance(context)
        val hasTrack = controller.playbackState.value.currentTrack != null
        when (action) {
            ACTION_PLAY_PAUSE -> {
                if (hasTrack) {
                    controller.togglePlayPause()
                } else {
                    launchMainApp(context)
                }
            }
            ACTION_PREV -> {
                if (hasTrack) {
                    controller.playPrevious()
                } else {
                    launchMainApp(context)
                }
            }
            ACTION_NEXT -> {
                if (hasTrack) {
                    controller.playNext()
                } else {
                    launchMainApp(context)
                }
            }
        }
    }

    private fun launchMainApp(context: Context) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        runCatching { context.startActivity(launchIntent) }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.widget.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.example.widget.ACTION_PREV"
        const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"

        private const val PREFS_NAME = "amdv_widget_cache"
        private const val KEY_TITLE = "last_title"
        private const val KEY_ARTIST = "last_artist"

        @Volatile
        private var lastArtwork: Bitmap? = null

        fun updateAllWidgets(
            context: Context,
            track: TrackUiModel?,
            isPlaying: Boolean,
            artwork: Bitmap? = null
        ) {
            val appCtx = context.applicationContext
            if (artwork != null) {
                lastArtwork = createRoundedArtwork(artwork, 160, 26f)
            } else if (track == null) {
                lastArtwork = null
            }

            if (track != null) {
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putString(KEY_TITLE, track.title)
                    .putString(KEY_ARTIST, track.artist)
                    .apply()
            }

            val manager = AppWidgetManager.getInstance(appCtx) ?: return
            val component = ComponentName(appCtx, AmdvMusicWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids == null || ids.isEmpty()) return

            val views = buildRemoteViews(
                context = appCtx,
                title = track?.title,
                artist = track?.artist,
                isPlaying = isPlaying,
                artwork = lastArtwork
            )
            ids.forEach { widgetId ->
                runCatching { manager.updateAppWidget(widgetId, views) }
            }
        }

        private fun buildRemoteViews(
            context: Context,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            artwork: Bitmap?
        ): RemoteViews {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val resolvedTitle = title?.takeIf { it.isNotBlank() }
                ?: prefs.getString(KEY_TITLE, null)
                ?: context.getString(R.string.widget_idle_title)
            val resolvedArtist = artist?.takeIf { it.isNotBlank() }
                ?: prefs.getString(KEY_ARTIST, null)
                ?: context.getString(R.string.widget_idle_subtitle)

            val views = RemoteViews(context.packageName, R.layout.widget_amdv_player)
            views.setTextViewText(R.id.widget_title, resolvedTitle)
            views.setTextViewText(R.id.widget_artist, resolvedArtist)
            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
            )
            if (artwork != null) {
                views.setImageViewBitmap(R.id.widget_cover, artwork)
            } else {
                views.setImageViewResource(R.id.widget_cover, R.drawable.amdv_tune_logo)
            }

            // Clicking cover or text opens AMDV-TUNE straight to the player
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_FULL_SCREEN_PLAYER, true)
            }
            val openPending = PendingIntent.getActivity(
                context,
                300,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_cover, openPending)
            views.setOnClickPendingIntent(R.id.widget_text_container, openPending)

            views.setOnClickPendingIntent(R.id.widget_btn_prev, broadcastPendingIntent(context, ACTION_PREV, 301))
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, broadcastPendingIntent(context, ACTION_PLAY_PAUSE, 302))
            views.setOnClickPendingIntent(R.id.widget_btn_next, broadcastPendingIntent(context, ACTION_NEXT, 303))

            return views
        }

        private fun broadcastPendingIntent(context: Context, action: String, reqCode: Int): PendingIntent {
            val intent = Intent(context, AmdvMusicWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createRoundedArtwork(src: Bitmap, sizePx: Int, radiusPx: Float): Bitmap {
            return try {
                val scaled = Bitmap.createScaledBitmap(src, sizePx, sizePx, true)
                val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(out)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
                canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(scaled, 0f, 0f, paint)
                out
            } catch (_: Exception) {
                src
            }
        }
    }
}
