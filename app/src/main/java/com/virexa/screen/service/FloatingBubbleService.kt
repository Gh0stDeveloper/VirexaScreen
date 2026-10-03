package com.virexa.screen.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.virexa.screen.MainActivity
import com.virexa.screen.R
import com.virexa.screen.data.PreferencesRepository
import com.virexa.screen.data.RecordingSession
import com.virexa.screen.data.UserPreferences
import kotlin.math.max
import kotlin.math.roundToInt

class FloatingBubbleService : LifecycleService(), SavedStateRegistryOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        const val ACTION_CLOSE = "com.virexa.screen.action.CLOSE_BUBBLE"

        fun start(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                RecordingSession.setMessage("Activa notificaciones para mostrar la ventana flotante")
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                RecordingSession.setMessage("Concede permiso de superposición para mostrar la ventana flotante")
                return false
            }
            return runCatching {
                ContextCompat.startForegroundService(context, Intent(context, FloatingBubbleService::class.java))
                true
            }.getOrElse {
                RecordingSession.setMessage("No se pudo iniciar la ventana flotante: ${it.message}")
                false
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, FloatingBubbleService::class.java)) }
        }
    }

    private lateinit var windowManager: WindowManager
    private val preferencesRepository by lazy { PreferencesRepository(applicationContext) }
    private var bubbleView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.ensureChannels(this)
        if (intent?.action == ACTION_CLOSE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return runCatching {
            ServiceCompat.startForeground(this, 2, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
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
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 220
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingBubbleService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBubbleService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val preferences by preferencesRepository.preferencesFlow.collectAsState(initial = UserPreferences())
                BubbleRoot(
                    showQuickControls = preferences.showQuickControls,
                    showTimer = preferences.showTimerOnBubble,
                    onOpenApp = ::openApp,
                    onPause = { sendRecordAction(ScreenRecordService.ACTION_PAUSE) },
                    onResume = { sendRecordAction(ScreenRecordService.ACTION_RESUME) },
                    onStop = { sendRecordAction(ScreenRecordService.ACTION_STOP) },
                    onClose = { stopSelf() },
                    onDrag = ::offsetBy,
                    onDragEnd = ::snapToEdge,
                    onSizeChanged = ::keepOnScreen,
                )
            }
        }
        bubbleView = view
        runCatching { windowManager.addView(view, params) }.onFailure {
            bubbleView = null
            throw it
        }
        view.post { snapToEdge() }
    }

    private fun offsetBy(dx: Float, dy: Float) {
        params?.let {
            it.x += dx.roundToInt()
            it.y += dy.roundToInt()
            runCatching { windowManager.updateViewLayout(bubbleView, it) }
        }
    }

    private fun snapToEdge() {
        keepOnScreen(snapToNearestEdge = true)
    }

    private fun keepOnScreen() {
        bubbleView?.post { keepOnScreen(snapToNearestEdge = false) }
    }

    private fun keepOnScreen(snapToNearestEdge: Boolean) {
        val p = params ?: return
        val view = bubbleView ?: return
        val screenWidth: Int
        val screenHeight: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val point = android.graphics.Point()
            @Suppress("DEPRECATION")
            display.getSize(point)
            screenWidth = point.x
            screenHeight = point.y
        }
        val margin = 16.dpToPx()
        val viewWidth = max(view.width, 56.dpToPx())
        val viewHeight = max(view.height, 56.dpToPx())
        p.x = if (snapToNearestEdge) {
            if (p.x + viewWidth / 2 < screenWidth / 2) margin else screenWidth - viewWidth - margin
        } else {
            p.x.coerceIn(margin, max(margin, screenWidth - viewWidth - margin))
        }
        val maxY = max(margin * 2, screenHeight - viewHeight - (margin * 4))
        p.y = p.y.coerceIn(margin * 2, maxY)
        runCatching { windowManager.updateViewLayout(view, p) }
    }

    private fun openApp() {
        runCatching {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }.onFailure {
            RecordingSession.setMessage("No se pudo abrir la app: ${it.message}")
        }
    }

    private fun sendRecordAction(action: String) {
        startService(Intent(this, ScreenRecordService::class.java).apply { this.action = action })
    }

    override fun onDestroy() {
        savedStateRegistryController.performSave(Bundle())
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val openPi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val closePi = PendingIntent.getService(this, 10, Intent(this, FloatingBubbleService::class.java).apply { action = ACTION_CLOSE }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_BUBBLE_ID)
            .setContentTitle("Virexa Screen")
            .setContentText("Control flotante profesional")
            .setSmallIcon(R.drawable.ic_qs_virexa)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cerrar burbuja", closePi)
            .build()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).roundToInt()
}

