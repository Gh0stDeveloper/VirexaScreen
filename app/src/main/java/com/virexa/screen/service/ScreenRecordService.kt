package com.virexa.screen.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.VirtualDisplay
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.provider.Settings
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.virexa.screen.MainActivity
import com.virexa.screen.R
import com.virexa.screen.data.AudioMode
import com.virexa.screen.data.RecordingRepository
import com.virexa.screen.data.RecordingSession
import com.virexa.screen.data.VideoEncoder
import java.io.File

class ScreenRecordService : android.app.Service() {

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
        const val EXTRA_WATERMARK = "extra_watermark"
        const val EXTRA_MAX_DURATION_MS = "extra_max_duration_ms"
        const val EXTRA_SILENCE_AUTO_PAUSE = "extra_silence_auto_pause"
        const val EXTRA_SILENCE_THRESHOLD_S = "extra_silence_threshold_s"
        const val EXTRA_NOISE_SUPPRESSION = "extra_noise_suppression"
        const val EXTRA_MIC_BOOST = "extra_mic_boost"
        const val EXTRA_DND = "extra_dnd"
        const val EXTRA_SHOW_BUBBLE = "extra_show_bubble"
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
    private var wakeLock: PowerManager.WakeLock? = null

    // Max duration auto-stop
    private var maxDurationMs = 0L

    // Silence detection
    private var silenceAutoPause = false
    private var silenceThresholdMs = 10_000L
    private var silenceHandler = Handler(Looper.getMainLooper())
    private var silenceRunnable: Runnable? = null
    private var lastAudioActivityMs = 0L

    // DND
    private var dndEnabled = false
    private var showBubbleOnStart = false
    private var previousDndMode = NotificationManager.INTERRUPTION_FILTER_ALL

