package com.yuyan.imemodule.ui.fragment.theme

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeArchiveUtils
import com.yuyan.imemodule.data.theme.ThemeFilesManager
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.view.widget.applyNavBarInsetsBottomPadding
import kotlinx.coroutines.launch
import splitties.dimensions.dp
import splitties.resources.styledDrawable

class ThemeListFragment : Fragment() {

    private lateinit var imageLauncher: ActivityResultLauncher<Theme.Custom?>
    private lateinit var importYusLauncher: ActivityResultLauncher<String>
    private lateinit var exportYusLauncher: ActivityResultLauncher<String>
    private var themeToExport: Theme.Custom? = null

    private lateinit var themeListAdapter: ThemeListAdapter

    private var followSystemDayNightTheme by ThemeManager.prefs.followSystemDayNightTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageLauncher = registerForActivityResult(CustomThemeActivity.Contract()) { result ->
            if (result == null) return@registerForActivityResult
            when (result) {
                is CustomThemeActivity.BackgroundResult.Created -> {
                    val theme = result.theme
                    themeListAdapter.prependTheme(theme)
                    ThemeManager.saveTheme(theme)
                    if (!followSystemDayNightTheme) {
                        ThemeManager.setNormalModeTheme(theme)
                    }
                }
                is CustomThemeActivity.BackgroundResult.Deleted -> {
                    val name = result.name
                    themeListAdapter.removeTheme(name)
                    ThemeManager.deleteTheme(name)
                }
                is CustomThemeActivity.BackgroundResult.Updated -> {
                    val theme = result.theme
                    ThemeManager.saveTheme(theme)
                    ThemeManager.refreshThemes()
                    themeListAdapter.setThemes(ThemeManager.getAllThemes())
                    updateSelectedThemes()
                }
            }
        }

        importYusLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                var successCount = 0
                var failCount = 0
                var lastImportedTheme: Theme.Custom? = null

                for (uri in uris) {
                    val imported = ThemeArchiveUtils.importThemeFromYus(requireContext(), uri)
                    if (imported != null) {
                        successCount++
                        lastImportedTheme = imported
                    } else {
                        failCount++
                    }
                }

