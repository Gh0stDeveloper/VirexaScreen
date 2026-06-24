package com.virexa.screen.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        // Advanced
        val videoEncoder = stringPreferencesKey("video_encoder")
        val bitrateMode = stringPreferencesKey("bitrate_mode")
        val customBitrateMbps = intPreferencesKey("custom_bitrate_mbps")
        val frameRate = intPreferencesKey("frame_rate")
        val showTimerOnBubble = booleanPreferencesKey("show_timer_on_bubble")
        val autoPauseOnCall = booleanPreferencesKey("auto_pause_on_call")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val showTouchIndicator = booleanPreferencesKey("show_touch_indicator")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            profileName = prefs[Keys.profileName] ?: "Usuario",
            language = runCatching { LanguageOption.valueOf(prefs[Keys.language] ?: LanguageOption.SPANISH.name) }.getOrDefault(LanguageOption.SPANISH),
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.themeMode] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
            defaultQualityId = prefs[Keys.defaultQualityId] ?: QualityOption.default().id,
            defaultAudioMode = runCatching { AudioMode.valueOf(prefs[Keys.defaultAudioMode] ?: AudioMode.MICROPHONE.name) }.getOrDefault(AudioMode.MICROPHONE),
            floatingBubbleEnabled = prefs[Keys.floatingBubbleEnabled] ?: true,
            showQuickControls = prefs[Keys.showQuickControls] ?: true,
            outputFolderName = prefs[Keys.outputFolderName] ?: "VirexaScreen",
            onboardingCompleted = prefs[Keys.onboardingCompleted] ?: false,
            videoEncoder = runCatching { VideoEncoder.valueOf(prefs[Keys.videoEncoder] ?: VideoEncoder.H264.name) }.getOrDefault(VideoEncoder.H264),
            bitrateMode = runCatching { BitrateMode.valueOf(prefs[Keys.bitrateMode] ?: BitrateMode.AUTO.name) }.getOrDefault(BitrateMode.AUTO),
            customBitrateMbps = prefs[Keys.customBitrateMbps] ?: 8,
            frameRate = prefs[Keys.frameRate] ?: 60,
            showTimerOnBubble = prefs[Keys.showTimerOnBubble] ?: true,
            autoPauseOnCall = prefs[Keys.autoPauseOnCall] ?: false,
            keepScreenOn = prefs[Keys.keepScreenOn] ?: true,
            showTouchIndicator = prefs[Keys.showTouchIndicator] ?: false,
        )
    }

    suspend fun updateProfileName(value: String) = context.dataStore.edit { it[Keys.profileName] = value }
    suspend fun updateLanguage(value: LanguageOption) = context.dataStore.edit { it[Keys.language] = value.name }
    suspend fun updateThemeMode(value: ThemeMode) = context.dataStore.edit { it[Keys.themeMode] = value.name }
    suspend fun updateDefaultQualityId(value: String) = context.dataStore.edit { it[Keys.defaultQualityId] = value }
    suspend fun updateDefaultAudioMode(value: AudioMode) = context.dataStore.edit { it[Keys.defaultAudioMode] = value.name }
    suspend fun updateFloatingBubbleEnabled(value: Boolean) = context.dataStore.edit { it[Keys.floatingBubbleEnabled] = value }
    suspend fun updateShowQuickControls(value: Boolean) = context.dataStore.edit { it[Keys.showQuickControls] = value }
    suspend fun updateOutputFolderName(value: String) = context.dataStore.edit { it[Keys.outputFolderName] = value.ifBlank { "VirexaScreen" } }
    suspend fun markOnboardingCompleted() = context.dataStore.edit { it[Keys.onboardingCompleted] = true }
    suspend fun updateVideoEncoder(value: VideoEncoder) = context.dataStore.edit { it[Keys.videoEncoder] = value.name }
    suspend fun updateBitrateMode(value: BitrateMode) = context.dataStore.edit { it[Keys.bitrateMode] = value.name }
    suspend fun updateCustomBitrateMbps(value: Int) = context.dataStore.edit { it[Keys.customBitrateMbps] = value.coerceIn(1, 50) }
    suspend fun updateFrameRate(value: Int) = context.dataStore.edit { it[Keys.frameRate] = value }
    suspend fun updateShowTimerOnBubble(value: Boolean) = context.dataStore.edit { it[Keys.showTimerOnBubble] = value }
    suspend fun updateAutoPauseOnCall(value: Boolean) = context.dataStore.edit { it[Keys.autoPauseOnCall] = value }
    suspend fun updateKeepScreenOn(value: Boolean) = context.dataStore.edit { it[Keys.keepScreenOn] = value }
    suspend fun updateShowTouchIndicator(value: Boolean) = context.dataStore.edit { it[Keys.showTouchIndicator] = value }
}
