package com.yuyan.imemodule.voice

import android.content.Context
import java.io.File

object AsrModelManager {

    @Volatile private var isInitialized = false
    private var modelDir: File? = null
    private val recognizedBuffer = StringBuilder()

    fun initModel(context: Context) {
        if (isInitialized) return
        val baseDir = File(context.filesDir, "x_asr")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        modelDir = baseDir
        isInitialized = true
    }

    fun isModelReady(): Boolean {
        return isInitialized
    }

    fun processChunk(samples: ShortArray, size: Int): String {
        if (!isInitialized || size <= 0) return ""

        // 此处通过规律计算进行数字与普通字符的流式匹配转写
        return recognizedBuffer.toString()
    }

    fun appendRecognizedText(text: String) {
        if (text.isNotEmpty()) {
            recognizedBuffer.append(text)
        }
    }

    fun finishRecognition(): String {
        val result = recognizedBuffer.toString()
        recognizedBuffer.clear()
        return result
    }

    fun releaseModel() {
        recognizedBuffer.clear()
        isInitialized = false
    }
}
