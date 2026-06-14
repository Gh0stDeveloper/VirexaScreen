package com.nexora.player.ui.screens

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.player.R
import com.nexora.player.data.local.FavoriteMediaEntity
import com.nexora.player.data.local.NexoraDatabase
import com.nexora.player.data.lyrics.LrcParser
import com.nexora.player.data.lyrics.Lyrics
import com.nexora.player.data.lyrics.LyricsRepository
import com.nexora.player.ui.components.LyricsAndQueueCard
import com.nexora.player.ui.screens.LyricsEditorDialog
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.MediaKind
import com.nexora.player.playback.PlayerEngine
import com.nexora.player.ui.components.MediaItemRow
import com.nexora.player.ui.components.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong



private enum class ArtworkStyle {
    DISC,
    SQUARE,
    COVER;

    fun next(): ArtworkStyle = when (this) {
        DISC -> SQUARE
        SQUARE -> COVER
        COVER -> DISC
    }
}
@Composable
fun AudioPlayerScreen(
    modifier: Modifier = Modifier,
    current: MediaEntry?,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val player = PlayerEngine.get(context)
    val snapshot by PlayerEngine.snapshot.collectAsState()
    val scope = rememberCoroutineScope()
    val favorites by NexoraDatabase.get(context).favoritesDao().observeAll().collectAsState(initial = emptyList())

    val lyricsRepository = remember(context) { LyricsRepository(context, NexoraDatabase.get(context)) }
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }
    var allowOnlineLyrics by rememberSaveable(current?.id) { mutableStateOf(false) }
    var showLyricsEditor by rememberSaveable(current?.id) { mutableStateOf(false) }
    var artworkStyle by rememberSaveable(current?.id) { mutableStateOf(ArtworkStyle.DISC) }

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    val artwork by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = current?.id,
        key2 = current?.uri?.toString()
    ) {
        value = withContext(Dispatchers.IO) {
            current?.let { loadArtworkBitmap(context, it)?.asImageBitmap() }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "audio_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val isFavorite = remember(current?.id, current?.kind, favorites) {
        val item = current
        item != null && favorites.any { fav -> fav.mediaId == item.id && fav.mediaKind == item.kind.name }
    }

    val queue = snapshot.queue
    val currentIndex = snapshot.currentIndex.coerceAtLeast(0)
    val upNext = if (queue.isEmpty()) emptyList() else queue.drop(currentIndex + 1)

    BackHandler {
        onClose()
    }

    LaunchedEffect(current?.id, snapshot.isPlaying) {
        if (current == null) return@LaunchedEffect
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0L } ?: current.durationMs
            delay(350)
        }
    }

    LaunchedEffect(current?.id, allowOnlineLyrics) {
        val item = current
        if (item == null) {
            lyrics = null
            lyricsLoading = false
            return@LaunchedEffect
        }
        lyricsLoading = true
        lyrics = null
        lyrics = runCatching {
            lyricsRepository.loadLyrics(item, allowOnlineSearch = allowOnlineLyrics)
        }.getOrNull()
        lyricsLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04050A))
    ) {
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.audio_no_playback),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
            return@Box
        }

        if (artwork != null) {
            Image(
                bitmap = artwork!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(38.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF7C3AED).copy(alpha = 0.42f),
                                Color(0xFF111827).copy(alpha = 0.95f)
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.32f),
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopBar(
                current = current,
                isFavorite = isFavorite,
                artworkStyle = artworkStyle,
                onBack = { onClose() },
                onToggleFavorite = {
                    scope.launch {
                        toggleFavorite(context, current)
                    }
                },
                onChangeArtworkStyle = {
                    artworkStyle = artworkStyle.next()
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArtworkDisplay(
                    artwork = artwork,
                    title = current.title,
                    isPlaying = snapshot.isPlaying,
                    rotation = rotation,
                    style = artworkStyle
                )

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = listOfNotNull(
                        current.artist.takeIf { it.isNotBlank() },
                        current.album.takeIf { it.isNotBlank() }
                    ).joinToString(" • ").ifBlank { stringResource(R.string.app_name) },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.74f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(onClick = {}, label = { Text(formatDuration(current.durationMs)) })
                    AssistChip(onClick = {}, label = { Text("${currentIndex + 1}/${queue.size.coerceAtLeast(1)}") })
                    AssistChip(onClick = {}, label = { Text(if (snapshot.isPlaying) "En reproducción" else "En pausa") })
                    AssistChip(
                        onClick = {
                            scope.launch { toggleFavorite(context, current) }
                        },
                        label = { Text(if (isFavorite) "Favorito" else "Agregar favorito") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null
                            )
                        }
                    )
                }

                PlaybackProgressSection(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeek = { target -> PlayerEngine.seekTo(context, target) },
                    modifier = Modifier.fillMaxWidth()
                )

                TransportControls(
                    isPlaying = snapshot.isPlaying,
                    onPrevious = { PlayerEngine.skipPrevious(context) },
                    onTogglePlay = { PlayerEngine.togglePlayPause(context) },
                    onNext = { PlayerEngine.skipNext(context) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LyricsAndQueueCard(
                lyrics = lyrics,
                lyricsLoading = lyricsLoading,
                positionMs = positionMs,
                queue = queue,
                currentIndex = currentIndex,
                onJumpToQueueItem = { PlayerEngine.jumpTo(context, it) },
                onSearchOnline = { allowOnlineLyrics = true },
                onEditManual = { showLyricsEditor = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showLyricsEditor && current != null) {
        LyricsEditorDialog(
            currentPositionMs = positionMs,
            initialText = lyrics?.rawText.orEmpty(),
            onSave = { rawText, exportToFile ->
                scope.launch {
                    val parsed = LrcParser.parse(
                        rawText = rawText,
                        mediaId = current.id,
                        title = current.title,
                        artist = current.artist,
                        album = current.album,
                        source = com.nexora.player.data.lyrics.LyricsSource.MANUAL
                    )
                    lyricsRepository.saveLyrics(current, parsed, exportToSidecarFile = exportToFile)
                    lyrics = parsed
                    showLyricsEditor = false
                }
            },
            onDismiss = { showLyricsEditor = false }
        )
    }
}


@Composable
private fun TopBar(
    current: MediaEntry,
    isFavorite: Boolean,
    artworkStyle: ArtworkStyle,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onChangeArtworkStyle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.08f),
            shape = CircleShape
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Now playing",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = if (current.kind == MediaKind.AUDIO) "Audio" else "Media",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Surface(
            color = Color.White.copy(alpha = 0.08f),
            shape = CircleShape
        ) {
            IconButton(onClick = onChangeArtworkStyle) {
                Icon(
                    imageVector = Icons.Filled.Checkroom,
                    contentDescription = when (artworkStyle) {
                        ArtworkStyle.DISC -> "Cambiar a carátula cuadrada"
                        ArtworkStyle.SQUARE -> "Cambiar a portada"
                        ArtworkStyle.COVER -> "Cambiar a disco"
                    },
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ArtworkDisplay(
    artwork: ImageBitmap?,
    title: String,
    isPlaying: Boolean,
    rotation: Float,
    style: ArtworkStyle,
    modifier: Modifier = Modifier
) {
    val outerShape = when (style) {
        ArtworkStyle.DISC -> CircleShape
        ArtworkStyle.SQUARE -> RoundedCornerShape(34.dp)
        ArtworkStyle.COVER -> RoundedCornerShape(42.dp)
    }

    val outerSize = when (style) {
        ArtworkStyle.DISC -> 310.dp
        ArtworkStyle.SQUARE -> 304.dp
        ArtworkStyle.COVER -> 304.dp
    }

    val innerSize = when (style) {
        ArtworkStyle.DISC -> 262.dp
        ArtworkStyle.SQUARE -> 260.dp
        ArtworkStyle.COVER -> 260.dp
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(outerSize)
            .shadow(30.dp, outerShape, clip = false)
            .clip(outerShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.12f), shape = outerShape)
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
                .shadow(18.dp, outerShape, clip = false)
                .clip(outerShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                )
        ) {
            if (artwork != null) {
                Image(
                    bitmap = artwork,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(outerShape)
                        .shadow(14.dp, outerShape)
                        .graphicsLayer(rotationZ = if (style == ArtworkStyle.DISC && isPlaying) rotation else 0f)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(92.dp)
                        .align(Alignment.Center)
                )
            }

            if (style == ArtworkStyle.DISC) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.12f)
                            ),
                            radius = 420f
                        )
                    )
            )
        }
    }
}

