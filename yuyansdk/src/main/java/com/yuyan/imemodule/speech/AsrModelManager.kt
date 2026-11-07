package com.yuyan.imemodule.speech

import android.content.Context
import com.yuyan.imemodule.speech.models.AsrModelProfile
import com.yuyan.imemodule.speech.models.AsrModelRegistry
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
