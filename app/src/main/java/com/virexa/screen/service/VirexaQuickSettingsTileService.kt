package com.virexa.screen.service

import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.virexa.screen.MainActivity
import com.virexa.screen.data.RecordingSession

class VirexaQuickSettingsTileService : TileService() {

    companion object {
        // Called externally to force the tile to refresh
        fun requestTileUpdate(context: android.content.Context) {
            requestListeningState(context, android.content.ComponentName(context, VirexaQuickSettingsTileService::class.java))
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val state = RecordingSession.uiState.value

        when {
            // If recording → stop
            state.isRecording && !state.isPaused -> {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, ScreenRecordService::class.java).apply { action = ScreenRecordService.ACTION_STOP }
                )
                refreshTile()
            }
            // If paused → resume
            state.isPaused -> {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, ScreenRecordService::class.java).apply { action = ScreenRecordService.ACTION_RESUME }
                )
                refreshTile()
            }
            // Not recording → open app or launch bubble
            else -> {
                if (Settings.canDrawOverlays(this)) {
                    ContextCompat.startForegroundService(this, Intent(this, FloatingBubbleService::class.java))
                    // Open app to start the screen capture permission flow
                    startActivityAndCollapse(
                        Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("action", "start_recording")
                        }
                    )
                } else {
                    startActivityAndCollapse(
                        Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                    )
                }
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val state = RecordingSession.uiState.value

        when {
            state.isRecording && !state.isPaused -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Virexa · Grabando"
                tile.contentDescription = "Toca para detener la grabación"
                if (Build.VERSION_INT >= 29) {
                    tile.subtitle = formatElapsed(state.elapsedMs)
                }
            }
            state.isPaused -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Virexa · En pausa"
                tile.contentDescription = "Toca para reanudar la grabación"
                if (Build.VERSION_INT >= 29) {
                    tile.subtitle = "Pausado ${formatElapsed(state.elapsedMs)}"
                }
            }
            else -> {
                tile.state = if (Settings.canDrawOverlays(this)) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
                tile.label = "Virexa Screen"
                tile.contentDescription = if (Settings.canDrawOverlays(this))
                    "Toca para abrir e iniciar grabación"
                else
                    "Permiso de superposición requerido"
                if (Build.VERSION_INT >= 29) {
                    tile.subtitle = if (Settings.canDrawOverlays(this)) "Listo" else "Sin permiso"
                }
            }
        }
        tile.updateTile()
    }

    private object Build {
        val VERSION_INT = android.os.Build.VERSION.SDK_INT
    }

    private fun formatElapsed(ms: Long): String {
        val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}