@Composable
private fun BubbleRoot(
    showQuickControls: Boolean,
    showTimer: Boolean,
    onOpenApp: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onSizeChanged: () -> Unit,
) {
    val state by RecordingSession.uiState.collectAsState()
    var minimized by remember { mutableStateOf(false) }
    val currentDrag by rememberUpdatedState(onDrag)
    val currentDragEnd by rememberUpdatedState(onDragEnd)
    val currentSizeChanged by rememberUpdatedState(onSizeChanged)

    LaunchedEffect(minimized) { currentSizeChanged() }

    MaterialTheme {
        AnimatedContent(targetState = minimized, label = "bubble_mode") { isMinimized ->
            if (isMinimized) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xEE0E1014),
                    tonalElevation = 10.dp,
                    shadowElevation = 18.dp,
                    modifier = Modifier
                        .size(62.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { currentDragEnd() },
                                onDragCancel = { currentDragEnd() },
                            ) { change, dragAmount ->
                                change.consume()
                                currentDrag(dragAmount.x, dragAmount.y)
                            }
                        },
                    onClick = { minimized = false },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.virexa_brand_icon),
                            contentDescription = "Virexa",
                            modifier = Modifier.size(34.dp),
                        )
                        if (state.isRecording && !state.isPaused) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(10.dp)
                                    .background(Color(0xFFFF3B30), CircleShape),
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xF013151A),
                    tonalElevation = 12.dp,
                    shadowElevation = 24.dp,
                    modifier = Modifier.widthIn(min = 210.dp, max = 286.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragEnd = { currentDragEnd() },
                                        onDragCancel = { currentDragEnd() },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        currentDrag(dragAmount.x, dragAmount.y)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.virexa_brand_icon),
                                    contentDescription = "Virexa",
                                    modifier = Modifier.size(26.dp),
                                )
                                Column {
                                    Text(
                                        text = when {
                                            state.isRecording && !state.isPaused -> "Grabando"
                                            state.isPaused -> "En pausa"
                                            else -> "Virexa Screen"
                                        },
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = if (state.isRecording && showTimer) formatElapsed(state.elapsedMs) else "Control flotante",
                                        color = Color(0xFF9FA4AD),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { minimized = true }) {
                                    Icon(Icons.Default.Minimize, contentDescription = "Minimizar", tint = Color.White)
                                }
                                IconButton(onClick = onClose) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                                }
                            }
                        }

                        AnimatedVisibility(visible = showQuickControls) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HorizontalDivider(color = Color(0x22FFFFFF))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    BubbleAction(
                                        icon = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        label = if (state.isPaused) "Reanudar" else "Pausar",
                                        tint = if (state.isPaused) Color(0xFFFFB340) else Color(0xFFFF5A52),
                                        enabled = state.isRecording,
                                        onClick = if (state.isPaused) onResume else onPause,
                                        modifier = Modifier.weight(1f),
                                    )
                                    BubbleAction(
                                        icon = Icons.Default.Stop,
                                        label = "Detener",
                                        tint = Color(0xFFFF3B30),
                                        enabled = state.isRecording,
                                        onClick = onStop,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BubbleSecondaryAction(
                                icon = Icons.Default.OpenInNew,
                                label = "Abrir app",
                                onClick = onOpenApp,
                                modifier = Modifier.weight(1f),
                            )
                            BubbleSecondaryAction(
                                icon = Icons.Default.Settings,
                                label = "Ajustes",
                                onClick = onOpenApp,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        AnimatedVisibility(visible = state.message != null, enter = fadeIn(), exit = fadeOut()) {
                            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF1C2027), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = state.message.orEmpty(),
                                    color = Color(0xFFD7DBE2),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            }
                        }

                        if (!state.isRecording) {
                            Text(
                                text = "Cuando toques iniciar, Android mostrará el permiso de captura. La burbuja se puede arrastrar desde la cabecera.",
                                color = Color(0xFF9FA4AD),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) tint.copy(alpha = 0.14f) else Color(0xFF1A1D23),
        modifier = modifier.height(68.dp),
        onClick = { if (enabled) onClick() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(icon, contentDescription = label, tint = if (enabled) tint else Color(0xFF606772))
            Text(label, color = if (enabled) Color.White else Color(0xFF606772), fontSize = 11.sp)
        }
    }
}

@Composable
private fun BubbleSecondaryAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1A1D23),
        modifier = modifier.height(56.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color(0xFFCDD3DC))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color(0xFFCDD3DC), fontSize = 12.sp)
        }
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