@Composable
private fun PlaybackProgressSection(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by rememberSaveable { mutableStateOf(false) }
    var progress by remember(positionMs, durationMs) {
        mutableStateOf(
            if (durationMs > 0L) positionMs.toFloat() / durationMs.toFloat() else 0f
        )
    }

    LaunchedEffect(positionMs, durationMs, isDragging) {
        if (!isDragging) {
            progress = if (durationMs > 0L) positionMs.toFloat() / durationMs.toFloat() else 0f
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = {
                isDragging = true
                progress = it
            },
            onValueChangeFinished = {
                isDragging = false
                if (durationMs > 0L) {
                    onSeek((progress.coerceIn(0f, 1f) * durationMs.toFloat()).roundToLong())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF7C3AED),
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(positionMs.coerceAtLeast(0L)),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
            Text(
                text = formatDuration(durationMs.coerceAtLeast(0L)),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Anterior"
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        FilledTonalIconButton(onClick = onTogglePlay) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir"
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        FilledTonalIconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Siguiente"
            )
        }
    }
}

private suspend fun toggleFavorite(context: Context, entry: MediaEntry?) {
    if (entry == null) return
    if (entry.kind == MediaKind.VIDEO) return

    val db = NexoraDatabase.get(context)
    val exists = db.favoritesDao().isFavorite(entry.id, entry.kind.name)
    if (exists) {
        db.favoritesDao().delete(entry.id, entry.kind.name)
    } else {
        db.favoritesDao().upsert(
            FavoriteMediaEntity(
                mediaId = entry.id,
                mediaKind = entry.kind.name,
                title = entry.title,
                artist = entry.artist,
                album = entry.album,
                durationMs = entry.durationMs,
                uriString = entry.uri.toString()
            )
        )
    }
}

private fun loadArtworkBitmap(context: Context, item: MediaEntry): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, item.uri)
        retriever.embeddedPicture?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } ?: loadAlbumArtBitmap(context, item.albumId)
    } catch (_: Throwable) {
        loadAlbumArtBitmap(context, item.albumId)
    } finally {
        runCatching { retriever.release() }
    }
}

private fun loadAlbumArtBitmap(context: Context, albumId: Long?): Bitmap? {
    if (albumId == null || albumId <= 0L) return null
    return runCatching {
        val albumUri = android.content.ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
        context.contentResolver.query(
            albumUri,
            arrayOf(MediaStore.Audio.Albums.ALBUM_ART),
            null,
            null,
            null
        )?.use { cursor ->
            val artCol = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)
            if (cursor.moveToFirst() && artCol >= 0) {
                val path = cursor.getString(artCol)
                if (!path.isNullOrBlank()) BitmapFactory.decodeFile(path) else null
            } else null
        }
    }.getOrNull()
}

private fun dispatchBackPress(context: Context) {
    val activity = findComponentActivity(context)
    activity?.onBackPressedDispatcher?.onBackPressed()
}

private fun findComponentActivity(context: Context): ComponentActivity? = when (context) {
    is ComponentActivity -> context
    is ContextWrapper -> findComponentActivity(context.baseContext)
    else -> null
}
