
package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexora.player.R
import com.nexora.player.data.model.AppLanguage
import com.nexora.player.data.model.AppThemeMode
import com.nexora.player.data.model.DownloadStorageMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    themeMode: AppThemeMode,
    dynamicColor: Boolean,
    hiddenAudioCount: Int,
    onlineMusicSearchEnabled: Boolean,
    downloadStorageMode: DownloadStorageMode,
    currentLanguage: AppLanguage,
    onThemeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOnlineMusicSearchChange: (Boolean) -> Unit,
    onDownloadStorageModeChange: (DownloadStorageMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onRestoreHiddenAudio: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val showAbout = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        ElevatedCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onLanguageChange(AppLanguage.SYSTEM) }) { Text(stringResource(AppLanguage.SYSTEM.labelRes)) }
                    OutlinedButton(onClick = { onLanguageChange(AppLanguage.SPANISH) }) { Text(stringResource(AppLanguage.SPANISH.labelRes)) }
                    OutlinedButton(onClick = { onLanguageChange(AppLanguage.ENGLISH) }) { Text(stringResource(AppLanguage.ENGLISH.labelRes)) }
                }

                HorizontalDivider()

                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onThemeChange(AppThemeMode.SYSTEM) }) { Text(stringResource(R.string.settings_system)) }
                    OutlinedButton(onClick = { onThemeChange(AppThemeMode.LIGHT) }) { Text(stringResource(R.string.settings_light)) }
                    OutlinedButton(onClick = { onThemeChange(AppThemeMode.DARK) }) { Text(stringResource(R.string.settings_dark)) }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(stringResource(R.string.settings_dynamic_color))
                        Text(
                            stringResource(R.string.settings_dynamic_color_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
                }
            }
        }

        ElevatedCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Online search", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Enable or disable only internet music search. The rest of the app, including lyrics, keeps working normally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.8f)) {
                        Text("Online mode for music")
                        Text(
                            if (onlineMusicSearchEnabled) "Online search is enabled."
                            else "Online search is disabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = onlineMusicSearchEnabled,
                        onCheckedChange = onOnlineMusicSearchChange
                    )
                }
            }
        }

        ElevatedCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Download storage", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Choose where NexoraPlayer stores music downloaded from online sources.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onDownloadStorageModeChange(DownloadStorageMode.ASK_FIRST_TIME) }
                    ) {
                        Text("Ask first")
                    }
                    OutlinedButton(
                        onClick = { onDownloadStorageModeChange(DownloadStorageMode.APP_PRIVATE) }
                    ) {
                        Text("Only in app")
                    }
                    OutlinedButton(
                        onClick = { onDownloadStorageModeChange(DownloadStorageMode.PUBLIC_DOWNLOADS) }
                    ) {
                        Text("Visible folder")
                    }
                }
                Text(
                    "Visible downloads are saved to Downloads/NexoraPlayer/audios/ when the source allows it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ElevatedCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.settings_library_privacy), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_hidden_audio_count, hiddenAudioCount),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(R.string.settings_hidden_audio_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onRestoreHiddenAudio,
                    enabled = hiddenAudioCount > 0
                ) {
                    Text(stringResource(R.string.settings_restore_hidden))
                }
            }
        }

        ElevatedCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.settings_developer), style = MaterialTheme.typography.titleMedium)
                Text("Ghost Developer · CHICO-CP")
                Text(
                    stringResource(R.string.settings_free_notice),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { uriHandler.openUri("https://github.com/CHICO-CP") },
                        label = { Text(stringResource(R.string.settings_github)) },
                        leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Code, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = { uriHandler.openUri("https://t.me/Gh0stDeveloper") },
                        label = { Text(stringResource(R.string.settings_profile)) },
                        leadingIcon = { androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
                    )
                }
                AssistChip(
                    onClick = { showAbout.value = !showAbout.value },
                    label = {
                        Text(
                            if (showAbout.value) stringResource(R.string.settings_about_hide)
                            else stringResource(R.string.settings_about_show)
                        )
                    },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Verified, contentDescription = null) }
                )
            }
        }

        if (showAbout.value) {
            ElevatedCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(stringResource(R.string.settings_about_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_about_line1))
                    Text(stringResource(R.string.settings_about_line2))
                    Text(stringResource(R.string.settings_about_line3))
                }
            }
        }

        ElevatedCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.settings_status_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.settings_status_line1))
                Text(stringResource(R.string.settings_status_line2))
                Text(stringResource(R.string.settings_status_line3))
            }
        }
    }
}
