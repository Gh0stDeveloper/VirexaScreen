package com.virexa.screen.data

import android.net.Uri
import java.io.File
import java.util.UUID

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class LanguageOption(val label: String) {
    SPANISH("Español"),
    ENGLISH("English"),
}

enum class VideoEncoder(val label: String, val codecName: String) {
    H264("H.264 (AVC)", "video/avc"),
    H265("H.265 (HEVC)", "video/hevc"),
}

enum class BitrateMode(val label: String) {
    AUTO("Automático"),
    CUSTOM("Personalizado"),
}

enum class CountdownOption(val label: String, val seconds: Int) {
    NONE("Sin cuenta", 0),
    THREE("3 segundos", 3),
    FIVE("5 segundos", 5),
    TEN("10 segundos", 10),
}

enum class MicBoostLevel(val label: String, val gain: Float) {
    NORMAL("Normal", 1.0f),
    MEDIUM("Medio", 1.5f),
    HIGH("Alto", 2.0f),
}

enum class AudioMode(
    val label: String,
    val description: String,
    val usesMicrophone: Boolean,
    val requestsSystemAudio: Boolean,
    val requestsCallAudio: Boolean,
) {
    NONE("Sin audio", "Solo captura de pantalla, sin micrófono ni audio interno.", false, false, false),
    MICROPHONE("Micrófono", "Graba tu voz o el ambiente con el micrófono del dispositivo.", true, false, false),
    SYSTEM("Audio del sistema", "Captura el audio reproducido por aplicaciones compatibles.", false, true, false),
    MIXED("Micrófono + sistema", "Combina tu voz con el audio interno cuando el dispositivo lo permite.", true, true, false),
    CALLS("Llamadas / apps de voz", "Intenta capturar audio de llamadas o apps de voz en dispositivos compatibles.", true, false, true),
}

data class QualityOption(
    val id: String,
    val label: String,
    val resolutionLabel: String,
    val width: Int,
    val height: Int,
    val aspectRatio: String,
    val suggestedBitrate: String,
    val estimatedSizePerMinute: String,
    val batteryImpact: String,
    val frameRate: Int = 60,
) {
    val displayTitle: String get() = "$resolutionLabel — $label"

    companion object {
        val presets = listOf(
            QualityOption("720p", "HD", "1280 × 720", 1280, 720, "16:9", "3–5 Mbps", "20–35 MB", "Bajo"),
            QualityOption("1080p", "Full HD", "1920 × 1080", 1920, 1080, "16:9", "6–10 Mbps", "45–75 MB", "Medio"),
            QualityOption("1440p", "2K / QHD", "2560 × 1440", 2560, 1440, "16:9", "12–18 Mbps", "90–140 MB", "Alto"),
            QualityOption("2160p", "4K", "3840 × 2160", 3840, 2160, "16:9", "20–35 Mbps", "150–260 MB", "Muy alto"),
        )
        fun default() = presets[1]
        fun fromId(id: String?) = presets.firstOrNull { it.id == id } ?: default()
    }
}

data class UserPreferences(
    val profileName: String = "Usuario",
    val language: LanguageOption = LanguageOption.SPANISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultQualityId: String = QualityOption.default().id,
    val defaultAudioMode: AudioMode = AudioMode.MICROPHONE,
    val floatingBubbleEnabled: Boolean = true,
    val showQuickControls: Boolean = true,
    val outputFolderName: String = "VirexaScreen",
    val onboardingCompleted: Boolean = false,
    // Advanced video
    val videoEncoder: VideoEncoder = VideoEncoder.H264,
    val bitrateMode: BitrateMode = BitrateMode.AUTO,
    val customBitrateMbps: Int = 8,
    val frameRate: Int = 60,
    // Advanced behavior
    val showTimerOnBubble: Boolean = true,
    val autoPauseOnCall: Boolean = false,
    val keepScreenOn: Boolean = true,
    val showTouchIndicator: Boolean = false,
    // Grabación - nuevos
    val countdownOption: CountdownOption = CountdownOption.THREE,
    val maxDurationMinutes: Int = 0,          // 0 = sin límite
    val watermarkText: String = "",            // vacío = sin marca de agua
    val watermarkEnabled: Boolean = false,
    // Audio avanzado
    val micBoostLevel: MicBoostLevel = MicBoostLevel.NORMAL,
    val noiseSuppression: Boolean = false,
    val silenceAutoPause: Boolean = false,
    val silenceThresholdSeconds: Int = 10,
    // UX / accesibilidad
    val doNotDisturbDuringRecording: Boolean = false,
    val hapticFeedback: Boolean = true,
    val autoShareAfterStop: Boolean = false,
    val quickShareTarget: String = "",          // package name del app destino
)

data class RecordingFile(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val resolution: String,
    val createdAtMillis: Long,
    val audioMode: AudioMode,
) {
    val displayName: String get() = fileName.removeSuffix(".mp4")
    val isContentUri: Boolean get() = filePath.startsWith("content://")
    val mediaUri: Uri get() = if (isContentUri) Uri.parse(filePath) else Uri.fromFile(File(filePath))
}

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val activeFilePath: String? = null,
    val message: String? = null,
    val elapsedMs: Long = 0L,
    val countdown: Int = 0,               // 0 = no countdown activo
    val silenceDetected: Boolean = false,
)

data class RecordingStats(
    val totalRecordings: Int = 0,
    val totalDurationMs: Long = 0L,
    val totalSizeBytes: Long = 0L,
    val longestDurationMs: Long = 0L,
    val largestSizeBytes: Long = 0L,
    val averageDurationMs: Long = 0L,
    val thisWeekCount: Int = 0,
    val thisMonthCount: Int = 0,
)
