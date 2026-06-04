
package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.ui.components.MediaItemRow

@Composable
fun SearchResultsScreen(
    modifier: Modifier = Modifier,
    query: String,
    audio: List<MediaEntry>,
    videos: List<MediaEntry>,
    onPlayAudio: (List<MediaEntry>, MediaEntry) -> Unit,
    onPlayVideo: (List<MediaEntry>, MediaEntry) -> Unit,
    onToggleFavorite: (MediaEntry) -> Unit,
    favoriteIds: Set<Long>
) {
    val total = audio.size + videos.size
    val summaryText = if (total == 0) {
        "No hay coincidencias para "$query""
    } else {
        "$total coincidencias encontradas"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resultados de búsqueda", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (audio.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AssistChip(onClick = {}, label = { Text("Audio / Audios (${audio.size})") })
                }
            }
            items(audio, key = { it.id }) { item ->
                MediaItemRow(
                    item = item,
                    isFavorite = favoriteIds.contains(item.id),
                    onClick = { onPlayAudio(audio, item) },
                    onFavoriteClick = { onToggleFavorite(item) }
                )
            }
        }

        if (audio.isNotEmpty() && videos.isNotEmpty()) {
            item { Spacer(Modifier.height(4.dp)); Divider() }
        }

        if (videos.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AssistChip(onClick = {}, label = { Text("Videos (${videos.size})") })
                }
            }
            items(videos, key = { it.id }) { item ->
                MediaItemRow(
                    item = item,
                    onClick = { onPlayVideo(videos, item) }
                )
            }
        }
    }
}
