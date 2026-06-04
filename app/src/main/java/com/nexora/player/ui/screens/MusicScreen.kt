package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.SortMode
import com.nexora.player.ui.components.MediaItemRow
import com.nexora.player.ui.components.SortSelector

@Composable
fun MusicScreen(
    modifier: Modifier = Modifier,
    items: List<MediaEntry>,
    favorites: Set<Long>,
    sortMode: SortMode,
    onPlay: (List<MediaEntry>, MediaEntry) -> Unit,
    onToggleFavorite: (MediaEntry) -> Unit,
    onRefresh: () -> Unit,
    onSortSelected: (SortMode) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = onRefresh,
                label = { Text("Actualizar") }
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
                    onFavoriteClick = { onToggleFavorite(item) }
                )
            }
        }
    }
}
