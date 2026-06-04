package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.local.FavoriteMediaEntity
import com.nexora.player.ui.components.MediaItemRow
import com.nexora.player.ui.components.PremiumHeroCard
import com.nexora.player.ui.components.StatPill

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    favorites: List<FavoriteMediaEntity>
) {
    Column(modifier = modifier.fillMaxSize()) {
        PremiumHeroCard(
            title = "Favoritos",
            subtitle = "Acceso rápido a tu contenido marcado.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            StatPill("${favorites.size}")
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(favorites, key = { it.id }) { favorite ->
                MediaItemRow(
                    item = com.nexora.player.data.model.MediaEntry(
                        id = favorite.mediaId,
                        kind = if (favorite.mediaKind == "VIDEO") com.nexora.player.data.model.MediaKind.VIDEO else com.nexora.player.data.model.MediaKind.AUDIO,
                        uri = android.net.Uri.parse(favorite.uriString),
                        title = favorite.title,
                        artist = favorite.artist,
                        album = favorite.album,
                        durationMs = favorite.durationMs
                    ),
                    onClick = { }
                )
            }
        }
    }
}
