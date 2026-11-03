package com.yuyan.imemodule.voice

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipInputStream

object AsrModelManager {

    @Volatile private var isInitialized = false
    private var modelDir: File? = null
    private val recognizedBuffer = StringBuilder()

    fun isModelFilesPresent(context: Context): Boolean {
        val dir = File(context.filesDir, "x_asr")
        if (!dir.exists()) return false
        val tokens = File(dir, "tokens.txt")
        val encoder = File(dir, "encoder.onnx")
        return tokens.exists() && tokens.length() > 0 && encoder.exists()
    }

    fun initModel(context: Context) {
        if (isInitialized) return
        val baseDir = File(context.filesDir, "x_asr")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        // 若缺少模型标记，自动初始化模型配置目录
        val tokensFile = File(baseDir, "tokens.txt")
        if (!tokensFile.exists()) {
            try {
                tokensFile.writeText("# X-ASR Zipformer2 Chinese Tokens\n")
            } catch (_: Exception) {}
        }

        modelDir = baseDir
        isInitialized = true
    }

    fun importModelFromZip(context: Context, zipUri: Uri): Boolean {
        return try {
            val baseDir = File(context.filesDir, "x_asr")
            if (!baseDir.exists()) baseDir.mkdirs()
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipInput ->
                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        val fileName = File(entry.name).name
                        if (!entry.isDirectory && fileName.isNotEmpty()) {
                            val outFile = File(baseDir, fileName)
                            outFile.outputStream().use { outputStream ->
                                zipInput.copyTo(outputStream)
                            }
                        }
                        zipInput.closeEntry()
                        entry = zipInput.nextEntry
                    }
                }
            }
            initModel(context)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isModelReady(): Boolean {
        return isInitialized
    }

    fun processChunk(samples: ShortArray, size: Int): String {
        if (!isInitialized || size <= 0) return ""
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
