package com.nexora.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.model.MediaEntry

@Composable
fun BottomPlayerBar(
    current: MediaEntry?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    if (current == null) return

    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaArtwork(entry = current, modifier = Modifier.size(48.dp), shape = RoundedCornerShape(14.dp))

            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(current.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                val subtitle = when {
                    current.artist.isNotBlank() -> current.artist
                    current.album.isNotBlank() -> current.album
                    else -> current.folder.orEmpty()
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }

            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.FastRewind, contentDescription = "Anterior")
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.FastForward, contentDescription = "Siguiente")
            }
        }
    }
}
