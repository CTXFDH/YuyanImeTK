package com.yuyan.imemodule.voice

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
        val hasTokens = tokens.exists() && tokens.length() > 0
        val files = dir.listFiles() ?: emptyArray()
        val hasEncoder = files.any { it.name.startsWith("encoder") && it.name.endsWith(".onnx") } || File(dir, "encoder.onnx").exists()
        return hasTokens && hasEncoder
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
        var success = false
        val baseDir = File(context.filesDir, "x_asr")
        if (!baseDir.exists()) baseDir.mkdirs()

        try {
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
                            // 自动规范化命名兼容最新 release 模型包 (如 encoder-epoch-99-avg-1.onnx -> encoder.onnx)
                            val lowerName = fileName.lowercase()
                            if (lowerName.startsWith("encoder") && lowerName.endsWith(".onnx") && fileName != "encoder.onnx") {
                                try { outFile.copyTo(File(baseDir, "encoder.onnx"), overwrite = true) } catch (_: Exception) {}
                            } else if (lowerName.startsWith("decoder") && lowerName.endsWith(".onnx") && fileName != "decoder.onnx") {
                                try { outFile.copyTo(File(baseDir, "decoder.onnx"), overwrite = true) } catch (_: Exception) {}
                            } else if (lowerName.startsWith("joiner") && lowerName.endsWith(".onnx") && fileName != "joiner.onnx") {
                                try { outFile.copyTo(File(baseDir, "joiner.onnx"), overwrite = true) } catch (_: Exception) {}
                            }
                            success = true
                        }
                        zipInput.closeEntry()
                        entry = zipInput.nextEntry
                    }
                }
            }
        } catch (_: Exception) {}

        if (!success) {
            try {
                val fileName = context.contentResolver.query(zipUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
                } ?: "encoder.onnx"
                val outFile = File(baseDir, File(fileName).name)
                context.contentResolver.openInputStream(zipUri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                success = true
            } catch (_: Exception) {}
        }

        if (success) {
            initModel(context)
        }
        return success
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
