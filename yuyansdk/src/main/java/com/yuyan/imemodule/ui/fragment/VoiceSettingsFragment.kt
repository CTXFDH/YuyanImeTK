package com.yuyan.imemodule.ui.fragment

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.yuyan.imemodule.R
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.ui.fragment.base.ManagedPreferenceFragment
import com.yuyan.imemodule.utils.thread.ThreadPoolUtils
import com.yuyan.imemodule.voice.AsrModelManager

class VoiceSettingsFragment : ManagedPreferenceFragment(AppPrefs.getInstance().voice) {

    private var modelStatusPreference: Preference? = null

    private val modelPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val context = requireContext()
            Toast.makeText(context, "正在解压导入 X-ASR 离线语音模型...", Toast.LENGTH_SHORT).show()
            ThreadPoolUtils.executeSingleton {
                val success = AsrModelManager.importModelFromZip(context, uri)
                requireActivity().runOnUiThread {
                    if (success) {
                        Toast.makeText(context, "X-ASR 离线语音识别模型导入成功！", Toast.LENGTH_SHORT).show()
                        updateModelStatus()
                    } else {
                        Toast.makeText(context, "模型导入失败，请检查压缩包格式", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onPreferenceUiCreated(screen: PreferenceScreen) {
        super.onPreferenceUiCreated(screen)
        val context = requireContext()
        val isInstalled = AsrModelManager.isModelFilesPresent(context)

        modelStatusPreference = Preference(context).apply {
            isIconSpaceReserved = false
            icon = ContextCompat.getDrawable(context, R.drawable.ic_menu_voice)
            layoutResource = R.layout.sdk_item_preference_card_single
            title = getString(R.string.voice_model_card_title)
            summary = if (isInstalled) {
                "${getString(R.string.voice_model_card_summary)}  ✔ ${getString(R.string.voice_model_installed)}"
            } else {
                "${getString(R.string.voice_model_card_summary)}  ✕ ${getString(R.string.voice_model_not_installed)}"
            }
            setOnPreferenceClickListener {
                Toast.makeText(context, "请选择本地保存的 X-ASR 模型压缩包 (x_asr.zip)", Toast.LENGTH_SHORT).show()
                modelPickerLauncher.launch("*/*")
                true
            }
        }
        screen.addPreference(modelStatusPreference!!)
    }

    private fun updateModelStatus() {
        val context = context ?: return
        val isInstalled = AsrModelManager.isModelFilesPresent(context)
        modelStatusPreference?.summary = if (isInstalled) {
            "${getString(R.string.voice_model_card_summary)}  ✔ ${getString(R.string.voice_model_installed)}"
        } else {
            "${getString(R.string.voice_model_card_summary)}  ✕ ${getString(R.string.voice_model_not_installed)}"
        }
    }
}
