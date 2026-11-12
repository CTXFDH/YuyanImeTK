package com.yuyan.imemodule.ui.fragment.theme

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.yuyan.imemodule.data.theme.CustomFontManager
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.ui.fragment.base.ManagedPreferenceFragment
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.utils.queryFileName

class ThemeSettingsFragment : ManagedPreferenceFragment(ThemeManager.prefs) {

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
                    updateFontPreferencesSummary()
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
        setupFontPreferenceListeners()
        updateFontPreferencesSummary()
    }

    override fun onResume() {
        super.onResume()
        setupFontPreferenceListeners()
        updateFontPreferencesSummary()
    }

    private fun setupFontPreferenceListeners() {
        findPreference<Preference>("pref_import_font")?.setOnPreferenceClickListener {
            fontImportLauncher.launch("*/*")
            true
        }
        findPreference<Preference>("pref_clear_font")?.setOnPreferenceClickListener {
            CustomFontManager.clearCustomFont()
            Toast.makeText(requireContext(), "已恢复默认字体", Toast.LENGTH_SHORT).show()
            refreshKeyboardFont()
            updateFontPreferencesSummary()
            true
        }
    }

    private fun updateFontPreferencesSummary() {
        val importPref = findPreference<Preference>("pref_import_font")
        if (importPref != null) {
            importPref.summary = getFontSummary()
        }
        val clearPref = findPreference<Preference>("pref_clear_font")
        if (clearPref != null) {
            clearPref.isVisible = CustomFontManager.hasCustomFont()
        }
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
}
