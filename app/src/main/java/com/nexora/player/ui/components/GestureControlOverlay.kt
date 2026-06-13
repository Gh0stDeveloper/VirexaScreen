package com.nexora.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private data class GestureHint(
    val title: String,
    val valueLabel: String,
    val icon: ImageVector,
    val progress: Float
)

@Composable
fun GestureControlOverlay(
    modifier: Modifier = Modifier,
    brightness: Float,
    volume: Float,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    var hint by remember { mutableStateOf<GestureHint?>(null) }

    LaunchedEffect(hint) {
        if (hint != null) {
            delay(650)
            hint = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(brightness, volume) {
                detectVerticalDragGestures { change, dragAmount ->
                    val delta = -dragAmount / size.height.toFloat()
                    val leftSide = change.position.x < size.width / 2f
                    if (leftSide) {
                        val next = (brightness + delta).coerceIn(0.05f, 1f)
                        onBrightnessChange(next)
                        hint = GestureHint(
                            title = "Brillo",
                            valueLabel = "${(next * 100).roundToInt()}%",
                            icon = Icons.Filled.Brightness6,
                            progress = next
                        )
                    } else {
                        val next = (volume + delta).coerceIn(0f, 1f)
                        onVolumeChange(next)
                        hint = GestureHint(
                            title = "Volumen",
                            valueLabel = "${(next * 100).roundToInt()}%",
                            icon = Icons.Filled.VolumeUp,
                            progress = next
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = hint != null, enter = fadeIn(), exit = fadeOut()) {
            hint?.let { current ->
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color.Black.copy(alpha = 0.72f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 18.dp,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = current.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = current.valueLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        LinearProgressIndicator(
                            progress = { current.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            trackColor = Color.White.copy(alpha = 0.12f),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
