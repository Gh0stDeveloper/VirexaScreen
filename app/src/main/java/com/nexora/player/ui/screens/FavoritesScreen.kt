package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.local.FavoriteMediaEntity
import com.nexora.player.ui.components.formatDuration

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    favorites: List<FavoriteMediaEntity>
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Favoritos",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(favorites, key = { it.id }) { favorite ->
                ElevatedCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(favorite.title, style = MaterialTheme.typography.titleMedium)
                        Text(listOf(favorite.artist, favorite.album).filter { it.isNotBlank() }.joinToString(" • "))
                        Text(formatDuration(favorite.durationMs), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
