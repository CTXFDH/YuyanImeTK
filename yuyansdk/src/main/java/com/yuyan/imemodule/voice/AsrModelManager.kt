package com.yuyan.imemodule.voice

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream

object AsrModelManager {

    private const val TAG = "X_ASR_NATIVE"
    @Volatile private var isInitialized = false
    private var modelDir: File? = null
    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private val recognizedBuffer = StringBuilder()

    fun isModelFilesPresent(context: Context): Boolean {
        val dir = File(context.filesDir, "x_asr")
        if (!dir.exists()) return false
        val files = dir.walkTopDown().filter { it.isFile }.toList()
        if (files.isEmpty()) return false
        val tokensFile = files.firstOrNull { it.name.contains("tokens") || it.name.endsWith(".txt") }
        val hasValidTokens = tokensFile != null && tokensFile.length() > 100
        val hasEncoder = files.any { it.name.contains("encoder") || it.name.endsWith(".onnx") }
        return hasValidTokens && hasEncoder
    }

    fun initModel(context: Context) {
        if (isInitialized && recognizer?.isNativeReady() == true) return
        val baseDir = File(context.filesDir, "x_asr")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }

        // 自动检查并从工程 assets/x_asr 自动解压打包进 APK 的模型文件
        copyAssetsModelIfNeeded(context, baseDir)

        modelDir = baseDir

        try {
            val files = baseDir.walkTopDown().filter { it.isFile }.toList()
            val tokensFile = files.firstOrNull { it.name.contains("tokens") || it.name.endsWith(".txt") }
            val encoderFile = files.firstOrNull { it.name.contains("encoder") && it.name.endsWith(".onnx") }
            val decoderFile = files.firstOrNull { it.name.contains("decoder") && it.name.endsWith(".onnx") }
            val joinerFile = files.firstOrNull { it.name.contains("joiner") && it.name.endsWith(".onnx") }

            Log.d(TAG, "Init model files check - encoder: ${encoderFile?.name}, decoder: ${decoderFile?.name}, joiner: ${joinerFile?.name}, tokens: ${tokensFile?.name}")

            if (encoderFile != null && tokensFile != null && tokensFile.length() > 100) {
                val config = OnlineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                    modelConfig = OnlineModelConfig(
                        transducer = OnlineTransducerModelConfig(
                            encoder = encoderFile.absolutePath,
                            decoder = decoderFile?.absolutePath ?: "",
                            joiner = joinerFile?.absolutePath ?: "",
                            tokens = tokensFile.absolutePath
                        ),
                        numThreads = 2,
                        provider = "cpu"
                    )
                )
                recognizer = OnlineRecognizer(config)
                if (recognizer?.isNativeReady() == true) {
                    stream = recognizer?.createStream()
                    Log.i(TAG, "X-ASR C++ ONNX Native Recognizer & Stream initialized successfully!")
                } else {
                    Log.e(TAG, "OnlineRecognizer native ptr is null. Check JNI library loading.")
                }
            } else {
                Log.w(TAG, "X-ASR model files not complete. Encoder: ${encoderFile != null}, Tokens: ${tokensFile != null}")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing X-ASR model: ${e.message}", e)
        }

        isInitialized = true
    }

    private fun copyAssetsModelIfNeeded(context: Context, baseDir: File) {
        try {
            val assetFiles = context.assets.list("x_asr") ?: return
            if (assetFiles.isNotEmpty()) {
                for (fileName in assetFiles) {
                    val outFile = File(baseDir, fileName)
                    if (!outFile.exists() || outFile.length() == 0L) {
                        context.assets.open("x_asr/$fileName").use { input ->
                            outFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        normalizeModelFileNames(baseDir, fileName, outFile)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun processChunk(samples: ShortArray, size: Int): String {
        if (size <= 0) return recognizedBuffer.toString()

        val rec = recognizer
        val stm = stream

        if (rec != null && stm != null && rec.isNativeReady()) {
            try {
                val floatSamples = FloatArray(size) { i -> samples[i] / 32768.0f }
                stm.acceptWaveform(floatSamples, sampleRate = 16000)
                while (rec.isReady(stm)) {
                    rec.decode(stm)
                }
                val text = rec.getResult(stm).text
                if (text.isNotEmpty()) {
                    recognizedBuffer.clear()
                    recognizedBuffer.append(text)
                    Log.d(TAG, "ONNX Streaming Recognized: $text")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "ONNX processChunk error: ${e.message}", e)
            }
        }

        return recognizedBuffer.toString()
    }

    fun finishRecognition(): String {
        val rec = recognizer
        val stm = stream
        if (rec != null && stm != null && rec.isNativeReady()) {
            try {
                stm.inputFinished()
                while (rec.isReady(stm)) {
                    rec.decode(stm)
                }
                val text = rec.getResult(stm).text
                if (text.isNotEmpty()) {
                    recognizedBuffer.clear()
                    recognizedBuffer.append(text)
                }
                rec.reset(stm)
                Log.d(TAG, "ONNX Final Result: $text")
            } catch (e: Throwable) {
                Log.e(TAG, "ONNX finishRecognition error: ${e.message}", e)
            }
        }

        val result = recognizedBuffer.toString().trim()
        recognizedBuffer.clear()
        return result
    }

    fun releaseModel() {
        try {
            stream?.release()
            recognizer?.release()
        } catch (_: Throwable) {}
        stream = null
        recognizer = null
        recognizedBuffer.clear()
        isInitialized = false
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

        // 2. 尝试作为 .tar.bz2 / .bz2 / .tar 归档解压
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

    fun appendRecognizedText(text: String) {
        if (text.isNotEmpty()) {
            recognizedBuffer.clear()
            recognizedBuffer.append(text)
        }
    }
}
