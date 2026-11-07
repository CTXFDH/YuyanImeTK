package com.yuyan.imemodule.speech

import android.content.Context
import com.yuyan.imemodule.speech.models.AsrModelProfile
import com.yuyan.imemodule.speech.models.AsrModelRegistry
import com.yuyan.imemodule.utils.queryFileName
import java.io.File

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
            val fileName = cr.queryFileName(uri)?.lowercase() ?: ""
            val inputStream = cr.openInputStream(uri) ?: return false

            val success = when {
                fileName.endsWith(".tar.bz2") || fileName.endsWith(".tbz2") || fileName.endsWith(".bz2") -> {
                    extractTarBz2(inputStream, targetDir)
                }
                fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") -> {
                    extractTarGz(inputStream, targetDir)
                }
                fileName.endsWith(".tar") -> {
                    extractTar(inputStream, targetDir)
                }
                else -> {
                    if (!extractZip(inputStream, targetDir)) {
                        cr.openInputStream(uri)?.use { stream -> extractTarBz2(stream, targetDir) } ?: false
                    } else true
                }
            }
            if (success) setModel(modelId)
            isModelInstalled(modelId)
        } catch (_: Exception) {
            false
        }
    }

    private fun extractZip(inputStream: java.io.InputStream, targetDir: File): Boolean {
        return try {
            val zipIn = java.util.zip.ZipInputStream(inputStream)
            var entry = zipIn.nextEntry
            var count = 0
            while (entry != null) {
                val fName = File(entry.name).name
                if (!entry.isDirectory && fName.isNotBlank()) {
                    val destFile = File(targetDir, fName)
                    destFile.outputStream().use { out -> zipIn.copyTo(out) }
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

    private fun extractTarBz2(inputStream: java.io.InputStream, targetDir: File): Boolean {
        return try {
            val bz2In = org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream(inputStream)
            val tarIn = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(bz2In)
            extractTarStream(tarIn, targetDir)
        } catch (_: Exception) {
            false
        }
    }

    private fun extractTarGz(inputStream: java.io.InputStream, targetDir: File): Boolean {
        return try {
            val gzIn = org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream(inputStream)
            val tarIn = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzIn)
            extractTarStream(tarIn, targetDir)
        } catch (_: Exception) {
            false
        }
    }

    private fun extractTar(inputStream: java.io.InputStream, targetDir: File): Boolean {
        return try {
            val tarIn = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(inputStream)
            extractTarStream(tarIn, targetDir)
        } catch (_: Exception) {
            false
        }
    }

    private fun extractTarStream(tarIn: org.apache.commons.compress.archivers.tar.TarArchiveInputStream, targetDir: File): Boolean {
        return try {
            var entry = tarIn.nextEntry
            var count = 0
            while (entry != null) {
                val fName = File(entry.name).name
                if (!entry.isDirectory && fName.isNotBlank()) {
                    val destFile = File(targetDir, fName)
                    destFile.outputStream().use { out -> tarIn.copyTo(out) }
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
