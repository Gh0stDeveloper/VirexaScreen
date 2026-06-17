package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.player.data.local.PlaylistEntity
import com.nexora.player.data.local.PlaylistItemEntity
import com.nexora.player.ui.components.MediaArtwork
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.Flow

@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    playlists: List<PlaylistEntity>,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onOpenPlaylist: (PlaylistEntity) -> Unit,
    playlistPreviewItems: (Long) -> Flow<List<PlaylistItemEntity>>
) {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Button(onClick = { showDialog = true }) {
                Text("Nueva lista")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(playlists, key = { it.id }) { playlist ->
                val preview by playlistPreviewItems(playlist.id)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenPlaylist(playlist) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.widthIn(max = 210.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(playlist.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                DateFormat.getDateInstance().format(Date(playlist.createdAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (preview.isEmpty()) "Sin canciones aún" else "Vista previa disponible",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f, fill = true))

                        PlaylistPreviewMosaic(
                            items = preview,
                            modifier = Modifier.size(112.dp)
                        )

                        FilledTonalIconButton(
                            onClick = { onDeletePlaylist(playlist) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Crear lista") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        onCreatePlaylist(name.trim())
                    }
                    name = ""
                    showDialog = false
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PlaylistPreviewMosaic(
    items: List<PlaylistItemEntity>,
    modifier: Modifier = Modifier
) {
    val preview = items.take(4)

    ElevatedCard(
        modifier = modifier.clip(RoundedCornerShape(22.dp))
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

private fun PlaylistItemEntity.toMediaEntry() = com.nexora.player.data.model.MediaEntry(
    id = mediaId,
    kind = if (mediaKind == com.nexora.player.data.model.MediaKind.VIDEO.name) {
        com.nexora.player.data.model.MediaKind.VIDEO
    } else {
        com.nexora.player.data.model.MediaKind.AUDIO
    },
    uri = android.net.Uri.parse(uriString),
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs
)
