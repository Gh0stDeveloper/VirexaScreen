package com.virexa.screen.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.virexa.screen.data.*
import com.virexa.screen.ui.components.*
import kotlinx.coroutines.delay
import java.io.File

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
                    Text("Grabación limpia. Controles rápidos. Diseño minimalista.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            BrandMark()
            PremiumScreenHeader(
                title = "Tu perfil, listo en minutos",
                subtitle = "Virexa ajusta la calidad, el audio y la burbuja flotante con una interfaz más simple y menos técnica.",
            )
            SectionCard {
                OutlinedTextField(value = preferences.profileName, onValueChange = onUpdateName, modifier = Modifier.fillMaxWidth(), label = { Text("Nombre o alias") }, shape = RoundedCornerShape(22.dp), singleLine = true)
                Text("Idioma", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    LanguageOption.entries.forEach { lang -> SecondaryPill(text = lang.label, selected = preferences.language == lang, onClick = { onUpdateLanguage(lang) }) }
                }
            }
            SectionCard {
                Text("Tema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                        SecondaryPill(text = when (mode) { ThemeMode.SYSTEM -> "Sistema"; ThemeMode.LIGHT -> "Claro"; ThemeMode.DARK -> "Oscuro" }, selected = preferences.themeMode == mode, onClick = { onUpdateTheme(mode) })
                    }
                }
            }
            SectionCard {
                Text("Calidad predeterminada", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                QualityCard(option = selectedQuality, selected = true, onClick = {})
                QualityOption.presets.forEach { option -> if (option.id != selectedQuality.id) QualityCard(option = option, selected = false, onClick = { onUpdateQuality(option.id) }) }
            }
            SectionCard {
                Text("Audio predeterminado", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                AudioMode.entries.forEach { mode -> AudioModeCard(mode = mode, selected = preferences.defaultAudioMode == mode, onClick = { onUpdateAudio(mode) }) }
            }
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Burbuja flotante", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Control rápido sobre otras apps.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = preferences.floatingBubbleEnabled, onCheckedChange = onUpdateBubble)
                }
            }
            PrimaryButton(
                text = "Continuar",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(Icons.Default.ArrowForward, contentDescription = null) },
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

