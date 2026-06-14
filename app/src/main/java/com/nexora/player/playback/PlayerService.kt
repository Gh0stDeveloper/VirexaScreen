package com.nexora.player.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.nexora.player.MainActivity
import com.nexora.player.R
import com.nexora.player.data.local.NexoraDatabase
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.MediaKind
import com.nexora.player.data.model.PlaybackSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class PlayerService : MediaSessionService() {

    companion object {
        private const val CHANNEL_ID = "nexora_playback"
        private const val NOTIFICATION_ID = 4201

        const val ACTION_PLAY_PAUSE = "com.nexora.player.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.nexora.player.action.NEXT"
        const val ACTION_PREVIOUS = "com.nexora.player.action.PREVIOUS"
        const val ACTION_FAVORITE = "com.nexora.player.action.FAVORITE"
        const val ACTION_STOP = "com.nexora.player.action.STOP"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val database by lazy { NexoraDatabase.get(applicationContext) }

    private var mediaSession: MediaSession? = null
    private var favoriteIds: Set<Long> = emptySet()
    private var progressTickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        setupMediaSession()
        observeFavorites()
        observePlayback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> PlayerEngine.togglePlayPause(this)
                ACTION_NEXT -> PlayerEngine.skipNext(this)
                ACTION_PREVIOUS -> PlayerEngine.skipPrevious(this)
                ACTION_FAVORITE -> toggleFavoriteForCurrentItem()
                ACTION_STOP -> stopPlaybackAndHideNotification()
            }
        } catch (_: Throwable) {
            // Nunca dejes que una acción de notificación tumbe el servicio.
        }
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        progressTickerJob?.cancel()
        serviceScope.cancel()

        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null

        PlayerEngine.release()
        super.onDestroy()
    }

    private fun setupMediaSession() {
        val player = PlayerEngine.get(this)
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(contentIntent())
            .build()
    }

    private fun observeFavorites() {
        serviceScope.launch {
            database.favoritesDao().observeAll().collectLatest { favorites ->
                favoriteIds = favorites.map { it.mediaId }.toSet()
                refreshNotification(PlayerEngine.snapshot.value)
            }
        }
    }

    private fun observePlayback() {
        serviceScope.launch {
            PlayerEngine.snapshot.collectLatest { snapshot ->
                refreshNotification(snapshot)
            }
        }
    }

    private fun refreshNotification(snapshot: PlaybackSnapshot) {
        val current = snapshot.currentItem

        if (current == null || current.kind != MediaKind.AUDIO || snapshot.queue.isEmpty()) {
            progressTickerJob?.cancel()
            progressTickerJob = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
            return
        }

        try {
            val player = PlayerEngine.get(this)
            val notification = buildNotification(
                snapshot = snapshot,
                current = current,
                isFavorite = favoriteIds.contains(current.id),
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = resolveDurationMs(current, player.duration)
            )

            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            updateProgressTicker(snapshot)
        } catch (_: Throwable) {
            // Si algo falla aquí, mejor no crashear la app.
        }
    }

    private fun buildNotification(
        snapshot: PlaybackSnapshot,
        current: MediaEntry,
        isFavorite: Boolean,
        positionMs: Long,
        durationMs: Long
    ): Notification {
        val session = mediaSession ?: throw IllegalStateException("MediaSession no inicializada")
        val accentColor = ContextCompat.getColor(this, R.color.nexora_accent)
        val artwork = loadArtworkBitmap(current)

        val contentText = buildList {
            if (current.artist.isNotBlank()) add(current.artist)
            if (current.album.isNotBlank()) add(current.album)
        }.joinToString(" • ").ifBlank { getString(R.string.app_name) }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_playback)
            .setContentTitle(current.title)
            .setContentText(contentText)
            .setSubText(getString(R.string.app_name))
            .setLargeIcon(artwork)
            .setContentIntent(contentIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(accentColor)
            .setColorized(true)
            .addAction(
                android.R.drawable.ic_media_previous,
                getString(R.string.action_previous),
                serviceAction(ACTION_PREVIOUS)
            )
            .addAction(
                if (snapshot.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                getString(if (snapshot.isPlaying) R.string.action_pause else R.string.action_play),
                serviceAction(ACTION_PLAY_PAUSE)
            )
            .addAction(
                android.R.drawable.ic_media_next,
                getString(R.string.action_next),
                serviceAction(ACTION_NEXT)
            )
            // Si quieres corazón real, reemplaza estos dos drawables por tus assets vectoriales.
            .addAction(
                if (isFavorite) R.drawable.ic_notification_favorite_filled else R.drawable.ic_notification_favorite_outline,
                getString(if (isFavorite) R.string.media_favorite_remove else R.string.media_favorite_add),
                serviceAction(ACTION_FAVORITE)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_stop),
                serviceAction(ACTION_STOP)
            )
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(session).setShowActionsInCompactView(
                    intArrayOf(0, 1, 2)
                )
            )

        if (durationMs > 0L) {
            val max = durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val progress = positionMs.coerceIn(0L, durationMs).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            builder.setProgress(max, progress, false)
        }

        return builder.build()
    }

    private fun updateProgressTicker(snapshot: PlaybackSnapshot) {
        val current = snapshot.currentItem
        val shouldTick = snapshot.isPlaying && current?.kind == MediaKind.AUDIO

        if (!shouldTick) {
            progressTickerJob?.cancel()
            progressTickerJob = null
            return
        }

        if (progressTickerJob?.isActive == true) return

        progressTickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val latest = PlayerEngine.snapshot.value
                val latestCurrent = latest.currentItem
                if (!latest.isPlaying || latestCurrent?.kind != MediaKind.AUDIO) break
                refreshNotification(latest)
            }
        }
    }

    private fun toggleFavoriteForCurrentItem() {
        val current = PlayerEngine.snapshot.value.currentItem ?: return
        if (current.kind != MediaKind.AUDIO) return

        serviceScope.launch(Dispatchers.IO) {
            try {
                val dao = database.favoritesDao()
                val mediaKind = current.kind.name
                val exists = dao.isFavorite(current.id, mediaKind)

                if (exists) {
                    dao.delete(current.id, mediaKind)
                } else {
                    dao.upsert(
                        com.nexora.player.data.local.FavoriteMediaEntity(
                            mediaId = current.id,
                            mediaKind = mediaKind,
                            title = current.title,
                            artist = current.artist,
                            album = current.album,
                            durationMs = current.durationMs,
                            uriString = current.uri.toString()
                        )
                    )
                }
            } catch (_: Throwable) {
                // Ignorar para evitar crash desde la notificación.
            }
        }
    }

    private fun stopPlaybackAndHideNotification() {
        try {
            progressTickerJob?.cancel()
            progressTickerJob = null
            PlayerEngine.clear(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
            stopSelf()
        } catch (_: Throwable) {
            // No-op
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción Nexora",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción de música"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
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
        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        return PendingIntent.FLAG_UPDATE_CURRENT or immutable
    }

    private fun resolveDurationMs(current: MediaEntry, playerDurationMs: Long): Long {
        return when {
            current.durationMs > 0L -> current.durationMs
            playerDurationMs > 0L -> playerDurationMs
            else -> 0L
        }
    }

    private fun loadArtworkBitmap(item: MediaEntry): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, item.uri)
                retriever.embeddedPicture?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } ?: loadAppIconBitmap()
            } finally {
                runCatching { retriever.release() }
            }
        } catch (_: Throwable) {
            loadAppIconBitmap()
        }
    }

    private fun loadAppIconBitmap(): Bitmap? {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_launcher_foreground) ?: return null
        if (drawable is BitmapDrawable) return drawable.bitmap

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 256
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 256
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}