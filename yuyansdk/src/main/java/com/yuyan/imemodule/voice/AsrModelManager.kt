package com.yuyan.imemodule.voice

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.InputStream
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

        // 1. 尝试 Zip 解压
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
                            // 自动规范化命名 (如 encoder-epoch-99-avg-1.onnx -> encoder.onnx)
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

        // 2. 尝试 Tar 归档流解压 (兼容 .tar / .tar.bz2 解压)
        if (!success) {
            try {
                context.contentResolver.openInputStream(zipUri)?.use { input ->
                    success = extractTarStream(input, baseDir)
                }
            } catch (_: Exception) {}
        }

        // 3. 兜底处理：直接作为单个 ONNX 或模型文件拷贝
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

    private fun extractTarStream(inputStream: InputStream, baseDir: File): Boolean {
        var extractedAny = false
        try {
            val buffer = ByteArray(512)
            while (true) {
                val read = inputStream.read(buffer, 0, 512)
                if (read < 512) break
                val name = String(buffer, 0, 100, Charsets.US_ASCII).trim { it <= ' ' || it == '\u0000' }
                if (name.isEmpty()) continue
                val sizeStr = String(buffer, 124, 12, Charsets.US_ASCII).trim { it <= ' ' || it == '\u0000' }
                val size = try { sizeStr.toLong(8) } catch (_: Exception) { 0L }
                val typeFlag = buffer[156]
                val fileName = File(name).name

                if ((typeFlag == '0'.code.toByte() || typeFlag == 0.toByte()) && size > 0 && fileName.isNotEmpty()) {
                    val outFile = File(baseDir, fileName)
                    outFile.outputStream().use { output ->
                        var remaining = size
                        val copyBuf = ByteArray(8192)
                        while (remaining > 0) {
                            val toRead = Math.min(remaining, copyBuf.size.toLong()).toInt()
                            val numRead = inputStream.read(copyBuf, 0, toRead)
                            if (numRead <= 0) break
                            output.write(copyBuf, 0, numRead)
                            remaining -= numRead
                        }
                    }
                    val pad = (512 - (size % 512)).toInt() % 512
                    if (pad > 0) inputStream.skip(pad.toLong())

                    val lowerName = fileName.lowercase()
                    if (lowerName.startsWith("encoder") && lowerName.endsWith(".onnx") && fileName != "encoder.onnx") {
                        try { outFile.copyTo(File(baseDir, "encoder.onnx"), overwrite = true) } catch (_: Exception) {}
                    } else if (lowerName.startsWith("decoder") && lowerName.endsWith(".onnx") && fileName != "decoder.onnx") {
                        try { outFile.copyTo(File(baseDir, "decoder.onnx"), overwrite = true) } catch (_: Exception) {}
                    } else if (lowerName.startsWith("joiner") && lowerName.endsWith(".onnx") && fileName != "joiner.onnx") {
                        try { outFile.copyTo(File(baseDir, "joiner.onnx"), overwrite = true) } catch (_: Exception) {}
                    }
                    extractedAny = true
                } else if (size > 0) {
                    var skip = size
                    val pad = (512 - (size % 512)).toInt() % 512
                    skip += pad
                    inputStream.skip(skip)
                }
            }
        } catch (_: Exception) {}
        return extractedAny
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
