package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.local.PlaybackHistoryEntity
import com.nexora.player.ui.components.formatDuration
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    history: List<PlaybackHistoryEntity>
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Historial",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history, key = { it.id }) { item ->
                ElevatedCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(listOf(item.artist, item.album).filter { it.isNotBlank() }.joinToString(" • "))
                        Text(formatDuration(item.durationMs), style = MaterialTheme.typography.bodySmall)
                        Text(DateFormat.getDateTimeInstance().format(Date(item.playedAt)), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
