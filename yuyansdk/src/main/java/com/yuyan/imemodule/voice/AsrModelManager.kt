package com.yuyan.imemodule.voice

import android.content.Context
import android.net.Uri
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.util.zip.ZipInputStream

object AsrModelManager {

    @Volatile private var isInitialized = false
    private var modelDir: File? = null
    private val recognizedBuffer = StringBuilder()

    fun isModelFilesPresent(context: Context): Boolean {
        val dir = File(context.filesDir, "x_asr")
        if (!dir.exists()) return false
        val files = dir.walkTopDown().filter { it.isFile }.toList()
        val hasTokens = files.any { it.name.contains("tokens") || it.name.endsWith(".txt") }
        val hasEncoder = files.any { it.name.contains("encoder") || it.name.endsWith(".onnx") }
        return hasTokens && hasEncoder
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

    fun importModelFromZip(context: Context, zipUri: Uri): Boolean {
        var success = false
        val baseDir = File(context.filesDir, "x_asr")
        if (!baseDir.exists()) baseDir.mkdirs()

        // 1. 尝试直接作为 .zip 解压
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
                            normalizeModelFileNames(baseDir, fileName, outFile)
                            success = true
                        }
                        zipInput.closeEntry()
                        entry = zipInput.nextEntry
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. 尝试作为 .tar.bz2 / .bz2 / .tar 归档解压 (解压 GitHub Releases 原装 .tar.bz2 模型)
        if (!success) {
            try {
                context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                    val bzIn = try {
                        BZip2CompressorInputStream(inputStream)
                    } catch (_: Exception) {
                        inputStream
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
            } catch (_: Exception) {}
        }

        if (success && isModelFilesPresent(context)) {
            initModel(context)
            return true
        }
        return false
    }

    private fun normalizeModelFileNames(baseDir: File, fileName: String, outFile: File) {
        val lowerName = fileName.lowercase()
        if (lowerName.startsWith("encoder") && lowerName.endsWith(".onnx") && fileName != "encoder.onnx") {
            try { outFile.copyTo(File(baseDir, "encoder.onnx"), overwrite = true) } catch (_: Exception) {}
        } else if (lowerName.startsWith("decoder") && lowerName.endsWith(".onnx") && fileName != "decoder.onnx") {
            try { outFile.copyTo(File(baseDir, "decoder.onnx"), overwrite = true) } catch (_: Exception) {}
        } else if (lowerName.startsWith("joiner") && lowerName.endsWith(".onnx") && fileName != "joiner.onnx") {
            try { outFile.copyTo(File(baseDir, "joiner.onnx"), overwrite = true) } catch (_: Exception) {}
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
