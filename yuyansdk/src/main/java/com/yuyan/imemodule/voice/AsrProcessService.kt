package com.yuyan.imemodule.voice

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.yuyan.imemodule.prefs.AppPrefs

class AsrProcessService : Service() {

    inner class AsrBinder : Binder() {
        fun getService(): AsrProcessService = this@AsrProcessService
    }

    interface AsrCallback {
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(msg: String)
    }

    private val binder = AsrBinder()
    private val handler = Handler(Looper.getMainLooper())
    private var callback: AsrCallback? = null
    private var isEngineLoaded = false

    private val autoReleaseRunnable = Runnable {
        val keepResident = AppPrefs.getInstance().voice.voiceKeepResident.getValue()
        if (!keepResident) {
            unloadEngine()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun setCallback(cb: AsrCallback?) {
        this.callback = cb
    }

    fun prepareEngine() {
        handler.removeCallbacks(autoReleaseRunnable)
        if (isEngineLoaded) return
        try {
            AsrModelManager.initModel(applicationContext)
            isEngineLoaded = true
        } catch (e: Exception) {
            callback?.onError("引擎初始化失败: ${e.message}")
        }
    }

    fun processAudioChunk(data: ShortArray, size: Int) {
        handler.removeCallbacks(autoReleaseRunnable)
        if (!isEngineLoaded) {
            prepareEngine()
        }

        try {
            val text = AsrModelManager.processChunk(data, size)
            if (text.isNotEmpty()) {
                val formattedText = formatWithPunctuation(text)
                callback?.onPartialResult(formattedText)
            }
        } catch (e: Exception) {
            callback?.onError("识别异常: ${e.message}")
        }
    }

    fun appendRecognizedText(text: String) {
        AsrModelManager.appendRecognizedText(text)
    }

    fun finishSpeech() {
        try {
            val finalText = AsrModelManager.finishRecognition()
            val formatted = formatWithPunctuation(finalText)
            callback?.onFinalResult(formatted)
        } catch (e: Exception) {
            callback?.onError("识别完成阶段异常: ${e.message}")
        } finally {
            scheduleAutoRelease()
        }
    }

    private fun formatWithPunctuation(rawText: String): String {
        val autoPunc = AppPrefs.getInstance().voice.voiceAutoPunctuation.getValue()
        if (!autoPunc || rawText.isEmpty()) return rawText
        return if (!rawText.endsWith("。") && !rawText.endsWith("，") && !rawText.endsWith("？")) {
            "$rawText。"
        } else {
            rawText
        }
    }

    private fun scheduleAutoRelease() {
        handler.removeCallbacks(autoReleaseRunnable)
        handler.postDelayed(autoReleaseRunnable, 10000) // 10秒闲置自动释放内存
    }

    private fun unloadEngine() {
        try {
            AsrModelManager.releaseModel()
            isEngineLoaded = false
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoReleaseRunnable)
        unloadEngine()
        super.onDestroy()
    }
}
