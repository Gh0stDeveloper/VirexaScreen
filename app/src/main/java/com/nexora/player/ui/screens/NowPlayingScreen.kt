package com.nexora.player.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.nexora.player.data.model.MediaKind
import com.nexora.player.playback.PlayerEngine
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(context) { PlayerEngine.get(context) }
    val snapshot by PlayerEngine.snapshot.collectAsStateWithLifecycle()
    val current = snapshot.currentItem
    val progress by rememberPlaybackProgress(player, current?.id)

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Reproducción", style = MaterialTheme.typography.titleLarge)

        if (current?.kind == MediaKind.VIDEO) {
            Card(
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            ) {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            this.player = player
                            useController = false
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            MediaArtwork(
                entry = current,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(current?.title ?: "Nada en reproducción", style = MaterialTheme.typography.headlineSmall)
            Text(
                current?.artist?.ifBlank { current.album } ?: "Selecciona una pista o un video",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Slider(
                    value = progress.positionMs.toFloat(),
                    onValueChange = { player.seekTo(it.toLong()) },
                    valueRange = 0f..progress.durationMs.coerceAtLeast(1L).toFloat()
                )
                androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(progress.positionMs))
                    Text(formatTime(progress.durationMs))
                }
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { PlayerEngine.skipPrevious(context) }) { Icon(Icons.Filled.FastRewind, contentDescription = "Anterior") }
                    IconButton(onClick = { PlayerEngine.togglePlayPause(context) }, modifier = Modifier.height(72.dp)) {
                        Icon(
                            imageVector = if (snapshot.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = { PlayerEngine.skipNext(context) }) { Icon(Icons.Filled.FastForward, contentDescription = "Siguiente") }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cola actual", style = MaterialTheme.typography.titleMedium)
                Text("${snapshot.queue.size} elementos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Controles multimedia activos también en notificación y pantalla bloqueada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private data class PlaybackProgress(val positionMs: Long, val durationMs: Long)

@Composable
private fun rememberPlaybackProgress(player: androidx.media3.exoplayer.ExoPlayer, key: Any?): androidx.compose.runtime.State<PlaybackProgress> {
    return produceState(initialValue = PlaybackProgress(0L, 0L), key1 = player, key2 = key) {
        while (true) {
            val duration = player.duration.takeIf { it > 0 } ?: 0L
            value = PlaybackProgress(player.currentPosition.coerceAtLeast(0L), duration)
            delay(250)
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
