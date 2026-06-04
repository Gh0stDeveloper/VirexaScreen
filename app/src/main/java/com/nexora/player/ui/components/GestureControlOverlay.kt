
package com.nexora.player.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun GestureControlOverlay(
    modifier: Modifier = Modifier,
    brightness: Float,
    volume: Float,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    var hint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(hint) {
        if (hint != null) {
            kotlinx.coroutines.delay(700)
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
                        hint = "Brillo ${(next * 100).roundToInt()}%"
                    } else {
                        val next = (volume + delta).coerceIn(0f, 1f)
                        onVolumeChange(next)
                        hint = "Volumen ${(next * 100).roundToInt()}%"
                    }
                }
            },
        contentAlignment = Alignment.TopEnd
    ) {
        if (hint != null) {
            Card {
                Text(
                    text = hint.orEmpty(),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
