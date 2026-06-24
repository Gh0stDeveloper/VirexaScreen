package com.virexa.screen.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.virexa.screen.data.*
import com.virexa.screen.ui.components.*
import com.virexa.screen.util.formatBytes
import com.virexa.screen.util.formatDate
import com.virexa.screen.util.formatDuration
import kotlinx.coroutines.delay

// ─── Splash ───────────────────────────────────────────────────────────────────

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) { delay(900); onDone() }
    PremiumBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                BrandMark(modifier = Modifier.size(92.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Virexa Screen", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    Text("Grabación limpia. Controles rápidos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Cargando experiencia")
                    }
                }
            }
        }
    }
}

// ─── Onboarding ───────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    preferences: UserPreferences,
    onFinish: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateLanguage: (LanguageOption) -> Unit,
    onUpdateTheme: (ThemeMode) -> Unit,
    onUpdateBubble: (Boolean) -> Unit,
    onUpdateAudio: (AudioMode) -> Unit,
    onUpdateQuality: (String) -> Unit,
) {
    val selectedQuality = QualityOption.fromId(preferences.defaultQualityId)
    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(4.dp))
            BrandMark()
            PremiumScreenHeader(title = "Tu perfil, listo en minutos", subtitle = "Virexa ajusta la calidad, el audio y la burbuja flotante con una interfaz simple.")
            SectionCard {
                OutlinedTextField(value = preferences.profileName, onValueChange = onUpdateName, modifier = Modifier.fillMaxWidth(), label = { Text("Nombre o alias") }, shape = RoundedCornerShape(22.dp), singleLine = true)
                Text("Idioma", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { LanguageOption.entries.forEach { lang -> SecondaryPill(lang.label, preferences.language == lang) { onUpdateLanguage(lang) } } }
            }
            SectionCard {
                Text("Tema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.SYSTEM to "Sistema", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Oscuro").forEach { (mode, label) ->
                        SecondaryPill(label, preferences.themeMode == mode) { onUpdateTheme(mode) }
                    }
                }
            }
            SectionCard {
                Text("Calidad predeterminada", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                QualityOption.presets.forEach { option -> QualityCard(option, preferences.defaultQualityId == option.id) { onUpdateQuality(option.id) } }
            }
            SectionCard {
                Text("Audio predeterminado", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                AudioMode.entries.forEach { mode -> AudioModeCard(mode, preferences.defaultAudioMode == mode) { onUpdateAudio(mode) } }
            }
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) { Text("Burbuja flotante", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold); Text("Control rápido sobre otras apps.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(checked = preferences.floatingBubbleEnabled, onCheckedChange = onUpdateBubble)
                }
            }
            PrimaryButton("Continuar", onFinish, Modifier.fillMaxWidth()) { Icon(Icons.Default.ArrowForward, null) }
            Spacer(Modifier.height(18.dp))
        }
    }
}

