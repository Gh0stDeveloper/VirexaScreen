package com.virexa.screen.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.virexa.screen.MainActivity
import com.virexa.screen.ui.components.BubbleMiniLabel
import com.virexa.screen.data.RecordingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var visibilityJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.ensureChannel(this)
        ServiceCompat.startForeground(this, 2, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        showBubble()
        observeRecordingState()
        return START_STICKY
    }

    private fun observeRecordingState() {
        if (visibilityJob != null) return
        visibilityJob = serviceScope.launch {
            RecordingSession.uiState.collectLatest { state ->
                bubbleView?.visibility = if (state.isRecording && !state.isPaused) View.GONE else View.VISIBLE
                bubbleView?.alpha = if (state.isRecording && !state.isPaused) 0f else 1f
            }
        }
    }

    private fun showBubble() {
        if (bubbleView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                BubbleSurface(
                    onOpenApp = { openApp() },
                    onPause = { sendRecordAction(ScreenRecordService.ACTION_PAUSE) },
                    onResume = { sendRecordAction(ScreenRecordService.ACTION_RESUME) },
                    onStop = { sendRecordAction(ScreenRecordService.ACTION_STOP) },
                    onClose = { stopSelf() },
                )
            }
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 64
            y = 220
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params?.x = (initialX + (event.rawX - initialTouchX)).roundToInt()
                        params?.y = (initialY + (event.rawY - initialTouchY)).roundToInt()
                        runCatching { windowManager.updateViewLayout(view, params) }
                        return true
                    }
                }
                return false
            }
        })

        bubbleView = view
        windowManager.addView(view, params)
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun sendRecordAction(action: String) {
        startService(Intent(this, ScreenRecordService::class.java).apply { this.action = action })
    }

    override fun onDestroy() {
        super.onDestroy()
        visibilityJob?.cancel()
        visibilityJob = null
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("Virexa Screen")
            .setContentText("Burbuja flotante activa")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

@Composable
private fun BubbleSurface(
    onOpenApp: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
) {
    val state by RecordingSession.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    MaterialTheme {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
            shadowElevation = 14.dp,
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(width = 104.dp, height = 16.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f), Color.Transparent),
                            ),
                            shape = RoundedCornerShape(999.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("", color = Color.Transparent)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BubbleIconChip(
                        icon = Icons.Default.Settings,
                        label = if (expanded) "Cerrar" else "Ventana",
                        onClick = { expanded = !expanded },
                    )
                    BubbleIconChip(
                        icon = Icons.Default.PlayArrow,
                        label = "Abrir",
                        onClick = onOpenApp,
                    )
                    BubbleIconChip(
                        icon = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        label = if (state.isPaused) "Seguir" else "Pausa",
                        onClick = if (state.isPaused) onResume else onPause,
                    )
                    BubbleIconChip(
                        icon = Icons.Default.Stop,
                        label = "Stop",
                        onClick = onStop,
                    )
                    BubbleIconChip(
                        icon = Icons.Default.Close,
                        label = "Cerrar",
                        onClick = onClose,
                    )
                }

                if (expanded) {
                    BubbleMiniLabel(if (state.isRecording) "Panel flotante activo" else "Panel flotante listo")
                }

                if (state.isRecording) {
                    Text(if (state.isPaused) "Grabación en pausa" else "Grabación activa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Atajo flotante", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BubbleIconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
