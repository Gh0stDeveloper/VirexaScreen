
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
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.local.PlaylistEntity
import com.nexora.player.data.local.PlaylistItemEntity
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.ui.components.formatDuration

@Composable
fun PlaylistDetailScreen(
    modifier: Modifier = Modifier,
    playlist: PlaylistEntity,
    playlistItems: List<PlaylistItemEntity>,
    availableSongs: List<MediaEntry>,
    onBack: () -> Unit,
    onPlayItem: (PlaylistItemEntity) -> Unit,
    onRemoveItem: (PlaylistItemEntity) -> Unit,
    onAddSong: (MediaEntry) -> Unit
) {
    val existingIds = playlistItems.map { it.mediaId }.toSet()
    val candidates = availableSongs.filterNot { existingIds.contains(it.id) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Volver")
            }
        }

        Text(
            text = playlist.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Canciones en la playlist",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        text = "Aún no hay canciones en esta playlist.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(items, key = { it.id }) { item ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onPlayItem(item) }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOf(item.artist, item.album).filter { it.isNotBlank() }.joinToString(" • ")
                            )
                            Text(formatDuration(item.durationMs), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { onRemoveItem(item) }) {
                                Text("Quitar de la playlist")
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            }

            item {
                Text(
                    text = "Añadir canciones",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (candidates.isEmpty()) {
                item {
                    Text(
                        text = "No hay más canciones disponibles para agregar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(candidates, key = { it.id }) { song ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(song.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOf(song.artist, song.album).filter { it.isNotBlank() }.joinToString(" • ")
                            )
                            Text(formatDuration(song.durationMs), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { onAddSong(song) }) {
                                Text("Agregar")
                            }
                        }
                    }
                }
            }
        }
    }
}
