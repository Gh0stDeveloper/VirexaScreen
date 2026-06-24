package com.virexa.screen.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CHANNEL_RECORDING_ID = "virexa_recording"
    const val CHANNEL_BUBBLE_ID = "virexa_bubble"

    // legacy alias used by old callers — maps to recording channel
    const val CHANNEL_ID = CHANNEL_RECORDING_ID

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        // Recording channel — low importance (no sound during recording)
        if (manager.getNotificationChannel(CHANNEL_RECORDING_ID) == null) {
            val rec = NotificationChannel(
                CHANNEL_RECORDING_ID,
                "Grabación de pantalla",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Controles de grabación activa: pausar, reanudar, detener."
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(rec)
        }

        // Bubble channel — min importance, just to keep service alive
        if (manager.getNotificationChannel(CHANNEL_BUBBLE_ID) == null) {
            val bubble = NotificationChannel(
                CHANNEL_BUBBLE_ID,
                "Ventana flotante",
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "Mantiene activa la burbuja flotante de control."
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(bubble)
        }
    }

    // backward-compat — some callers use ensureChannel (singular)
    fun ensureChannel(context: Context) = ensureChannels(context)
}
