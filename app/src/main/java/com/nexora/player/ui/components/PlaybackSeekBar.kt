
package com.nexora.player.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PlaybackSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val fraction = (positionMs.coerceIn(0L, safeDuration).toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Slider(
            value = fraction,
            onValueChange = { onSeekTo((safeDuration * it).toLong()) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(positionMs), style = MaterialTheme.typography.labelMedium)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelMedium)
        }
    }
}
