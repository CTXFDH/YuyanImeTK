package com.yuyan.imemodule.ui.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.yuyan.imemodule.R
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.database.entry.UserWord
import com.yuyan.imemodule.libs.pinyin4j.PinyinHelper
import com.yuyan.imemodule.manager.UserWordManager
import com.yuyan.imemodule.utils.CardPreferenceStyler
import com.yuyan.imemodule.utils.DialogStyleUtils
import com.yuyan.imemodule.utils.addCategory
import com.yuyan.imemodule.utils.addPreference
import splitties.dimensions.dp

class UserDictFragment : PreferenceFragmentCompat() {

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.title = "用户词库"
        UserWordManager.importFromCustomPhraseFile()
        onCreatePreferences(null, null)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {

            // 1. 新增自造词入口
            addCategory("自造词管理") {
                isIconSpaceReserved = false

                addPreference(
                    "添加新自造词",
                    "只需输入词条，系统将自动识别拼音与键码",
                    R.drawable.ic_menu_plus
                ) {
                    showAddUserWordDialog()
                }

                val userWordsCheck = DataBaseKT.instance.userWordDao().getAll()
                if (userWordsCheck.isNotEmpty()) {
                    addPreference(
                        "批量多选删除自造词",
                        "勾选多个自造词进行批量删除",
                        R.drawable.ic_menu_setting
                    ) {
                        showBatchDeleteDialog()
                    }
                }
            }

            // 2. 已保存的用户自造词列表
            val userWords = DataBaseKT.instance.userWordDao().getAll()
            addCategory("已保存的用户自造词 (${userWords.size})") {
                isIconSpaceReserved = false

                if (userWords.isEmpty()) {
                    addPreference(
                        "暂无用户自造词",
                        "点击上方按钮添加，输入词条即可自动生成拼音与词频置顶",
                        R.drawable.ic_menu_language
                    )
                } else {
                    for (uw in userWords) {
                        val label = "拼音: ${uw.pinyin}   |   词频: ${uw.frequency}"
                        addPreference(
                            uw.word,
                            label,
                            R.drawable.ic_menu_language
                        ) {
                            showEditOrDeleteDialog(uw)
                        }
                    }
                }
            }

            CardPreferenceStyler.stylePreferenceScreen(this)
        }
    }

    private fun showAddUserWordDialog(existingUserWord: UserWord? = null) {
        val context = requireContext()
        val isDarkUi = DialogStyleUtils.isDarkUiMode(context)

        val labelTextColor = if (isDarkUi) Color.parseColor("#E5E7EB") else Color.parseColor("#374151")
        val inputTextColor = if (isDarkUi) Color.WHITE else Color.parseColor("#111827")
        val inputHintColor = if (isDarkUi) Color.parseColor("#9CA3AF") else Color.parseColor("#6B7280")
        val inputBgColor = if (isDarkUi) Color.parseColor("#33353A") else Color.parseColor("#F3F4F6")
        val strokeColor = if (isDarkUi) 0x30FFFFFF.toInt() else Color.parseColor("#E5E7EB")

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }

        // 词条标签与输入框
        val tvWordLabel = TextView(context).apply {
            text = "自造词词条（支持汉字或词组）"
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(labelTextColor)
            setPadding(dp(4), dp(8), dp(4), dp(6))
        }

        val etWord = EditText(context).apply {
            hint = "如：语燕 或 人工智能"
            textSize = 15f
            setTextColor(inputTextColor)
            setHintTextColor(inputHintColor)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10f)
                setColor(inputBgColor)
                setStroke(dp(1), strokeColor)
            }
            existingUserWord?.let { setText(it.word) }
        }

        // 词频标签与输入框
        val tvFreqLabel = TextView(context).apply {
            text = "词频权重（数值越高出词优先级越高）"
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(labelTextColor)
            setPadding(dp(4), dp(14), dp(4), dp(6))
        }

        val etFreq = EditText(context).apply {
            hint = "词频（默认 100）"
            textSize = 15f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(inputTextColor)
            setHintTextColor(inputHintColor)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10f)
                setColor(inputBgColor)
                setStroke(dp(1), strokeColor)
            }
            setText(existingUserWord?.frequency?.toString() ?: "100")
        }

        rootLayout.addView(tvWordLabel)
        rootLayout.addView(etWord)
        rootLayout.addView(tvFreqLabel)
        rootLayout.addView(etFreq)

        val dialogTitle = if (existingUserWord != null) "编辑用户自造词" else "添加用户自造词"

        val dialog = AlertDialog.Builder(context)
            .setTitle(dialogTitle)
            .setView(rootLayout)
            .setPositiveButton("保存") { _, _ ->
                val word = etWord.text.toString().trim()
                val freqStr = etFreq.text.toString().trim()
                val freq = freqStr.toIntOrNull() ?: 100

                if (word.isBlank()) {
                    Toast.makeText(context, "词条不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val pinyin = UserWordManager.autoGeneratePinyin(word)
                val pinyinHead = runCatching { PinyinHelper.getPinYinHeadChar(word).lowercase() }.getOrDefault("")
                val t9Code = UserWordManager.wordToT9Key(pinyin)

                existingUserWord?.let {
                    DataBaseKT.instance.userWordDao().deleteById(it.id)
                }

                val userWord = UserWord(
                    word = word,
                    pinyin = pinyin,
                    pinyinHead = pinyinHead,
                    t9 = t9Code,
                    frequency = freq
                )

                DataBaseKT.instance.userWordDao().insert(userWord)
                UserWordManager.syncCustomPhraseFile()
                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                onCreatePreferences(null, null)
            }
            .create()

        DialogStyleUtils.styleDialog(dialog, context)
        dialog.show()
    }

    private fun showEditOrDeleteDialog(userWord: UserWord) {
        val options = arrayOf("✏️ 编辑词条", "🗑️ 删除词条")

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(userWord.word)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddUserWordDialog(userWord)
                    1 -> {
                        DataBaseKT.instance.userWordDao().deleteById(userWord.id)
                        UserWordManager.syncCustomPhraseFile()
                        Toast.makeText(requireContext(), "已删除自造词", Toast.LENGTH_SHORT).show()
                        onCreatePreferences(null, null)
                    }
                }
            }
            .create()

        DialogStyleUtils.styleDialog(dialog, requireContext())
        dialog.show()
    }

    private fun showBatchDeleteDialog() {
        val context = requireContext()
        val userWords = DataBaseKT.instance.userWordDao().getAll()
        if (userWords.isEmpty()) {
            Toast.makeText(context, "暂无自造词可删除", Toast.LENGTH_SHORT).show()
            return
        }

        val items = userWords.map { "${it.word}  (${it.pinyin})" }.toTypedArray()
        val checkedItems = BooleanArray(userWords.size) { false }

        val dialog = AlertDialog.Builder(context)
            .setTitle("批量多选删除自造词")
            .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("删除选中") { _, _ ->
                val dao = DataBaseKT.instance.userWordDao()
                var count = 0
                for (i in checkedItems.indices) {
                    if (checkedItems[i]) {
                        dao.deleteById(userWords[i].id)
                        count++
                    }
                }
                UserWordManager.syncCustomPhraseFile()
                Toast.makeText(context, "已成功删除 $count 个自造词", Toast.LENGTH_SHORT).show()
                onCreatePreferences(null, null)
            }
            .setNegativeButton("取消", null)
            .create()

        DialogStyleUtils.styleDialog(dialog, context)
        dialog.show()
    }
}
