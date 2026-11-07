package com.yuyan.imemodule.speech

import android.content.Context
import com.yuyan.imemodule.speech.models.AsrModelProfile
import com.yuyan.imemodule.speech.models.AsrModelRegistry
import com.yuyan.imemodule.utils.queryFileName
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

class AsrModelManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "asr_model"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val TOKENS_FILE = "tokens.txt"

        private fun AsrModelProfile.toAsrModelInfo(): AsrModelInfo = AsrModelInfo(
            id = id,
            name = name,
            description = description,
            language = language,
            size = size,
            downloadUrl = downloadUrl,
            modelType = "transducer",
            files = listOf(encoderFile, decoderFile, joinerFile, tokensFile),
            encoderFile = encoderFile,
            decoderFile = decoderFile,
            joinerFile = joinerFile
        )
    }

    data class AsrModelInfo(
        val id: String,
        val name: String,
        val description: String = "",
        val language: String,
        val size: String,
        val downloadUrl: String,
        val modelType: String = "transducer",
        val files: List<String>,
        val encoderFile: String = "",
        val decoderFile: String = "",
        val joinerFile: String = "",
        val needsAutoPunctuation: Boolean = true
    )

    fun getAsrModels(): List<AsrModelInfo> {
        return AsrModelRegistry.profiles.map { it.toAsrModelInfo() }
    }

    fun isModelReady(): Boolean = getSelectedModelInfo() != null

    fun getModelInfo(modelId: String): AsrModelInfo =
        getAsrModels().find { it.id == modelId }
            ?: AsrModelRegistry.profileOrDefault(modelId).toAsrModelInfo()

    fun isModelInstalled(modelId: String): Boolean {
        val dir = getModelDir(modelId)
        if (!dir.isDirectory) return false
        val info = getModelInfo(modelId)
        return listOf(info.encoderFile, info.decoderFile, info.joinerFile, TOKENS_FILE)
            .all { name -> findFile(dir, name)?.let { it.exists() && it.length() > 0 } == true }
    }

    fun getModelDir(modelId: String): File {
        return File(context.filesDir, "models/$modelId")
    }

    fun getSelectedModelDir(): File? {
        val info = getSelectedModelInfo() ?: return null
        return getModelDir(info.id)
    }

    fun getSelectedModelId(): String? {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_SELECTED_MODEL, null)?.takeIf { it.isNotBlank() }
    }

    fun getSelectedModelInfo(): AsrModelInfo? {
        val models = getAsrModels()
        val selectedId = getSelectedModelId()
        if (selectedId != null && isModelInstalled(selectedId)) {
            return models.find { it.id == selectedId }
                ?: AsrModelRegistry.profileOrDefault(selectedId).toAsrModelInfo()
        }
        return models.find { isModelInstalled(it.id) }
    }

    fun setModel(modelId: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(KEY_SELECTED_MODEL, modelId).apply()
    }

    fun importModelFromArchive(uri: android.net.Uri, modelId: String = "zipformer-zh-int8"): Boolean {
        val targetDir = getModelDir(modelId).apply { mkdirs() }
        val cr = context.contentResolver
        return try {
            val rawStream = cr.openInputStream(uri) ?: return false
            val bufferedStream = BufferedInputStream(rawStream, 128 * 1024)

            bufferedStream.mark(4)
            val header = ByteArray(4)
            val readCount = bufferedStream.read(header, 0, 4)
            bufferedStream.reset()

            val fileName = cr.queryFileName(uri)?.lowercase() ?: ""

            val success = when {
                (readCount >= 2 && header[0] == 'B'.code.toByte() && header[1] == 'Z'.code.toByte()) || fileName.contains(".bz2") -> {
                    extractTarBz2(bufferedStream, targetDir)
                }
                (readCount >= 2 && header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()) || fileName.contains(".gz") -> {
                    extractTarGz(bufferedStream, targetDir)
                }
                (readCount >= 4 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte() && header[2] == 3.toByte() && header[3] == 4.toByte()) || fileName.endsWith(".zip") -> {
                    extractZip(bufferedStream, targetDir)
                }
                else -> {
                    extractTarBz2(bufferedStream, targetDir) || extractZip(bufferedStream, targetDir)
                }
            }
            if (success) setModel(modelId)
            isModelInstalled(modelId)
        } catch (_: Exception) {
            false
        }
    }

    private fun extractZip(inputStream: InputStream, targetDir: File): Boolean {
        return try {
            val zipIn = ZipInputStream(inputStream)
            var entry = zipIn.nextEntry
            var count = 0
            val buffer = ByteArray(64 * 1024)
            while (entry != null) {
                val fName = File(entry.name).name
                if (!entry.isDirectory && fName.isNotBlank()) {
                    val destFile = File(targetDir, fName)
                    BufferedOutputStream(destFile.outputStream(), 64 * 1024).use { out ->
                        var len: Int
                        while (zipIn.read(buffer).also { len = it } != -1) {
                            out.write(buffer, 0, len)
                        }
                    }
                    count++
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            zipIn.close()
            count > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun extractTarBz2(inputStream: InputStream, targetDir: File): Boolean {
        return try {
            val bz2In = BZip2CompressorInputStream(inputStream, true)
            val tarIn = TarArchiveInputStream(bz2In)
            extractTarStream(tarIn, targetDir)
        } catch (_: Exception) {
            false
        }
    }

    private fun extractTarGz(inputStream: InputStream, targetDir: File): Boolean {
        return try {
            val gzIn = GzipCompressorInputStream(inputStream, true)
            val tarIn = TarArchiveInputStream(gzIn)
            extractTarStream(tarIn, targetDir)
        } catch (_: Exception) {
            false
        }
    }

    private fun extractTarStream(tarIn: TarArchiveInputStream, targetDir: File): Boolean {
        return try {
            var entry = tarIn.nextEntry
            var count = 0
            val buffer = ByteArray(64 * 1024)
            while (entry != null) {
                val fName = File(entry.name).name
                if (!entry.isDirectory && fName.isNotBlank()) {
                    val destFile = File(targetDir, fName)
                    BufferedOutputStream(destFile.outputStream(), 64 * 1024).use { out ->
                        var len: Int
                        while (tarIn.read(buffer).also { len = it } != -1) {
                            out.write(buffer, 0, len)
                        }
                    }
                    count++
                }
                entry = tarIn.nextEntry
            }
            tarIn.close()
            count > 0
        } catch (_: Exception) {
            false
        }
    }

    fun findFile(dir: File, fileName: String): File? {
        val direct = File(dir, fileName)
        if (direct.exists()) return direct
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                val found = findFile(child, fileName)
                if (found != null) return found
            }
        }
        return null
    }
}
