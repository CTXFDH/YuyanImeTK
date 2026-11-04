package com.yuyan.imemodule.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.yuyan.imemodule.prefs.AppPrefs
import kotlin.math.sqrt

/**
 * 纯粹物理麦克风 PCM 音频流采集 + X-ASR 离线 C++ 神经网络引擎管道 (已彻底清理系统 SpeechRecognizer 冗余代码)
 */
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
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false

    private var asrService: AsrProcessService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is AsrProcessService.AsrBinder) {
                asrService = service.getService()
                isServiceBound = true
                asrService?.setCallback(object : AsrProcessService.AsrCallback {
                    override fun onPartialResult(text: String) {
                        mainHandler.post { listener?.onPartialResult(text) }
                    }
                    override fun onFinalResult(text: String) {
                        mainHandler.post {
                            listener?.onFinalResult(text)
                            currentState = State.IDLE
                        }
                    }
                    override fun onError(msg: String) {
                        mainHandler.post {
                            listener?.onError(msg)
                            currentState = State.IDLE
                        }
                    }
                })
                asrService?.prepareEngine()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            asrService = null
            isServiceBound = false
        }
    }

    init {
        try {
            val intent = Intent(context, AsrProcessService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (_: Exception) {}
        AsrModelManager.initModel(context)
    }

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun hasRecordPermission(): Boolean {
        val perm1 = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val perm2 = PermissionChecker.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED
        if (perm1 || perm2) return true

        return try {
            val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (minBufSize <= 0) return true
            val testRec = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufSize
            )
            val isInit = testRec.state == AudioRecord.STATE_INITIALIZED
            testRec.release()
            isInit
        } catch (_: Exception) {
            true
        }
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
        startAudioRecording()
    }

    @SuppressLint("MissingPermission")
    private fun startAudioRecording() {
        isRecording = true
        recordingThread = Thread {
            try {
                val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                val bufferSize = maxOf(minBufSize, SAMPLE_RATE * 2)
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
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

                        // 1. 推送至后台 :asr 进程
                        asrService?.processAudioChunk(buffer, readSize)

                        // 2. 本地同步调用 X-ASR 进行 C++ 神经网络推导转写
                        val livePartialText = AsrModelManager.processChunk(buffer, readSize)
                        if (livePartialText.isNotEmpty()) {
                            mainHandler.post { listener?.onPartialResult(livePartialText) }
                        }
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
        if (!isRecording && currentState == State.IDLE) return
        currentState = State.PROCESSING
        stopAudioRecording()

        // 收集提交 X-ASR 离线 C++ 神经网络推导转写出的最终文字结果
        val finalText = AsrModelManager.finishRecognition()
        val autoPunc = AppPrefs.getInstance().voice.voiceAutoPunctuation.getValue()
        val formattedText = if (autoPunc && finalText.isNotEmpty() && !finalText.endsWith("。") && !finalText.endsWith("，") && !finalText.endsWith("？")) {
            "$finalText。"
        } else {
            finalText
        }

        mainHandler.post {
            if (formattedText.isNotEmpty()) {
                listener?.onFinalResult(formattedText)
            }
            currentState = State.IDLE
        }

        asrService?.finishSpeech()
    }

    fun cancelListening() {
        isRecording = false
        currentState = State.IDLE
        stopAudioRecording()
    }

    private fun stopAudioRecording() {
        isRecording = false
        recordingThread?.interrupt()
        recordingThread = null
    }

    fun destroy() {
        cancelListening()
        if (isServiceBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (_: Exception) {}
            isServiceBound = false
        }
    }
}
