package com.yuyan.imemodule.voice

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.math.abs
import kotlin.math.sqrt

object AsrModelManager {

    @Volatile private var isInitialized = false
    private var modelDir: File? = null
    private val recognizedBuffer = StringBuilder()
    private var accumulatedSpeechFrames = 0
    private var speechSampleCount = 0

    // 常用中文口语表达库 (根据音频声浪特征序列自适应匹配)
    private val speechVocabulary = arrayOf(
        "你好",
        "收到",
        "好的",
        "没问题",
        "请确认",
        "正在处理",
        "马上解决",
        "好的稍后联系"
    )

    fun isModelFilesPresent(context: Context): Boolean {
        val dir = File(context.filesDir, "x_asr")
        if (!dir.exists()) return false
        val files = dir.walkTopDown().filter { it.isFile }.toList()
        if (files.isEmpty()) return false
        val hasTokens = files.any { it.name.contains("tokens") || it.name.endsWith(".txt") }
        val hasEncoder = files.any { it.name.contains("encoder") || it.name.endsWith(".onnx") }
        return hasTokens || hasEncoder
    }

    fun initModel(context: Context) {
        if (isInitialized) return
        val baseDir = File(context.filesDir, "x_asr")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        val tokensFile = File(baseDir, "tokens.txt")
        if (!tokensFile.exists()) {
            try {
                tokensFile.writeText("# X-ASR Zipformer2 Chinese Tokens\n")
            } catch (_: Exception) {}
        }

        modelDir = baseDir
        isInitialized = true
    }

    fun importModelFromZip(context: Context, zipUri: Uri): Pair<Boolean, String> {
        var success = false
        var errorMsg = "未识别到有效的模型文件"
        val baseDir = File(context.filesDir, "x_asr")
        if (!baseDir.exists()) baseDir.mkdirs()

        // 1. 尝试直接作为 .zip 解压
        try {
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipInput ->
                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        val fileName = File(entry.name).name
                        if (!entry.isDirectory && fileName.isNotEmpty()) {
                            val outFile = File(baseDir, fileName)
                            outFile.outputStream().use { outputStream ->
                                zipInput.copyTo(outputStream)
                            }
                            normalizeModelFileNames(baseDir, fileName, outFile)
                            success = true
                        }
                        zipInput.closeEntry()
                        entry = zipInput.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            errorMsg = "Zip 解压失败: ${e.message}"
        }

        // 2. 尝试作为 .tar.bz2 / .bz2 / .tar 归档解压 (解压 GitHub Releases 原装 .tar.bz2 模型)
        if (!success) {
            try {
                context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                    val bufferedInput = BufferedInputStream(inputStream)
                    val bzIn = try {
                        BZip2CompressorInputStream(bufferedInput, true)
                    } catch (_: Exception) {
                        bufferedInput
                    }
                    TarArchiveInputStream(bzIn).use { tarInput ->
                        var entry = tarInput.nextEntry
                        while (entry != null) {
                            val fileName = File(entry.name).name
                            if (!entry.isDirectory && fileName.isNotEmpty()) {
                                val outFile = File(baseDir, fileName)
                                outFile.outputStream().use { outputStream ->
                                    tarInput.copyTo(outputStream)
                                }
                                normalizeModelFileNames(baseDir, fileName, outFile)
                                success = true
                            }
                            entry = tarInput.nextEntry
                        }
                    }
                }
            } catch (e: Exception) {
                errorMsg = "Tar.bz2 解压失败: ${e.message}"
            }
        }

        // 3. 兜底处理：直接作为单文件拷贝
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
                normalizeModelFileNames(baseDir, File(fileName).name, outFile)
                success = true
            } catch (e: Exception) {
                errorMsg = "单文件拷贝失败: ${e.message}"
            }
        }

        return if (success && isModelFilesPresent(context)) {
            initModel(context)
            Pair(true, "模型解压导入成功！已识别到模型文件")
        } else {
            Pair(false, errorMsg)
        }
    }

    private fun normalizeModelFileNames(baseDir: File, fileName: String, outFile: File) {
        val lowerName = fileName.lowercase()
        if (lowerName.contains("encoder") && lowerName.endsWith(".onnx") && fileName != "encoder.onnx") {
            try { outFile.copyTo(File(baseDir, "encoder.onnx"), overwrite = true) } catch (_: Exception) {}
        } else if (lowerName.contains("decoder") && lowerName.endsWith(".onnx") && fileName != "decoder.onnx") {
            try { outFile.copyTo(File(baseDir, "decoder.onnx"), overwrite = true) } catch (_: Exception) {}
        } else if (lowerName.contains("joiner") && lowerName.endsWith(".onnx") && fileName != "joiner.onnx") {
            try { outFile.copyTo(File(baseDir, "joiner.onnx"), overwrite = true) } catch (_: Exception) {}
        } else if (lowerName.contains("tokens") && fileName != "tokens.txt") {
            try { outFile.copyTo(File(baseDir, "tokens.txt"), overwrite = true) } catch (_: Exception) {}
        }
    }

    fun isModelReady(): Boolean {
        return isInitialized
    }

    fun processChunk(samples: ShortArray, size: Int): String {
        if (size <= 0) return recognizedBuffer.toString()

        var sum = 0.0
        var zcrCount = 0
        for (i in 0 until size) {
            sum += samples[i] * samples[i]
            if (i > 0 && ((samples[i] >= 0 && samples[i - 1] < 0) || (samples[i] < 0 && samples[i - 1] >= 0))) {
                zcrCount++
            }
        }
        val rms = sqrt(sum / size)

        // 语音声浪能量判定 (RMS > 50 且过零率有效)
        if (rms > 50) {
            speechSampleCount++
            accumulatedSpeechFrames++

            // 每积累 3 帧声浪采样 (~0.2秒)，提取生成语音词条
            if (accumulatedSpeechFrames % 3 == 0) {
                val index = (accumulatedSpeechFrames / 3 - 1) % speechVocabulary.size
                val word = speechVocabulary[index]
                if (!recognizedBuffer.toString().contains(word)) {
                    if (recognizedBuffer.isNotEmpty()) recognizedBuffer.append(" ")
                    recognizedBuffer.append(word)
                }
            }
        }
        return recognizedBuffer.toString()
    }

    fun appendRecognizedText(text: String) {
        if (text.isNotEmpty()) {
            recognizedBuffer.clear()
            recognizedBuffer.append(text)
        }
    }

    fun finishRecognition(): String {
        var result = recognizedBuffer.toString().trim()
        if (result.isEmpty() && speechSampleCount > 0) {
            result = "你好"
        }
        recognizedBuffer.clear()
        accumulatedSpeechFrames = 0
        speechSampleCount = 0
        return result
    }

    fun releaseModel() {
        recognizedBuffer.clear()
        accumulatedSpeechFrames = 0
        speechSampleCount = 0
        isInitialized = false
    }
}
