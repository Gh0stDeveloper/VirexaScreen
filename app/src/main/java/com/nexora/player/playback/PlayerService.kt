package com.nexora.player.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nexora.player.MainActivity
import com.nexora.player.R
import com.nexora.player.data.local.FavoriteMediaEntity
import com.nexora.player.data.local.NexoraDatabase
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.MediaKind
import com.nexora.player.data.model.PlaybackSnapshot
import com.nexora.player.ui.components.formatDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerService : MediaSessionService() {

    companion object {
        private const val CHANNEL_ID = "nexora_playback"
        private const val NOTIFICATION_ID = 4201

        const val ACTION_PLAY_PAUSE = "com.nexora.player.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.nexora.player.action.NEXT"
        const val ACTION_PREVIOUS = "com.nexora.player.action.PREVIOUS"
        const val ACTION_SEEK_BACK_10 = "com.nexora.player.action.SEEK_BACK_10"
        const val ACTION_SEEK_FORWARD_10 = "com.nexora.player.action.SEEK_FORWARD_10"
        const val ACTION_STOP = "com.nexora.player.action.STOP"
        const val ACTION_TOGGLE_FAVORITE = "com.nexora.player.action.TOGGLE_FAVORITE"
    }

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationVisible = false
    private var cachedFavoriteKey: String? = null
    private var cachedFavoriteState: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val player = PlayerEngine.get(this)
        mediaSession = MediaSession.Builder(this, player).build()

        serviceScope.launch {
            while (isActive) {
                renderNotification(PlayerEngine.snapshot.value)
                delay(1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> PlayerEngine.togglePlayPause(this)
            ACTION_NEXT -> PlayerEngine.skipNext(this)
            ACTION_PREVIOUS -> PlayerEngine.skipPrevious(this)
            ACTION_SEEK_BACK_10 -> PlayerEngine.seekBy(this, -10_000L)
            ACTION_SEEK_FORWARD_10 -> PlayerEngine.seekBy(this, 10_000L)
            ACTION_STOP -> {
                PlayerEngine.clear(this)
                hideNotification()
            }
            ACTION_TOGGLE_FAVORITE -> serviceScope.launch {
                toggleFavoriteForCurrent()
                renderNotification(PlayerEngine.snapshot.value, forceRefreshFavorite = true)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        hideNotification()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        PlayerEngine.release()
        super.onDestroy()
    }

    private suspend fun renderNotification(
        snapshot: PlaybackSnapshot,
        forceRefreshFavorite: Boolean = false
    ) {
        val current = snapshot.currentItem
        if (current == null || current.kind != MediaKind.AUDIO) {
            hideNotification()
            return
        }

        val player = PlayerEngine.get(this)
        val favoriteState = resolveFavoriteState(current, forceRefreshFavorite)
        val notification = buildNotification(current, player, favoriteState)

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        if (!notificationVisible) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            notificationVisible = true
        }
    }

    private suspend fun buildNotification(
        current: MediaEntry,
        player: androidx.media3.common.Player,
        isFavorite: Boolean
    ): android.app.Notification {
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val durationMs = when {
            player.duration > 0L && player.duration != androidx.media3.common.C.TIME_UNSET -> player.duration
            current.durationMs > 0L -> current.durationMs
            else -> 0L
        }
        val progress = if (durationMs > 0L) {
            ((positionMs * 1000L) / durationMs).toInt().coerceIn(0, 1000)
        } else {
            0
        }

        val compact = RemoteViews(packageName, R.layout.notification_player_compact)
        val expanded = RemoteViews(packageName, R.layout.notification_player_expanded)

        bindViews(compact, current, player, positionMs, durationMs, progress, isFavorite, expanded = false)
        bindViews(expanded, current, player, positionMs, durationMs, progress, isFavorite, expanded = true)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_playback)
            .setLargeIcon(loadLargeIcon())
            .setContentTitle(current.title)
            .setContentText(current.artist.ifBlank { current.album.ifBlank { getString(R.string.notification_subtitle_playing) } })
            .setContentIntent(contentIntent())
            .setDeleteIntent(serviceAction(ACTION_STOP))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(ContextCompat.getColor(this, R.color.nexora_accent))
            .setColorized(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compact)
            .setCustomBigContentView(expanded)
            .setCustomHeadsUpContentView(compact)
            .build()
    }

    private fun bindViews(
        views: RemoteViews,
        current: MediaEntry,
        player: androidx.media3.common.Player,
        positionMs: Long,
        durationMs: Long,
        progress: Int,
        isFavorite: Boolean,
        expanded: Boolean
    ) {
        val subtitle = current.artist.ifBlank { current.album.ifBlank { getString(R.string.notification_subtitle_playing) } }
        val albumLabel = current.album.ifBlank { current.folder.orEmpty() }
        val totalTime = if (durationMs > 0L) formatDuration(durationMs) else "—"
        val playPauseIcon = if (player.isPlaying) R.drawable.ic_notification_pause else R.drawable.ic_notification_playback
        val favoriteIcon = if (isFavorite) R.drawable.ic_notification_favorite_filled else R.drawable.ic_notification_favorite_outline
        val favoriteBg = if (isFavorite) R.drawable.bg_notification_button_active else R.drawable.bg_notification_button

        views.setTextViewText(R.id.notification_title, current.title)
        views.setTextViewText(R.id.notification_artist, subtitle)
        views.setTextViewText(R.id.notification_album, if (albumLabel.isBlank()) "" else albumLabel)
        views.setTextViewText(R.id.notification_time_current, formatDuration(positionMs))
        views.setTextViewText(R.id.notification_time_total, totalTime)
        views.setProgressBar(R.id.notification_progress, 1000, progress, false)

        views.setImageViewResource(R.id.notification_favorite, favoriteIcon)
        views.setImageViewResource(R.id.notification_prev, R.drawable.ic_notification_previous)
        views.setImageViewResource(R.id.notification_play_pause, playPauseIcon)
        views.setImageViewResource(R.id.notification_next, R.drawable.ic_notification_next)
        views.setImageViewResource(R.id.notification_stop, R.drawable.ic_notification_stop)

        views.setInt(R.id.notification_favorite, "setBackgroundResource", favoriteBg)
        views.setInt(R.id.notification_prev, "setBackgroundResource", R.drawable.bg_notification_button)
        views.setInt(R.id.notification_play_pause, "setBackgroundResource", R.drawable.bg_notification_button)
        views.setInt(R.id.notification_next, "setBackgroundResource", R.drawable.bg_notification_button)
        views.setInt(R.id.notification_stop, "setBackgroundResource", R.drawable.bg_notification_button)

        views.setOnClickPendingIntent(R.id.notification_favorite, serviceAction(ACTION_TOGGLE_FAVORITE))
        views.setOnClickPendingIntent(R.id.notification_prev, serviceAction(ACTION_PREVIOUS))
        views.setOnClickPendingIntent(R.id.notification_play_pause, serviceAction(ACTION_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.notification_next, serviceAction(ACTION_NEXT))
        views.setOnClickPendingIntent(R.id.notification_stop, serviceAction(ACTION_STOP))

        if (expanded) {
            views.setOnClickPendingIntent(R.id.notification_seek_back, serviceAction(ACTION_SEEK_BACK_10))
            views.setOnClickPendingIntent(R.id.notification_seek_forward, serviceAction(ACTION_SEEK_FORWARD_10))
            views.setInt(R.id.notification_seek_back, "setBackgroundResource", R.drawable.bg_notification_button)
            views.setInt(R.id.notification_seek_forward, "setBackgroundResource", R.drawable.bg_notification_button)
            views.setImageViewResource(R.id.notification_seek_back, R.drawable.ic_notification_seek_back)
            views.setImageViewResource(R.id.notification_seek_forward, R.drawable.ic_notification_seek_forward)
        }

        views.setViewVisibility(R.id.notification_album, if (albumLabel.isBlank()) View.GONE else View.VISIBLE)

        if (expanded) {
            views.setInt(R.id.notification_title, "setTextSize", 16)
            views.setInt(R.id.notification_artist, "setTextSize", 13)
        } else {
            views.setInt(R.id.notification_title, "setTextSize", 15)
            views.setInt(R.id.notification_artist, "setTextSize", 12)
        }
    }

    private suspend fun resolveFavoriteState(current: MediaEntry, forceRefresh: Boolean): Boolean {
        val key = favoriteKey(current)
        if (forceRefresh || cachedFavoriteKey != key) {
            cachedFavoriteState = withContext(Dispatchers.IO) {
                NexoraDatabase.get(this@PlayerService)
                    .favoritesDao()
                    .isFavorite(current.id, current.kind.name)
            }
            cachedFavoriteKey = key
        }
        return cachedFavoriteState
    }

    private suspend fun toggleFavoriteForCurrent() {
        val current = PlayerEngine.snapshot.value.currentItem ?: return
        if (current.kind != MediaKind.AUDIO) return
        withContext(Dispatchers.IO) {
            val dao = NexoraDatabase.get(this@PlayerService).favoritesDao()
            val exists = dao.isFavorite(current.id, current.kind.name)
            if (exists) {
                dao.delete(current.id, current.kind.name)
            } else {
                dao.upsert(
                    FavoriteMediaEntity(
                        mediaId = current.id,
                        mediaKind = current.kind.name,
                        title = current.title,
                        artist = current.artist,
                        album = current.album,
                        durationMs = current.durationMs,
                        uriString = current.uri.toString()
                    )
                )
            }
        }
        cachedFavoriteKey = favoriteKey(current)
        cachedFavoriteState = !cachedFavoriteState
    }

    private fun favoriteKey(current: MediaEntry): String = "${current.kind.name}:${current.id}"

    private fun hideNotification() {
        if (notificationVisible) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationVisible = false
        }
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        cachedFavoriteKey = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_playback_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_playback_desc)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun contentIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        pendingIntentFlags()
    )

    private fun serviceAction(action: String) = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, PlayerService::class.java).setAction(action),
        pendingIntentFlags()
    )

    private fun pendingIntentFlags(): Int {
        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.FLAG_UPDATE_CURRENT or immutable
    }

    private fun loadLargeIcon(): Bitmap? {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_launcher_foreground) ?: return null
        if (drawable is BitmapDrawable) return drawable.bitmap
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 192
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 192
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
