package com.virexa.screen.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import androidx.lifecycle.LifecycleService
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.virexa.screen.MainActivity
import com.virexa.screen.data.RecordingSession
import kotlin.math.abs
import kotlin.math.roundToInt

class FloatingBubbleService : LifecycleService() {

    companion object {
        const val ACTION_CLOSE = "com.virexa.screen.action.CLOSE_BUBBLE"
        const val ACTION_OPEN_PANEL = "com.virexa.screen.action.OPEN_PANEL"
        private const val CLICK_THRESHOLD_PX = 12f

        fun start(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                RecordingSession.setMessage("Activa notificaciones para mostrar la ventana flotante")
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                RecordingSession.setMessage("Concede permiso de superposición para mostrar la ventana flotante")
                return false
            }

            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, FloatingBubbleService::class.java)
                )
            }.onFailure {
                RecordingSession.setMessage("No se pudo iniciar la ventana flotante: ${it.message}")
                return false
            }

            return true
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, FloatingBubbleService::class.java)) }
        }
    }

    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.ensureChannels(this)

        if (intent?.action == ACTION_CLOSE) {
            stopSelf()
            return START_NOT_STICKY
        }

        return runCatching {
            ServiceCompat.startForeground(
                this,
                2,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
            showBubble()
            START_STICKY
        }.getOrElse {
            RecordingSession.setMessage("No se pudo abrir la ventana flotante: ${it.message}")
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun showBubble() {
        if (bubbleView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 200
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingBubbleService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                BubbleRoot(
                    onOpenApp = { openApp() },
                    onPause = { sendRecordAction(ScreenRecordService.ACTION_PAUSE) },
                    onResume = { sendRecordAction(ScreenRecordService.ACTION_RESUME) },
                    onStop = { sendRecordAction(ScreenRecordService.ACTION_STOP) },
                    onNew = { sendRecordAction(ScreenRecordService.ACTION_NEW) },
                    onClose = { stopSelf() },
                )
            }
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initX = 0
            private var initY = 0
            private var initTouchX = 0f
            private var initTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                return when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initX = params?.x ?: 0
                        initY = params?.y ?: 0
                        initTouchX = event.rawX
                        initTouchY = event.rawY
                        isDragging = false
                        false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initTouchX
                        val dy = event.rawY - initTouchY

                        if (!isDragging && (abs(dx) > CLICK_THRESHOLD_PX || abs(dy) > CLICK_THRESHOLD_PX)) {
                            isDragging = true
                        }

                        if (isDragging) {
                            params?.x = (initX + dx).roundToInt()
                            params?.y = (initY + dy).roundToInt()
                            runCatching { windowManager.updateViewLayout(view, params) }
                            true
                        } else {
                            false
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            isDragging = false
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            }
        })

        bubbleView = view
        runCatching { windowManager.addView(view, params) }.onFailure {
            bubbleView = null
            throw it
        }
    }

    private fun openApp() {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
            )
        }.onFailure {
            RecordingSession.setMessage("No se pudo abrir la app: ${it.message}")
        }
    }

    private fun sendRecordAction(action: String) {
        startService(Intent(this, ScreenRecordService::class.java).apply { this.action = action })
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
    }

    private fun buildNotification(): Notification {
        val openPi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val closePi = PendingIntent.getService(
            this,
            10,
            Intent(this, FloatingBubbleService::class.java).apply { action = ACTION_CLOSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_BUBBLE_ID)
            .setContentTitle("Virexa Screen")
            .setContentText("Ventana flotante activa")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cerrar burbuja", closePi)
            .build()
    }
}

@Composable
private fun BubbleRoot(
    onOpenApp: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onNew: () -> Unit,
    onClose: () -> Unit,
) {
    val state by RecordingSession.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    val isRecording = state.isRecording
    val isPaused = state.isPaused

    val accentColor by animateColorAsState(
        targetValue = when {
            isRecording && !isPaused -> Color(0xFFE53935)
            isPaused -> Color(0xFFFF9800)
            else -> Color(0xFF6C63FF)
        },
        animationSpec = tween(400),
        label = "accent",
    )

    MaterialTheme {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, accentColor.copy(alpha = 0.5f)
            ),
            shadowElevation = 16.dp,
            modifier = Modifier.widthIn(min = 160.dp, max = 260.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accentColor, CircleShape),
                        )
                        Text(
                            text = when {
                                isRecording && !isPaused -> "Grabando"
                                isPaused -> "En pausa"
                                else -> "Virexa"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                        )
                    }

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Colapsar" else "Expandir",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                AnimatedVisibility(visible = isRecording) {
                    Text(
                        text = formatElapsed(state.elapsedMs),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFeatureSettings = "tnum",
                            letterSpacing = 2.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isRecording) {
                        BubbleBtn(
                            icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            label = if (isPaused) "Reanudar" else "Pausar",
                            tint = accentColor,
                            onClick = if (isPaused) onResume else onPause,
                        )
                        BubbleBtn(
                            icon = Icons.Default.Stop,
                            label = "Detener",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = onStop,
                        )
                    } else {
                        BubbleBtn(
                            icon = Icons.Default.FiberManualRecord,
                            label = "Nueva",
                            tint = accentColor,
                            onClick = onNew,
                        )
                    }

                    BubbleBtn(
                        icon = Icons.Default.OpenInNew,
                        label = "Abrir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onOpenApp,
                    )
                    BubbleBtn(
                        icon = Icons.Default.Close,
                        label = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onClose,
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(
                            text = if (isRecording) "Sesión activa" else "Sin grabación activa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!isRecording) {
                            BubbleBtn(
                                icon = Icons.Default.FiberManualRecord,
                                label = "Iniciar nueva",
                                tint = accentColor,
                                onClick = onNew,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleBtn(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp),
            onClick = onClick,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
        )
    }
}

private fun formatElapsed(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
