package com.nexora.player.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object ArtworkMemoryCache {
    private val cache = object : LruCache<String, Bitmap>(40) {}

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bitmap: Bitmap?) {
        if (bitmap != null) cache.put(key, bitmap)
    }
}

@Composable
fun rememberArtworkBitmap(entry: MediaEntry?): Bitmap? {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, key1 = entry?.id, key2 = entry?.uri) {
        val current = entry ?: return@produceState
        val key = "${current.kind}:${current.id}:${current.uri}"
        ArtworkMemoryCache.get(key)?.let {
            value = it
            return@produceState
        }

        value = withContext(Dispatchers.IO) {
            extractArtworkBitmap(context, current).also { ArtworkMemoryCache.put(key, it) }
        }
    }.value
}

@Composable
fun MediaArtwork(
    entry: MediaEntry?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    placeholderIcon: @Composable (() -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap = rememberArtworkBitmap(entry)
    val accent = when (entry?.kind) {
        MediaKind.VIDEO -> MaterialTheme.colorScheme.tertiary
        MediaKind.AUDIO -> MaterialTheme.colorScheme.secondary
        null -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier,
        shape = shape,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                bitmap != null -> androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = entry?.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (placeholderIcon != null) {
                            placeholderIcon()
                        } else {
                            Icon(
                                imageVector = if (entry?.kind == MediaKind.VIDEO) Icons.Outlined.Movie else Icons.Outlined.Album,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.height(40.dp)
                            )
                        }
                        Text(
                            text = entry?.title?.take(2)?.uppercase().orEmpty().ifBlank { if (entry?.kind == MediaKind.VIDEO) "VID" else "AUD" },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f))
                        )
                    )
            )
        }
    }
}

@Composable
fun PremiumSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun StatPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun PremiumHeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                content()
            }
        }
    }
}

private fun extractArtworkBitmap(context: android.content.Context, entry: MediaEntry): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, entry.uri)
        when (entry.kind) {
            MediaKind.AUDIO -> retriever.embeddedPicture?.let { data -> BitmapFactory.decodeByteArray(data, 0, data.size) }
            MediaKind.VIDEO -> retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            else -> null
        }
    } catch (_: Throwable) {
        null
    } finally {
        try {
            retriever.release()
        } catch (_: Throwable) {
        }
    }
}
