package com.virexa.screen.ui

import android.app.Application
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virexa.screen.data.*
import com.virexa.screen.service.ScreenRecordService
import com.virexa.screen.service.FloatingBubbleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)
    private val recordingsRepo = RecordingRepository(application)

    val preferences: StateFlow<UserPreferences> = prefs.preferencesFlow.stateInViewModel(
        viewModelScope,
        UserPreferences()
    )

    val recordingUiState: StateFlow<RecordingUiState> = RecordingSession.uiState

    private val _recordings = MutableStateFlow<List<RecordingFile>>(emptyList())
    val recordings: StateFlow<List<RecordingFile>> = _recordings.asStateFlow()

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

    fun updateProfileName(value: String) = viewModelScope.launch { prefs.updateProfileName(value) }
    fun updateLanguage(value: LanguageOption) = viewModelScope.launch { prefs.updateLanguage(value) }
    fun updateThemeMode(value: ThemeMode) = viewModelScope.launch { prefs.updateThemeMode(value) }
    fun updateDefaultQuality(value: String) = viewModelScope.launch { prefs.updateDefaultQualityId(value) }
    fun updateDefaultAudioMode(value: AudioMode) = viewModelScope.launch { prefs.updateDefaultAudioMode(value) }
    fun updateFloatingBubbleEnabled(value: Boolean) = viewModelScope.launch { prefs.updateFloatingBubbleEnabled(value) }
    fun updateShowQuickControls(value: Boolean) = viewModelScope.launch { prefs.updateShowQuickControls(value) }
    fun updateOutputFolder(value: String) = viewModelScope.launch { prefs.updateOutputFolderName(value) }
    fun completeOnboarding() = viewModelScope.launch { prefs.markOnboardingCompleted() }

    fun startRecording(
        permissionResultCode: Int,
        permissionData: Intent,
        quality: QualityOption,
        audioMode: AudioMode,
    ) {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, permissionResultCode)
            putExtra(ScreenRecordService.EXTRA_DATA, permissionData)
            putExtra(ScreenRecordService.EXTRA_WIDTH, quality.width)
            putExtra(ScreenRecordService.EXTRA_HEIGHT, quality.height)
            putExtra(ScreenRecordService.EXTRA_DENSITY, context.resources.displayMetrics.densityDpi)
            putExtra(ScreenRecordService.EXTRA_FPS, quality.frameRate)
            putExtra(ScreenRecordService.EXTRA_BITRATE, bitrateForQuality(quality))
            putExtra(ScreenRecordService.EXTRA_AUDIO_MODE, audioMode.name)
            putExtra(ScreenRecordService.EXTRA_OUTPUT_FOLDER, preferences.value.outputFolderName)
        }
        androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
        RecordingSession.update { it.copy(message = "Solicitando captura de pantalla") }
    }

    fun pauseRecording() = sendServiceAction(ScreenRecordService.ACTION_PAUSE)
    fun resumeRecording() = sendServiceAction(ScreenRecordService.ACTION_RESUME)
    fun stopRecording() = sendServiceAction(ScreenRecordService.ACTION_STOP)

    fun startBubbleService() {
        val context = getApplication<Application>()
        androidx.core.content.ContextCompat.startForegroundService(
            context,
            Intent(context, FloatingBubbleService::class.java)
        )
    }

    fun stopBubbleService() {
        getApplication<Application>().stopService(Intent(getApplication(), FloatingBubbleService::class.java))
    }

    private fun sendServiceAction(action: String) {
        val context = getApplication<Application>()
        context.startService(Intent(context, ScreenRecordService::class.java).apply { this.action = action })
    }

    fun deleteRecording(file: RecordingFile) {
        viewModelScope.launch {
            if (recordingsRepo.deleteRecording(file)) refreshRecordings()
        }
    }

    fun renameRecording(file: RecordingFile, newName: String) {
        viewModelScope.launch {
            recordingsRepo.renameRecording(file, newName)?.let {
                refreshRecordings()
            }
        }
    }

    private fun bitrateForQuality(quality: QualityOption): Int = when (quality.id) {
        "720p" -> 4_000_000
        "1080p" -> 8_000_000
        "1440p" -> 14_000_000
        else -> 24_000_000
    }
}

private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInViewModel(scope: kotlinx.coroutines.CoroutineScope, initialValue: T): StateFlow<T> {
    return kotlinx.coroutines.flow.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), initialValue)
}
