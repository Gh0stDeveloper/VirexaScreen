package com.virexa.screen.service

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.virexa.screen.MainActivity
import com.virexa.screen.R
import com.virexa.screen.data.AudioMode
import com.virexa.screen.data.RecordingRepository
import com.virexa.screen.data.RecordingSession
import com.virexa.screen.data.VideoEncoder
import java.io.File

class ScreenRecordService : Service() {

    companion object {
        const val ACTION_START = "com.virexa.screen.action.START"
        const val ACTION_PAUSE = "com.virexa.screen.action.PAUSE"
        const val ACTION_RESUME = "com.virexa.screen.action.RESUME"
        const val ACTION_STOP = "com.virexa.screen.action.STOP"
        const val ACTION_NEW = "com.virexa.screen.action.NEW"
        const val ACTION_CLOSE_BUBBLE = FloatingBubbleService.ACTION_CLOSE

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA = "extra_data"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_DENSITY = "extra_density"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_AUDIO_MODE = "extra_audio_mode"
        const val EXTRA_OUTPUT_FOLDER = "extra_output_folder"
        const val EXTRA_ENCODER = "extra_encoder"
    }

    private val recorderRepository by lazy { RecordingRepository(applicationContext) }
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFile: File? = null
    private var outputUri: Uri? = null
    private var outputPfd: ParcelFileDescriptor? = null
    private var started = false
    private var currentForegroundType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION

    // Timer
    private val timerHandler = Handler(Looper.getMainLooper())
    private var recordingStartMs = 0L
    private var pausedAccumulatedMs = 0L
    private var pauseStartMs = 0L
    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = pausedAccumulatedMs + (System.currentTimeMillis() - recordingStartMs)
            RecordingSession.setElapsed(elapsed)
            timerHandler.postDelayed(this, 500)
        }
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            handleProjectionStopped("La grabación se detuvo")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.ensureChannels(this)
        when (intent?.action) {
            ACTION_START -> startCapture(intent)
            ACTION_PAUSE -> pauseCapture()
            ACTION_RESUME -> resumeCapture()
            ACTION_STOP -> stopCapture()
            ACTION_NEW -> openApp()
            ACTION_CLOSE_BUBBLE -> closeBubble()
        }
        return START_STICKY
    }

    private fun startCapture(intent: Intent) {
        if (started) return

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
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
        val encoderEnum = runCatching {
            VideoEncoder.valueOf(intent.getStringExtra(EXTRA_ENCODER) ?: VideoEncoder.H264.name)
        }.getOrDefault(VideoEncoder.H264)

        currentForegroundType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
            if (audioMode.usesMicrophone) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0

        ServiceCompat.startForeground(
            this, 1,
            buildNotification("Preparando grabación…", isPaused = false),
            currentForegroundType,
        )

        try {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, projectionData)
                ?: throw IllegalStateException("No se pudo obtener MediaProjection")
            mediaProjection?.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

            val outputFolder = intent.getStringExtra(EXTRA_OUTPUT_FOLDER) ?: "VirexaScreen"
            val destination = recorderRepository.createRecordingDestination(outputFolder)
            outputFile = destination.file
            outputUri = destination.uri
            outputPfd = destination.parcelFileDescriptor

            val videoEncoderConst = when (encoderEnum) {
                VideoEncoder.H265 -> MediaRecorder.VideoEncoder.HEVC
                else -> MediaRecorder.VideoEncoder.H264
            }

            mediaRecorder = MediaRecorder().apply {
                if (audioMode.usesMicrophone) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                if (audioMode.usesMicrophone) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(192_000)
                    setAudioSamplingRate(44_100)
                }
                setVideoEncoder(videoEncoderConst)
                setVideoSize(width, height)
                setVideoFrameRate(fps)
                setVideoEncodingBitRate(bitrate)
                if (outputPfd != null) {
                    setOutputFile(outputPfd!!.fileDescriptor)
                } else {
                    setOutputFile(outputFile!!.absolutePath)
                }
                prepare()
            }

            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "VirexaScreenCapture",
                width, height, density,
                0,
                mediaRecorder!!.surface,
                null, null,
            )

            mediaRecorder?.start()
            started = true

            // Start elapsed timer
            recordingStartMs = System.currentTimeMillis()
            pausedAccumulatedMs = 0L
            timerHandler.post(timerRunnable)

            RecordingSession.update {
                it.copy(
                    isRecording = true,
                    isPaused = false,
                    activeFilePath = destination.displayPath,
                    elapsedMs = 0L,
                    message = if (audioMode.requestsSystemAudio) "Grabación iniciada. Audio interno sujeto al sistema." else "Grabación iniciada",
                )
            }
            ServiceCompat.startForeground(
                this, 1,
                buildNotification("Grabando pantalla", isPaused = false),
                currentForegroundType,
            )
        } catch (t: Throwable) {
            timerHandler.removeCallbacks(timerRunnable)
            cleanupOutput(shouldDelete = true)
            RecordingSession.update { it.copy(isRecording = false, isPaused = false, message = "No se pudo iniciar: ${t.message}") }
            stopSelf()
        }
    }

    private fun pauseCapture() {
        if (!started) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                pauseStartMs = System.currentTimeMillis()
                timerHandler.removeCallbacks(timerRunnable)
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
                if (pauseStartMs > 0) {
                    pausedAccumulatedMs += System.currentTimeMillis() - pauseStartMs
                    pauseStartMs = 0L
                }
                recordingStartMs = System.currentTimeMillis() - pausedAccumulatedMs
                pausedAccumulatedMs = 0L
                timerHandler.post(timerRunnable)
                RecordingSession.update { it.copy(isPaused = false, message = "Grabación reanudada") }
                updateNotification(paused = false)
            }
        } catch (t: Throwable) {
            RecordingSession.setMessage("No se pudo reanudar: ${t.message}")
        }
    }

    private fun stopCapture() {
        if (!started && mediaProjection == null) {
            stopSelf()
            return
        }
        handleProjectionStopped("Grabación finalizada")
    }

    private fun handleProjectionStopped(message: String) {
        timerHandler.removeCallbacks(timerRunnable)
        runCatching { mediaProjection?.unregisterCallback(projectionCallback) }

        var stopError: Throwable? = null
        try {
            mediaRecorder?.apply { stop(); reset(); release() }
        } catch (t: Throwable) {
            stopError = t
        } finally {
            runCatching { virtualDisplay?.release() }
            runCatching { mediaProjection?.stop() }
            mediaRecorder = null
            virtualDisplay = null
            mediaProjection = null
            started = false
            val saved = outputUri?.toString() ?: outputFile?.absolutePath
            cleanupOutput(shouldDelete = stopError != null)
            RecordingSession.update {
                it.copy(
                    isRecording = false,
                    isPaused = false,
                    activeFilePath = saved,
                    elapsedMs = 0L,
                    message = if (saved != null) message else "Grabación finalizada",
                )
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun cleanupOutput(shouldDelete: Boolean) {
        val uri = outputUri
        val pfd = outputPfd
        val file = outputFile
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
            runCatching {
                val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                if (shouldDelete) contentResolver.delete(uri, null, null)
                else contentResolver.update(uri, values, null, null)
            }
        } else if (shouldDelete) {
            runCatching { file?.delete() }
        }
        runCatching { pfd?.close() }
        outputFile = null
        outputUri = null
        outputPfd = null
    }

    private fun updateNotification(paused: Boolean) {
        val text = if (paused) "En pausa" else "Grabando pantalla"
        ServiceCompat.startForeground(this, 1, buildNotification(text, isPaused = paused), currentForegroundType)
    }

    private fun buildNotification(text: String, isPaused: Boolean): Notification {
        val openPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val pauseResumePi = PendingIntent.getService(
            this, 1,
            Intent(this, ScreenRecordService::class.java).apply {
                action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopPi = PendingIntent.getService(
            this, 2,
            Intent(this, ScreenRecordService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val pauseIcon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
        val pauseLabel = if (isPaused) "▶ Reanudar" else "⏸ Pausar"

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_RECORDING_ID)
            .setContentTitle(if (isPaused) "⏸ Virexa Screen — En pausa" else "⏺ Virexa Screen — Grabando")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(MediaStyle().setShowActionsInCompactView(0, 1))
            .addAction(pauseIcon, pauseLabel, pauseResumePi)
            .addAction(android.R.drawable.ic_delete, "⏹ Detener", stopPi)
            .setColor(if (isPaused) 0xFFFF9800.toInt() else 0xFFE53935.toInt())
            .setColorized(true)
            .build()
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun closeBubble() {
        stopService(Intent(this, FloatingBubbleService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        runCatching { mediaRecorder?.release() }
        runCatching { virtualDisplay?.release() }
        runCatching { mediaProjection?.stop() }
        runCatching { outputPfd?.close() }
    }
}