// ─── Home ─────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
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
    val selectedQuality = QualityOption.fromId(preferences.defaultQualityId)
    val isRecording = recordingState.isRecording
    val isPaused = recordingState.isPaused

    val accentColor by animateColorAsState(
        targetValue = when { isRecording && !isPaused -> MaterialTheme.colorScheme.error; isPaused -> Color(0xFFFF9800); else -> MaterialTheme.colorScheme.primary },
        animationSpec = tween(500), label = "accent"
    )

    PremiumBackground {
        // Countdown overlay
        AnimatedVisibility(visible = countdown > 0, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Grabación en", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("$countdown", style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(12.dp))

            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Virexa Screen", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Hola, ${preferences.profileName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BrandMark(modifier = Modifier.size(52.dp))
            }

            // Status card
            Surface(shape = RoundedCornerShape(20.dp), color = accentColor.copy(alpha = 0.08f), border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(accentColor, CircleShape))
                            Text(
                                text = when { isRecording && !isPaused -> "Grabando"; isPaused -> "En pausa"; else -> "Listo" },
                                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accentColor
                            )
                        }
                        AnimatedVisibility(visible = isRecording) {
                            Text(formatElapsed(recordingState.elapsedMs), style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = 2.sp)
                        }
                    }

                    // Silence detected warning
                    AnimatedVisibility(visible = recordingState.silenceDetected) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeOff, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text("Silencio detectado — grabación en pausa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricPill("Calidad", selectedQuality.label)
                        MetricPill("Audio", preferences.defaultAudioMode.label)
                        MetricPill("FPS", "${preferences.frameRate}")
                    }
                    Text("${selectedQuality.resolutionLabel} · ${selectedQuality.suggestedBitrate}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    AnimatedVisibility(visible = recordingState.message != null) {
                        AssistChip(onClick = {}, label = { Text(recordingState.message.orEmpty(), style = MaterialTheme.typography.bodySmall) })
                    }
                }
            }

            // Main action button
            AnimatedContent(targetState = Triple(isRecording, isPaused, Unit), transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "main_btn") { (rec, paused, _) ->
                PrimaryButton(
                    text = when { rec && !paused -> "Detener grabación"; paused -> "Reanudar"; else -> "Iniciar grabación" },
                    onClick = when { rec && !paused -> onStopRecording; paused -> onResumeRecording; else -> onStartRecording },
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(when { rec && !paused -> Icons.Default.Stop; paused -> Icons.Default.PlayArrow; else -> Icons.Default.FiberManualRecord }, null) }
            }

            // Secondary controls
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(if (isPaused) "Reanudar" else "Pausar", isRecording, if (isPaused) onResumeRecording else onPauseRecording, Modifier.weight(1f))
                SecondaryActionButton("Detener", isRecording, onStopRecording, Modifier.weight(1f))
            }

            // Nav row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                NavSquareBtn(Icons.Default.VideoLibrary, "Biblioteca", onOpenLibrary, Modifier.weight(1f))
                NavSquareBtn(Icons.Default.Settings, "Ajustes", onOpenSettings, Modifier.weight(1f))
                NavSquareBtn(Icons.Default.BubbleChart, "Burbuja", onEnableBubble, Modifier.weight(1f))
                NavSquareBtn(Icons.Default.Refresh, "Actualizar", onRefresh, Modifier.weight(1f))
            }

            // Stats mini dashboard
            if (stats.totalRecordings > 0) {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("Resumen rápido", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricPill("Total", "${stats.totalRecordings} videos")
                        MetricPill("Esta semana", "${stats.thisWeekCount}")
                        MetricPill("Espacio", formatBytes(stats.totalSizeBytes))
                    }
                }
            }

            // Active config summary
            SectionCard {
                Text("Configuración activa", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricPill("Encoder", if (preferences.videoEncoder == VideoEncoder.H265) "H.265" else "H.264")
                    MetricPill("Bitrate", if (preferences.bitrateMode == BitrateMode.AUTO) "Auto" else "${preferences.customBitrateMbps}M")
                    MetricPill("Cuenta", preferences.countdownOption.label.take(8))
                }
                if (preferences.watermarkEnabled && preferences.watermarkText.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Waterfall, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Marca: ${preferences.watermarkText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ─── Stats / Analytics ────────────────────────────────────────────────────────

@Composable
fun StatsScreen(
    stats: RecordingStats,
    recordings: List<RecordingFile>,
    onBack: () -> Unit,
) {
    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Atrás") }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Estadísticas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Actividad de grabación", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (stats.totalRecordings == 0) {
                SectionCard {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.BarChart, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text("Sin datos aún", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Realiza tu primera grabación para ver estadísticas.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                // Main metrics grid
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    BigStatCard("Total", "${stats.totalRecordings}", "grabaciones", Icons.Default.VideoLibrary, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    BigStatCard("Espacio", formatBytes(stats.totalSizeBytes), "utilizados", Icons.Default.Storage, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    BigStatCard("Duración", formatDuration(stats.totalDurationMs), "acumuladas", Icons.Default.AccessTime, Color(0xFF4CAF50), Modifier.weight(1f))
                    BigStatCard("Promedio", formatDuration(stats.averageDurationMs), "por video", Icons.Default.Equalizer, Color(0xFFFF9800), Modifier.weight(1f))
                }

                // Period stats
                SectionCard {
                    Text("Actividad reciente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    StatRow("Esta semana", "${stats.thisWeekCount} grabaciones", Icons.Default.DateRange)
                    StatRow("Este mes", "${stats.thisMonthCount} grabaciones", Icons.Default.CalendarMonth)
                    StatRow("Más larga", formatDuration(stats.longestDurationMs), Icons.Default.Timer)
                    StatRow("Más pesada", formatBytes(stats.largestSizeBytes), Icons.Default.FileDownload)
                }

                // Audio mode breakdown
                val audioBreakdown = recordings.groupBy { it.audioMode }
                if (audioBreakdown.size > 1) {
                    SectionCard {
                        Text("Distribución por audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        audioBreakdown.forEach { (mode, list) ->
                            val pct = (list.size * 100f / stats.totalRecordings).toInt()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(mode.label, style = MaterialTheme.typography.bodySmall)
                                    Text("$pct% (${list.size})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }

                // Resolution breakdown
                val resBreakdown = recordings.groupBy { it.resolution }
                SectionCard {
                    Text("Distribución por resolución", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    resBreakdown.entries.sortedByDescending { it.value.size }.forEach { (res, list) ->
                        val pct = (list.size * 100f / stats.totalRecordings).toInt()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(res, style = MaterialTheme.typography.bodySmall)
                                Text("$pct% (${list.size})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = MaterialTheme.colorScheme.tertiary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─── Library ──────────────────────────────────────────────────────────────────

@Composable
fun LibraryScreen(
    recordings: List<RecordingFile>,
    onBack: () -> Unit,
    onOpen: (RecordingFile) -> Unit,
    onDelete: (RecordingFile) -> Unit,
    onRefresh: () -> Unit,
    onOpenStats: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SortMode.DATE_DESC) }

    val filtered = remember(recordings, searchQuery, sortMode) {
        recordings
            .filter { searchQuery.isBlank() || it.displayName.contains(searchQuery, ignoreCase = true) }
            .let { list ->
                when (sortMode) {
                    SortMode.DATE_DESC -> list.sortedByDescending { it.createdAtMillis }
                    SortMode.DATE_ASC -> list.sortedBy { it.createdAtMillis }
                    SortMode.DURATION_DESC -> list.sortedByDescending { it.durationMs }
                    SortMode.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
                }
            }
    }

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Atrás") }
                Spacer(Modifier.width(8.dp))
                Text("Biblioteca", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenStats) { Icon(Icons.Default.BarChart, "Estadísticas") }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar") }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar grabación…") },
                shape = RoundedCornerShape(22.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton({ searchQuery = "" }) { Icon(Icons.Default.Close, null) } },
            )

            // Sort chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ordenar:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
                SortMode.entries.forEach { mode ->
                    SecondaryPill(mode.label, sortMode == mode) { sortMode = mode }
                }
            }

            if (filtered.isEmpty()) {
                SectionCard {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text(if (searchQuery.isNotBlank()) "Sin resultados" else "Sin grabaciones aún", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(if (searchQuery.isNotBlank()) "Prueba con otro nombre." else "Cuando detengas una grabación aparecerá aquí.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                Text("${filtered.size} de ${recordings.size} grabaciones", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                filtered.forEach { recording -> RecordingCard(recording, { onOpen(recording) }) { onDelete(recording) } }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private enum class SortMode(val label: String) {
    DATE_DESC("Reciente"), DATE_ASC("Antiguo"), DURATION_DESC("Más largo"), SIZE_DESC("Más pesado")
}

// ─── Recording Detail ─────────────────────────────────────────────────────────

@Composable
fun RecordingDetailScreen(recording: RecordingFile, onBack: () -> Unit, onDelete: () -> Unit, onRename: (String) -> Unit) {
    val context = LocalContext.current
    val player = remember(recording.filePath) {
        ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(recording.mediaUri)); prepare(); playWhenReady = false }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    var renameValue by remember(recording.displayName) { mutableStateOf(recording.displayName) }

    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Atrás") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            }
            SectionCard { AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxWidth().height(240.dp)) }
            SectionCard {
                Text(recording.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricPill("Res", recording.resolution)
                    MetricPill("Dur", formatDuration(recording.durationMs))
                    MetricPill("Peso", formatBytes(recording.sizeBytes))
                }
                Text("Fecha: ${formatDate(recording.createdAtMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text("Audio: ${recording.audioMode.label}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            SectionCard {
                Text("Renombrar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = renameValue, onValueChange = { renameValue = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Nuevo nombre") }, shape = RoundedCornerShape(22.dp), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryActionButton("Renombrar", renameValue.isNotBlank(), { onRename(renameValue) }, Modifier.weight(1f))
                    val shareIntent = remember(recording.filePath) { Intent(Intent.ACTION_SEND).apply { type = "video/mp4"; putExtra(Intent.EXTRA_STREAM, recording.mediaUri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
                    PrimaryButton("Compartir", { context.startActivity(Intent.createChooser(shareIntent, "Compartir video")) }, Modifier.weight(1f)) { Icon(Icons.Default.Share, null) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─── Settings ─────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateLanguage: (LanguageOption) -> Unit,
    onUpdateTheme: (ThemeMode) -> Unit,
    onUpdateBubble: (Boolean) -> Unit,
    onUpdateQuickControls: (Boolean) -> Unit,
    onUpdateAudio: (AudioMode) -> Unit,
    onUpdateQuality: (String) -> Unit,
    onUpdateFolder: (String) -> Unit,
    onStartBubble: () -> Unit,
    onOpenAdvanced: () -> Unit,
) {
    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Atrás") }
                Spacer(Modifier.width(8.dp))
                Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            SectionCard {
                Text("Cuenta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = preferences.profileName, onValueChange = onUpdateName, modifier = Modifier.fillMaxWidth(), label = { Text("Nombre de perfil") }, shape = RoundedCornerShape(22.dp), singleLine = true)
                OutlinedTextField(value = preferences.outputFolderName, onValueChange = onUpdateFolder, modifier = Modifier.fillMaxWidth(), label = { Text("Carpeta de guardado") }, shape = RoundedCornerShape(22.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.FolderOpen, null) })
            }
            SectionCard {
                Text("Apariencia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Idioma", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { LanguageOption.entries.forEach { lang -> SecondaryPill(lang.label, preferences.language == lang) { onUpdateLanguage(lang) } } }
                Text("Tema", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.SYSTEM to "Sistema", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Oscuro").forEach { (mode, label) -> SecondaryPill(label, preferences.themeMode == mode) { onUpdateTheme(mode) } }
                }
            }
            SectionCard {
                Text("Video y audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Resolución", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QualityOption.presets.forEach { opt -> SecondaryPill(opt.label, preferences.defaultQualityId == opt.id) { onUpdateQuality(opt.id) } } }
                Text("Fuente de audio", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AudioMode.entries.forEach { mode -> AudioModeCard(mode, preferences.defaultAudioMode == mode) { onUpdateAudio(mode) } }
            }
            SectionCard {
                Text("Burbuja flotante", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ToggleRow("Activar burbuja", "Se muestra sobre otras apps durante la grabación.", preferences.floatingBubbleEnabled, onUpdateBubble)
                ToggleRow("Controles rápidos", "Muestra accesos directos dentro de la burbuja.", preferences.showQuickControls, onUpdateQuickControls)
                if (preferences.floatingBubbleEnabled) PrimaryButton("Abrir burbuja ahora", onStartBubble, Modifier.fillMaxWidth()) { Icon(Icons.Default.BubbleChart, null) }
            }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), onClick = onOpenAdvanced) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary)
                        Column { Text("Ajustes avanzados", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall); Text("Encoder · Bitrate · FPS · Audio · Grabación", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─── Advanced Settings ────────────────────────────────────────────────────────

@Composable
fun AdvancedSettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onUpdateEncoder: (VideoEncoder) -> Unit,
    onUpdateBitrateMode: (BitrateMode) -> Unit,
    onUpdateCustomBitrate: (Int) -> Unit,
    onUpdateFrameRate: (Int) -> Unit,
    onUpdateShowTimerOnBubble: (Boolean) -> Unit,
    onUpdateAutoPauseOnCall: (Boolean) -> Unit,
    onUpdateKeepScreenOn: (Boolean) -> Unit,
    onUpdateShowTouchIndicator: (Boolean) -> Unit,
    onUpdateCountdown: (CountdownOption) -> Unit,
    onUpdateMaxDuration: (Int) -> Unit,
    onUpdateWatermarkEnabled: (Boolean) -> Unit,
    onUpdateWatermarkText: (String) -> Unit,
    onUpdateMicBoost: (MicBoostLevel) -> Unit,
    onUpdateNoiseSuppression: (Boolean) -> Unit,
    onUpdateSilenceAutoPause: (Boolean) -> Unit,
    onUpdateSilenceThreshold: (Int) -> Unit,
    onUpdateDoNotDisturb: (Boolean) -> Unit,
    onUpdateHapticFeedback: (Boolean) -> Unit,
    onUpdateAutoShare: (Boolean) -> Unit,
) {
    PremiumBackground {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Atrás") }
                Spacer(Modifier.width(8.dp))
                Column { Text("Ajustes avanzados", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold); Text("Grabación · Audio · Encoder · UX", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            // ── Grabación ─────────────────────────────────────────────────
            AdvancedSection(icon = Icons.Default.FiberManualRecord, title = "Grabación", tint = MaterialTheme.colorScheme.error) {
                Text("Cuenta regresiva", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Muestra un contador visible antes de iniciar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CountdownOption.entries.forEach { opt -> SecondaryPill(opt.label, preferences.countdownOption == opt) { onUpdateCountdown(opt) } }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Duración máxima", style = MaterialTheme.typography.labelMedium); Text(if (preferences.maxDurationMinutes == 0) "Sin límite" else "${preferences.maxDurationMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(if (preferences.maxDurationMinutes == 0) "∞" else "${preferences.maxDurationMinutes}m", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Slider(value = preferences.maxDurationMinutes.toFloat(), onValueChange = { onUpdateMaxDuration(it.toInt()) }, valueRange = 0f..180f, steps = 35)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sin límite", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("180 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow("Marca de agua", "Añade texto sobre el video grabado (visible en el resultado final).", preferences.watermarkEnabled, onUpdateWatermarkEnabled)
                AnimatedVisibility(visible = preferences.watermarkEnabled) {
                    OutlinedTextField(value = preferences.watermarkText, onValueChange = onUpdateWatermarkText, modifier = Modifier.fillMaxWidth(), label = { Text("Texto de marca de agua") }, shape = RoundedCornerShape(22.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.TextFields, null) }, placeholder = { Text("ej. @usuario · 2025") })
                }
            }

            // ── Encoder / Video ───────────────────────────────────────────
            AdvancedSection(icon = Icons.Default.VideoSettings, title = "Codificador de video", tint = MaterialTheme.colorScheme.primary) {
                Text("H.265 produce archivos más pequeños; H.264 es más compatible.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { VideoEncoder.entries.forEach { enc -> SecondaryPill(enc.label, preferences.videoEncoder == enc) { onUpdateEncoder(enc) } } }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Text("Tasa de bits (Bitrate)", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { BitrateMode.entries.forEach { mode -> SecondaryPill(mode.label, preferences.bitrateMode == mode) { onUpdateBitrateMode(mode) } } }
                AnimatedVisibility(visible = preferences.bitrateMode == BitrateMode.CUSTOM) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bitrate personalizado")
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) { Text("${preferences.customBitrateMbps} Mbps", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                        }
                        Slider(value = preferences.customBitrateMbps.toFloat(), onValueChange = { onUpdateCustomBitrate(it.toInt()) }, valueRange = 1f..50f, steps = 48)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("1 Mbps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("50 Mbps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Text("Frames por segundo (FPS)", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(24, 30, 60).forEach { fps -> SecondaryPill("$fps fps", preferences.frameRate == fps) { onUpdateFrameRate(fps) } } }
            }

            // ── Audio ─────────────────────────────────────────────────────
            AdvancedSection(icon = Icons.Default.Mic, title = "Audio avanzado", tint = Color(0xFF4CAF50)) {
                Text("Boost de micrófono", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MicBoostLevel.entries.forEach { lvl -> SecondaryPill(lvl.label, preferences.micBoostLevel == lvl) { onUpdateMicBoost(lvl) } } }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow("Supresión de ruido", "Reduce el ruido de fondo del micrófono usando el modo VOICE_RECOGNITION.", preferences.noiseSuppression, onUpdateNoiseSuppression)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow("Pausar en silencio", "Pausa la grabación automáticamente cuando no se detecta audio.", preferences.silenceAutoPause, onUpdateSilenceAutoPause)
                AnimatedVisibility(visible = preferences.silenceAutoPause) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Umbral de silencio")
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) { Text("${preferences.silenceThresholdSeconds}s", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                        }
                        Slider(value = preferences.silenceThresholdSeconds.toFloat(), onValueChange = { onUpdateSilenceThreshold(it.toInt()) }, valueRange = 3f..60f, steps = 56)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("3 s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("60 s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }

            // ── Comportamiento / UX ───────────────────────────────────────
            AdvancedSection(icon = Icons.Default.Tune, title = "Comportamiento y UX", tint = MaterialTheme.colorScheme.tertiary) {
                ToggleRow("Timer en la burbuja", "Muestra el tiempo transcurrido en la ventana flotante.", preferences.showTimerOnBubble, onUpdateShowTimerOnBubble)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow("Pausar al recibir llamada", "Pausa la grabación automáticamente cuando entra una llamada.", preferences.autoPauseOnCall, onUpdateAutoPauseOnCall)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow("Mantener pantalla encendida", "Evita que el dispositivo se bloquee durante la grabación.", preferences.keepScreenOn, onUpdateKeepScreenOn)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow("Indicador de toque", "Muestra círculos donde tocas la pantalla durante la grabación.", preferences.showTouchIndicator, onUpdateShowTouchIndicator)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow("No molestar durante grabación", "Activa el modo silencioso del sistema mientras grabas (requiere permiso de acceso a notificaciones).", preferences.doNotDisturbDuringRecording, onUpdateDoNotDisturb)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow("Vibración en acciones", "Retroalimentación háptica al pausar, reanudar y detener la grabación.", preferences.hapticFeedback, onUpdateHapticFeedback)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow("Compartir automáticamente", "Abre el menú de compartir al detener la grabación.", preferences.autoShareAfterStop, onUpdateAutoShare)
            }

            // Info tip
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                    Text("Los cambios en encoder, bitrate y FPS se aplican en la siguiente grabación. Si el dispositivo no soporta H.265, Virexa usa H.264 automáticamente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

@Composable
private fun NavSquareBtn(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, label, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdvancedSection(icon: ImageVector, title: String, tint: Color = MaterialTheme.colorScheme.primary, content: @Composable ColumnScope.() -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        content()
    }
}

@Composable
private fun BigStatCard(label: String, value: String, sublabel: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = color.copy(alpha = 0.1f), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)), modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Text(sublabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun ToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

internal fun formatElapsed(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}
