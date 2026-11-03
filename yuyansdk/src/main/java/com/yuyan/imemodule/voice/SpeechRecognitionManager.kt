package com.yuyan.imemodule.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.ArrayDeque
import kotlin.math.sqrt

class SpeechRecognitionManager(private val context: Context) {

    enum class State {
        IDLE,
        LISTENING,
        PROCESSING,
        ERROR
    }

    interface Listener {
        fun onStateChanged(state: State)
        fun onAmplitudeChanged(amplitude: Float)
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(errorMsg: String)
    }

    var listener: Listener? = null
    var currentState: State = State.IDLE
        private set(value) {
            field = value
            mainHandler.post { listener?.onStateChanged(value) }
        }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false

    // VAD 前置环形缓冲队列 (~0.4秒音频缓存)
    private val preSpeechBuffer = ArrayDeque<ByteArray>(4)

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (!hasRecordPermission()) {
            currentState = State.ERROR
            mainHandler.post { listener?.onError("未授权录音权限，请点击开启") }
            return
        }

        if (currentState == State.LISTENING || currentState == State.PROCESSING) return

        currentState = State.LISTENING
        preSpeechBuffer.clear()

        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                startSystemSpeechRecognizer()
            }
        } catch (_: Exception) {}

        startAudioRecording()
    }

    private fun startSystemSpeechRecognizer() {
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                }
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        currentState = State.LISTENING
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        val norm = (rmsdB / 10f).coerceIn(0f, 1f)
                        listener?.onAmplitudeChanged(norm)
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        currentState = State.PROCESSING
                    }
                    override fun onError(error: Int) {
                        currentState = State.IDLE
                        stopAudioRecording()
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            listener?.onFinalResult(text)
                        }
                        currentState = State.IDLE
                        stopAudioRecording()
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            listener?.onPartialResult(text)
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(intent)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioRecording() {
        if (!hasRecordPermission()) return
        isRecording = true
        recordingThread = Thread {
            try {
                val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                val bufferSize = maxOf(minBufSize, SAMPLE_RATE * 2)
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    stopAudioRecording()
                    return@Thread
                }
                audioRecord?.startRecording()

                val buffer = ShortArray(1024)
                while (isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / readSize)
                        val normAmp = (rms / 3000.0).coerceIn(0.0, 1.0).toFloat()
                        mainHandler.post { listener?.onAmplitudeChanged(normAmp) }
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
                audioRecord = null
            }
        }.also { it.start() }
    }

    fun stopListening() {
        if (!isRecording) return
        currentState = State.PROCESSING
        stopAudioRecording()
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
        }
    }

    fun cancelListening() {
        isRecording = false
        currentState = State.IDLE
        stopAudioRecording()
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
        }
    }

    private fun stopAudioRecording() {
        isRecording = false
        recordingThread?.interrupt()
        recordingThread = null
    }

    fun destroy() {
        cancelListening()
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }
}
