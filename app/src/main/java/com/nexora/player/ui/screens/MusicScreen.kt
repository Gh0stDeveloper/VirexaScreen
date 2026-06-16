package com.nexora.player.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.nexora.player.R
import com.nexora.player.data.local.PlaylistEntity
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.SortMode
import com.nexora.player.ui.components.MediaItemRow
import com.nexora.player.ui.components.SortSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    modifier: Modifier = Modifier,
    items: List<MediaEntry>,
    favorites: Set<Long>,
    playlists: List<PlaylistEntity>,
    sortMode: SortMode,
    onPlay: (List<MediaEntry>, MediaEntry) -> Unit,
    onToggleFavorite: (MediaEntry) -> Unit,
    onAddToPlaylist: (PlaylistEntity, MediaEntry) -> Unit,
    onHideFromLibrary: (MediaEntry) -> Unit,
    onDeleteFromLibrary: (MediaEntry) -> Unit,
    onRefresh: () -> Unit,
    onSortSelected: (SortMode) -> Unit
) {
    var selectedItem by remember { mutableStateOf<MediaEntry?>(null) }
    var deleteCandidate by remember { mutableStateOf<MediaEntry?>(null) }
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val item = deleteCandidate
        if (result.resultCode == Activity.RESULT_OK && item != null) {
            onDeleteFromLibrary(item)
        }
        deleteCandidate = null
        selectedItem = null
    }

    Column(modifier = modifier.fillMaxSize()) {
        ElevatedCard(
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.music_library_title), style = MaterialTheme.typography.headlineSmall)
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
            }
        }

        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.no_visible_music), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.hidden_tracks_restore_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
    }

    selectedItem?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ElevatedCard {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = listOfNotNull(
                                item.artist.takeIf { it.isNotBlank() },
                                item.album.takeIf { it.isNotBlank() }
                            ).joinToString(" • ").ifBlank { stringResource(R.string.app_name) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            onToggleFavorite(item)
                            selectedItem = null
                        },
                        label = {
                            Text(
                                if (favorites.contains(item.id)) {
                                    stringResource(R.string.media_favorite_remove)
                                } else {
                                    stringResource(R.string.media_favorite_add)
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (favorites.contains(item.id)) androidx.compose.material.icons.Icons.Filled.Favorite else androidx.compose.material.icons.Icons.Filled.FavoriteBorder,
                                contentDescription = null
                            )
                        }
                    )
                    AssistChip(
                        onClick = {
                            onHideFromLibrary(item)
                            selectedItem = null
                        },
                        label = { Text(stringResource(R.string.hide_from_library)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.VisibilityOff, contentDescription = null)
                        }
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Acciones rápidas",
                    style = MaterialTheme.typography.titleMedium
                )

                if (playlists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_playlists_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    playlists.forEach { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name) },
                            supportingContent = { Text(stringResource(R.string.add_to_playlist)) },
                            leadingContent = {
                                Icon(
                                    imageVector = PlaylistPlay,
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    onAddToPlaylist(playlist, item)
                                    selectedItem = null
                                }) {
                                    Icon(imageVector = Icons.Filled.PlaylistAdd, contentDescription = null)
                                }
                            }
                        )
                    }
                }

                ListItem(
                    headlineContent = { Text("Información de la canción") },
                    supportingContent = {
                        Text(
                            text = listOfNotNull(
                                item.artist.takeIf { it.isNotBlank() },
                                item.album.takeIf { it.isNotBlank() },
                                item.folder?.takeIf { it.isNotBlank() }
                            ).joinToString(" • ").ifBlank { "Sin metadatos adicionales" }
                        )
                    },
                    leadingContent = {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = null)
                    }
                )

                ListItem(
                    headlineContent = { Text("Eliminar del dispositivo") },
                    supportingContent = {
                        Text("Se borrará el archivo y se limpiarán sus rastros en la app.")
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    trailingContent = {
                        Button(
                            onClick = { deleteCandidate = item },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Borrar")
                        }
                    }
                )

                TextButton(
                    onClick = { selectedItem = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.close))
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    deleteCandidate?.let { item ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Eliminar \"${item.title}\"") },
            text = {
                Text("Esto quitará el archivo del dispositivo y actualizará la biblioteca.")
            },
            confirmButton = {
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        runCatching {
                            val request = MediaStore.createDeleteRequest(context.contentResolver, listOf(item.uri))
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(request.intentSender).build()
                            )
                        }.onFailure {
                            onDeleteFromLibrary(item)
                            deleteCandidate = null
                            selectedItem = null
                        }
                    } else {
                        onDeleteFromLibrary(item)
                        deleteCandidate = null
                        selectedItem = null
                    }
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
