package com.virexa.screen.data

import java.util.UUID

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class LanguageOption(val label: String) {
    SPANISH("Español"),
    ENGLISH("English");
}

enum class AudioMode(
    val label: String,
    val description: String,
    val usesMicrophone: Boolean,
    val requestsSystemAudio: Boolean,
    val requestsCallAudio: Boolean,
) {
    NONE(
        label = "Sin audio",
        description = "Solo captura de pantalla, sin micrófono ni audio interno.",
        usesMicrophone = false,
        requestsSystemAudio = false,
        requestsCallAudio = false,
    ),
    MICROPHONE(
        label = "Micrófono",
        description = "Graba tu voz o el ambiente con el micrófono del dispositivo.",
        usesMicrophone = true,
        requestsSystemAudio = false,
        requestsCallAudio = false,
    ),
    SYSTEM(
        label = "Audio del sistema",
        description = "Captura el audio reproducido por aplicaciones compatibles. Puede estar limitado por el sistema.",
        usesMicrophone = false,
        requestsSystemAudio = true,
        requestsCallAudio = false,
    ),
    MIXED(
        label = "Micrófono + sistema",
        description = "Combina tu voz con el audio interno cuando el dispositivo lo permite.",
        usesMicrophone = true,
        requestsSystemAudio = true,
        requestsCallAudio = false,
    ),
    CALLS(
        label = "Llamadas / apps de voz",
        description = "Intenta capturar audio de llamadas o apps de voz solo en dispositivos compatibles y con las restricciones del sistema.",
        usesMicrophone = true,
        requestsSystemAudio = false,
        requestsCallAudio = true,
    );
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
            QualityOption(
                id = "720p",
                label = "HD",
                resolutionLabel = "1280 × 720",
                width = 1280,
                height = 720,
                aspectRatio = "16:9",
                suggestedBitrate = "3–5 Mbps",
                estimatedSizePerMinute = "20–35 MB",
                batteryImpact = "Bajo",
            ),
            QualityOption(
                id = "1080p",
                label = "Full HD",
                resolutionLabel = "1920 × 1080",
                width = 1920,
                height = 1080,
                aspectRatio = "16:9",
                suggestedBitrate = "6–10 Mbps",
                estimatedSizePerMinute = "45–75 MB",
                batteryImpact = "Medio",
            ),
            QualityOption(
                id = "1440p",
                label = "2K / QHD",
                resolutionLabel = "2560 × 1440",
                width = 2560,
                height = 1440,
                aspectRatio = "16:9",
                suggestedBitrate = "12–18 Mbps",
                estimatedSizePerMinute = "90–140 MB",
                batteryImpact = "Alto",
            ),
            QualityOption(
                id = "2160p",
                label = "4K",
                resolutionLabel = "3840 × 2160",
                width = 3840,
                height = 2160,
                aspectRatio = "16:9",
                suggestedBitrate = "20–35 Mbps",
                estimatedSizePerMinute = "150–260 MB",
                batteryImpact = "Muy alto",
            ),
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
}

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val activeFilePath: String? = null,
    val message: String? = null,
)
