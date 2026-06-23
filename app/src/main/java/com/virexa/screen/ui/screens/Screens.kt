package com.virexa.screen.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.virexa.screen.data.AudioMode
import com.virexa.screen.data.LanguageOption
import com.virexa.screen.data.QualityOption
import com.virexa.screen.data.RecordingFile
import com.virexa.screen.data.RecordingUiState
import com.virexa.screen.data.ThemeMode
import com.virexa.screen.data.UserPreferences
import com.virexa.screen.ui.components.AudioModeCard
import com.virexa.screen.ui.components.BottomTabBar
import com.virexa.screen.ui.components.BrandMark
import com.virexa.screen.ui.components.BubbleActionButton
import com.virexa.screen.ui.components.BubbleMiniLabel
import com.virexa.screen.ui.components.MetricPill
import com.virexa.screen.ui.components.PermissionStatusCard
import com.virexa.screen.ui.components.PremiumBackground
import com.virexa.screen.ui.components.PremiumScreenHeader
import com.virexa.screen.ui.components.PrimaryButton
import com.virexa.screen.ui.components.QualityCard
import com.virexa.screen.ui.components.RecordingCard
import com.virexa.screen.ui.components.SecondaryActionButton
import com.virexa.screen.ui.components.SecondaryPill
import com.virexa.screen.ui.components.SectionCard
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(900)
        onDone()
    }

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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            BrandMark()
            PremiumScreenHeader(
                title = "Tu perfil, listo en minutos",
                subtitle = "Virexa ajusta la calidad, el audio y la burbuja flotante con una interfaz más simple y menos técnica.",
            )

            SectionCard {
                OutlinedTextField(
                    value = preferences.profileName,
                    onValueChange = onUpdateName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre o alias") },
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                )
                Text("Idioma", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    LanguageOption.entries.forEach { lang ->
                        SecondaryPill(text = lang.label, selected = preferences.language == lang, onClick = { onUpdateLanguage(lang) })
                    }
                }
            }

            SectionCard {
                Text("Tema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                        SecondaryPill(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "Sistema"
                                ThemeMode.LIGHT -> "Claro"
                                ThemeMode.DARK -> "Oscuro"
                            },
                            selected = preferences.themeMode == mode,
                            onClick = { onUpdateTheme(mode) },
                        )
                    }
                }
            }

            SectionCard {
                Text("Calidad predeterminada", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("La interfaz traduce los números a etiquetas claras como HD, Full HD, 2K y 4K.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                QualityCard(option = selectedQuality, selected = true, onClick = {})
                QualityOption.presets.forEach { option ->
                    if (option.id != selectedQuality.id) {
                        QualityCard(option = option, selected = false, onClick = { onUpdateQuality(option.id) })
                    }
                }
            }

            SectionCard {
                Text("Audio predeterminado", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                AudioMode.entries.forEach { mode ->
                    AudioModeCard(mode = mode, selected = preferences.defaultAudioMode == mode, onClick = { onUpdateAudio(mode) })
                }
            }

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Burbuja flotante", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Control rápido sobre otras apps. Se oculta durante la captura activa para no entrar al video.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = preferences.floatingBubbleEnabled, onCheckedChange = onUpdateBubble)
                }
            }

            PrimaryButton(
                text = "Continuar",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                icon = { androidx.compose.material3.Icon(Icons.Default.ArrowForward, contentDescription = null) },
            )

            Spacer(Modifier.height(18.dp))
        }
    }
}

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

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Virexa Screen", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    Text("Grabación premium con una estructura simple.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BrandMark(modifier = Modifier.size(56.dp))
            }

            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricPill("Calidad", selectedQuality.label)
                    MetricPill("Audio", preferences.defaultAudioMode.label)
                    MetricPill("Estado", if (recordingState.isRecording) if (recordingState.isPaused) "Pausado" else "Grabando" else "Listo")
                }
                Text(selectedQuality.displayTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${selectedQuality.resolutionLabel} · ${selectedQuality.aspectRatio} · ${selectedQuality.suggestedBitrate}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AnimatedVisibility(visible = recordingState.message != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text(recordingState.message.orEmpty()) },
                        leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Refresh, contentDescription = null) },
                    )
                }
            }

            PrimaryButton(
                text = when {
                    recordingState.isRecording && !recordingState.isPaused -> "Detener"
                    recordingState.isPaused -> "Reanudar"
                    else -> "Iniciar grabación"
                },
                onClick = {
                    when {
                        recordingState.isRecording && !recordingState.isPaused -> onStopRecording()
                        recordingState.isPaused -> onResumeRecording()
                        else -> onStartRecording()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = when {
                            recordingState.isRecording && !recordingState.isPaused -> Icons.Default.Stop
                            recordingState.isPaused -> Icons.Default.PlayArrow
                            else -> Icons.Default.FiberManualRecord
                        },
                        contentDescription = null,
                    )
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(
                    text = "Pausar",
                    enabled = recordingState.isRecording && !recordingState.isPaused,
                    onClick = onPauseRecording,
                    modifier = Modifier.weight(1f),
                )
                SecondaryActionButton(
                    text = "Detener",
                    enabled = recordingState.isRecording,
                    onClick = onStopRecording,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(text = "Biblioteca", enabled = true, onClick = onOpenLibrary, modifier = Modifier.weight(1f))
                SecondaryActionButton(text = "Ajustes", enabled = true, onClick = onOpenSettings, modifier = Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryActionButton(
                    text = "Ventana flotante",
                    enabled = preferences.floatingBubbleEnabled,
                    onClick = onEnableBubble,
                    modifier = Modifier.weight(1f),
                )
                SecondaryActionButton(
                    text = "Actualizar",
                    enabled = true,
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Burbuja flotante", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Movible, compacta y menos invasiva. Abre su panel desde el botón de ventana flotante.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onEnableBubble) { Text(if (preferences.floatingBubbleEnabled) "Gestionar" else "Activar") }
                }
            }

            SectionCard {
                Text("Ajuste rápido", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${selectedQuality.displayTitle} · ${preferences.defaultAudioMode.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricPill("Guardar en", preferences.outputFolderName)
                    MetricPill("Burbuja", if (preferences.floatingBubbleEnabled) "Sí" else "No")
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Atrás") }
                Spacer(Modifier.width(8.dp))
                Text("Biblioteca", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            SectionCard {
                Text("Tus grabaciones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Abre, comparte, renombra o elimina videos guardados en el dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRefresh) { Text("Actualizar") }
            }
            if (recordings.isEmpty()) {
                SectionCard {
                    Text("Todavía no hay grabaciones.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Cuando detengas una grabación, aparecerá aquí automáticamente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                recordings.forEach { recording ->
                    RecordingCard(recording = recording, onClick = { onOpen(recording) }, onDelete = { onDelete(recording) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

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
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(recording.filePath))))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var renameValue by remember(recording.displayName) { mutableStateOf(recording.displayName) }

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Atrás") }
                Spacer(Modifier.width(8.dp))
                Text("Detalle", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("Eliminar") }
            }
            SectionCard {
                AndroidView(
                    factory = { PlayerView(it).apply { this.player = player } },
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                )
            }
            SectionCard {
                Text(recording.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Resolución: ${recording.resolution}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Duración: ${com.virexa.screen.util.formatDuration(recording.durationMs)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tamaño: ${com.virexa.screen.util.formatBytes(recording.sizeBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Fecha: ${com.virexa.screen.util.formatDate(recording.createdAtMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionCard {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nuevo nombre") },
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryActionButton(text = "Renombrar", enabled = renameValue.isNotBlank(), onClick = { onRename(renameValue) }, modifier = Modifier.weight(1f))
                    val shareIntent = remember(recording.filePath) {
                        val file = File(recording.filePath)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        Intent(Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    PrimaryButton(
                        text = "Compartir",
                        onClick = { context.startActivity(Intent.createChooser(shareIntent, "Compartir video")) },
                        modifier = Modifier.weight(1f),
                        icon = { androidx.compose.material3.Icon(Icons.Default.Share, contentDescription = null) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

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
) {
    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Atrás") }
                Spacer(Modifier.width(8.dp))
                Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onStartBubble) { Text("Burbuja") }
            }

            SectionCard {
                Text("Cuenta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = preferences.profileName,
                    onValueChange = onUpdateName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Perfil") },
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = preferences.outputFolderName,
                    onValueChange = onUpdateFolder,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Carpeta de guardado") },
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                )
            }

            SectionCard {
                Text("Idioma y tema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    LanguageOption.entries.forEach { lang ->
                        SecondaryPill(text = lang.label, selected = preferences.language == lang, onClick = { onUpdateLanguage(lang) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                        SecondaryPill(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "Sistema"
                                ThemeMode.LIGHT -> "Claro"
                                ThemeMode.DARK -> "Oscuro"
                            },
                            selected = preferences.themeMode == mode,
                            onClick = { onUpdateTheme(mode) },
                        )
                    }
                }
            }

            SectionCard {
                Text("Video y audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                AudioMode.entries.forEach { mode ->
                    AudioModeCard(mode = mode, selected = preferences.defaultAudioMode == mode, onClick = { onUpdateAudio(mode) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    QualityOption.presets.forEach { option ->
                        SecondaryPill(text = option.label, selected = preferences.defaultQualityId == option.id, onClick = { onUpdateQuality(option.id) })
                    }
                }
            }

            SectionCard {
                Text("Burbuja y controles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ToggleRow(
                    title = "Burbuja flotante",
                    description = "Se muestra sobre otras apps y se oculta durante la captura activa.",
                    checked = preferences.floatingBubbleEnabled,
                    onCheckedChange = onUpdateBubble,
                )
                ToggleRow(
                    title = "Controles rápidos",
                    description = "Muestra accesos directos dentro de la burbuja.",
                    checked = preferences.showQuickControls,
                    onCheckedChange = onUpdateQuickControls,
                )
                TextButton(onClick = onStartBubble) { Text("Activar burbuja ahora") }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
