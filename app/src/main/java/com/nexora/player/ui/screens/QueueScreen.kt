package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.ui.components.MediaItemRow
import com.nexora.player.ui.components.PremiumHeroCard
import com.nexora.player.ui.components.StatPill

@Composable
fun QueueScreen(
    modifier: Modifier = Modifier,
    queue: List<MediaEntry>,
    currentIndex: Int,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearQueue: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        PremiumHeroCard(
            title = "Cola de reproducción",
            subtitle = if (queue.isEmpty()) "Todavía no hay elementos en cola" else "${queue.size} elementos preparados",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            StatPill("${queue.size}")
        }

        if (queue.isEmpty()) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text("No hay elementos en la cola todavía.", style = MaterialTheme.typography.bodyLarge)
            }
            return
        }

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AssistChip(onClick = {}, label = { Text("Siguiente: ${queue.getOrNull(currentIndex + 1)?.title ?: "—"}") })
            TextButton(onClick = onClearQueue) { Text("Vaciar cola") }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
                ElevatedCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        if (index == currentIndex) {
                            AssistChip(onClick = { onPlayItem(index) }, label = { Text("Reproduciendo") })
                        }
                        MediaItemRow(
                            item = item,
                            onClick = { onPlayItem(index) }
                        )
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onPlayItem(index) }) { Text("Ir a") }
                            TextButton(onClick = { onRemoveItem(index) }) { Text("Quitar") }
                        }
                    }
                }
            }
        }
    }
}
