package com.virexa.screen.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("virexa_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val profileName = stringPreferencesKey("profile_name")
        val language = stringPreferencesKey("language")
        val themeMode = stringPreferencesKey("theme_mode")
        val defaultQualityId = stringPreferencesKey("default_quality_id")
        val defaultAudioMode = stringPreferencesKey("default_audio_mode")
        val floatingBubbleEnabled = booleanPreferencesKey("floating_bubble_enabled")
        val showQuickControls = booleanPreferencesKey("show_quick_controls")
        val outputFolderName = stringPreferencesKey("output_folder_name")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val videoEncoder = stringPreferencesKey("video_encoder")
        val bitrateMode = stringPreferencesKey("bitrate_mode")
        val customBitrateMbps = intPreferencesKey("custom_bitrate_mbps")
        val frameRate = intPreferencesKey("frame_rate")
        val showTimerOnBubble = booleanPreferencesKey("show_timer_on_bubble")
        val autoPauseOnCall = booleanPreferencesKey("auto_pause_on_call")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val showTouchIndicator = booleanPreferencesKey("show_touch_indicator")
        // NEW
        val countdownOption = stringPreferencesKey("countdown_option")
        val maxDurationMinutes = intPreferencesKey("max_duration_minutes")
        val watermarkText = stringPreferencesKey("watermark_text")
        val watermarkEnabled = booleanPreferencesKey("watermark_enabled")
        val micBoostLevel = stringPreferencesKey("mic_boost_level")
        val noiseSuppression = booleanPreferencesKey("noise_suppression")
        val silenceAutoPause = booleanPreferencesKey("silence_auto_pause")
        val silenceThresholdSeconds = intPreferencesKey("silence_threshold_seconds")
        val doNotDisturbDuringRecording = booleanPreferencesKey("dnd_during_recording")
        val hapticFeedback = booleanPreferencesKey("haptic_feedback")
        val autoShareAfterStop = booleanPreferencesKey("auto_share_after_stop")
        val quickShareTarget = stringPreferencesKey("quick_share_target")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { p ->
        UserPreferences(
            profileName = p[Keys.profileName] ?: "Usuario",
            language = enumOf(p[Keys.language], LanguageOption.SPANISH),
            themeMode = enumOf(p[Keys.themeMode], ThemeMode.SYSTEM),
            defaultQualityId = p[Keys.defaultQualityId] ?: QualityOption.default().id,
            defaultAudioMode = enumOf(p[Keys.defaultAudioMode], AudioMode.MICROPHONE),
            floatingBubbleEnabled = p[Keys.floatingBubbleEnabled] ?: true,
            showQuickControls = p[Keys.showQuickControls] ?: true,
            outputFolderName = p[Keys.outputFolderName] ?: "VirexaScreen",
            onboardingCompleted = p[Keys.onboardingCompleted] ?: false,
            videoEncoder = enumOf(p[Keys.videoEncoder], VideoEncoder.H264),
            bitrateMode = enumOf(p[Keys.bitrateMode], BitrateMode.AUTO),
            customBitrateMbps = p[Keys.customBitrateMbps] ?: 8,
            frameRate = p[Keys.frameRate] ?: 60,
            showTimerOnBubble = p[Keys.showTimerOnBubble] ?: true,
            autoPauseOnCall = p[Keys.autoPauseOnCall] ?: false,
            keepScreenOn = p[Keys.keepScreenOn] ?: true,
            showTouchIndicator = p[Keys.showTouchIndicator] ?: false,
            countdownOption = enumOf(p[Keys.countdownOption], CountdownOption.THREE),
            maxDurationMinutes = p[Keys.maxDurationMinutes] ?: 0,
            watermarkText = p[Keys.watermarkText] ?: "",
            watermarkEnabled = p[Keys.watermarkEnabled] ?: false,
            micBoostLevel = enumOf(p[Keys.micBoostLevel], MicBoostLevel.NORMAL),
            noiseSuppression = p[Keys.noiseSuppression] ?: false,
            silenceAutoPause = p[Keys.silenceAutoPause] ?: false,
            silenceThresholdSeconds = p[Keys.silenceThresholdSeconds] ?: 10,
            doNotDisturbDuringRecording = p[Keys.doNotDisturbDuringRecording] ?: false,
            hapticFeedback = p[Keys.hapticFeedback] ?: true,
            autoShareAfterStop = p[Keys.autoShareAfterStop] ?: false,
            quickShareTarget = p[Keys.quickShareTarget] ?: "",
        )
    }

    private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
        runCatching { if (name != null) enumValueOf<T>(name) else default }.getOrDefault(default)

    suspend fun updateProfileName(v: String) = edit { it[Keys.profileName] = v }
    suspend fun updateLanguage(v: LanguageOption) = edit { it[Keys.language] = v.name }
    suspend fun updateThemeMode(v: ThemeMode) = edit { it[Keys.themeMode] = v.name }
    suspend fun updateDefaultQualityId(v: String) = edit { it[Keys.defaultQualityId] = v }
    suspend fun updateDefaultAudioMode(v: AudioMode) = edit { it[Keys.defaultAudioMode] = v.name }
    suspend fun updateFloatingBubbleEnabled(v: Boolean) = edit { it[Keys.floatingBubbleEnabled] = v }
    suspend fun updateShowQuickControls(v: Boolean) = edit { it[Keys.showQuickControls] = v }
    suspend fun updateOutputFolderName(v: String) = edit { it[Keys.outputFolderName] = v.ifBlank { "VirexaScreen" } }
    suspend fun markOnboardingCompleted() = edit { it[Keys.onboardingCompleted] = true }
    suspend fun updateVideoEncoder(v: VideoEncoder) = edit { it[Keys.videoEncoder] = v.name }
    suspend fun updateBitrateMode(v: BitrateMode) = edit { it[Keys.bitrateMode] = v.name }
    suspend fun updateCustomBitrateMbps(v: Int) = edit { it[Keys.customBitrateMbps] = v.coerceIn(1, 50) }
    suspend fun updateFrameRate(v: Int) = edit { it[Keys.frameRate] = v }
    suspend fun updateShowTimerOnBubble(v: Boolean) = edit { it[Keys.showTimerOnBubble] = v }
    suspend fun updateAutoPauseOnCall(v: Boolean) = edit { it[Keys.autoPauseOnCall] = v }
    suspend fun updateKeepScreenOn(v: Boolean) = edit { it[Keys.keepScreenOn] = v }
    suspend fun updateShowTouchIndicator(v: Boolean) = edit { it[Keys.showTouchIndicator] = v }
    suspend fun updateCountdownOption(v: CountdownOption) = edit { it[Keys.countdownOption] = v.name }
    suspend fun updateMaxDurationMinutes(v: Int) = edit { it[Keys.maxDurationMinutes] = v.coerceIn(0, 180) }
    suspend fun updateWatermarkText(v: String) = edit { it[Keys.watermarkText] = v }
    suspend fun updateWatermarkEnabled(v: Boolean) = edit { it[Keys.watermarkEnabled] = v }
    suspend fun updateMicBoostLevel(v: MicBoostLevel) = edit { it[Keys.micBoostLevel] = v.name }
    suspend fun updateNoiseSuppression(v: Boolean) = edit { it[Keys.noiseSuppression] = v }
    suspend fun updateSilenceAutoPause(v: Boolean) = edit { it[Keys.silenceAutoPause] = v }
    suspend fun updateSilenceThresholdSeconds(v: Int) = edit { it[Keys.silenceThresholdSeconds] = v.coerceIn(3, 60) }
    suspend fun updateDoNotDisturb(v: Boolean) = edit { it[Keys.doNotDisturbDuringRecording] = v }
    suspend fun updateHapticFeedback(v: Boolean) = edit { it[Keys.hapticFeedback] = v }
    suspend fun updateAutoShareAfterStop(v: Boolean) = edit { it[Keys.autoShareAfterStop] = v }
    suspend fun updateQuickShareTarget(v: String) = edit { it[Keys.quickShareTarget] = v }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
