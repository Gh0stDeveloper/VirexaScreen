package com.virexa.screen.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
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
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
        const val EXTRA_AUTO_START_BUBBLE = "extra_auto_start_bubble"
    }

    private data class CaptureSize(val width: Int, val height: Int) {
        val pixels: Long get() = width.toLong() * height.toLong()
        val label: String get() = "${width}×${height}"
    }

    private data class CaptureAttempt(
        val size: CaptureSize,
        val fps: Int,
        val bitrate: Int,
        val encoder: Int,
        val encoderLabel: String,
    )

    private val recorderRepository by lazy { RecordingRepository(applicationContext) }
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFile: File? = null
    private var outputUri: Uri? = null
    private var outputPfd: ParcelFileDescriptor? = null
    private var started = false
    private var stopping = false
    private var currentForegroundType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
    private var wakeLock: PowerManager.WakeLock? = null
    private var activeWatermarkText: String? = null
    private var watermarkView: View? = null
    private var watermarkWindowManager: WindowManager? = null
    private var watermarkVisible = false

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
    private var previousDndMode = NotificationManager.INTERRUPTION_FILTER_ALL

    // Timer
    private val timerHandler = Handler(Looper.getMainLooper())
    private var activeSegmentStartedAtMs = 0L
    private var recordedElapsedMs = 0L
    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = recordedElapsedMs + if (RecordingSession.uiState.value.isPaused) {
                0L
            } else {
                (System.currentTimeMillis() - activeSegmentStartedAtMs).coerceAtLeast(0L)
            }
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
        override fun onStop() {
            if (!stopping) handleProjectionStopped("La grabación se detuvo")
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
        return START_NOT_STICKY
    }

    private fun startCapture(intent: Intent) {
        if (started) return
        stopping = false
        try {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            require(resultCode == Activity.RESULT_OK) { "Permiso de captura no concedido" }
            val projectionData: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_DATA)
            } ?: throw IllegalArgumentException("Faltan los datos del permiso de captura")

            val requestedWidth = intent.getIntExtra(EXTRA_WIDTH, 1080)
            val requestedHeight = intent.getIntExtra(EXTRA_HEIGHT, 1920)
            val screenSize = getRealScreenSize()
            val primarySize = resolveFullScreenSize(requestedWidth, requestedHeight, screenSize.width, screenSize.height)
            val density = intent.getIntExtra(EXTRA_DENSITY, resources.displayMetrics.densityDpi)
                .coerceAtLeast(DisplayMetrics.DENSITY_LOW)
            val requestedFps = intent.getIntExtra(EXTRA_FPS, 60)
            val requestedBitrate = intent.getIntExtra(EXTRA_BITRATE, 8_000_000)
            val audioMode = runCatching {
                AudioMode.valueOf(intent.getStringExtra(EXTRA_AUDIO_MODE) ?: AudioMode.MICROPHONE.name)
            }.getOrDefault(AudioMode.MICROPHONE)
            val encoderEnum = runCatching {
                VideoEncoder.valueOf(intent.getStringExtra(EXTRA_ENCODER) ?: VideoEncoder.H264.name)
            }.getOrDefault(VideoEncoder.H264)

            maxDurationMs = intent.getLongExtra(EXTRA_MAX_DURATION_MS, 0L).coerceAtLeast(0L)
            silenceAutoPause = intent.getBooleanExtra(EXTRA_SILENCE_AUTO_PAUSE, false)
            silenceThresholdMs = intent.getIntExtra(EXTRA_SILENCE_THRESHOLD_S, 10).coerceIn(3, 60) * 1000L
            activeWatermarkText = intent.getStringExtra(EXTRA_WATERMARK)?.trim().orEmpty().ifBlank { null }
            val noiseSuppressionEnabled = intent.getBooleanExtra(EXTRA_NOISE_SUPPRESSION, false)
            dndEnabled = intent.getBooleanExtra(EXTRA_DND, false)

            currentForegroundType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                if (audioMode.usesMicrophone) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0

            ServiceCompat.startForeground(this, 1, buildNotification("Preparando grabación…", false), currentForegroundType)
            CountdownOverlayService.stop(this)

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

            val preferredEncoder = if (encoderEnum == VideoEncoder.H265) {
                MediaRecorder.VideoEncoder.HEVC
            } else {
                MediaRecorder.VideoEncoder.H264
            }
            val fallbackSize = fallbackSizeFor(primarySize)
            val attempts = mutableListOf<CaptureAttempt>()

            fun addAttempt(size: CaptureSize, encoder: Int, label: String) {
                val attempt = CaptureAttempt(
                    size = size,
                    fps = safeFrameRate(requestedFps, requestedWidth, requestedHeight, size),
                    bitrate = clampBitrate(requestedBitrate, size),
                    encoder = encoder,
                    encoderLabel = label,
                )
                if (attempts.none { it.size == attempt.size && it.encoder == attempt.encoder }) attempts += attempt
            }

            addAttempt(primarySize, preferredEncoder, if (preferredEncoder == MediaRecorder.VideoEncoder.HEVC) "H.265" else "H.264")
            if (preferredEncoder != MediaRecorder.VideoEncoder.H264) {
                addAttempt(primarySize, MediaRecorder.VideoEncoder.H264, "H.264")
            }
            if (fallbackSize != primarySize) {
                addAttempt(fallbackSize, preferredEncoder, if (preferredEncoder == MediaRecorder.VideoEncoder.HEVC) "H.265" else "H.264")
                if (preferredEncoder != MediaRecorder.VideoEncoder.H264) {
                    addAttempt(fallbackSize, MediaRecorder.VideoEncoder.H264, "H.264")
                }
            }

            var selectedAttempt: CaptureAttempt? = null
            var preparationError: Throwable? = null
            for (attempt in attempts) {
                try {
                    mediaRecorder = buildPreparedRecorder(
                        size = attempt.size,
                        fps = attempt.fps,
                        bitrate = attempt.bitrate,
                        videoEncoder = attempt.encoder,
                        audioMode = audioMode,
                        noiseSuppressionEnabled = noiseSuppressionEnabled,
                    )
                    selectedAttempt = attempt
                    break
                } catch (t: Throwable) {
                    preparationError = t
                }
            }
            val selected = selectedAttempt ?: throw IllegalStateException(
                "El dispositivo no pudo preparar un perfil de video compatible",
                preparationError,
            )

            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "VirexaScreenCapture",
                selected.size.width,
                selected.size.height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder!!.surface,
                null,
                null,
            )
            mediaRecorder?.start()
            started = true

            watermarkVisible = activeWatermarkText?.let(::showWatermarkOverlay) == true

            // Wake lock
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VirexaScreen:RecordingWakeLock").apply {
                acquire(maxDurationMs.takeIf { it > 0 } ?: 6 * 60 * 60 * 1000L)
            }

            // DND
            if (dndEnabled) activateDnd()

            // Timer
            activeSegmentStartedAtMs = System.currentTimeMillis()
            recordedElapsedMs = 0L
            lastAudioActivityMs = activeSegmentStartedAtMs
            timerHandler.post(timerRunnable)

            // Silence detection (simple amplitude poll via separate thread - triggers pause)
            if (silenceAutoPause && audioMode.usesMicrophone) startSilenceDetection()

            RecordingSession.update {
                it.copy(
                    isRecording = true,
                    isPaused = false,
                    activeFilePath = destination.displayPath,
                    elapsedMs = 0L,
                    message = buildStartMessage(audioMode, selected),
                )
            }

            if (intent.getBooleanExtra(EXTRA_AUTO_START_BUBBLE, false)) {
                FloatingBubbleService.start(this)
            }

            ServiceCompat.startForeground(this, 1, buildNotification("Grabando pantalla", false), currentForegroundType)
        } catch (t: Throwable) {
            timerHandler.removeCallbacks(timerRunnable)
            stopping = true
            removeWatermarkOverlay()
            runCatching { mediaRecorder?.reset() }
            runCatching { mediaRecorder?.release() }
            runCatching { virtualDisplay?.release() }
            runCatching { mediaProjection?.unregisterCallback(projectionCallback) }
            runCatching { mediaProjection?.stop() }
            mediaRecorder = null
            virtualDisplay = null
            mediaProjection = null
            started = false
            cleanupOutput(shouldDelete = true)
            RecordingSession.update { it.copy(isRecording = false, isPaused = false, message = "No se pudo iniciar: ${t.message}") }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildPreparedRecorder(
        size: CaptureSize,
        fps: Int,
        bitrate: Int,
        videoEncoder: Int,
        audioMode: AudioMode,
        noiseSuppressionEnabled: Boolean,
    ): MediaRecorder {
        val recorder = MediaRecorder()
        try {
            recorder.apply {
                if (audioMode.usesMicrophone) {
                    setAudioSource(if (noiseSuppressionEnabled) MediaRecorder.AudioSource.VOICE_RECOGNITION else MediaRecorder.AudioSource.MIC)
                }
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                if (audioMode.usesMicrophone) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000)
                    setAudioSamplingRate(44_100)
                    setAudioChannels(1)
                }
                setVideoEncoder(videoEncoder)
                setVideoSize(size.width, size.height)
                setVideoFrameRate(fps)
                setVideoEncodingBitRate(bitrate)
                if (outputPfd != null) setOutputFile(outputPfd!!.fileDescriptor)
                else setOutputFile(outputFile!!.absolutePath)
                prepare()
            }
            return recorder
        } catch (t: Throwable) {
            runCatching { recorder.reset() }
            runCatching { recorder.release() }
            throw t
        }
    }

    private fun startSilenceDetection() {
        silenceRunnable?.let { silenceHandler.removeCallbacks(it) }
        silenceRunnable = object : Runnable {
            override fun run() {
                if (!started) return
                if (!RecordingSession.uiState.value.isPaused) {
                    val amplitude = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    if (amplitude > 800) {
                        lastAudioActivityMs = System.currentTimeMillis()
                        RecordingSession.setSilenceDetected(false)
                    } else if (System.currentTimeMillis() - lastAudioActivityMs >= silenceThresholdMs) {
                        RecordingSession.setSilenceDetected(true)
                        pauseCapture()
                    }
                }
                silenceHandler.postDelayed(this, 250L)
            }
        }.also { silenceHandler.post(it) }
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
        if (!started || RecordingSession.uiState.value.isPaused) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                recordedElapsedMs += (System.currentTimeMillis() - activeSegmentStartedAtMs).coerceAtLeast(0L)
                RecordingSession.setElapsed(recordedElapsedMs)
                timerHandler.removeCallbacks(timerRunnable)
                RecordingSession.update { it.copy(isPaused = true, message = "Grabación en pausa") }
                updateNotification(true)
            }
        } catch (t: Throwable) {
            RecordingSession.setMessage("No se pudo pausar: ${t.message}")
        }
    }

    private fun resumeCapture() {
        if (!started || !RecordingSession.uiState.value.isPaused) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                activeSegmentStartedAtMs = System.currentTimeMillis()
                lastAudioActivityMs = activeSegmentStartedAtMs
                timerHandler.post(timerRunnable)
                RecordingSession.update { it.copy(isPaused = false, silenceDetected = false, message = "Grabación reanudada") }
                updateNotification(false)
            }
        } catch (t: Throwable) {
            RecordingSession.setMessage("No se pudo reanudar: ${t.message}")
        }
    }

    private fun stopCapture() {
        if (!started && mediaProjection == null) { stopSelf(); return }
        handleProjectionStopped("Grabación finalizada")
    }

    private fun handleProjectionStopped(message: String) {
        if (stopping) return
        stopping = true
        timerHandler.removeCallbacks(timerRunnable)
        runCatching { silenceRunnable?.let { silenceHandler.removeCallbacks(it) } }
        runCatching { mediaProjection?.unregisterCallback(projectionCallback) }
        deactivateDnd()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null

        var stopError: Throwable? = null
        try { mediaRecorder?.apply { stop(); reset(); release() } } catch (t: Throwable) { stopError = t }
        removeWatermarkOverlay()
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
                countdown = 0,
                silenceDetected = false,
                message = when {
                    stopError != null -> "No se pudo finalizar el video correctamente"
                    saved != null -> message
                    else -> "Grabación finalizada"
                },
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanupOutput(shouldDelete: Boolean) {
        val uri = outputUri
        val pfd = outputPfd
        val file = outputFile
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
            runCatching {
                if (shouldDelete) contentResolver.delete(uri, null, null)
                else contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
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
        ServiceCompat.startForeground(this, 1, buildNotification(text, paused), currentForegroundType)
    }

    private fun buildNotification(text: String, isPaused: Boolean): Notification {
        val openPi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val pauseResumePi = PendingIntent.getService(
            this,
            1,
            Intent(this, ScreenRecordService::class.java).apply { action = if (isPaused) ACTION_RESUME else ACTION_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopPi = PendingIntent.getService(
            this,
            2,
            Intent(this, ScreenRecordService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val elapsed = RecordingSession.uiState.value.elapsedMs
        val timerStr = if (elapsed > 0) " · ${formatElapsed(elapsed)}" else ""

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_RECORDING_ID)
            .setContentTitle(if (isPaused) "Virexa Screen · En pausa" else "Virexa Screen · Grabando")
            .setContentText("$text$timerStr")
            .setSmallIcon(R.drawable.ic_qs_virexa)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(MediaStyle().setShowActionsInCompactView(0, 1))
            .addAction(if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause, if (isPaused) "Reanudar" else "Pausar", pauseResumePi)
            .addAction(android.R.drawable.ic_delete, "Detener", stopPi)
            .setColor(if (isPaused) 0xFFFF9800.toInt() else 0xFFE53935.toInt())
            .setColorized(true)
            .build()
    }

    private fun openApp() {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }
    }

    private fun closeBubble() {
        FloatingBubbleService.stop(this)
    }

    override fun onDestroy() {
        timerHandler.removeCallbacks(timerRunnable)
        runCatching { silenceRunnable?.let { silenceHandler.removeCallbacks(it) } }
        removeWatermarkOverlay()
        wakeLock?.let { if (it.isHeld) it.release() }
        runCatching { mediaRecorder?.release() }
        runCatching { virtualDisplay?.release() }
        runCatching { mediaProjection?.stop() }
        runCatching { outputPfd?.close() }
        super.onDestroy()
    }

    private fun getRealScreenSize(): CaptureSize {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            CaptureSize(alignEven(bounds.width()), alignEven(bounds.height()))
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics().also { wm.defaultDisplay.getRealMetrics(it) }
            CaptureSize(alignEven(metrics.widthPixels), alignEven(metrics.heightPixels))
        }
    }

    private fun resolveFullScreenSize(requestedWidth: Int, requestedHeight: Int, screenWidth: Int, screenHeight: Int): CaptureSize {
        val nativeWidth = screenWidth.coerceAtLeast(2)
        val nativeHeight = screenHeight.coerceAtLeast(2)
        val nativeShort = min(nativeWidth, nativeHeight)
        val nativeLong = max(nativeWidth, nativeHeight)
        val requestedShort = min(requestedWidth.coerceAtLeast(2), requestedHeight.coerceAtLeast(2))
        val targetShort = min(nativeShort, requestedShort)
        val scale = targetShort.toFloat() / nativeShort.toFloat()
        val targetLong = (nativeLong * scale).roundToInt()

        return if (nativeWidth <= nativeHeight) {
            CaptureSize(alignEven(targetShort), alignEven(targetLong))
        } else {
            CaptureSize(alignEven(targetLong), alignEven(targetShort))
        }
    }

    private fun fallbackSizeFor(size: CaptureSize): CaptureSize {
        val shortEdge = min(size.width, size.height)
        if (shortEdge <= 720) return size

        val fallbackShort = if (shortEdge > 1080) 1080 else 720
        val longEdge = max(size.width, size.height)
        val fallbackLong = (longEdge * (fallbackShort.toFloat() / shortEdge.toFloat())).roundToInt()

        return if (size.width <= size.height) {
            CaptureSize(alignEven(fallbackShort), alignEven(fallbackLong))
        } else {
            CaptureSize(alignEven(fallbackLong), alignEven(fallbackShort))
        }
    }

    private fun safeFrameRate(requestedFps: Int, requestedWidth: Int, requestedHeight: Int, size: CaptureSize): Int {
        val requested = requestedFps.coerceIn(24, 60)
        val requestedLongEdge = max(requestedWidth, requestedHeight)
        return if (requestedLongEdge >= 2560 || size.pixels > 3_700_000L) {
            requested.coerceAtMost(30)
        } else {
            requested
        }
    }

    private fun clampBitrate(value: Int, size: CaptureSize): Int {
        val maxBitrate = when {
            size.pixels >= 8_000_000L -> 40_000_000
            size.pixels >= 4_000_000L -> 26_000_000
            size.pixels >= 2_000_000L -> 18_000_000
            else -> 10_000_000
        }
        return value.coerceIn(2_000_000, maxBitrate)
    }

    private fun buildStartMessage(audioMode: AudioMode, attempt: CaptureAttempt): String {
        val audioText = if (audioMode.requestsSystemAudio) " · audio interno sujeto al sistema" else ""
        val watermarkText = when {
            activeWatermarkText.isNullOrBlank() -> ""
            watermarkVisible -> " · marca de agua activa"
            else -> " · marca de agua omitida (sin permiso de superposición)"
        }
        return "Grabando pantalla completa ${attempt.size.label} @ ${attempt.fps}fps · ${attempt.encoderLabel}$audioText$watermarkText"
    }

    private fun showWatermarkOverlay(text: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return false
        removeWatermarkOverlay()

        val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val watermark = TextView(this).apply {
            this.text = text.take(80)
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            val horizontal = 14.dpToPx()
            val vertical = 8.dpToPx()
            setPadding(horizontal, vertical, horizontal, vertical)
            background = GradientDrawable().apply {
                setColor(0xB315171C.toInt())
                setStroke(1.dpToPx(), 0x55FFFFFF)
                cornerRadius = 14.dpToPx().toFloat()
            }
            alpha = 0.9f
            isClickable = false
            isFocusable = false
        }
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 20.dpToPx()
            y = 48.dpToPx()
        }

        return runCatching {
            manager.addView(watermark, layoutParams)
            watermarkWindowManager = manager
            watermarkView = watermark
            true
        }.getOrElse {
            watermarkWindowManager = null
            watermarkView = null
            false
        }
    }

    private fun removeWatermarkOverlay() {
        val view = watermarkView
        val manager = watermarkWindowManager
        if (view != null && manager != null) runCatching { manager.removeViewImmediate(view) }
        watermarkView = null
        watermarkWindowManager = null
        watermarkVisible = false
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).roundToInt()

    private fun alignEven(value: Int): Int = (value.coerceAtLeast(2) / 2) * 2

    private fun formatElapsed(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}
