package com.nexora.player.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nexora.player.R
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.playback.PlayerEngine
import com.nexora.player.ui.components.MediaArtwork
import com.nexora.player.ui.components.PlaybackSeekBar
import com.nexora.player.ui.components.formatDuration
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

// ── Paleta Nexora Player ──────────────────────────────────────────────────────
private val NxAccent   = Color(0xFF00C2FF)          // cyan-blue
private val NxAccent2  = Color(0xFF7B61FF)          // purple (gradiente secundario)
private val NxBg       = Color(0xFF080808)
private val NxCard     = Color(0xFF141414)
private val NxDivider  = Color.White.copy(alpha = 0.07f)

private val GradientAccent = Brush.horizontalGradient(listOf(NxAccent, NxAccent2))

// ═════════════════════════════════════════════════════════════════════════════
// SCREEN PRINCIPAL
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun VideoPlayerScreen(
    modifier: Modifier = Modifier,
    current: MediaEntry?,
    onClose: () -> Unit = {}
) {
    val context    = LocalContext.current
    val view       = LocalView.current
    val activity   = context.findActivity()
    val exoPlayer  = PlayerEngine.get(context)
    val snapshot   by PlayerEngine.snapshot.collectAsState()
    val audioManager = context.getSystemService<AudioManager>()
    val maxVolume  = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15

    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val currentItem   = snapshot.currentItem ?: current

    // ── Estados reactivos ────────────────────────────────────────────────────
    var brightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.65f
        )
    }
    // volumeFraction es state local → se actualiza en setVolume para reflejarse en UI
    var volumeFraction by remember {
        val cv = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        mutableFloatStateOf(cv.toFloat() / maxVolume.toFloat().coerceAtLeast(1f))
    }

    var landscapeControlsVisible by remember { mutableStateOf(true) }
    var showQueuePanel           by remember { mutableStateOf(false) }
    var seekFeedbackMs           by remember { mutableLongStateOf(0L) }
    var showBrightnessHud        by remember { mutableStateOf(false) }
    var showVolumeHud            by remember { mutableStateOf(false) }

    // ── Orientación / barras del sistema ─────────────────────────────────────
    DisposableEffect(isLandscape) {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.window?.let { win ->
                WindowCompat.getInsetsController(win, view).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
        onDispose {
            activity?.window?.let { win ->
                WindowCompat.getInsetsController(win, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Auto-ocultar controles SOLO en landscape
    LaunchedEffect(landscapeControlsVisible, isLandscape) {
        if (isLandscape && landscapeControlsVisible) {
            delay(5_000L)
            landscapeControlsVisible = false
        }
    }

    LaunchedEffect(seekFeedbackMs)    { if (seekFeedbackMs != 0L)   { delay(700);   seekFeedbackMs = 0L } }
    LaunchedEffect(showBrightnessHud) { if (showBrightnessHud)      { delay(1_600); showBrightnessHud = false } }
    LaunchedEffect(showVolumeHud)     { if (showVolumeHud)          { delay(1_600); showVolumeHud = false } }

    BackHandler {
        if (isLandscape) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        else onClose()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    fun setBrightness(value: Float) {
        brightness = value.coerceIn(0.05f, 1f)
        activity?.window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = brightness
            win.attributes = lp
        }
        showBrightnessHud = true
    }

    fun setVolume(value: Float) {
        volumeFraction = value.coerceIn(0f, 1f)
        audioManager?.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (volumeFraction * maxVolume).roundToInt(),
            0
        )
        showVolumeHud = true
    }

    fun seekBy(deltaMs: Long) {
        val dur = exoPlayer.duration.takeIf { it > 0L } ?: currentItem?.durationMs ?: 0L
        if (dur <= 0L) return
        exoPlayer.seekTo((exoPlayer.currentPosition + deltaMs).coerceIn(0L, dur))
        seekFeedbackMs = deltaMs
    }

    if (currentItem == null) {
        Box(
            modifier = modifier.fillMaxSize().background(NxBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.video_no_playback),
                color = Color.White.copy(0.7f),
                fontSize = 16.sp
            )
        }
        return
    }

    val durationMs       = exoPlayer.duration.takeIf { it > 0L } ?: currentItem.durationMs
    val queue            = snapshot.queue
    val queueFromCurrent = remember(snapshot.queue, snapshot.currentIndex) {
        if (snapshot.currentIndex in queue.indices) queue.drop(snapshot.currentIndex + 1)
        else queue.drop(1)
    }
    val queueStartIndex = if (snapshot.currentIndex in queue.indices) snapshot.currentIndex + 1 else 1

    if (isLandscape) {
        LandscapeScreen(
            modifier              = modifier,
            exoPlayer             = exoPlayer,
            currentItem           = currentItem,
            queue                 = queue,
            queueFromCurrent      = queueFromCurrent,
            queueStartIndex       = queueStartIndex,
            isPlaying             = snapshot.isPlaying,
            positionMs            = exoPlayer.currentPosition,
            durationMs            = durationMs,
            brightness            = brightness,
            volumeFraction        = volumeFraction,
            controlsVisible       = landscapeControlsVisible,
            showQueuePanel        = showQueuePanel,
            seekFeedbackMs        = seekFeedbackMs,
            showBrightnessHud     = showBrightnessHud,
            showVolumeHud         = showVolumeHud,
            onToggleControls      = { landscapeControlsVisible = !landscapeControlsVisible },
            onDoubleTapLeft       = { seekBy(-10_000L); landscapeControlsVisible = true },
            onDoubleTapRight      = { seekBy(10_000L);  landscapeControlsVisible = true },
            onBrightnessSwipe     = { delta -> setBrightness(brightness + delta) },
            onVolumeSwipe         = { delta -> setVolume(volumeFraction + delta) },
            onSeekTo              = { exoPlayer.seekTo(it) },
            onPrevious            = { PlayerEngine.skipPrevious(context); landscapeControlsVisible = true },
            onTogglePlay          = { PlayerEngine.togglePlayPause(context); landscapeControlsVisible = true },
            onNext                = { PlayerEngine.skipNext(context); landscapeControlsVisible = true },
            onExitFullscreen      = {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            },
            onToggleQueue         = { showQueuePanel = !showQueuePanel; landscapeControlsVisible = true },
            onBack                = {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            },
            onJumpToQueueIndex    = { idx ->
                PlayerEngine.jumpTo(context, idx)
                showQueuePanel = false
                landscapeControlsVisible = true
            }
        )
    } else {
        PortraitScreen(
            modifier           = modifier,
            exoPlayer          = exoPlayer,
            currentItem        = currentItem,
            queueFromCurrent   = queueFromCurrent,
            queueStartIndex    = queueStartIndex,
            isPlaying          = snapshot.isPlaying,
            positionMs         = exoPlayer.currentPosition,
            durationMs         = durationMs,
            brightness         = brightness,
            volumeFraction     = volumeFraction,
            seekFeedbackMs     = seekFeedbackMs,
            onSeekTo           = { exoPlayer.seekTo(it) },
            onPrevious         = { PlayerEngine.skipPrevious(context) },
            onTogglePlay       = { PlayerEngine.togglePlayPause(context) },
            onNext             = { PlayerEngine.skipNext(context) },
            onEnterFullscreen  = {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            },
            onClose            = onClose,
            onBrightnessChange = ::setBrightness,
            onVolumeChange     = ::setVolume,
            onJumpToQueueIndex = { idx -> PlayerEngine.jumpTo(context, idx) },
            onDoubleTap        = { right -> if (right) seekBy(10_000L) else seekBy(-10_000L) }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PORTRAIT
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PortraitScreen(
    modifier           : Modifier = Modifier,
    exoPlayer          : androidx.media3.common.Player,
    currentItem        : MediaEntry,
    queueFromCurrent   : List<MediaEntry>,
    queueStartIndex    : Int,
    isPlaying          : Boolean,
    positionMs         : Long,
    durationMs         : Long,
    brightness         : Float,
    volumeFraction     : Float,
    seekFeedbackMs     : Long,
    onSeekTo           : (Long) -> Unit,
    onPrevious         : () -> Unit,
    onTogglePlay       : () -> Unit,
    onNext             : () -> Unit,
    onEnterFullscreen  : () -> Unit,
    onClose            : () -> Unit,
    onBrightnessChange : (Float) -> Unit,
    onVolumeChange     : (Float) -> Unit,
    onJumpToQueueIndex : (Int) -> Unit,
    onDoubleTap        : (right: Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NxBg)
    ) {

        // ── Video 16:9 con barra encima ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            // Player surface
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player      = exoPlayer
                        useController = false
                        resizeMode  = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Gradiente superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(0.65f), Color.Transparent))
                    )
            )
            // Gradiente inferior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.55f)))
                    )
            )

            // Capa de doble-tap para seek
            // IMPORTANTE: se compone ANTES de la barra superior para quedar
            // DEBAJO en el orden de hit-testing. Así los botones de Volver
            // y Pantalla completa reciben el toque primero.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            var lastTapTime = 0L
                            var lastTapX    = 0f
                            while (true) {
                                val down    = awaitFirstDown(requireUnconsumed = false)
                                val startX  = down.position.x
                                val downMs  = System.currentTimeMillis()
                                // espera release
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    if (ev.changes.none { it.pressed }) break
                                }
                                val elapsed = System.currentTimeMillis() - downMs
                                if (elapsed < 250L) {
                                    val sinceLast = downMs - lastTapTime
                                    if (sinceLast < 350L) {
                                        onDoubleTap(lastTapX > size.width / 2f)
                                        lastTapTime = 0L
                                    } else {
                                        lastTapTime = downMs
                                        lastTapX    = startX
                                    }
                                }
                            }
                        }
                    }
            )

            // Top bar (Back + Fullscreen)
            // Compuesta DESPUÉS de la capa de doble-tap → queda arriba y
            // sus botones (clickable) consumen el toque antes que la capa de gestos.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NxIconBtn(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onClick = onClose)
                Spacer(Modifier.weight(1f))
                NxIconBtn(Icons.Filled.Fullscreen, "Pantalla completa", onClick = onEnterFullscreen)
            }

            // Seek feedback HUD — Column provee ColumnScope explícito,
            // evitando la ambigüedad con el ColumnScope externo del layout.
            // No intercepta toques porque no tiene handler de gestos.
            Column(
                modifier            = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = seekFeedbackMs != 0L,
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    SeekFeedbackHud(seekFeedbackMs)
                }
            }
        }

        // ── Panel de controles (scrollable) ────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {

            // Título + meta
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text     = currentItem.title,
                    color    = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillTag(currentItem.resolutionLabel)
                    Text(
                        text  = currentItem.folder?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.video_local_label),
                        color = Color.White.copy(0.45f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Barra de progreso + tiempos
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaybackSeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeekTo   = onSeekTo,
                    modifier   = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatDuration(positionMs),
                        color    = NxAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        formatDuration(durationMs),
                        color = Color.White.copy(0.4f),
                        fontSize = 12.sp
                    )
                }
            }

            // Controles de reproducción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NxControlBtn(Icons.Filled.Replay10, "−10 s", iconSize = 24.dp) {
                    onSeekTo((positionMs - 10_000L).coerceAtLeast(0L))
                }
                NxControlBtn(Icons.Filled.SkipPrevious, "Anterior", iconSize = 26.dp, onClick = onPrevious)

                // Botón play/pause con gradiente
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(GradientAccent)
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                NxControlBtn(Icons.Filled.SkipNext, "Siguiente", iconSize = 26.dp, onClick = onNext)
                NxControlBtn(Icons.Filled.Forward10, "+10 s", iconSize = 24.dp) {
                    onSeekTo((positionMs + 10_000L).coerceAtMost(durationMs))
                }
            }

            // Separador sutil
            HorizontalDivider(color = NxDivider)

            // Sliders de brillo y volumen
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NxSlider(
                    icon    = Icons.Filled.Brightness6,
                    label   = "Brillo",
                    value   = brightness,
                    range   = 0.05f..1f,
                    onChange = onBrightnessChange
                )
                NxSlider(
                    icon    = if (volumeFraction < 0.01f) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    label   = "Volumen",
                    value   = volumeFraction,
                    range   = 0f..1f,
                    onChange = onVolumeChange
                )
            }

            // Cola de reproducción
            if (queueFromCurrent.isNotEmpty()) {
                HorizontalDivider(color = NxDivider)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = null,
                            tint = NxAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Continuará",
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${queueFromCurrent.size} videos",
                            color    = Color.White.copy(0.4f),
                            fontSize = 12.sp
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(queueFromCurrent.take(8)) { index, item ->
                            QueueItemCard(
                                item    = item,
                                index   = queueStartIndex + index,
                                onClick = { onJumpToQueueIndex(queueStartIndex + index) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// LANDSCAPE
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun LandscapeScreen(
    modifier           : Modifier = Modifier,
    exoPlayer          : androidx.media3.common.Player,
    currentItem        : MediaEntry,
    queue              : List<MediaEntry>,
    queueFromCurrent   : List<MediaEntry>,
    queueStartIndex    : Int,
    isPlaying          : Boolean,
    positionMs         : Long,
    durationMs         : Long,
    brightness         : Float,
    volumeFraction     : Float,
    controlsVisible    : Boolean,
    showQueuePanel     : Boolean,
    seekFeedbackMs     : Long,
    showBrightnessHud  : Boolean,
    showVolumeHud      : Boolean,
    onToggleControls   : () -> Unit,
    onDoubleTapLeft    : () -> Unit,
    onDoubleTapRight   : () -> Unit,
    onBrightnessSwipe  : (Float) -> Unit,
    onVolumeSwipe      : (Float) -> Unit,
    onSeekTo           : (Long) -> Unit,
    onPrevious         : () -> Unit,
    onTogglePlay       : () -> Unit,
    onNext             : () -> Unit,
    onExitFullscreen   : () -> Unit,
    onToggleQueue      : () -> Unit,
    onBack             : () -> Unit,
    onJumpToQueueIndex : (Int) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player        = exoPlayer
                    useController = false
                    resizeMode    = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams  = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Capa unificada de gestos ──────────────────────────────────────────
        // Detecta: swipe vertical (brillo/volumen) + tap simple + doble tap (seek)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Variables persistentes entre gestos dentro del mismo pointerInput
                    var lastTapTime = 0L
                    var lastTapX    = 0f

                    awaitEachGesture {
                        val down    = awaitFirstDown(requireUnconsumed = false)
                        val startX  = down.position.x
                        val downMs  = System.currentTimeMillis()
                        var totalDy = 0f
                        var didDrag = false

                        while (true) {
                            val event  = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break

                            val dy = change.position.y - change.previousPosition.y
                            totalDy += dy

                            if (abs(totalDy) > 18f) {
                                didDrag = true
                                change.consume()
                                // dy negativo = mover hacia arriba = aumentar
                                val delta = -dy / size.height.toFloat() * 2.2f
                                if (startX < size.width / 2f) onBrightnessSwipe(delta)
                                else onVolumeSwipe(delta)
                            }
                        }

                        if (!didDrag) {
                            val elapsed   = System.currentTimeMillis() - downMs
                            if (elapsed < 280L) {
                                val sinceLast = downMs - lastTapTime
                                if (sinceLast < 360L) {
                                    // Doble tap → seek
                                    if (lastTapX < size.width / 2f) onDoubleTapLeft()
                                    else onDoubleTapRight()
                                    lastTapTime = 0L
                                } else {
                                    // Tap simple → toggle controles
                                    lastTapTime = downMs
                                    lastTapX    = startX
                                    onToggleControls()
                                }
                            }
                        }
                    }
                }
        )

        // ── HUD de brillo (izquierda) ─────────────────────────────────────────
        AnimatedVisibility(
            visible  = showBrightnessHud,
            enter    = fadeIn(tween(150)),
            exit     = fadeOut(tween(400)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp)
                .zIndex(3f)
        ) {
            GestureHud(
                icon  = Icons.Filled.Brightness6,
                value = brightness,
                label = "${(brightness * 100).roundToInt()}%"
            )
        }

        // ── HUD de volumen (derecha) ──────────────────────────────────────────
        AnimatedVisibility(
            visible  = showVolumeHud,
            enter    = fadeIn(tween(150)),
            exit     = fadeOut(tween(400)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp)
                .zIndex(3f)
        ) {
            GestureHud(
                icon  = if (volumeFraction < 0.01f) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                value = volumeFraction,
                label = "${(volumeFraction * 100).roundToInt()}%"
            )
        }

        // ── HUD de seek ───────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = seekFeedbackMs != 0L,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(3f)
        ) {
            SeekFeedbackHud(seekFeedbackMs)
        }

        // ── Overlay de controles (se oculta con tap) ──────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter   = fadeIn(tween(220)),
            exit    = fadeOut(tween(220))
        ) {
            LandscapeControls(
                modifier           = Modifier.fillMaxSize().zIndex(2f),
                currentItem        = currentItem,
                queueCount         = queue.size,
                queueFromCurrent   = queueFromCurrent,
                queueStartIndex    = queueStartIndex,
                isPlaying          = isPlaying,
                positionMs         = positionMs,
                durationMs         = durationMs,
                onSeekTo           = onSeekTo,
                onPrevious         = onPrevious,
                onTogglePlay       = onTogglePlay,
                onNext             = onNext,
                onExitFullscreen   = onExitFullscreen,
                onToggleQueue      = onToggleQueue,
                onBack             = onBack,
                onJumpToQueueIndex = onJumpToQueueIndex
            )
        }

        // ── Panel de cola ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = showQueuePanel && queueFromCurrent.isNotEmpty(),
            enter    = expandVertically(),
            exit     = shrinkVertically(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(4f)
        ) {
            QueueDrawer(
                modifier    = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                items       = queueFromCurrent,
                startIndex  = queueStartIndex,
                onItemClick = onJumpToQueueIndex
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Controles internos de landscape
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LandscapeControls(
    modifier           : Modifier = Modifier,
    currentItem        : MediaEntry,
    queueCount         : Int,
    queueFromCurrent   : List<MediaEntry>,
    queueStartIndex    : Int,
    isPlaying          : Boolean,
    positionMs         : Long,
    durationMs         : Long,
    onSeekTo           : (Long) -> Unit,
    onPrevious         : () -> Unit,
    onTogglePlay       : () -> Unit,
    onNext             : () -> Unit,
    onExitFullscreen   : () -> Unit,
    onToggleQueue      : () -> Unit,
    onBack             : () -> Unit,
    onJumpToQueueIndex : (Int) -> Unit
) {
    Box(modifier = modifier) {

        // Gradiente superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Black.copy(0.82f), Color.Transparent))
                )
        )
        // Gradiente inferior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.88f)))
                )
        )

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NxIconBtn(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onClick = onBack)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    currentItem.title,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text  = buildString {
                        val folder = currentItem.folder?.takeIf { it.isNotBlank() }
                        if (folder != null) append("$folder  •  ")
                        append(currentItem.resolutionLabel)
                    },
                    color    = Color.White.copy(0.5f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            if (queueCount > 1) {
                NxIconBtn(Icons.AutoMirrored.Filled.PlaylistPlay, "Playlist", onClick = onToggleQueue)
            }
            NxIconBtn(Icons.Filled.FullscreenExit, "Salir", onClick = onExitFullscreen)
        }

        // ── Botón play/pause central ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(0.45f))
                .clickable(onClick = onTogglePlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        // ── Controles inferiores ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cola preview
            if (queueFromCurrent.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(queueFromCurrent.take(5)) { index, item ->
                        QueueItemCard(
                            item    = item,
                            index   = queueStartIndex + index,
                            onClick = { onJumpToQueueIndex(queueStartIndex + index) },
                            compact = true
                        )
                    }
                }
            }

            // Botones skip + seek
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                LscBtn(Icons.Filled.SkipPrevious, "Prev",  onClick = onPrevious)
                Spacer(Modifier.width(14.dp))
                LscBtn(Icons.Filled.Replay10,     "−10 s", onClick = {
                    onSeekTo((positionMs - 10_000L).coerceAtLeast(0L))
                })
                Spacer(Modifier.width(14.dp))
                LscBtn(Icons.Filled.Forward10,    "+10 s", onClick = {
                    onSeekTo((positionMs + 10_000L).coerceAtMost(durationMs))
                })
                Spacer(Modifier.width(14.dp))
                LscBtn(Icons.Filled.SkipNext,     "Next",  onClick = onNext)
            }

            // Seekbar + tiempos
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    formatDuration(positionMs),
                    color      = NxAccent,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                PlaybackSeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeekTo   = onSeekTo,
                    modifier   = Modifier.weight(1f)
                )
                Text(
                    formatDuration(durationMs),
                    color    = Color.White.copy(0.45f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COMPONENTES COMPARTIDOS
// ═════════════════════════════════════════════════════════════════════════════

/** Botón icono con fondo circular semitransparente */
@Composable
private fun NxIconBtn(
    icon  : ImageVector,
    label : String,
    size  : Dp = 40.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Control de reproducción en portrait (icono grande + tint blanco) */
@Composable
private fun NxControlBtn(
    icon     : ImageVector,
    label    : String,
    iconSize : Dp = 24.dp,
    onClick  : () -> Unit
) {
    IconButton(
        onClick  = onClick,
        modifier = Modifier.size(52.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

/** Botón circular en landscape */
@Composable
private fun LscBtn(
    icon   : ImageVector,
    label  : String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.11f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Slider de brillo / volumen para portrait */
@Composable
private fun NxSlider(
    icon    : ImageVector,
    label   : String,
    value   : Float,
    range   : ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint     = NxAccent,
            modifier = Modifier.size(19.dp)
        )
        Slider(
            value       = value,
            onValueChange = onChange,
            valueRange  = range,
            modifier    = Modifier.weight(1f),
            colors      = SliderDefaults.colors(
                thumbColor         = NxAccent,
                activeTrackColor   = NxAccent,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f)
            )
        )
        Text(
            "${(value / (range.endInclusive - range.start) * 100).coerceIn(0f, 100f).roundToInt()}%",
            color    = Color.White.copy(0.5f),
            fontSize = 12.sp,
            modifier = Modifier.width(34.dp)
        )
    }
}

/** HUD flotante de brillo / volumen en landscape (deslizando) */
@Composable
private fun GestureHud(
    icon  : ImageVector,
    value : Float,
    label : String
) {
    Surface(
        shape  = RoundedCornerShape(18.dp),
        color  = Color.Black.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, NxAccent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier              = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = NxAccent, modifier = Modifier.size(24.dp))
            // Barra vertical de nivel
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(value.coerceIn(0f, 1f))
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(3.dp))
                        .background(GradientAccent)
                )
            }
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Tag tipo "pill" para mostrar resolución u otros metadatos */
@Composable
private fun PillTag(text: String) {
    Text(
        text     = text,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NxAccent.copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        color    = NxAccent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}

/** Drawer de cola completa (modal) */
@Composable
private fun QueueDrawer(
    modifier   : Modifier = Modifier,
    items      : List<MediaEntry>,
    startIndex : Int,
    onItemClick: (Int) -> Unit
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(24.dp),
        color    = NxCard,
        border   = BorderStroke(1.dp, NxDivider)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = NxAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Siguiente reproducción", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Toca para saltar", color = Color.White.copy(0.45f), fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = NxDivider)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(items) { index, item ->
                    QueueItemCard(
                        item    = item,
                        index   = startIndex + index,
                        onClick = { onItemClick(startIndex + index) }
                    )
                }
            }
        }
    }
}

