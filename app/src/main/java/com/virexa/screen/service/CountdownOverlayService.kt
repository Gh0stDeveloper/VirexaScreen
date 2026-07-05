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
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.virexa.screen.MainActivity
import com.virexa.screen.data.RecordingSession

class CountdownOverlayService : Service() {

    companion object {
        private const val ACTION_START = "com.virexa.screen.action.COUNTDOWN_START"
        private const val ACTION_STOP = "com.virexa.screen.action.COUNTDOWN_STOP"
        private const val EXTRA_SECONDS = "extra_seconds"
        private const val NOTIFICATION_ID = 3

        fun start(context: Context, seconds: Int): Boolean {
            if (seconds <= 0) return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                RecordingSession.setMessage("Activa notificaciones para mostrar el contador flotante")
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                RecordingSession.setMessage("Concede permiso de superposición para mostrar el contador sobre otras apps")
                return false
            }

            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, CountdownOverlayService::class.java).apply {
                        action = ACTION_START
                        putExtra(EXTRA_SECONDS, seconds)
                    },
                )
            }.onFailure {
                RecordingSession.setMessage("No se pudo mostrar el contador flotante: ${it.message}")
                return false
            }

            return true
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, CountdownOverlayService::class.java).apply {
                        action = ACTION_STOP
                    },
                )
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var overlayView: FrameLayout? = null
    private var numberView: TextView? = null
    private var currentSecond = 0
    private var ticker: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.ensureChannels(this)

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val seconds = intent?.getIntExtra(EXTRA_SECONDS, 3)?.coerceAtLeast(1) ?: 3
        return runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
            showOverlay(seconds)
            START_STICKY
        }.getOrElse {
            RecordingSession.setMessage("No se pudo abrir el contador flotante: ${it.message}")
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun showOverlay(seconds: Int) {
        removeOverlay()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        val number = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 112f)
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            setShadowLayer(22f, 0f, 0f, Color.BLACK)
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(0x99000000.toInt())
            addView(
                number,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER,
                ),
            )
        }

        numberView = number
        overlayView = root
        windowManager.addView(root, params)

        currentSecond = seconds
        ticker = object : Runnable {
            override fun run() {
                if (currentSecond <= 0) {
                    RecordingSession.setCountdown(0)
                    stopSelf()
                    return
                }

                numberView?.text = currentSecond.toString()
                RecordingSession.setCountdown(currentSecond)
                currentSecond -= 1
                handler.postDelayed(this, 1_000)
            }
        }.also { handler.post(it) }
    }

    private fun removeOverlay() {
        ticker?.let { handler.removeCallbacks(it) }
        ticker = null
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        numberView = null
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
            .setContentText("Cuenta regresiva flotante")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        removeOverlay()
        RecordingSession.setCountdown(0)
        super.onDestroy()
    }
}