    // Timer
    private val timerHandler = Handler(Looper.getMainLooper())
    private var recordingStartMs = 0L
    private var pausedAccumulatedMs = 0L
    private var pauseStartMs = 0L
    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = pausedAccumulatedMs + (System.currentTimeMillis() - recordingStartMs)
            RecordingSession.setElapsed(elapsed)
            // Auto-stop by max duration
            if (maxDurationMs > 0 && elapsed >= maxDurationMs) {
                RecordingSession.setMessage("Duración máxima alcanzada, deteniendo…")
                stopCapture()
                return
            }
            timerHandler.postDelayed(this, 500)
        }
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() { handleProjectionStopped("La grabación se detuvo") }
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
        val projectionData: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)!!
        else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_DATA)!!

        val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
        val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)
        val density = intent.getIntExtra(EXTRA_DENSITY, resources.displayMetrics.densityDpi)
        val fps = intent.getIntExtra(EXTRA_FPS, 60)
        val bitrate = intent.getIntExtra(EXTRA_BITRATE, 8_000_000)
        val audioMode = runCatching { AudioMode.valueOf(intent.getStringExtra(EXTRA_AUDIO_MODE) ?: AudioMode.MICROPHONE.name) }.getOrDefault(AudioMode.MICROPHONE)
        val encoderEnum = runCatching { VideoEncoder.valueOf(intent.getStringExtra(EXTRA_ENCODER) ?: VideoEncoder.H264.name) }.getOrDefault(VideoEncoder.H264)
        maxDurationMs = intent.getLongExtra(EXTRA_MAX_DURATION_MS, 0L)
        silenceAutoPause = intent.getBooleanExtra(EXTRA_SILENCE_AUTO_PAUSE, false)
        silenceThresholdMs = intent.getIntExtra(EXTRA_SILENCE_THRESHOLD_S, 10) * 1000L
        val noiseSuppressionEnabled = intent.getBooleanExtra(EXTRA_NOISE_SUPPRESSION, false)
        dndEnabled = intent.getBooleanExtra(EXTRA_DND, false)
        showBubbleOnStart = intent.getBooleanExtra(EXTRA_SHOW_BUBBLE, false)

        currentForegroundType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
            if (audioMode.usesMicrophone) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0

        ServiceCompat.startForeground(this, 1, buildNotification("Preparando grabación…", false), currentForegroundType)

        try {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpManager.getMediaProjection(resultCode, projectionData)?.also {
                it.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))
            } ?: throw IllegalStateException("No se pudo obtener MediaProjection")

            val destination = recorderRepository.createRecordingDestination(
                intent.getStringExtra(EXTRA_OUTPUT_FOLDER) ?: "VirexaScreen"
            )
            outputFile = destination.file
            outputUri = destination.uri
            outputPfd = destination.parcelFileDescriptor

            val videoEncoderConst = if (encoderEnum == VideoEncoder.H265) MediaRecorder.VideoEncoder.HEVC else MediaRecorder.VideoEncoder.H264

            mediaRecorder = MediaRecorder().apply {
                if (audioMode.usesMicrophone) {
                    setAudioSource(if (noiseSuppressionEnabled) MediaRecorder.AudioSource.VOICE_RECOGNITION else MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                if (audioMode.usesMicrophone) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(192_000)
                    setAudioSamplingRate(44_100)
                    setAudioChannels(2)
                }
                setVideoEncoder(videoEncoderConst)
                setVideoSize(width, height)
                setVideoFrameRate(fps)
                setVideoEncodingBitRate(bitrate)
                if (outputPfd != null) setOutputFile(outputPfd!!.fileDescriptor)
                else setOutputFile(outputFile!!.absolutePath)
                prepare()
            }

            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "VirexaScreenCapture", width, height, density, 0, mediaRecorder!!.surface, null, null
            )
            mediaRecorder?.start()
            started = true

            // Wake lock
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VirexaScreen:RecordingWakeLock").apply { acquire(maxDurationMs.takeIf { it > 0 } ?: 6 * 60 * 60 * 1000L) }

            // DND
            if (dndEnabled) activateDnd()

            // Timer
            recordingStartMs = System.currentTimeMillis()
            pausedAccumulatedMs = 0L
            lastAudioActivityMs = recordingStartMs
            timerHandler.post(timerRunnable)

            // Silence detection (simple amplitude poll via separate thread - triggers pause)
            if (silenceAutoPause && audioMode.usesMicrophone) startSilenceDetection()

            RecordingSession.update {
                it.copy(isRecording = true, isPaused = false, activeFilePath = destination.displayPath, elapsedMs = 0L,
                    message = if (audioMode.requestsSystemAudio) "Grabando — audio interno sujeto al sistema." else "Grabación iniciada")
            }
            ServiceCompat.startForeground(this, 1, buildNotification("Grabando pantalla", false), currentForegroundType)

            if (showBubbleOnStart && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))) {
                runCatching { ContextCompat.startForegroundService(this, Intent(this, FloatingBubbleService::class.java)) }
            }
        } catch (t: Throwable) {
            timerHandler.removeCallbacks(timerRunnable)
            cleanupOutput(shouldDelete = true)
            RecordingSession.update { it.copy(isRecording = false, isPaused = false, message = "No se pudo iniciar: ${t.message}") }
            stopSelf()
        }
    }

    private fun startSilenceDetection() {
        val minBufSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        Thread {
            val record = runCatching {
                AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSize * 4)
            }.getOrNull() ?: return@Thread
            if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(record.audioSessionId)
            record.startRecording()
            val buf = ShortArray(minBufSize)
            while (started) {
                val read = record.read(buf, 0, buf.size)
                if (read > 0) {
                    val amp = buf.take(read).maxOf { it.toInt().and(0xFFFF) }
                    if (amp > 800) {
                        lastAudioActivityMs = System.currentTimeMillis()
                        if (RecordingSession.uiState.value.isPaused) {
                            // Auto-resume if we detect audio and were silence-paused
                        }
                        RecordingSession.setSilenceDetected(false)
                    } else {
                        val silentFor = System.currentTimeMillis() - lastAudioActivityMs
                        if (silentFor >= silenceThresholdMs && !RecordingSession.uiState.value.isPaused) {
                            RecordingSession.setSilenceDetected(true)
                            timerHandler.post { pauseCapture() }
                        }
                    }
                }
                Thread.sleep(100)
            }
            record.stop(); record.release()
        }.also { it.isDaemon = true; it.start() }
    }

    private fun activateDnd() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
            previousDndMode = nm.currentInterruptionFilter
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }

    private fun deactivateDnd() {
        if (!dndEnabled) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(previousDndMode)
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
                updateNotification(true)
            }
        } catch (t: Throwable) { RecordingSession.setMessage("No se pudo pausar: ${t.message}") }
    }

    private fun resumeCapture() {
        if (!started) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                if (pauseStartMs > 0) { pausedAccumulatedMs += System.currentTimeMillis() - pauseStartMs; pauseStartMs = 0L }
                recordingStartMs = System.currentTimeMillis() - pausedAccumulatedMs; pausedAccumulatedMs = 0L
                lastAudioActivityMs = System.currentTimeMillis()
                timerHandler.post(timerRunnable)
                RecordingSession.update { it.copy(isPaused = false, silenceDetected = false, message = "Grabación reanudada") }
                updateNotification(false)
            }
        } catch (t: Throwable) { RecordingSession.setMessage("No se pudo reanudar: ${t.message}") }
    }

    private fun stopCapture() {
        if (!started && mediaProjection == null) { stopSelf(); return }
        handleProjectionStopped("Grabación finalizada")
    }

    private fun handleProjectionStopped(message: String) {
        timerHandler.removeCallbacks(timerRunnable)
        runCatching { silenceRunnable?.let { silenceHandler.removeCallbacks(it) } }
        runCatching { mediaProjection?.unregisterCallback(projectionCallback) }
        deactivateDnd()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null

        var stopError: Throwable? = null
        try { mediaRecorder?.apply { stop(); reset(); release() } } catch (t: Throwable) { stopError = t }
        runCatching { virtualDisplay?.release() }
        runCatching { mediaProjection?.stop() }
        mediaRecorder = null; virtualDisplay = null; mediaProjection = null; started = false

        val saved = outputUri?.toString() ?: outputFile?.absolutePath
        cleanupOutput(shouldDelete = stopError != null)
        RecordingSession.update { it.copy(isRecording = false, isPaused = false, activeFilePath = saved, elapsedMs = 0L, countdown = 0, silenceDetected = false, message = if (saved != null) message else "Grabación finalizada") }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanupOutput(shouldDelete: Boolean) {
        val uri = outputUri; val pfd = outputPfd; val file = outputFile
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
            runCatching {
                if (shouldDelete) contentResolver.delete(uri, null, null)
                else contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }
        } else if (shouldDelete) runCatching { file?.delete() }
        runCatching { pfd?.close() }
        outputFile = null; outputUri = null; outputPfd = null
    }

    private fun updateNotification(paused: Boolean) {
        val text = if (paused) "En pausa" else "Grabando pantalla"
        ServiceCompat.startForeground(this, 1, buildNotification(text, paused), currentForegroundType)
    }

    private fun buildNotification(text: String, isPaused: Boolean): Notification {
        val openPi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pauseResumePi = PendingIntent.getService(this, 1, Intent(this, ScreenRecordService::class.java).apply { action = if (isPaused) ACTION_RESUME else ACTION_PAUSE }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stopPi = PendingIntent.getService(this, 2, Intent(this, ScreenRecordService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val elapsed = RecordingSession.uiState.value.elapsedMs
        val timerStr = if (elapsed > 0) " · ${formatElapsed(elapsed)}" else ""

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_RECORDING_ID)
            .setContentTitle(if (isPaused) "⏸ Virexa Screen — En pausa" else "⏺ Virexa Screen — Grabando")
            .setContentText("$text$timerStr")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(openPi)
            .setOngoing(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(MediaStyle().setShowActionsInCompactView(0, 1))
            .addAction(if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause, if (isPaused) "▶ Reanudar" else "⏸ Pausar", pauseResumePi)
            .addAction(android.R.drawable.ic_delete, "⏹ Detener", stopPi)
            .setColor(if (isPaused) 0xFFFF9800.toInt() else 0xFFE53935.toInt())
            .setColorized(true)
            .build()
    }

    private fun openApp() { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    private fun closeBubble() { stopService(Intent(this, FloatingBubbleService::class.java)) }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        wakeLock?.let { if (it.isHeld) it.release() }
        runCatching { mediaRecorder?.release() }
        runCatching { virtualDisplay?.release() }
        runCatching { mediaProjection?.stop() }
        runCatching { outputPfd?.close() }
    }

    private fun formatElapsed(ms: Long): String {
        val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}
