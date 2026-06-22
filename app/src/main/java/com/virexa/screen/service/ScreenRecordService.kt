package com.virexa.screen.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.virexa.screen.MainActivity
import com.virexa.screen.data.AudioMode
import com.virexa.screen.data.RecordingRepository
import com.virexa.screen.data.RecordingSession
import java.io.File

class ScreenRecordService : Service() {

    companion object {
        const val ACTION_START = "com.virexa.screen.action.START"
        const val ACTION_PAUSE = "com.virexa.screen.action.PAUSE"
        const val ACTION_RESUME = "com.virexa.screen.action.RESUME"
        const val ACTION_STOP = "com.virexa.screen.action.STOP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_DENSITY = "extra_density"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_AUDIO_MODE = "extra_audio_mode"
        const val EXTRA_OUTPUT_FOLDER = "extra_output_folder"
    }

    private val recorderRepository by lazy { RecordingRepository(applicationContext) }
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFile: File? = null
    private var started = false
    private var currentForegroundType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.ensureChannel(this)
        when (intent?.action) {
            ACTION_START -> startCapture(intent)
            ACTION_PAUSE -> pauseCapture()
            ACTION_RESUME -> resumeCapture()
            ACTION_STOP -> stopCapture()
        }
        return START_STICKY
    }

    private fun startCapture(intent: Intent) {
        if (started) return

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, RESULT_CANCELED)
        val projectionData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DATA)
        } ?: return

        val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
        val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)
        val density = intent.getIntExtra(EXTRA_DENSITY, resources.displayMetrics.densityDpi)
        val fps = intent.getIntExtra(EXTRA_FPS, 60)
        val bitrate = intent.getIntExtra(EXTRA_BITRATE, 8_000_000)
        val audioMode = runCatching {
            AudioMode.valueOf(intent.getStringExtra(EXTRA_AUDIO_MODE) ?: AudioMode.MICROPHONE.name)
        }.getOrDefault(AudioMode.MICROPHONE)

        currentForegroundType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or if (audioMode.usesMicrophone) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        ServiceCompat.startForeground(
            this,
            1,
            buildNotification("Preparando grabación", isPaused = false),
            currentForegroundType,
        )

        try {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, projectionData)
            val outputFolder = intent.getStringExtra(EXTRA_OUTPUT_FOLDER) ?: "VirexaScreen"
            outputFile = recorderRepository.outputFileForNewRecording(outputFolder)

            mediaRecorder = MediaRecorder().apply {
                if (audioMode.usesMicrophone) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                if (audioMode.usesMicrophone) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000)
                    setAudioSamplingRate(44_100)
                }
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(width, height)
                setVideoFrameRate(fps)
                setVideoEncodingBitRate(bitrate)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
            }

            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "VirexaScreenCapture",
                width,
                height,
                density,
                0,
                mediaRecorder!!.surface,
                null,
                null,
            )

            mediaRecorder?.start()
            started = true
            RecordingSession.update {
                it.copy(
                    isRecording = true,
                    isPaused = false,
                    activeFilePath = outputFile?.absolutePath,
                    message = if (audioMode.requestsSystemAudio) "Grabación iniciada. Audio interno sujeto al sistema." else "Grabación iniciada",
                )
            }
            ServiceCompat.startForeground(
                this,
                1,
                buildNotification("Grabando pantalla", isPaused = false),
                currentForegroundType,
            )
        } catch (t: Throwable) {
            RecordingSession.update { it.copy(isRecording = false, isPaused = false, message = "No se pudo iniciar: ${t.message}") }
            stopSelf()
        }
    }

    private fun pauseCapture() {
        if (!started) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                RecordingSession.update { it.copy(isPaused = true, message = "Grabación en pausa") }
                updateNotification(paused = true)
            }
        } catch (t: Throwable) {
            RecordingSession.setMessage("No se pudo pausar: ${t.message}")
        }
    }

    private fun resumeCapture() {
        if (!started) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                RecordingSession.update { it.copy(isPaused = false, message = "Grabación reanudada") }
                updateNotification(paused = false)
            }
        } catch (t: Throwable) {
            RecordingSession.setMessage("No se pudo reanudar: ${t.message}")
        }
    }

    private fun stopCapture() {
        if (!started) {
            stopSelf()
            return
        }

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (_: Throwable) {
        } finally {
            virtualDisplay?.release()
            mediaProjection?.stop()
            mediaRecorder = null
            virtualDisplay = null
            mediaProjection = null
            started = false
            val saved = outputFile?.absolutePath
            RecordingSession.update {
                it.copy(
                    isRecording = false,
                    isPaused = false,
                    activeFilePath = saved,
                    message = if (saved != null) "Grabación guardada" else "Grabación finalizada",
                )
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun updateNotification(paused: Boolean) {
        val current = if (paused) "Grabación en pausa" else "Grabando pantalla"
        ServiceCompat.startForeground(
            this,
            1,
            buildNotification(current, isPaused = paused),
            currentForegroundType,
        )
    }

    private fun buildNotification(text: String, isPaused: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val pauseResumeAction = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        val pauseResumeLabel = if (isPaused) "Seguir" else "Pausar"
        val pauseResumeIcon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause

        val pauseResumePendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ScreenRecordService::class.java).apply { action = pauseResumeAction },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, ScreenRecordService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("Virexa Screen")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(pauseResumeIcon, pauseResumeLabel, pauseResumePendingIntent)
            .addAction(android.R.drawable.ic_delete, "Detener", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { mediaRecorder?.release() }
        runCatching { virtualDisplay?.release() }
        runCatching { mediaProjection?.stop() }
    }
}
