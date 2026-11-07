package com.yuyan.imemodule.ui.fragment

import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import com.yuyan.imemodule.R
import com.yuyan.imemodule.speech.AsrModelManager
import com.yuyan.imemodule.speech.AsrSupport
import com.yuyan.imemodule.utils.addCategory
import com.yuyan.imemodule.utils.addPreference
import com.yuyan.imemodule.utils.addSwitchPreference

class VoiceSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {

            addCategory("离线语音引擎") {
                isIconSpaceReserved = false

                val modelManager = AsrModelManager(requireContext())
                val selectedInfo = modelManager.getSelectedModelInfo()
                val isInstalled = selectedInfo != null

                addPreference(
                    "语音识别模型",
                    if (isInstalled) "当前使用模型：${selectedInfo?.name}" else "未下载离线语音模型（需要下载模型）",
                    R.drawable.ic_menu_language
                ) {
                    Toast.makeText(requireContext(), if (isInstalled) "模型已就绪：${selectedInfo?.name}" else "请下载或导入 Zipformer 离线语音模型", Toast.LENGTH_SHORT).show()
                }

                addSwitchPreference(
                    "离线模型常驻内存",
                    "保持语音引擎常驻，消除首次点击按住说话时的 1 秒模型加载延时",
                    "asr_keep_engine_alive",
                    false,
                    R.drawable.ic_menu_touch
                ) { isChecked ->
                    if (isChecked) {
                        AsrSupport.warmup(requireContext())
                    } else {
                        AsrSupport.releaseModel()
                    }
                }
            }

            addCategory("使用说明与建议") {
                isIconSpaceReserved = false

                addPreference(
                    "长按说话使用说明",
                    "在语音键盘中长按【按住说话】按钮开始录音，说话完毕松开即可自动识别并上屏；顶部框支持左右滑动快速控制光标。",
                    R.drawable.ic_menu_feedback
                )
            }

            com.yuyan.imemodule.utils.CardPreferenceStyler.stylePreferenceScreen(this)
        }
    }
}
