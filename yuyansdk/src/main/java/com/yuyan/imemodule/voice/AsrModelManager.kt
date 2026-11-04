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
import kotlin.math.sqrt

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

    fun getEngineDiagnosticInfo(context: Context): String {
        val baseDir = File(context.filesDir, "x_asr")
        if (!baseDir.exists()) return "⚠️ 模型目录不存在 (x_asr)"
        val files = baseDir.walkTopDown().filter { it.isFile }.toList()
        val tokensFile = files.firstOrNull { it.name.contains("tokens") || it.name.endsWith(".txt") }
        val encoderFile = files.firstOrNull { it.name.contains("encoder") && it.name.endsWith(".onnx") }
        if (tokensFile == null || tokensFile.length() < 100) return "⚠️ 离线词表缺失或无效 (tokens.txt)"
        if (encoderFile == null) return "⚠️ 声学模型缺失 (encoder.onnx)"
        if (recognizer == null) {
            initModel(context)
        }
        val rec = recognizer
        if (rec == null || !rec.isNativeReady()) {
            return "⚠️ C++ JNI 指针为空 (libsherpa-onnx-jni.so)"
        }
        return "🟢 离线 X-ASR 引擎就绪"
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
                        tokens = tokensFile.absolutePath,
                        numThreads = 2,
                        debug = false,
                        provider = "cpu",
                        modelType = "" // 自动探测 Zipformer 模型结构
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

    fun prepareNewSession() {
        val rec = recognizer
        if (rec != null && rec.isNativeReady()) {
            try {
                stream?.release()
                stream = rec.createStream()
                recognizedBuffer.clear()
                Log.d(TAG, "New OnlineStream created for fresh speech session.")
            } catch (e: Throwable) {
                Log.e(TAG, "prepareNewSession error: ${e.message}", e)
            }
        }
    }

    private fun copyAssetsModelIfNeeded(context: Context, baseDir: File) {
        try {
            val assetFiles = context.assets.list("x_asr") ?: return
            Log.d(TAG, "Found ${assetFiles.size} asset files in assets/x_asr: ${assetFiles.joinToString()}")
            if (assetFiles.isNotEmpty()) {
                for (fileName in assetFiles) {
                    val outFile = File(baseDir, fileName)
                    val assetLength = try {
                        context.assets.openFd("x_asr/$fileName").length
                    } catch (_: Exception) { -1L }

                    // 若手机私有目录下的旧文件不存在，或者小于 1000 字节，强效覆盖提取内置模型！
                    if (!outFile.exists() || outFile.length() < 1000L || (assetLength > 0 && outFile.length() != assetLength)) {
                        context.assets.open("x_asr/$fileName").use { input ->
                            outFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        normalizeModelFileNames(baseDir, fileName, outFile)
                        Log.i(TAG, "Successfully extracted $fileName (${outFile.length()} bytes) to ${outFile.absolutePath}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying assets model: ${e.message}", e)
        }
    }

    fun processChunk(samples: ShortArray, size: Int): String {
        if (size <= 0) return recognizedBuffer.toString()

        val rec = recognizer
        val stm = stream

        if (rec != null && stm != null && rec.isNativeReady()) {
            try {
                // 1:1 复制曦码 (Xime) 官方 C++ 引擎的精细 AGC 自动增益与归一化算法
                var sumSq = 0.0
                for (i in 0 until size) {
                    val s = samples[i].toDouble()
                    sumSq += s * s
                }
                val rms = sqrt(sumSq / size)

                val targetRmsFloat = 0.008
                val maxGain = 20.0
                val silenceRms = 100.0

                var agc = 1.0
                if (rms > silenceRms) {
                    val targetInt16 = targetRmsFloat * 32768.0
                    agc = targetInt16 / rms
                    if (agc > maxGain) agc = maxGain
                    if (agc < 1.0) agc = 1.0
                }

                val floatSamples = FloatArray(size) { i ->
                    val v = (samples[i] * agc).coerceIn(-32767.0, 32767.0)
                    (v / 32768.0).toFloat()
                }

                stm.acceptWaveform(floatSamples, sampleRate = 16000)

                var maxLoop = 5
                while (rec.isReady(stm) && maxLoop > 0) {
                    rec.decode(stm)
                    maxLoop--
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
                // 1. 喂入 0.4s 零点 Padding 音频，刷出 Zipformer Encoder 队列中的隐状态帧
                val padding = FloatArray(6400) { 0f }
                stm.acceptWaveform(padding, sampleRate = 16000)
                stm.inputFinished()

                // 2. 解码刷出最终文字
                var maxLoop = 10
                while (rec.isReady(stm) && maxLoop > 0) {
                    rec.decode(stm)
                    maxLoop--
                }
                val text = rec.getResult(stm).text
                if (text.isNotEmpty()) {
                    recognizedBuffer.clear()
                    recognizedBuffer.append(text)
                }
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
