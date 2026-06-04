
package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.playback.PlayerEngine
import com.nexora.player.ui.components.MediaArtwork
import com.nexora.player.ui.components.PlaybackSeekBar
import com.nexora.player.ui.components.PlayerControlsRow
import com.nexora.player.ui.components.PlayerMetadata
import com.nexora.player.ui.components.formatDuration
import kotlinx.coroutines.delay

@Composable
fun AudioPlayerScreen(
    modifier: Modifier = Modifier,
    current: MediaEntry?
) {
    val context = LocalContext.current
    val player = PlayerEngine.get(context)
    val snapshot by PlayerEngine.snapshot.collectAsState()
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(current?.id, snapshot.isPlaying) {
        if (current == null) return@LaunchedEffect
        while (true) {
            positionMs = player.currentPosition
            durationMs = player.duration.takeIf { it > 0L } ?: current.durationMs
            delay(500)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (current == null) {
            Text("No hay contenido en reproducción", style = MaterialTheme.typography.headlineSmall)
            return@Column
        }

        MediaArtwork(
            item = current,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        )

        PlayerMetadata(
            title = current.title,
            subtitle = listOfNotNull(
                current.artist.takeIf { it.isNotBlank() },
                current.album.takeIf { it.isNotBlank() }
            ).joinToString(" • "),
            trailingLabel = formatDuration(current.durationMs)
        )

        PlaybackSeekBar(
            positionMs = positionMs,
            durationMs = durationMs,
            onSeekTo = { player.seekTo(it) }
        )

        PlayerControlsRow(
            isPlaying = snapshot.isPlaying,
            onPrevious = { PlayerEngine.skipPrevious(context) },
            onTogglePlay = { PlayerEngine.togglePlayPause(context) },
            onNext = { PlayerEngine.skipNext(context) }
        )

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vista expandida de audio", style = MaterialTheme.typography.titleMedium)
                Text("La portada y los controles quedan al centro para una lectura más limpia y premium.")
            }
        }
    }
}
