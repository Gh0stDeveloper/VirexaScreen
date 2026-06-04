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
import com.nexora.player.data.local.PlaybackHistoryEntity
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.MediaKind
import com.nexora.player.ui.components.MediaItemRow
import com.nexora.player.ui.components.PremiumHeroCard
import com.nexora.player.ui.components.StatPill
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    history: List<PlaybackHistoryEntity>
) {
    Column(modifier = modifier.fillMaxSize()) {
        PremiumHeroCard(
            title = "Historial",
            subtitle = "Últimas reproducciones guardadas localmente.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            StatPill("${history.size}")
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(history, key = { it.id }) { item ->
                MediaItemRow(
                    item = MediaEntry(
                        id = item.mediaId,
                        kind = if (item.mediaKind == "VIDEO") MediaKind.VIDEO else MediaKind.AUDIO,
                        uri = android.net.Uri.parse(item.uriString),
                        title = item.title,
                        artist = item.artist,
                        album = item.album,
                        durationMs = item.durationMs
                    ),
                    onClick = { }
                )
            }
        }
    }
}
