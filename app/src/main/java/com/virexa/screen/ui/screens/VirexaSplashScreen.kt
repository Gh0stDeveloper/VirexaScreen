package com.virexa.screen.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.virexa.screen.R
import kotlinx.coroutines.delay

@Composable
fun VirexaSplashScreen(onDone: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "splash")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1700
                0.94f at 0 using FastOutSlowInEasing
                1.03f at 850 using FastOutSlowInEasing
                0.98f at 1700
            },
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.84f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
                0.84f at 0
                1f at 750
                0.9f at 1500
            },
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    LaunchedEffect(Unit) {
        delay(1700)
        onDone()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF090B0F), Color(0xFF11141A), Color(0xFF090B0F))
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painter = painterResource(id = R.drawable.virexa_brand_icon),
                contentDescription = "Virexa Screen",
                modifier = Modifier.size(124.dp).scale(scale).alpha(alpha),
            )
            Spacer(Modifier.height(24.dp))
            AnimatedVisibility(visible = true, enter = fadeIn()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Virexa Screen", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("Captura profesional, limpia y estable", color = Color(0xFFAFB5BF), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(color = Color(0xFFFF4E45), trackColor = Color(0x26FFFFFF))
        }
    }
}
