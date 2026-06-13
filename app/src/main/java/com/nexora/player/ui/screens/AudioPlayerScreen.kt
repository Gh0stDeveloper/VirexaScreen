package com.nexora.player.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexora.player.R
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.playback.PlayerEngine
import com.nexora.player.ui.components.formatDuration
import kotlinx.coroutines.delay

@Composable
fun AudioPlayerScreen(
    modifier: Modifier = Modifier,
    current: MediaEntry?
) {
    val context = LocalContext.current
    val player = PlayerEngine.get(context)
    val snapshot by PlayerEngine.snapshot.collectAsState()
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    // Animación de giro del disco
    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Colores extraídos de la portada
    var dominantColor by remember { mutableStateOf(Color.DarkGray) }
    // Color base para la interfaz
    LaunchedEffect(current?.uri) {
        dominantColor = when (current?.kind) {
            com.nexora.player.data.model.MediaKind.VIDEO -> Color(0xFF2563EB)
            else -> Color(0xFF7C3AED)
        }
    }

    // Actualizar posición periódicamente
    LaunchedEffect(current?.id, snapshot.isPlaying) {
        if (current == null) return@LaunchedEffect
        while (true) {
            positionMs = player.currentPosition
            durationMs = player.duration.takeIf { it > 0L } ?: current.durationMs
            delay(400)
        }
    }

    // --- Interfaz principal ---
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (current == null) {
            // Sin contenido
            Text(
                text = stringResource(R.string.audio_no_playback),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        // Fondo desenfocado con la portada
        AsyncImage(
            model = current.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp) // alto desenfoque para atmósfera
        )
        // Capa oscura semi-transparente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Encabezado: botón volver y opciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Acción de volver, como navegar hacia atrás */ }) {
                    Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White)
                }
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Portada y metadatos centrados
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Disco giratorio con la carátula
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape)
                        .shadow(16.dp, CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    AsyncImage(
                        model = current.uri,
                        contentDescription = "Portada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(260.dp)
                            .clip(CircleShape)
                            .rotate(if (snapshot.isPlaying) rotation else 0f) // gira al reproducir
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Título y artista/álbum
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(
                        current.artist.takeIf { it.isNotBlank() },
                        current.album.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Controles inferiores
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra de progreso con tiempos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatDuration(positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Slider(
                        value = positionMs.toFloat(),
                        onValueChange = { player.seekTo(it.toLong()) },
                        valueRange = 0f..durationMs.toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Text(
                        text = formatDuration(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controles de reproducción principales
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Shuffle (puedes omitirlo si no lo usas)
                    IconButton(onClick = { /* toggle shuffle */ }) {
                        Icon(
                            Icons.Outlined.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    // Anterior
                    IconButton(onClick = { PlayerEngine.skipPrevious(context) }) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    // Play/Pausa central enorme
                    IconButton(
                        onClick = { PlayerEngine.togglePlayPause(context) },
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = Color.White,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (snapshot.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (snapshot.isPlaying) "Pausa" else "Reproducir",
                            tint = dominantColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    // Siguiente
                    IconButton(onClick = { PlayerEngine.skipNext(context) }) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = "Siguiente",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    // Repetir
                    IconButton(onClick = { /* toggle repeat */ }) {
                        Icon(
                            Icons.Outlined.Repeat,
                            contentDescription = "Repetir",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones inferiores: dispositivos, cola, me gusta
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { /* dispositivos */ }) {
                        Icon(
                            Icons.Filled.Devices,
                            contentDescription = "Dispositivos",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { /* cola de reproducción */ }) {
                        Icon(
                            Icons.Filled.QueueMusic,
                            contentDescription = "Cola",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { /* toggle favorito */ }) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Me gusta",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}