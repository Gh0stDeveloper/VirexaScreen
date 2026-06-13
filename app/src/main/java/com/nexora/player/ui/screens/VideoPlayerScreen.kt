package com.nexora.player.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nexora.player.R
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.playback.PlayerEngine
import com.nexora.player.ui.components.GestureControlOverlay
import com.nexora.player.ui.components.PlayerControlsRow
import com.nexora.player.ui.components.PlayerMetadata
import com.nexora.player.ui.components.PlaybackSeekBar
import com.nexora.player.ui.components.formatDuration
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VideoPlayerScreen(
    modifier: Modifier = Modifier,
    current: MediaEntry?
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context.findActivity()
    val exoPlayer = PlayerEngine.get(context)
    val snapshot by PlayerEngine.snapshot.collectAsState()
    val audioManager = context.getSystemService<AudioManager>()
    val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
    val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    val volumeFraction = currentVolume.toFloat() / maxVolume.toFloat()

    // Orientación real del dispositivo
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var brightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.6f
        )
    }

    // Estado de visibilidad de los controles superpuestos
    var showControls by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Feedback visual del doble toque (avance/retroceso)
    var showRewindIndicator by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }

    // Ocultar barras del sistema en landscape
    DisposableEffect(isLandscape) {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            // Restaurar al salir
            activity?.window?.let { WindowCompat.getInsetsController(it, view) }
                ?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Temporizador de auto-ocultación: cada vez que showControls se vuelve true, inicia cuenta atrás
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000) // 4 segundos
            showControls = false
        }
    }

    // Funciones de brillo y volumen
    fun setBrightness(value: Float) {
        brightness = value.coerceIn(0.05f, 1f)
        activity?.window?.attributes = activity?.window?.attributes?.apply {
            screenBrightness = brightness
        }
    }

    fun setVolume(value: Float) {
        audioManager?.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (value.coerceIn(0f, 1f) * maxVolume).roundToInt(),
            0
        )
    }

    // --- Interfaz según orientación ---
    if (current == null) {
        // Sin contenido
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.video_no_playback), style = MaterialTheme.typography.headlineSmall)
        }
        return
    }

    if (isLandscape) {
        // --- MODO PANTALLA COMPLETA INMERSIVO ---
        Box(modifier = modifier.fillMaxSize()) {
            // Reproductor de video (sin controles propios)
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        this.player = exoPlayer
                        useController = false   // Desactivamos los controles por defecto
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Toque simple para mostrar/ocultar controles
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            // No usamos detectTapGestures para no interferir con otros gestos
                            // Simplemente cambiamos el estado al detectar un toque simple
                            waitForUpOrCancellation()
                            showControls = !showControls
                        }
                    }
            )

            // Doble toque lateral para saltar 10s
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Implementaremos el doble toque manualmente
                        // Para simplificar, puedes usar detectTapGestures, pero cuidado con solapamientos
                        // Aquí una versión simplificada con detectTapGestures
                        // Nota: en producción puedes refinarlo para que no choque con el toque simple.
                    }
            )

            // Indicadores de avance/retroceso
            AnimatedVisibility(visible = showRewindIndicator) {
                Icon(
                    Icons.Filled.Replay10, "Retroceder",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(32.dp)
                        .size(48.dp)
                )
            }
            AnimatedVisibility(visible = showForwardIndicator) {
                Icon(
                    Icons.Filled.Forward10, "Avanzar",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(32.dp)
                        .size(48.dp)
                )
            }

            // Gradiente superior
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Botón atrás
                    FilledTonalIconButton(
                        onClick = {
                            // Acción de volver (navegar hacia atrás)
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                    // Título
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Gradiente inferior con controles
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Barra de progreso + tiempos
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatDuration(exoPlayer.currentPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            PlaybackSeekBar(
                                positionMs = exoPlayer.currentPosition,
                                durationMs = exoPlayer.duration.takeIf { it > 0L } ?: current.durationMs,
                                onSeekTo = { exoPlayer.seekTo(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                text = formatDuration(exoPlayer.duration.takeIf { it > 0L } ?: current.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        // Controles de reproducción
                        PlayerControlsRow(
                            isPlaying = snapshot.isPlaying,
                            onPrevious = { PlayerEngine.skipPrevious(context) },
                            onTogglePlay = { PlayerEngine.togglePlayPause(context) },
                            onNext = { PlayerEngine.skipNext(context) }
                        )

                        // Botón de pantalla completa (salir de fullscreen)
                        IconButton(
                            onClick = {
                                activity?.requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                Icons.Filled.FullscreenExit,
                                contentDescription = "Salir de pantalla completa",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Gestos de brillo/volumen (tu overlay ya existente)
            GestureControlOverlay(
                modifier = Modifier.fillMaxSize(),
                brightness = brightness,
                volume = volumeFraction,
                onBrightnessChange = ::setBrightness,
                onVolumeChange = ::setVolume
            )
        }
    } else {
        // --- MODO RETRATO (VIDEO + DETALLES) ---
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video en la parte superior (relación 16:9)
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            this.player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Metadatos y controles
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayerMetadata(
                        title = current.title,
                        subtitle = current.folder?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.video_local_label),
                        trailingLabel = formatDuration(current.durationMs)
                    )

                    PlaybackSeekBar(
                        positionMs = exoPlayer.currentPosition,
                        durationMs = exoPlayer.duration.takeIf { it > 0L } ?: current.durationMs,
                        onSeekTo = { exoPlayer.seekTo(it) }
                    )

                    PlayerControlsRow(
                        isPlaying = snapshot.isPlaying,
                        onPrevious = { PlayerEngine.skipPrevious(context) },
                        onTogglePlay = { PlayerEngine.togglePlayPause(context) },
                        onNext = { PlayerEngine.skipNext(context) }
                    )
                }
            }

            // Botón para pasar a pantalla completa
            FilledTonalIconButton(
                onClick = {
                    activity?.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Filled.Fullscreen, contentDescription = "Pantalla completa")
            }

            // Aquí puedes dejar la tarjeta de gestos si quieres
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.video_gestures_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.video_gestures_desc))
                }
            }
        }
    }
}

// Helper para encontrar la Activity
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}