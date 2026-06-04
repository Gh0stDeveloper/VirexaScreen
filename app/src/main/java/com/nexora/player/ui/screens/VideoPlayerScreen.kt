
package com.nexora.player.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.media3.ui.PlayerView
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.playback.PlayerEngine
import com.nexora.player.ui.components.GestureControlOverlay
import com.nexora.player.ui.components.MediaArtwork
import com.nexora.player.ui.components.PlayerControlsRow
import com.nexora.player.ui.components.PlayerMetadata
import com.nexora.player.ui.components.formatDuration
import kotlin.math.roundToInt

@Composable
fun VideoPlayerScreen(
    modifier: Modifier = Modifier,
    current: MediaEntry?
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val player = PlayerEngine.get(context)
    val snapshot by PlayerEngine.snapshot.collectAsState()
    val audioManager = context.getSystemService<AudioManager>()
    val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
    val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    val volumeFraction = currentVolume.toFloat() / maxVolume.toFloat()

    var isLandscape by remember { mutableStateOf(false) }
    var brightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
        )
    }

    fun applyOrientation() {
        activity?.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

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

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (current == null) {
            Text("No hay video en reproducción", style = MaterialTheme.typography.headlineSmall)
            return@Column
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        this.player = player
                        useController = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            GestureControlOverlay(
                modifier = Modifier.fillMaxSize(),
                brightness = brightness,
                volume = volumeFraction,
                onBrightnessChange = ::setBrightness,
                onVolumeChange = ::setVolume
            )

            FilledTonalIconButton(
                onClick = {
                    isLandscape = !isLandscape
                    applyOrientation()
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                Icon(
                    imageVector = if (isLandscape) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = null
                )
            }
        }

        MediaArtwork(
            item = current,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        PlayerMetadata(
            title = current.title,
            subtitle = current.folder?.takeIf { it.isNotBlank() } ?: "Video local",
            trailingLabel = formatDuration(current.durationMs)
        )

        PlayerControlsRow(
            isPlaying = snapshot.isPlaying,
            onPrevious = { PlayerEngine.skipPrevious(context) },
            onTogglePlay = { PlayerEngine.togglePlayPause(context) },
            onNext = { PlayerEngine.skipNext(context) }
        )

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Controles de video", style = MaterialTheme.typography.titleMedium)
                Text("Brillo en la mitad izquierda, volumen en la derecha y botón de orientación en la parte superior.")
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
