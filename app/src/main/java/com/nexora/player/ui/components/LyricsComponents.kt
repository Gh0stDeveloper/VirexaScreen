package com.nexora.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.nexora.player.R
import com.nexora.player.data.lyrics.LyricLine
import com.nexora.player.data.lyrics.Lyrics
import com.nexora.player.data.model.MediaEntry
import androidx.compose.ui.res.stringResource as uiStringResource

@Composable
fun LyricsAndQueueCard(
    lyrics: Lyrics?,
    lyricsLoading: Boolean,
    positionMs: Long,
    queue: List<MediaEntry>,
    currentIndex: Int,
    onJumpToQueueItem: (Int) -> Unit,
    onSearchOnline: () -> Unit,
    onEditManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tabIndex by rememberSaveable(lyrics?.mediaId) { mutableIntStateOf(0) }
    val upNext = if (queue.isEmpty()) emptyList() else queue.drop(currentIndex + 1)

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    label = { Text(uiStringResource(R.string.lyrics_tab_lyrics)) }
                )
                FilterChip(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    label = { Text(uiStringResource(R.string.lyrics_tab_queue)) }
                )
            }

            if (tabIndex == 0) {
                LyricsView(
                    lyrics = lyrics,
                    lyricsLoading = lyricsLoading,
                    positionMs = positionMs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp),
                    onSearchOnline = onSearchOnline,
                    onEditManual = onEditManual
                )
            } else {
                QueuePreview(
                    upNext = upNext,
                    currentIndex = currentIndex,
                    onJumpToQueueItem = onJumpToQueueItem,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun LyricsView(
    lyrics: Lyrics?,
    lyricsLoading: Boolean,
    positionMs: Long,
    modifier: Modifier = Modifier,
    onSearchOnline: () -> Unit,
    onEditManual: () -> Unit
) {
    val listState = rememberLazyListState()
    val accent = Color(0xFF7C3AED)

    val currentIndex = remember(lyrics?.mediaId, positionMs, lyrics?.updatedAt) {
        lyrics?.currentLineIndex(positionMs) ?: -1
    }

    LaunchedEffect(currentIndex, lyrics?.lines?.size) {
        if (currentIndex >= 0 && lyrics?.lines?.isNotEmpty() == true) {
            val target = (currentIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(4.dp)
    ) {
        when {
            lyricsLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiStringResource(R.string.lyrics_loading),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }

            lyrics == null || lyrics.lines.isEmpty() -> {
                EmptyLyricsState(
                    modifier = Modifier.fillMaxWidth(),
                    onSearchOnline = onSearchOnline,
                    onEditManual = onEditManual
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = uiStringResource(R.string.lyrics_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.76f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    itemsIndexed(lyrics.lines) { index, line ->
                        val active = index == currentIndex
                        val past = index < currentIndex

                        val scale by animateFloatAsState(
                            targetValue = if (active) 1.06f else 1f,
                            label = "lyrics_scale"
                        )
                        val color = when {
                            active -> accent
                            past -> Color.White
                            else -> Color.White.copy(alpha = 0.42f)
                        }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (active) Color.White.copy(alpha = 0.06f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = line.asAnnotatedString(positionMs, active, accent),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = color,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLyricsState(
    modifier: Modifier = Modifier,
    onSearchOnline: () -> Unit,
    onEditManual: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.06f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = uiStringResource(R.string.lyrics_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = uiStringResource(R.string.lyrics_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onSearchOnline) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiStringResource(R.string.lyrics_search_online))
                }
                OutlinedButton(onClick = onEditManual) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiStringResource(R.string.lyrics_edit_manual))
                }
            }
        }
    }
}

@Composable
private fun QueuePreview(
    upNext: List<MediaEntry>,
    currentIndex: Int,
    onJumpToQueueItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = uiStringResource(R.string.queue_next),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = upNext.size.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        if (upNext.isEmpty()) {
            Text(
                text = uiStringResource(R.string.queue_empty_short),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(upNext.take(4), key = { index, item -> item.id + index }) { index, item ->
                    MediaItemRow(
                        item = item,
                        onClick = { onJumpToQueueItem(currentIndex + index + 1) }
                    )
                }
            }
        }
    }
}

private fun LyricLine.asAnnotatedString(
    positionMs: Long,
    activeLine: Boolean,
    accent: Color
) = buildAnnotatedString {
    if (words.isNotEmpty()) {
        val activeWordIndex = words.indexOfLast { word ->
            val start = word.startMs
            val end = word.endMs ?: Long.MAX_VALUE
            positionMs >= start && positionMs < end
        }

        words.forEachIndexed { index, word ->
            if (index > 0) append(" ")
            withStyle(
                SpanStyle(
                    color = when {
                        activeLine && index == activeWordIndex -> accent
                        activeLine -> Color.White
                        else -> Color.White.copy(alpha = 0.55f)
                    },
                    fontWeight = if (activeLine && index == activeWordIndex) FontWeight.Bold else FontWeight.Normal
                )
            ) {
                append(word.text)
            }
        }
    } else {
        append(text)
    }
}