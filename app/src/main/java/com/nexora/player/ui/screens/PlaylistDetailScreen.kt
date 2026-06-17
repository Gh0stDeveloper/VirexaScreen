package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.player.data.local.PlaylistEntity
import com.nexora.player.data.local.PlaylistItemEntity
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.MediaKind
import com.nexora.player.ui.components.MediaArtwork
import com.nexora.player.ui.components.MediaItemRow
import com.nexora.player.ui.components.formatDuration

@Composable
fun PlaylistDetailScreen(
    modifier: Modifier = Modifier,
    playlist: PlaylistEntity,
    playlistItems: List<PlaylistItemEntity>,
    availableSongs: List<MediaEntry>,
    onBack: () -> Unit,
    onPlayItem: (List<PlaylistItemEntity>, PlaylistItemEntity) -> Unit,
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

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaylistPreviewMosaic(
                    items = playlistItems,
                    modifier = Modifier.size(120.dp)
                )

                Column(
                    modifier = Modifier.widthIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlistItems.size} canciones",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Solo se reproduce lo que está dentro de esta lista.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Canciones de la playlist",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (playlistItems.isNotEmpty()) {
                        TextButton(onClick = { onPlayItem(playlistItems, playlistItems.first()) }) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("Reproducir")
                        }
                    }
                }
            }

            if (playlistItems.isEmpty()) {
                item {
                    Text(
                        text = "Aún no hay canciones en esta playlist.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(items = playlistItems, key = { it.id }) { item ->
                    PlaylistTrackCard(
                        item = item,
                        onPlay = { onPlayItem(playlistItems, item) },
                        onRemove = { onRemoveItem(item) }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            }

            item {
                Text(
                    text = "Añadir canciones",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (candidates.isEmpty()) {
                item {
                    Text(
                        text = "No hay más canciones disponibles para agregar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(items = candidates, key = { it.id }) { song ->
                    MediaItemRow(
                        item = song,
                        onClick = { onAddSong(song) }
                    )
                    Button(onClick = { onAddSong(song) }) {
                        Text("Agregar a la playlist")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistTrackCard(
    item: PlaylistItemEntity,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPlay
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaArtwork(
                item = item.toMediaEntry(),
                modifier = Modifier.size(72.dp)
            )

            Column(
                modifier = Modifier.widthIn(max = 230.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(item.artist, item.album)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDuration(item.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))

            FilledTonalIconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Quitar de la playlist")
            }
        }
    }
}

@Composable
private fun PlaylistPreviewMosaic(
    items: List<PlaylistItemEntity>,
    modifier: Modifier = Modifier
) {
    val preview = items.take(4)

    ElevatedCard(
        modifier = modifier.clip(RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(2) { row ->
                Row(modifier = Modifier.weight(1f)) {
                    repeat(2) { col ->
                        val index = row * 2 + col
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            if (index < preview.size) {
                                MediaArtwork(
                                    item = preview[index].toMediaEntry(),
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun PlaylistItemEntity.toMediaEntry() = MediaEntry(
    id = mediaId,
    kind = if (mediaKind == MediaKind.VIDEO.name) MediaKind.VIDEO else MediaKind.AUDIO,
    uri = android.net.Uri.parse(uriString),
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs
)