/** Tarjeta de video en la cola */
@Composable
private fun QueueItemCard(
    item    : MediaEntry,
    index   : Int,
    onClick : () -> Unit,
    compact : Boolean = false
) {
    val cardW  = if (compact) 138.dp else 158.dp
    val cardH  = if (compact) 100.dp else 116.dp
    val thumbH = if (compact) 58.dp  else 70.dp

    Card(
        onClick = onClick,
        shape   = RoundedCornerShape(14.dp),
        colors  = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.055f)),
        border  = BorderStroke(1.dp, NxDivider),
        modifier = Modifier.size(width = cardW, height = cardH)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.height(thumbH)) {
                MediaArtwork(item = item, modifier = Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.5f)))
                        )
                )
                Text(
                    "#${index + 1}",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(NxAccent.copy(0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color      = Color.Black,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier            = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    item.title,
                    color    = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatDuration(item.durationMs),
                    color    = Color.White.copy(0.45f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/** HUD central de seek (+10s / -10s) */
@Composable
private fun SeekFeedbackHud(deltaMs: Long, modifier: Modifier = Modifier) {
    val forward = deltaMs >= 0L
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(18.dp),
        color    = Color.Black.copy(alpha = 0.72f),
        border   = BorderStroke(1.dp, NxAccent.copy(0.35f))
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(
                if (forward) Icons.Filled.Forward10 else Icons.Filled.Replay10,
                contentDescription = null,
                tint     = NxAccent,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    if (forward) "+10 segundos" else "−10 segundos",
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp
                )
                Text(
                    if (forward) "Avanzando" else "Retrocediendo",
                    color    = Color.White.copy(0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
private fun Context.findActivity(): Activity? = when (this) {
    is Activity      -> this
    is ContextWrapper -> baseContext.findActivity()
    else             -> null
}
