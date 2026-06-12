package com.nexora.player.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.player.R
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MediaArtwork(
    item: MediaEntry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        if (item.kind == MediaKind.VIDEO) {
            val thumbnailState = produceState<Bitmap?>(initialValue = null, key1 = item.uri) {
                value = loadVideoThumbnail(context, item.uri)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
            ) {
                val bitmap = thumbnailState.value
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF1D4ED8),
                                        Color(0xFF22D3EE)
                                    )
                                )
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_video_placeholder),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.title.firstOrNull()?.uppercaseChar()?.toString() ?: "V",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                }
            }
        } else {
            val colors = listOf(
                Color(0xFF111827),
                Color(0xFF7C3AED),
                Color(0xFF22D3EE)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = Brush.linearGradient(colors))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(58.dp)
                        .align(Alignment.Center)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.title.firstOrNull()?.uppercaseChar()?.toString() ?: "N",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                }
            }
        }
    }
}

private suspend fun loadVideoThumbnail(context: Context, uri: android.net.Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(720, 720), null)
            } else {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, uri)
                    retriever.getFrameAtTime(
                        0L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                }
            }
        }.getOrNull()
    }
}