package com.virexa.screen.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virexa.screen.data.*
import com.virexa.screen.service.FloatingBubbleService
import com.virexa.screen.service.ScreenRecordService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)
    private val recordingsRepo = RecordingRepository(application)

    val preferences: StateFlow<UserPreferences> = prefs.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    val recordingUiState: StateFlow<RecordingUiState> = RecordingSession.uiState

    private val _recordings = MutableStateFlow<List<RecordingFile>>(emptyList())
    val recordings: StateFlow<List<RecordingFile>> = _recordings.asStateFlow()

    val stats: StateFlow<RecordingStats> = _recordings.map { StatsRepository.compute(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordingStats())

    // Countdown state for UI
    private val _countdown = MutableStateFlow(0)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private var lastSyncedSavedPath: String? = null

    init {
        refreshRecordings()
        viewModelScope.launch {
            RecordingSession.uiState.collectLatest { state ->
                if (state.activeFilePath != null && state.activeFilePath != lastSyncedSavedPath && !state.isRecording) {
                    lastSyncedSavedPath = state.activeFilePath
                    refreshRecordings()
                }
            }
        }
    }

    fun refreshRecordings() {
        viewModelScope.launch {
            _recordings.value = recordingsRepo.listRecordings(preferences.value.outputFolderName)
        }
    }

    // ── Preferences ───────────────────────────────────────────────────────────
    fun updateProfileName(v: String) = launch { prefs.updateProfileName(v) }
    fun updateLanguage(v: LanguageOption) = launch { prefs.updateLanguage(v) }
    fun updateThemeMode(v: ThemeMode) = launch { prefs.updateThemeMode(v) }
    fun updateDefaultQuality(v: String) = launch { prefs.updateDefaultQualityId(v) }
    fun updateDefaultAudioMode(v: AudioMode) = launch { prefs.updateDefaultAudioMode(v) }
    fun updateFloatingBubbleEnabled(v: Boolean) = launch { prefs.updateFloatingBubbleEnabled(v) }
    fun updateShowQuickControls(v: Boolean) = launch { prefs.updateShowQuickControls(v) }
    fun updateOutputFolder(v: String) = launch { prefs.updateOutputFolderName(v) }
    fun completeOnboarding() = launch { prefs.markOnboardingCompleted() }
    fun updateVideoEncoder(v: VideoEncoder) = launch { prefs.updateVideoEncoder(v) }
    fun updateBitrateMode(v: BitrateMode) = launch { prefs.updateBitrateMode(v) }
    fun updateCustomBitrateMbps(v: Int) = launch { prefs.updateCustomBitrateMbps(v) }
    fun updateFrameRate(v: Int) = launch { prefs.updateFrameRate(v) }
    fun updateShowTimerOnBubble(v: Boolean) = launch { prefs.updateShowTimerOnBubble(v) }
    fun updateAutoPauseOnCall(v: Boolean) = launch { prefs.updateAutoPauseOnCall(v) }
    fun updateKeepScreenOn(v: Boolean) = launch { prefs.updateKeepScreenOn(v) }
    fun updateShowTouchIndicator(v: Boolean) = launch { prefs.updateShowTouchIndicator(v) }
    // New
    fun updateCountdownOption(v: CountdownOption) = launch { prefs.updateCountdownOption(v) }
    fun updateMaxDurationMinutes(v: Int) = launch { prefs.updateMaxDurationMinutes(v) }
    fun updateWatermarkText(v: String) = launch { prefs.updateWatermarkText(v) }
    fun updateWatermarkEnabled(v: Boolean) = launch { prefs.updateWatermarkEnabled(v) }
    fun updateMicBoostLevel(v: MicBoostLevel) = launch { prefs.updateMicBoostLevel(v) }
    fun updateNoiseSuppression(v: Boolean) = launch { prefs.updateNoiseSuppression(v) }
    fun updateSilenceAutoPause(v: Boolean) = launch { prefs.updateSilenceAutoPause(v) }
    fun updateSilenceThresholdSeconds(v: Int) = launch { prefs.updateSilenceThresholdSeconds(v) }
    fun updateDoNotDisturb(v: Boolean) = launch { prefs.updateDoNotDisturb(v) }
    fun updateHapticFeedback(v: Boolean) = launch { prefs.updateHapticFeedback(v) }
    fun updateAutoShareAfterStop(v: Boolean) = launch { prefs.updateAutoShareAfterStop(v) }
    fun updateQuickShareTarget(v: String) = launch { prefs.updateQuickShareTarget(v) }

    // ── Recording ──────────────────────────────────────────────────────────────
    fun startRecordingWithCountdown(
        permissionResultCode: Int,
        permissionData: Intent,
        quality: QualityOption,
        audioMode: AudioMode,
    ) {
        val p = preferences.value
        val countdownSecs = p.countdownOption.seconds
        if (countdownSecs <= 0) {
            startRecording(permissionResultCode, permissionData, quality, audioMode)
            return
        }
        viewModelScope.launch {
            for (i in countdownSecs downTo 1) {
                RecordingSession.setCountdown(i)
                _countdown.value = i
                kotlinx.coroutines.delay(1_000)
            }
            RecordingSession.setCountdown(0)
            _countdown.value = 0
            startRecording(permissionResultCode, permissionData, quality, audioMode)
        }
    }

    fun startRecording(permissionResultCode: Int, permissionData: Intent, quality: QualityOption, audioMode: AudioMode) {
        val context = getApplication<Application>()
        val p = preferences.value
        val bitrate = when (p.bitrateMode) {
            BitrateMode.CUSTOM -> p.customBitrateMbps * 1_000_000
            BitrateMode.AUTO -> bitrateForQuality(quality)
        }
        val maxDurationMs = if (p.maxDurationMinutes > 0) p.maxDurationMinutes * 60_000L else 0L

        val serviceIntent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, permissionResultCode)
            putExtra(ScreenRecordService.EXTRA_DATA, permissionData)
            putExtra(ScreenRecordService.EXTRA_WIDTH, quality.width)
            putExtra(ScreenRecordService.EXTRA_HEIGHT, quality.height)
            putExtra(ScreenRecordService.EXTRA_DENSITY, context.resources.displayMetrics.densityDpi)
            putExtra(ScreenRecordService.EXTRA_FPS, p.frameRate.takeIf { it > 0 } ?: quality.frameRate)
            putExtra(ScreenRecordService.EXTRA_BITRATE, bitrate)
            putExtra(ScreenRecordService.EXTRA_AUDIO_MODE, audioMode.name)
            putExtra(ScreenRecordService.EXTRA_OUTPUT_FOLDER, p.outputFolderName)
            putExtra(ScreenRecordService.EXTRA_ENCODER, p.videoEncoder.name)
            putExtra(ScreenRecordService.EXTRA_WATERMARK, if (p.watermarkEnabled) p.watermarkText else "")
            putExtra(ScreenRecordService.EXTRA_MAX_DURATION_MS, maxDurationMs)
            putExtra(ScreenRecordService.EXTRA_SILENCE_AUTO_PAUSE, p.silenceAutoPause)
            putExtra(ScreenRecordService.EXTRA_SILENCE_THRESHOLD_S, p.silenceThresholdSeconds)
            putExtra(ScreenRecordService.EXTRA_NOISE_SUPPRESSION, p.noiseSuppression)
            putExtra(ScreenRecordService.EXTRA_MIC_BOOST, p.micBoostLevel.name)
            putExtra(ScreenRecordService.EXTRA_DND, p.doNotDisturbDuringRecording)
            putExtra(ScreenRecordService.EXTRA_SHOW_BUBBLE, p.floatingBubbleEnabled)
        }
        androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
        RecordingSession.update { it.copy(message = "Iniciando grabación…") }
    }

    fun pauseRecording() = sendAction(ScreenRecordService.ACTION_PAUSE)
    fun resumeRecording() = sendAction(ScreenRecordService.ACTION_RESUME)
    fun stopRecording() = sendAction(ScreenRecordService.ACTION_STOP)

    fun startBubbleService() {
        val ctx = getApplication<Application>()
        androidx.core.content.ContextCompat.startForegroundService(ctx, Intent(ctx, FloatingBubbleService::class.java))
    }

    fun stopBubbleService() {
        getApplication<Application>().stopService(Intent(getApplication(), FloatingBubbleService::class.java))
    }

    fun deleteRecording(file: RecordingFile) = viewModelScope.launch {
        if (recordingsRepo.deleteRecording(file)) refreshRecordings()
    }

    fun renameRecording(file: RecordingFile, newName: String) = viewModelScope.launch {
        recordingsRepo.renameRecording(file, newName)?.let { refreshRecordings() }
    }

    private fun sendAction(action: String) {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, ScreenRecordService::class.java).apply { this.action = action })
    }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }

    private fun bitrateForQuality(quality: QualityOption) = when (quality.id) {
        "720p" -> 4_000_000; "1080p" -> 8_000_000; "1440p" -> 14_000_000; else -> 24_000_000
    }
}
