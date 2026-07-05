package com.virexa.screen.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.virexa.screen.MainActivity
import com.virexa.screen.data.RecordingSession

class WatermarkOverlayService : Service() {

    companion object {
        private const val ACTION_START = "com.virexa.screen.action.WATERMARK_START"
        private const val ACTION_STOP = "com.virexa.screen.action.WATERMARK_STOP"
        private const val EXTRA_TEXT = "extra_text"
        private const val NOTIFICATION_ID = 4

        fun start(context: Context, watermarkText: String): Boolean {
            if (watermarkText.isBlank()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return false
            return runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, WatermarkOverlayService::class.java).apply {
                        action = ACTION_START
                        putExtra(EXTRA_TEXT, watermarkText)
                    }
                )
                true
            }.getOrDefault(false)
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, WatermarkOverlayService::class.java).apply { action = ACTION_STOP }
                )
            }
        }
    }

    private lateinit var windowManager: WindowManager
    private var rootView: LinearLayout? = null
    private var secondaryText: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var ticker: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.ensureChannels(this)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val text = intent?.getStringExtra(EXTRA_TEXT).orEmpty().ifBlank { "Virexa Screen" }
        return runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            showOverlay(text)
            START_STICKY
        }.getOrElse {
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun showOverlay(text: String) {
        removeOverlay()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 68
        }

        val primary = TextView(this).apply {
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            this.text = text
        }
        val secondary = TextView(this).apply {
            setTextColor(0xD9FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            text = "REC 00:00"
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(26, 16, 26, 16)
            setBackgroundColor(0x7A101216)
            addView(primary)
            addView(secondary)
        }
        secondaryText = secondary
        rootView = container
        windowManager.addView(container, params)

        ticker = object : Runnable {
            override fun run() {
                secondaryText?.text = "REC ${formatElapsed(RecordingSession.uiState.value.elapsedMs)}"
                handler.postDelayed(this, 500)
            }
        }.also { handler.post(it) }
    }

    private fun removeOverlay() {
        ticker?.let { handler.removeCallbacks(it) }
        ticker = null
        rootView?.let { runCatching { windowManager.removeView(it) } }
        rootView = null
        secondaryText = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val openPi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_BUBBLE_ID)
            .setContentTitle("Virexa Screen")
            .setContentText("Marca de agua activa")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun formatElapsed(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
