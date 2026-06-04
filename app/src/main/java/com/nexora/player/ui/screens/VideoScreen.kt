package com.nexora.player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.SortMode
import com.nexora.player.ui.components.MediaArtwork
import com.nexora.player.ui.components.PremiumHeroCard
import com.nexora.player.ui.components.PremiumSectionHeader
import com.nexora.player.ui.components.SortSelector
import com.nexora.player.ui.components.StatPill
import com.nexora.player.ui.components.formatDuration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoScreen(
    modifier: Modifier = Modifier,
    items: List<MediaEntry>,
    sortMode: SortMode,
    onPlay: (List<MediaEntry>, MediaEntry) -> Unit,
    onRefresh: () -> Unit,
    onSortSelected: (SortMode) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        PremiumHeroCard(
            title = "Videoteca",
            subtitle = "Portadas automáticas, tarjetas cinemáticas y acceso rápido a cada clip.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            StatPill("${items.size} videos")
        }

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
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
                    SortMode.SIZE_ASC,
                    SortMode.SIZE_DESC,
                    SortMode.RESOLUTION_ASC,
                    SortMode.RESOLUTION_DESC
                ),
                onSelected = onSortSelected
            )
        }

        PremiumSectionHeader(
            title = "Videos",
            subtitle = "Diseño de tarjetas con miniatura y metadatos",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 165.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.id }) { item ->
                androidx.compose.material3.Card(
                    onClick = { onPlay(items, item) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MediaArtwork(
                            entry = item,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatPill(item.resolutionLabel)
                                StatPill(formatDuration(item.durationMs))
                            }
                        }
                    }
                }
            }
        }
    }
}
