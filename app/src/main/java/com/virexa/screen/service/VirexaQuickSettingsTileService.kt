
package com.virexa.screen.service

import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.virexa.screen.MainActivity

class VirexaQuickSettingsTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = if (Settings.canDrawOverlays(this)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Virexa Bubble"
        tile.contentDescription = "Abrir ventana flotante de Virexa Screen"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (Settings.canDrawOverlays(this)) {
            ContextCompat.startForegroundService(this, Intent(this, FloatingBubbleService::class.java))
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
