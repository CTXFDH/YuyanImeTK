package com.yuyan.imemodule.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import com.yuyan.imemodule.R
import com.yuyan.imemodule.speech.AsrModelManager
import com.yuyan.imemodule.speech.AsrSupport
import com.yuyan.imemodule.utils.addCategory
import com.yuyan.imemodule.utils.addPreference
import com.yuyan.imemodule.utils.addSwitchPreference
import com.yuyan.imemodule.view.widget.withLoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VoiceSettingsFragment : PreferenceFragmentCompat() {

    private lateinit var importModelLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        importModelLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            val ctx = requireContext()
            lifecycleScope.withLoadingDialog(ctx) {
                val ok = withContext(Dispatchers.IO) {
                    AsrModelManager(ctx).importModelFromArchive(uri)
                }
                withContext(Dispatchers.Main) {
                    if (ok) {
                        Toast.makeText(ctx, "离线语音识别模型导入并就绪成功！", Toast.LENGTH_LONG).show()
                        onCreatePreferences(null, null)
                    } else {
                        Toast.makeText(ctx, "模型导入失败，请检查 zip 文件中是否包含 onnx 模型文件", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {

            addCategory("离线语音引擎") {
                isIconSpaceReserved = false

                val modelManager = AsrModelManager(requireContext())
                val selectedInfo = modelManager.getSelectedModelInfo()
                val isInstalled = selectedInfo != null

                addPreference(
                    "语音识别模型",
                    if (isInstalled) "当前使用模型：${selectedInfo.name}" else "未导入离线语音模型（点击下方链接下载与导入）",
                    R.drawable.ic_menu_language
                ) {
                    Toast.makeText(requireContext(), if (isInstalled) "模型已就绪：${selectedInfo.name}" else "请导入 Zipformer 离线语音模型包 (.zip)", Toast.LENGTH_SHORT).show()
                }

                addPreference(
                    "下载离线语音模型 (GitHub)",
                    "跳转至 sherpa-onnx 开源模型发布页下载解压包：\nhttps://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models",
                    R.drawable.ic_jump_arrow
                ) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models"))
                        startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), "无法打开浏览器，请直接访问：https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models", Toast.LENGTH_LONG).show()
                    }
                }

                addPreference(
                    "导入离线语音模型 (.zip)",
                    "选择并解压导入本地下载的 Sherpa-onnx Zipformer 语音模型包",
                    R.drawable.ic_menu_edit
                ) {
                    importModelLauncher.launch("application/zip")
                }

                addSwitchPreference(
                    "离线模型常驻内存",
                    "保持语音引擎常驻，消除首次点击按住说话时的模型加载延时",
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
