package com.yuyan.imemodule.speech

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import java.util.ArrayDeque

class SpeechRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognitionManager"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_SECONDS = 0.1f
        private const val SPEECH_THRESHOLD = 25
        private const val JOIN_TIMEOUT_MS = 5000L
        private const val BACKEND_RELEASE_DELAY_MS = 3500L
    }

    private var backend: AsrBackend? = null
    private var recordingThread: RecordingThread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var sessionId = 0

    @Volatile
    private var pendingReleaseSession = -1
    private val pendingBackendRelease = Runnable {
        val session = pendingReleaseSession
        pendingReleaseSession = -1
        val b = synchronized(preloadLock) {
            if (session != -1 && sessionId == session) {
                val tmp = backend
                backend = null
                tmp
            } else null
        }
        if (b != null) {
            Thread { b.release() }.start()
        }
    }

    @Volatile
    private var loadingInProgress = false
    @Volatile
    private var loadingCancelled = false

    private var resultCallback: ((String) -> Unit)? = null
    private var partialResultCallback: ((String) -> Unit)? = null
    private var stateCallback: ((RecognitionState) -> Unit)? = null
    @Volatile
    private var currentState: RecognitionState = RecognitionState.IDLE
    private var errorCallback: ((String, Boolean) -> Unit)? = null
    private var amplitudeCallback: ((Float) -> Unit)? = null
    private var spectrumCallback: ((FloatArray) -> Unit)? = null

    private var preStartedRecord: AudioRecord? = null
    private val preStartTimeoutRunnable = Runnable { cancelPreStart() }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecognition() {
        synchronized(preloadLock) {
            while (isPreloading) {
                try {
                    preloadLock.wait()
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
            }
        }
        
        if (recordingThread != null) {
            return
        }

        setState(RecognitionState.PROCESSING)

        if (backend == null) {
            synchronized(preloadLock) {
                if (loadingInProgress) {
                    loadingCancelled = false
                    return
                }
                loadingInProgress = true
                loadingCancelled = false
            }
            Thread {
                try {
                    val ok = preload()
                    if (!ok || synchronized(preloadLock) { backend } == null) {
                        mainHandler.post {
                            errorCallback?.invoke("无法初始化语音引擎", true)
                            setState(RecognitionState.ERROR)
                        }
                        return@Thread
                    }
                    if (loadingCancelled) {
                        mainHandler.post {
                            setState(RecognitionState.IDLE)
                        }
                        return@Thread
                    }
                    mainHandler.post {
                        if (recordingThread == null && !loadingCancelled) {
                            startRecording()
                        }
                    }
                } finally {
                    synchronized(preloadLock) {
                        loadingInProgress = false
                        preloadLock.notifyAll()
                    }
                }
            }.start()
            return
        }

        startRecording()
    }

    private fun startRecording() {
        val currentBackend = synchronized(preloadLock) { backend } ?: return
        synchronized(preloadLock) { sessionId++ }

        mainHandler.removeCallbacks(pendingBackendRelease)
        pendingReleaseSession = -1

        var preStarted: AudioRecord? = null
        synchronized(this) {
            preStarted = preStartedRecord
            preStartedRecord = null
        }
        mainHandler.removeCallbacks(preStartTimeoutRunnable)

        recordingThread = RecordingThread(currentBackend, preStarted)
        recordingThread!!.start()
    }

    private fun scheduleBackendRelease(session: Int) {
        pendingReleaseSession = session
        mainHandler.removeCallbacks(pendingBackendRelease)
        mainHandler.postDelayed(pendingBackendRelease, BACKEND_RELEASE_DELAY_MS)
    }

    fun stopRecognition() {
        val thread = recordingThread
        if (thread == null) {
            if (loadingInProgress) {
                loadingCancelled = true
                mainHandler.post {
                    setState(RecognitionState.IDLE)
                }
            }
            return
        }
        recordingThread = null
        thread.stopRequested = true
        val session = synchronized(preloadLock) { sessionId }
        thread.forceStopAudio()
        Thread {
            try {
                thread.join(JOIN_TIMEOUT_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            if (thread.isAlive) {
                thread.interrupt()
            }
            scheduleBackendRelease(session)
            mainHandler.post {
                setState(RecognitionState.IDLE)
            }
        }.start()
    }

    fun cancelRecognition() {
        val thread = recordingThread
        if (thread == null) {
            if (loadingInProgress) {
                loadingCancelled = true
                mainHandler.post {
                    setState(RecognitionState.IDLE)
                }
            }
            return
        }
        recordingThread = null
        thread.stopRequested = true
        val session = synchronized(preloadLock) { sessionId }
        thread.forceStopAudio()
        Thread {
            try {
                thread.join(JOIN_TIMEOUT_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            if (thread.isAlive) {
                thread.interrupt()
            }
            scheduleBackendRelease(session)
            mainHandler.post {
                setState(RecognitionState.IDLE)
            }
        }.start()
    }

    fun getState(): RecognitionState = currentState

    private fun setState(state: RecognitionState) {
        currentState = state
        mainHandler.post {
            stateCallback?.invoke(state)
        }
    }

    fun setCallbacks(
        onResult: (String) -> Unit,
        onPartialResult: ((String) -> Unit)? = null,
        onStateChange: (RecognitionState) -> Unit,
        onError: (message: String, userVisible: Boolean) -> Unit,
        onAmplitude: ((Float) -> Unit)? = null,
        onSpectrum: ((FloatArray) -> Unit)? = null
    ) {
        resultCallback = onResult
        partialResultCallback = onPartialResult
        stateCallback = onStateChange
        errorCallback = onError
        amplitudeCallback = onAmplitude
        spectrumCallback = onSpectrum
    }

    fun startPreStart() {
        cancelPreStart()
        val record = createAudioRecord() ?: return
        record.startRecording()
        synchronized(this) {
            preStartedRecord = record
        }
        mainHandler.removeCallbacks(preStartTimeoutRunnable)
        mainHandler.postDelayed(preStartTimeoutRunnable, 2000)
    }

    fun cancelPreStart() {
        mainHandler.removeCallbacks(preStartTimeoutRunnable)
        synchronized(this) {
            val record = preStartedRecord
            preStartedRecord = null
            if (record != null) {
                try { record.stop() } catch (_: Exception) { }
                record.release()
            }
        }
    }

    fun release() {
        cancelPreStart()
        mainHandler.removeCallbacks(pendingBackendRelease)
        pendingReleaseSession = -1
        cancelRecognition()
        val b = synchronized(preloadLock) {
            val tmp = backend
            backend = null
            tmp
        }
        if (b != null) {
            Thread {
                b.release()
            }.start()
        }
    }

    private var isPreloading = false
    private val preloadLock = java.lang.Object()

    fun preload(): Boolean {
        synchronized(preloadLock) {
            if (backend != null) return true
            isPreloading = true
        }
        
        val newBackend = createBackend()
        if (newBackend == null) {
            synchronized(preloadLock) {
                isPreloading = false
                preloadLock.notifyAll()
            }
            return false
        }

        newBackend.setCallbacks(
            onResult = { text -> handleResult(text) },
            onPartialResult = { text -> handlePartialResult(text) },
            onStateChange = { state -> setState(state) },
            onError = { error -> handleError(error) }
        )

        if (!newBackend.initialize()) {
            synchronized(preloadLock) {
                isPreloading = false
                preloadLock.notifyAll()
            }
            return false
        }

        synchronized(preloadLock) {
            backend = newBackend
            isPreloading = false
            preloadLock.notifyAll()
        }

        return true
    }

    private fun createBackend(): AsrBackend? {
        return AsrBackendFactory.create(context)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun createAudioRecord(bufferSecs: Float = 2.0f): AudioRecord? {
        val bufferSize = (SAMPLE_RATE * bufferSecs).toInt()
        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                null
            } else {
                record
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AudioRecord", e)
            null
        }
    }

    private inner class RecordingThread(
        private val currentBackend: AsrBackend,
        private val preStarted: AudioRecord? = null
    ) : Thread("AsrRecording") {

        private val spectrumAnalyzer = SpectrumAnalyzer()

        @Volatile
        var stopRequested = false

        @Volatile
        private var audioRecord: AudioRecord? = null

        override fun run() {
            val audioRecord = preStarted ?: (createAudioRecord() ?: run {
                mainHandler.post {
                    errorCallback?.invoke("无法启动录音", false)
                    setState(RecognitionState.ERROR)
                }
                return
            })
            this.audioRecord = audioRecord

            if (!currentBackend.start()) {
                audioRecord.stop()
                audioRecord.release()
                mainHandler.post {
                    errorCallback?.invoke("启动引擎失败", false)
                    setState(RecognitionState.ERROR)
                }
                return
            }

            try {
                if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.startRecording()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    errorCallback?.invoke("录音启动失败，请检查麦克风权限", true)
                    setState(RecognitionState.ERROR)
                }
                return
            }
            mainHandler.post {
                setState(RecognitionState.LISTENING)
            }

            val buffer = ShortArray((SAMPLE_RATE * BUFFER_SIZE_SECONDS).toInt())
            val byteBuffer = ByteArray(buffer.size * 2)
            var speechDetected = false
            val preSpeechBuffer = ArrayDeque<ByteArray>()
            val maxPreSpeechChunks = 4

            try {
                while (!interrupted() && !stopRequested) {
                    val nread = audioRecord.read(buffer, 0, buffer.size)
                    if (nread > 0) {
                        var peak = 0
                        for (i in 0 until nread) {
                            val s = buffer[i].toInt()
                            byteBuffer[i * 2] = (s and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                            val abs = if (s < 0) -s else s
                            if (abs > peak) peak = abs
                        }
                        val normalized = (peak / 32768f).coerceIn(0f, 1f)
                        val spectrum = spectrumAnalyzer.analyze(buffer, nread)
                        mainHandler.post {
                            amplitudeCallback?.invoke(normalized)
                            spectrumCallback?.invoke(spectrum)
                        }
                        val chunk = byteBuffer.copyOf(nread * 2)
                        if (!speechDetected) {
                            preSpeechBuffer.addLast(chunk)
                            if (preSpeechBuffer.size >= maxPreSpeechChunks) {
                                speechDetected = true
                                while (preSpeechBuffer.isNotEmpty()) {
                                    currentBackend.processAudioChunk(preSpeechBuffer.removeFirst())
                                }
                            } else if (isSpeech(chunk)) {
                                speechDetected = true
                                while (preSpeechBuffer.isNotEmpty()) {
                                    currentBackend.processAudioChunk(preSpeechBuffer.removeFirst())
                                }
                            }
                        } else {
                            currentBackend.processAudioChunk(chunk)
                        }
                    } else if (nread < 0) {
                        break
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { audioRecord.stop() } catch (_: Exception) { }
                try { audioRecord.release() } catch (_: Exception) { }
            }

            currentBackend.stop()
        }

        fun forceStopAudio() {
            audioRecord?.let { record ->
                try { record.stop() } catch (_: Exception) { }
                try { record.release() } catch (_: Exception) { }
            }
        }

        private fun isSpeech(chunk: ByteArray): Boolean {
            var peak = 0
            for (i in 0 until chunk.size / 2) {
                val low = chunk[i * 2].toInt() and 0xFF
                val high = chunk[i * 2 + 1].toInt()
                val sample = ((high shl 8) or low).toShort().toInt()
                val abs = kotlin.math.abs(sample)
                if (abs > peak) peak = abs
            }
            return peak > SPEECH_THRESHOLD
        }
    }

    private fun handleResult(text: String) {
        mainHandler.post {
            resultCallback?.invoke(text)
        }
    }

    private fun handlePartialResult(text: String) {
        mainHandler.post {
            if (text.isNotEmpty()) {
                partialResultCallback?.invoke(text)
            }
        }
    }

    private fun handleError(error: String) {
        mainHandler.post {
            errorCallback?.invoke(error, true)
        }
    }
}
