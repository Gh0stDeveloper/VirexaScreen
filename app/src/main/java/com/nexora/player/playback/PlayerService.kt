package com.nexora.player.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nexora.player.MainActivity
import com.nexora.player.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlayerService : MediaSessionService() {

    companion object {
        private const val CHANNEL_ID = "nexora_playback"
        private const val NOTIFICATION_ID = 4201

        const val ACTION_PLAY_PAUSE = "com.nexora.player.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.nexora.player.action.NEXT"
        const val ACTION_PREVIOUS = "com.nexora.player.action.PREVIOUS"
        const val ACTION_STOP = "com.nexora.player.action.STOP"
    }

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val player = PlayerEngine.get(this)
        mediaSession = MediaSession.Builder(this, player).build()
        serviceScope.launch {
            PlayerEngine.snapshot.collectLatest { snapshot ->
                updateNotification(snapshot)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> PlayerEngine.togglePlayPause(this)
            ACTION_NEXT -> PlayerEngine.skipNext(this)
            ACTION_PREVIOUS -> PlayerEngine.skipPrevious(this)
            ACTION_STOP -> {
                PlayerEngine.clear(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        PlayerEngine.release()
        super.onDestroy()
    }

    private fun updateNotification(snapshot: com.nexora.player.data.model.PlaybackSnapshot) {
        if (snapshot.queue.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
            return
        }

        val current = snapshot.currentItem
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_playback)
            .setContentTitle(current?.title ?: getString(R.string.app_name))
            .setContentText(current?.artist?.ifBlank { current.album } ?: "Reproduciendo")
            .setContentIntent(contentIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(snapshot.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(android.R.drawable.ic_media_previous, getString(R.string.action_previous), serviceAction(ACTION_PREVIOUS))
            .addAction(
                if (snapshot.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                getString(if (snapshot.isPlaying) R.string.action_pause else R.string.action_play),
                serviceAction(ACTION_PLAY_PAUSE)
            )
            .addAction(android.R.drawable.ic_media_next, getString(R.string.action_next), serviceAction(ACTION_NEXT))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop), serviceAction(ACTION_STOP))
            .build()

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(NOTIFICATION_ID, notification)

        if (snapshot.isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción Nexora",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción multimedia"
                setShowBadge(false)
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
}