                if (successCount > 0) {
                    ThemeManager.refreshThemes()
                    themeListAdapter.setThemes(ThemeManager.getAllThemes())
                    updateSelectedThemes()
                    if (successCount == 1 && lastImportedTheme != null) {
                        android.widget.Toast.makeText(requireContext(), "已导入主题: ${lastImportedTheme.name}", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(requireContext(), "成功导入 $successCount 个主题", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else if (failCount > 0) {
                    android.widget.Toast.makeText(requireContext(), "主题导入失败", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        exportYusLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
            val theme = themeToExport
            if ((uri != null) && (theme != null)) {
                try {
                    ThemeArchiveUtils.exportThemeToYus(requireContext(), theme, uri)

                    // 检查并自动纠正系统文件选择器追加的 "123.yus (1)" 为规范的 "123(1).yus"
                    val docFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(requireContext(), uri)
                    val currentName = docFile?.name ?: ""
                    val regex = Regex("""^(.*)\.yus\s*\((.*)\)$""", RegexOption.IGNORE_CASE)
                    val match = regex.find(currentName)
                    if (match != null) {
                        val base = match.groupValues[1]
                        val num = match.groupValues[2]
                        val correctedName = "$base($num).yus"
                        try {
                            android.provider.DocumentsContract.renameDocument(requireContext().contentResolver, uri, correctedName)
                        } catch (_: Exception) {}
                    }

                    android.widget.Toast.makeText(requireContext(), "主题导出成功", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(requireContext(), "主题导出失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            themeToExport = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        themeListAdapter = object : ThemeListAdapter() {
            override fun onAddNewTheme() = addTheme()
            override fun onSelectTheme(theme: Theme) = selectTheme(theme)
            override fun onEditTheme(theme: Theme.Custom) = editTheme(theme)
            override fun onExportTheme(theme: Theme.Custom) = exportTheme(theme)
        }
        ThemeManager.refreshThemes()
        themeListAdapter.setThemes(ThemeManager.getAllThemes())
        updateSelectedThemes()
        return ResponsiveThemeListView(requireContext()).apply {
            adapter = themeListAdapter
            applyNavBarInsetsBottomPadding()
        }
    }

    private fun updateSelectedThemes(activeTheme: Theme? = null) {
        val active = activeTheme ?: ThemeManager.activeTheme
        var light: Theme? = null
        var dark: Theme? = null
        if (followSystemDayNightTheme) {
            light = ThemeManager.prefs.lightModeTheme.getValue()
            dark = ThemeManager.prefs.darkModeTheme.getValue()
        }
        themeListAdapter.setSelectedThemes(active, light, dark)
    }

    private fun addTheme() {
        val ctx = requireContext()
        val actions = arrayOf(
            "📸  " + getString(R.string.choose_image),
            "📋  " + getString(R.string.duplicate_builtin_theme),
            "📦  导入皮肤 (.yus)"
        )
        val dialog = AlertDialog.Builder(ctx)
            .setTitle(R.string.new_theme)
            .setItems(actions) { _, i ->
                when (i) {
                    0 -> imageLauncher.launch(null)
                    1 -> {
                        val view = ResponsiveThemeListView(ctx).apply {
                            setPadding(dp(16), dp(8), dp(16), dp(16))
                            clipToPadding = false
                        }
                        val dupDialog = AlertDialog.Builder(ctx)
                            .setTitle(getString(R.string.duplicate_builtin_theme).removeSuffix("…"))
                            .setView(view)
                            .create()
                        view.adapter = object :
                            SimpleThemeListAdapter<Theme>(ThemeManager.getAllThemes()) {
                            override fun onClick(theme: Theme) {
                                val newTheme = when (theme) {
                                    is Theme.Builtin -> theme.deriveCustomNoBackground(ThemeFilesManager.generateNextThemeName())
                                    is Theme.Custom -> theme.copy(name = ThemeFilesManager.generateNextThemeName())
                                }
                                themeListAdapter.prependTheme(newTheme)
                                ThemeManager.saveTheme(newTheme)
                                dupDialog.dismiss()
                            }
                        }
                        com.yuyan.imemodule.utils.DialogStyleUtils.styleDialog(dupDialog, ctx)
                        dupDialog.show()
                    }
                    2 -> importYusLauncher.launch("*/*")
                }
            }
            .create()
        com.yuyan.imemodule.utils.DialogStyleUtils.styleDialog(dialog, ctx)
        dialog.show()
    }

    private fun selectTheme(theme: Theme) {
        if (followSystemDayNightTheme) {
            val ctx = requireContext()
            val dialog = AlertDialog.Builder(ctx)
                .setIcon(ctx.styledDrawable(android.R.attr.alertDialogIcon))
                .setTitle(R.string.configure)
                .setMessage(R.string.theme_message_follow_system_day_night_mode_enabled)
                .setPositiveButton(R.string.disable_it) { _, _ ->
                    followSystemDayNightTheme = false
                    lifecycleScope.launch {
                        ThemeManager.setNormalModeTheme(theme)
                        updateSelectedThemes()
                    }
                }
                .create()
            com.yuyan.imemodule.utils.DialogStyleUtils.styleDialog(dialog, ctx)
            dialog.show()
            return
        }
        ThemeManager.setNormalModeTheme(theme)
        updateSelectedThemes()
    }

    private fun editTheme(theme: Theme.Custom) {
        imageLauncher.launch(theme)
    }

    private fun exportTheme(theme: Theme.Custom) {
        themeToExport = theme
        val baseName = theme.name.removeSuffix(".yus").removeSuffix(".json")
        exportYusLauncher.launch("$baseName.yus")
    }

}
