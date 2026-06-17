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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.player.R
import com.nexora.player.data.local.FavoriteMediaEntity
import com.nexora.player.ui.components.MediaItemRow
import com.nexora.player.ui.components.formatDuration

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    favorites: List<FavoriteMediaEntity>,
    onPlayFavoriteQueue: (List<FavoriteMediaEntity>, FavoriteMediaEntity) -> Unit,
    onToggleFavorite: (FavoriteMediaEntity) -> Unit
) {
    val audioFavorites = favorites

    Column(modifier = modifier.fillMaxSize()) {
        ElevatedCard(
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Favoritos",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = if (audioFavorites.isEmpty()) {
                                stringResource(R.string.no_visible_music)
                            } else {
                                "Solo se reproducen las canciones guardadas aquí"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (audioFavorites.isNotEmpty()) {
                    val totalDuration = audioFavorites.sumOf { it.durationMs }
                    Text(
                        text = "${audioFavorites.size} canciones • ${formatDuration(totalDuration)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { onPlayFavoriteQueue(audioFavorites, audioFavorites.first()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Reproducir favoritos")
                    }
                }
            }
        }

        if (audioFavorites.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Aún no hay favoritos",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Marca canciones con el botón de favorito para verlas aquí y reproducir solo esa lista.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(audioFavorites, key = { it.id }) { favorite ->
                    MediaItemRow(
                        item = favorite.toMediaEntry(),
                        isFavorite = true,
                        onClick = { onPlayFavoriteQueue(audioFavorites, favorite) },
                        onFavoriteClick = { onToggleFavorite(favorite) }
                    )
                }
            }
        }
    }
}

private fun FavoriteMediaEntity.toMediaEntry() = com.nexora.player.data.model.MediaEntry(
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
