package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.SortMode
import com.nexora.player.ui.components.MediaItemRow
import com.nexora.player.ui.components.PremiumHeroCard
import com.nexora.player.ui.components.PremiumSectionHeader
import com.nexora.player.ui.components.SortSelector
import com.nexora.player.ui.components.StatPill

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
        PremiumHeroCard(
            title = "Tu biblioteca de audio",
            subtitle = "Portadas, cola, favoritos y reproducción continua en una vista más limpia y premium.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            StatPill("${items.size} pistas")
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = onRefresh,
                label = { Text("Actualizar") },
                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) }
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

        PremiumSectionHeader(
            title = "Canciones",
            subtitle = "Toca una portada para abrir la reproducción",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
