package com.virexa.screen.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.virexa.screen.R
import com.virexa.screen.data.RecordingStats
import com.virexa.screen.data.RecordingUiState
import com.virexa.screen.data.UserPreferences
import com.virexa.screen.util.formatBytes

@Composable
fun VirexaHomeScreen(
    preferences: UserPreferences,
    recordingState: RecordingUiState,
    countdown: Int,
    stats: RecordingStats,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onEnableBubble: () -> Unit,
    onRefresh: () -> Unit,
) {
    val isRecording = recordingState.isRecording
    val isPaused = recordingState.isPaused
    val accent by animateColorAsState(
        targetValue = when {
            isRecording && !isPaused -> Color(0xFFFF453A)
            isPaused -> Color(0xFFFFB340)
            else -> Color(0xFF5C6CFF)
        },
        animationSpec = tween(350),
        label = "accent",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF090B0F), Color(0xFF12151C), Color(0xFF0A0C10))))
            .navigationBarsPadding(),
    ) {
        AnimatedVisibility(visible = countdown > 0) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Grabación en", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(countdown.toString(), color = accent, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Virexa Screen", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Hola, ${preferences.profileName}", color = Color(0xFF9FA6B2))
                }
                Image(
                    painter = painterResource(id = R.drawable.virexa_brand_icon),
                    contentDescription = "Virexa",
                    modifier = Modifier.height(58.dp),
                )
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF11151D),
                tonalElevation = 8.dp,
                shadowElevation = 18.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(accent, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = when {
                                isRecording && !isPaused -> "Grabación activa"
                                isPaused -> "Grabación en pausa"
                                else -> "Listo para grabar"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        if (isRecording) {
                            Text(formatHomeElapsed(recordingState.elapsedMs), color = accent, fontWeight = FontWeight.Black)
                        }
                    }

                    Text(
                        text = "Calidad ${preferences.defaultQualityId.uppercase()} · ${preferences.defaultAudioMode.label} · ${preferences.frameRate} FPS",
                        color = Color(0xFF9FA6B2),
                    )

                    if (!recordingState.message.isNullOrBlank()) {
                        AssistChip(onClick = {}, label = { Text(recordingState.message.orEmpty()) })
                    }

                    Button(
                        onClick = when {
                            isRecording && !isPaused -> onStopRecording
                            isPaused -> onResumeRecording
                            else -> onStartRecording
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Icon(
                            imageVector = when {
                                isRecording && !isPaused -> Icons.Default.Stop
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = when {
                                isRecording && !isPaused -> "Detener grabación"
                                isPaused -> "Reanudar grabación"
                                else -> "Iniciar grabación"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(
                            onClick = if (isPaused) onResumeRecording else onPauseRecording,
                            enabled = isRecording,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isPaused) "Reanudar" else "Pausar")
                        }

                        OutlinedButton(
                            onClick = onEnableBubble,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(Icons.Default.BubbleChart, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Burbuja")
                        }
                    }
                }
            }

            Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF0F131A)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Accesos rápidos", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        QuickTile(Icons.Default.VideoLibrary, "Biblioteca", onOpenLibrary, Modifier.weight(1f))
                        QuickTile(Icons.Default.Settings, "Ajustes", onOpenSettings, Modifier.weight(1f))
                        QuickTile(Icons.Default.Refresh, "Actualizar", onRefresh, Modifier.weight(1f))
                    }
                }
            }

            Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF0F131A)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Resumen", color = Color.White, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = Color(0x1FFFFFFF))
                    SummaryLine("Grabaciones", "${stats.totalRecordings}")
                    SummaryLine("Esta semana", "${stats.thisWeekCount}")
                    SummaryLine("Espacio usado", formatBytes(stats.totalSizeBytes))
                    SummaryLine("Timer flotante", if (preferences.showTimerOnBubble) "Activado" else "Desactivado")
                    SummaryLine("Marca de agua", if (preferences.watermarkEnabled) "Activa" else "Desactivada")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF171C24),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
            Text(label, color = Color(0xFFDCE2EA), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFF9FA6B2), modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatHomeElapsed(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
