package com.nexora.player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.player.R
import com.nexora.player.data.local.PlaybackHistoryEntity
import com.nexora.player.data.local.PlaylistEntity
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.SortMode
import com.nexora.player.ui.components.MediaArtwork
import com.nexora.player.ui.components.MediaItemRow
import com.nexora.player.ui.components.SortSelector
import com.nexora.player.ui.components.formatDuration

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    modifier: Modifier = Modifier,
    items: List<MediaEntry>,
    favorites: Set<Long>,
    playlists: List<PlaylistEntity>,
    history: List<PlaybackHistoryEntity>,
    sortMode: SortMode,
    hiddenAudioCount: Int,
    onPlay: (List<MediaEntry>, MediaEntry) -> Unit,
    onToggleFavorite: (MediaEntry) -> Unit,
    onAddToPlaylist: (PlaylistEntity, MediaEntry) -> Unit,
    onHideFromLibrary: (MediaEntry) -> Unit,
    onDeleteFromLibrary: (MediaEntry) -> Unit,
    onRefresh: () -> Unit,
    onSortSelected: (SortMode) -> Unit
) {
    var selectedItem by remember { mutableStateOf<MediaEntry?>(null) }
    var pendingHideItem by remember { mutableStateOf<MediaEntry?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<MediaEntry?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val visibleCount = items.size
    val recentItems = remember(history) { history.take(6) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.music_library_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.music_library_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = onRefresh,
                            label = { Text(stringResource(R.string.refresh)) }
                        )
                        SortSelector(
                            selected = sortMode,
                            options = listOf(
                                SortMode.DATE_ADDED_DESC,
                                SortMode.DATE_ADDED_ASC,
                                SortMode.TITLE_ASC,
                                SortMode.TITLE_DESC,
                                SortMode.DURATION_ASC,
                                SortMode.DURATION_DESC,
                                SortMode.ARTIST_ASC,
                                SortMode.ALBUM_ASC
                            ),
                            onSelected = onSortSelected
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        AssistChip(onClick = {}, label = { Text(localizedUiText("${visibleCount} visibles", "${visibleCount} visible")) })
                        AssistChip(onClick = {}, label = { Text(localizedUiText("${hiddenAudioCount} ocultas", "${hiddenAudioCount} hidden")) })
                        AssistChip(onClick = {}, label = { Text(localizedUiText("${playlists.size} listas", "${playlists.size} playlists")) })
                    }
                }
            }
        }

        if (recentItems.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = localizedUiText("Continuar escuchando", "Continue listening"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recentItems, key = { it.id }) { entry ->
                            val media = entry.toMediaEntry()
                            ElevatedCard(
                                onClick = { onPlay(items, media) },
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(190.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MediaArtwork(
                                        item = media,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                    )
                                    Text(media.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        media.artist.ifBlank { stringResource(R.string.audio_now_playing) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (items.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(R.string.no_visible_music),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.hidden_tracks_restore_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                MediaItemRow(
                    item = item,
                    isFavorite = favorites.contains(item.id),
                    onClick = { onPlay(items, item) },
                    onFavoriteClick = { onToggleFavorite(item) },
                    onMoreClick = { selectedItem = item }
                )
            }
        }
    }

    selectedItem?.let { item ->
        val isFavorite = favorites.contains(item.id)
        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MediaArtwork(
                        item = item,
                        modifier = Modifier
                            .width(72.dp)
                            .height(72.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            listOfNotNull(
                                item.artist.takeIf { it.isNotBlank() },
                                item.album.takeIf { it.isNotBlank() },
                                formatDuration(item.durationMs).takeIf { it.isNotBlank() }
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider()

                SheetAction(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    label = if (isFavorite) stringResource(R.string.media_favorite_remove) else stringResource(R.string.media_favorite_add),
                    onClick = {
                        onToggleFavorite(item)
                        selectedItem = null
                    }
                )

                SheetAction(
                    icon = Icons.Filled.PlaylistAdd,
                    label = stringResource(R.string.add_to_playlist),
                    onClick = {
                        selectedItem = null
                    }
                )

                SheetAction(
                    icon = Icons.Filled.VisibilityOff,
                    label = stringResource(R.string.hide_from_library),
                    onClick = {
                        pendingHideItem = item
                        selectedItem = null
                    }
                )

                SheetAction(
                    icon = Icons.Filled.Album,
                    label = localizedUiText("Ir al álbum", "Go to album"),
                    onClick = { selectedItem = null }
                )

                SheetAction(
                    icon = Icons.Filled.PersonOutline,
                    label = localizedUiText("Ver artista", "View artist"),
                    onClick = { selectedItem = null }
                )

                SheetAction(
                    icon = Icons.Filled.Info,
                    label = localizedUiText("Información", "Information"),
                    onClick = { selectedItem = null }
                )

                SheetAction(
                    icon = Icons.Filled.DeleteOutline,
                    label = localizedUiText("Eliminar", "Delete"),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        pendingDeleteItem = item
                        selectedItem = null
                    }
                )

                Spacer(Modifier.height(6.dp))

                TextButton(
                    onClick = { selectedItem = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }

    pendingHideItem?.let { item ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingHideItem = null },
            title = { Text(localizedUiText("Ocultar canción", "Hide song")) },
            text = { Text(localizedUiText("¿Quieres ocultar “${item.title}” de la biblioteca?", "Hide “${item.title}” from the library?")) },
            confirmButton = {
                TextButton(onClick = {
                    onHideFromLibrary(item)
                    pendingHideItem = null
                }) {
                    Text(localizedUiText("Ocultar", "Hide"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingHideItem = null }) {
                    Text(localizedUiText("No ocultar", "Keep visible"))
                }
            }
        )
    }

    pendingDeleteItem?.let { item ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            title = { Text(localizedUiText("Eliminar canción", "Delete song")) },
            text = { Text(localizedUiText("¿Quieres eliminar “${item.title}” del dispositivo?", "Delete “${item.title}” from the device?")) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFromLibrary(item)
                    pendingDeleteItem = null
                }) {
                    Text(localizedUiText("Eliminar", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItem = null }) {
                    Text(localizedUiText("Cancelar", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = tint,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun localizedUiText(es: String, en: String): String {
    val language = LocalContext.current.resources.configuration.locales[0]?.language.orEmpty().lowercase()
    return if (language.startsWith("en")) en else es
}

private fun PlaybackHistoryEntity.toMediaEntry(): MediaEntry {
    return MediaEntry(
        id = mediaId,
        kind = if (mediaKind == "VIDEO") com.nexora.player.data.model.MediaKind.VIDEO else com.nexora.player.data.model.MediaKind.AUDIO,
        uri = android.net.Uri.parse(uriString),
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs
    )
}