// ─── Home ─────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    preferences: UserPreferences,
    recordingState: RecordingUiState,
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
        targetValue = when {
            isRecording && !isPaused -> MaterialTheme.colorScheme.error
            isPaused -> Color(0xFFFF9800)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(500),
        label = "accent",
    )

    PremiumBackground {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Header ─────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Virexa Screen", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Hola, ${preferences.profileName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BrandMark(modifier = Modifier.size(52.dp))
            }

            // ── Status card ────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = accentColor.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(accentColor, CircleShape))
                            Text(
                                text = when { isRecording && !isPaused -> "Grabando"; isPaused -> "En pausa"; else -> "Listo" },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                            )
                        }
                        // Timer
                        AnimatedVisibility(visible = isRecording) {
                            Text(
                                text = formatElapsed(recordingState.elapsedMs),
                                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                letterSpacing = 2.sp,
                            )
                        }
                    }

                    // Quality & audio pills
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricPill("Calidad", selectedQuality.label)
                        MetricPill("Audio", preferences.defaultAudioMode.label)
                        MetricPill("FPS", "${preferences.frameRate}")
                    }

                    Text(
                        "${selectedQuality.resolutionLabel} · ${selectedQuality.suggestedBitrate}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )

                    // Message chip
                    AnimatedVisibility(visible = recordingState.message != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(recordingState.message.orEmpty(), style = MaterialTheme.typography.bodySmall) },
                        )
                    }
                }
            }

            // ── Main action ───────────────────────────────────────────────
            AnimatedContent(
                targetState = Triple(isRecording, isPaused, Unit),
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "main_btn",
            ) { (rec, paused, _) ->
                PrimaryButton(
                    text = when { rec && !paused -> "Detener grabación"; paused -> "Reanudar"; else -> "Iniciar grabación" },
                    onClick = when { rec && !paused -> onStopRecording; paused -> onResumeRecording; else -> onStartRecording },
                    modifier = Modifier.fillMaxWidth(),
                    icon = {
                        Icon(
                            imageVector = when { rec && !paused -> Icons.Default.Stop; paused -> Icons.Default.PlayArrow; else -> Icons.Default.FiberManualRecord },
                            contentDescription = null,
                        )
                    },
                )
            }

            // ── Secondary controls ────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(
                    text = if (isPaused) "Reanudar" else "Pausar",
                    enabled = isRecording,
                    onClick = if (isPaused) onResumeRecording else onPauseRecording,
                    modifier = Modifier.weight(1f),
                )
                SecondaryActionButton(
                    text = "Detener",
                    enabled = isRecording,
                    onClick = onStopRecording,
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Nav row ───────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                NavSquareBtn(icon = Icons.Default.VideoLibrary, label = "Biblioteca", onClick = onOpenLibrary, modifier = Modifier.weight(1f))
                NavSquareBtn(icon = Icons.Default.Settings, label = "Ajustes", onClick = onOpenSettings, modifier = Modifier.weight(1f))
                NavSquareBtn(icon = Icons.Default.BubbleChart, label = "Burbuja", onClick = onEnableBubble, modifier = Modifier.weight(1f))
                NavSquareBtn(icon = Icons.Default.Refresh, label = "Actualizar", onClick = onRefresh, modifier = Modifier.weight(1f))
            }

            // ── Quick summary ─────────────────────────────────────────────
            SectionCard {
                Text("Configuración activa", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricPill("Carpeta", preferences.outputFolderName)
                    MetricPill("Encoder", preferences.videoEncoder.label.take(5))
                    MetricPill("Bitrate", if (preferences.bitrateMode == BitrateMode.AUTO) "Auto" else "${preferences.customBitrateMbps}M")
                }
            }

            Spacer(Modifier.height(20.dp))
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
) {
    PremiumBackground {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Atrás") }
                Spacer(Modifier.width(8.dp))
                Text("Biblioteca", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Actualizar") }
            }
            if (recordings.isEmpty()) {
                SectionCard {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text("Sin grabaciones aún", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Cuando detengas una grabación, aparecerá aquí automáticamente.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                Text("${recordings.size} grabación${if (recordings.size != 1) "es" else ""}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                recordings.forEach { recording ->
                    RecordingCard(recording = recording, onClick = { onOpen(recording) }, onDelete = { onDelete(recording) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─── Recording Detail ─────────────────────────────────────────────────────────

@Composable
fun RecordingDetailScreen(
    recording: RecordingFile,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
) {
    val context = LocalContext.current
    val player = remember(recording.filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(recording.mediaUri))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    var renameValue by remember(recording.displayName) { mutableStateOf(recording.displayName) }

    PremiumBackground {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Atrás") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            }
            SectionCard {
                AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxWidth().height(240.dp))
            }
            SectionCard {
                Text(recording.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricPill("Res", recording.resolution)
                    MetricPill("Dur", com.virexa.screen.util.formatDuration(recording.durationMs))
                    MetricPill("Peso", com.virexa.screen.util.formatBytes(recording.sizeBytes))
                }
                Text("Fecha: ${com.virexa.screen.util.formatDate(recording.createdAtMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text("Audio: ${recording.audioMode.label}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            SectionCard {
                Text("Renombrar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = renameValue, onValueChange = { renameValue = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Nuevo nombre") }, shape = RoundedCornerShape(22.dp), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryActionButton(text = "Renombrar", enabled = renameValue.isNotBlank(), onClick = { onRename(renameValue) }, modifier = Modifier.weight(1f))
                    val shareIntent = remember(recording.filePath) {
                        Intent(Intent.ACTION_SEND).apply { type = "video/mp4"; putExtra(Intent.EXTRA_STREAM, recording.mediaUri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    }
                    PrimaryButton(text = "Compartir", onClick = { context.startActivity(Intent.createChooser(shareIntent, "Compartir video")) }, modifier = Modifier.weight(1f), icon = { Icon(Icons.Default.Share, contentDescription = null) })
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
    onOpenAdvanced: () -> Unit = {},
) {
    PremiumBackground {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Atrás") }
                Spacer(Modifier.width(8.dp))
                Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }

            // Cuenta
            SectionCard {
                Text("Cuenta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = preferences.profileName, onValueChange = onUpdateName, modifier = Modifier.fillMaxWidth(), label = { Text("Nombre de perfil") }, shape = RoundedCornerShape(22.dp), singleLine = true)
                OutlinedTextField(value = preferences.outputFolderName, onValueChange = onUpdateFolder, modifier = Modifier.fillMaxWidth(), label = { Text("Carpeta de guardado") }, shape = RoundedCornerShape(22.dp), singleLine = true, leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) })
            }

            // Apariencia
            SectionCard {
                Text("Apariencia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Idioma", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageOption.entries.forEach { lang -> SecondaryPill(text = lang.label, selected = preferences.language == lang, onClick = { onUpdateLanguage(lang) }) }
                }
                Text("Tema", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                        SecondaryPill(text = when (mode) { ThemeMode.SYSTEM -> "Sistema"; ThemeMode.LIGHT -> "Claro"; ThemeMode.DARK -> "Oscuro" }, selected = preferences.themeMode == mode, onClick = { onUpdateTheme(mode) })
                    }
                }
            }

            // Calidad y audio
            SectionCard {
                Text("Video y audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Resolución", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QualityOption.presets.forEach { opt -> SecondaryPill(text = opt.label, selected = preferences.defaultQualityId == opt.id, onClick = { onUpdateQuality(opt.id) }) }
                }
                Text("Fuente de audio", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AudioMode.entries.forEach { mode -> AudioModeCard(mode = mode, selected = preferences.defaultAudioMode == mode, onClick = { onUpdateAudio(mode) }) }
            }

            // Burbuja
            SectionCard {
                Text("Burbuja flotante", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ToggleRow(title = "Activar burbuja", description = "Se muestra sobre otras apps durante la grabación.", checked = preferences.floatingBubbleEnabled, onCheckedChange = onUpdateBubble)
                ToggleRow(title = "Controles rápidos", description = "Muestra accesos directos dentro de la burbuja.", checked = preferences.showQuickControls, onCheckedChange = onUpdateQuickControls)
                if (preferences.floatingBubbleEnabled) {
                    PrimaryButton(text = "Abrir burbuja ahora", onClick = onStartBubble, modifier = Modifier.fillMaxWidth(), icon = { Icon(Icons.Default.BubbleChart, contentDescription = null) })
                }
            }

            // Avanzado
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                onClick = onOpenAdvanced,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Ajustes avanzados", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Text("Encoder, bitrate, FPS y más", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
) {
    val frameRateOptions = listOf(24, 30, 60)

    PremiumBackground {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Atrás") }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Ajustes avanzados", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Encoder · Bitrate · FPS · Comportamiento", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Encoder
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.VideoSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("Codificador de video", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Text("H.265 produce archivos más pequeños con la misma calidad; H.264 es más compatible con apps antiguas.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoEncoder.entries.forEach { enc ->
                        SecondaryPill(text = enc.label, selected = preferences.videoEncoder == enc, onClick = { onUpdateEncoder(enc) })
                    }
                }
            }

            // Bitrate
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("Tasa de bits (Bitrate)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BitrateMode.entries.forEach { mode ->
                        SecondaryPill(text = mode.label, selected = preferences.bitrateMode == mode, onClick = { onUpdateBitrateMode(mode) })
                    }
                }
                AnimatedVisibility(visible = preferences.bitrateMode == BitrateMode.CUSTOM) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bitrate personalizado", style = MaterialTheme.typography.labelMedium)
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(text = "${preferences.customBitrateMbps} Mbps", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Slider(
                            value = preferences.customBitrateMbps.toFloat(),
                            onValueChange = { onUpdateCustomBitrate(it.toInt()) },
                            valueRange = 1f..50f,
                            steps = 48,
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1 Mbps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("50 Mbps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (preferences.bitrateMode == BitrateMode.AUTO) {
                    Text("El bitrate se selecciona automáticamente según la resolución elegida.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }

            // FPS
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("Frames por segundo (FPS)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Text("60 FPS es ideal para contenido de juegos. 30 FPS consume menos batería.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    frameRateOptions.forEach { fps ->
                        SecondaryPill(text = "$fps fps", selected = preferences.frameRate == fps, onClick = { onUpdateFrameRate(fps) })
                    }
                }
            }

            // Comportamiento
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("Comportamiento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                ToggleRow(title = "Timer en la burbuja", description = "Muestra el tiempo transcurrido dentro de la ventana flotante.", checked = preferences.showTimerOnBubble, onCheckedChange = onUpdateShowTimerOnBubble)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow(title = "Pausar al recibir llamada", description = "Pausa la grabación automáticamente cuando entra una llamada.", checked = preferences.autoPauseOnCall, onCheckedChange = onUpdateAutoPauseOnCall)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow(title = "Mantener pantalla encendida", description = "Evita que el dispositivo se bloquee durante la grabación.", checked = preferences.keepScreenOn, onCheckedChange = onUpdateKeepScreenOn)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ToggleRow(title = "Indicador de toque", description = "Muestra círculos donde tocas la pantalla durante la grabación.", checked = preferences.showTouchIndicator, onCheckedChange = onUpdateShowTouchIndicator)
            }

            // Info card
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                    Text(
                        "Los cambios en encoder, bitrate y FPS se aplican en la siguiente grabación. Si el dispositivo no soporta H.265, Virexa usa H.264 automáticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun NavSquareBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
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
    val totalSec = ms / 1000
    val h = totalSec / 3600; val m = (totalSec % 3600) / 60; val s = totalSec % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
