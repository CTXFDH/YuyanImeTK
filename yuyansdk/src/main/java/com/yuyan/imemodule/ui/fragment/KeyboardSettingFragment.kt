package com.yuyan.imemodule.ui.fragment

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.CustomFontManager
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.ui.fragment.base.ManagedPreferenceFragment
import com.yuyan.imemodule.utils.CardPreferenceStyler
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.utils.addPreference
import com.yuyan.imemodule.utils.queryFileName

class KeyboardSettingFragment : ManagedPreferenceFragment(AppPrefs.getInstance().keyboardSetting) {

    private lateinit var fontImportLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fontImportLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            val ctx = requireContext()
            val fileName = ctx.contentResolver.queryFileName(uri) ?: "font.ttf"
            try {
                val inputStream = ctx.contentResolver.openInputStream(uri) ?: return@registerForActivityResult
                val success = CustomFontManager.importFontFromStream(ctx, inputStream, fileName)
                if (success) {
                    Toast.makeText(ctx, "字体导入成功！", Toast.LENGTH_SHORT).show()
                    refreshKeyboardFont()
                    updateFontPreferences()
                } else {
                    Toast.makeText(ctx, "字体文件格式不支持或已损坏", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPreferenceUiCreated(screen: PreferenceScreen) {
        super.onPreferenceUiCreated(screen)
        updateFontPreferences()
    }

    private fun updateFontPreferences() {
        val screen = preferenceScreen ?: return
        val targetCategory = (0 until screen.preferenceCount)
            .mapNotNull { screen.getPreference(it) as? PreferenceCategory }
            .firstOrNull()
            ?: screen

        val importPref = targetCategory.findPreference<Preference>("pref_import_font")
        if (importPref == null) {
            targetCategory.addPreference(
                "导入字体",
                summary = getFontSummary(),
                key = "pref_import_font",
                icon = R.drawable.ic_menu_custom
            ) {
                fontImportLauncher.launch("*/*")
            }
        } else {
            importPref.summary = getFontSummary()
        }

        val clearPref = targetCategory.findPreference<Preference>("pref_clear_font")
        if (CustomFontManager.hasCustomFont()) {
            if (clearPref == null) {
                targetCategory.addPreference(
                    "恢复默认字体",
                    summary = "清除已导入的自定义字体",
                    key = "pref_clear_font",
                    icon = R.drawable.ic_menu_delete
                ) {
                    CustomFontManager.clearCustomFont()
                    Toast.makeText(requireContext(), "已恢复默认字体", Toast.LENGTH_SHORT).show()
                    refreshKeyboardFont()
                    updateFontPreferences()
                }
            }
        } else {
            clearPref?.let { targetCategory.removePreference(it) }
        }

        CardPreferenceStyler.stylePreferenceScreen(screen)
    }

    private fun getFontSummary(): String {
        return if (CustomFontManager.hasCustomFont()) {
            "已导入自定义字体 (点击更换)"
        } else {
            "导入 TTF/OTF 字体文件自定义键盘文字"
        }
    }

    private fun refreshKeyboardFont() {
        EnvironmentSingleton.instance.initData()
        KeyboardLoaderUtil.instance.clearKeyboardMap()
        KeyboardManager.instance.clearKeyboard()
    }

    override fun onStop() {
        super.onStop()
        refreshKeyboardFont()
    }
}
