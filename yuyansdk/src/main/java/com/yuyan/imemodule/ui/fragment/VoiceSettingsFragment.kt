package com.yuyan.imemodule.ui.fragment

import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.yuyan.imemodule.R
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.ui.fragment.base.ManagedPreferenceFragment
import com.yuyan.imemodule.voice.AsrModelManager

class VoiceSettingsFragment : ManagedPreferenceFragment(AppPrefs.getInstance().voice) {

    private var modelStatusPreference: Preference? = null

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
                if (AsrModelManager.isModelFilesPresent(context)) {
                    Toast.makeText(context, "X-ASR 离线语音识别模型已在本地就绪", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "正在初始化解压 X-ASR 133.9MB 离线模型...", Toast.LENGTH_SHORT).show()
                    AsrModelManager.initModel(context)
                    updateModelStatus()
                }
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
