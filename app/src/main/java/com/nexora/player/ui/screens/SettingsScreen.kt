package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.model.AppThemeMode
import com.nexora.player.ui.components.PremiumHeroCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    themeMode: AppThemeMode,
    dynamicColor: Boolean,
    onThemeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PremiumHeroCard(
            title = "Ajustes",
            subtitle = "Tema, color dinámico y estado actual de la plataforma.",
        ) { }

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tema")
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.SYSTEM,
                        onClick = { onThemeChange(AppThemeMode.SYSTEM) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) { Text("Sistema") }
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.LIGHT,
                        onClick = { onThemeChange(AppThemeMode.LIGHT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) { Text("Claro") }
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.DARK,
                        onClick = { onThemeChange(AppThemeMode.DARK) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) { Text("Oscuro") }
                }
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Color dinámico")
                    Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
                }
            }
        }

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estado actual")
                Text("Persistencia local: Room + DataStore")
                Text("Reproductor base: Media3 / ExoPlayer")
                Text("Listo para portadas, notificación de medios y reproducción de bloqueo.")
            }
        }
    }
}
