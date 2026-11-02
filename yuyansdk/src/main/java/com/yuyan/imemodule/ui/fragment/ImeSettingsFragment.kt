package com.yuyan.imemodule.ui.fragment

import android.content.Intent
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.yuyan.imemodule.R
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.utils.addCategory
import com.yuyan.imemodule.utils.addPreference

class ImeSettingsFragment : PreferenceFragmentCompat() {

    private fun PreferenceGroup.addDestinationPreference(
        @StringRes title: Int,
        summary: String? = null,
        @DrawableRes icon: Int,
        @IdRes destination: Int
    ) {
        if (summary != null) {
            addPreference(title, summaryStr = summary, icon = icon) {
                findNavController().navigate(destination)
            }
        } else {
            addPreference(title, icon = icon) {
                findNavController().navigate(destination)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
            addCategory(R.string.input_methods) {
                isIconSpaceReserved = false

                addDestinationPreference(
                    R.string.enable_ime,
                    "切换与启用语燕输入法",
                    R.drawable.ic_menu_language,
                    R.id.action_settingsFragment_to_setupActivity
                )
                addDestinationPreference(
                    R.string.setting_ime_input,
                    "拼音、双拼方案与出词习惯",
                    R.drawable.ic_menu_language,
                    R.id.action_settingsFragment_to_inputSettingsFragment
                )
                addDestinationPreference(
                    R.string.ime_settings_handwriting,
                    "手写识别与笔画轨迹设置",
                    R.drawable.ic_menu_handwriting,
                    R.id.action_settingsFragment_to_handwritingSettingsFragment
                )
            }
            addCategory(R.string.keyboard) {
                isIconSpaceReserved = false
                addDestinationPreference(
                    R.string.theme,
                    "皮肤壁纸、按键配色与外观定制",
                    R.drawable.ic_menu_theme,
                    R.id.action_settingsFragment_to_themeSettingsFragment
                )
                addDestinationPreference(
                    R.string.keyboard_feedback,
                    "按键音效和振动反馈",
                    R.drawable.ic_menu_touch,
                    R.id.action_settingsFragment_to_keyboardFeedbackFragment
                )
                addDestinationPreference(
                    R.string.setting_ime_keyboard,
                    "键盘布局、按键高度与划动设置",
                    R.drawable.ic_menu_keyboard,
                    R.id.action_settingsFragment_to_keyboardSettingFragment
                )
                addDestinationPreference(
                    R.string.clipboard,
                    "剪贴板历史与快捷常用语",
                    R.drawable.ic_menu_clipboard,
                    R.id.action_settingsFragment_to_clipboardSettingsFragment
                )

                addDestinationPreference(
                    R.string.full_display_keyboard,
                    "底部边距与全面屏适配",
                    R.drawable.ic_menu_keyboard_full,
                    R.id.action_settingsFragment_to_fullDisplayKeyboardFragment
                )
            }
            addCategory(R.string.advanced) {
                isIconSpaceReserved = false
                addDestinationPreference(
                    R.string.setting_ime_other,
                    "备份恢复、词库导出与高级参数",
                    R.drawable.ic_menu_more_horiz,
                    R.id.action_settingsFragment_to_otherSettingsFragment
                )
                addPreference(
                    R.string.feedback,
                    "反馈建议与社区互动",
                    R.drawable.ic_menu_edit
                ) {
                    startActivity(Intent(Intent.ACTION_VIEW, CustomConstant.FEEDBACK_TXC_REPO.toUri()))
                }
                addDestinationPreference(
                    R.string.about,
                    "版本信息、开源协议与团队",
                    R.drawable.ic_menu_feedback,
                    R.id.action_settingsFragment_to_aboutFragment
                )
            }
            com.yuyan.imemodule.utils.XimePreferenceStyler.stylePreferenceScreen(this)
        }
    }
}
