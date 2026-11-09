package com.yuyan.imemodule.ui.fragment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import com.yuyan.imemodule.R
import com.yuyan.imemodule.utils.CardPreferenceStyler
import com.yuyan.imemodule.utils.addCategory
import com.yuyan.imemodule.utils.addPreference

class DictSettingsFragment : PreferenceFragmentCompat() {

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.title = "词库管理"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
            addCategory("词库管理") {
                isIconSpaceReserved = false

                addPreference(
                    "用户词库",
                    "查看与新增用户自造词及调整词频",
                    R.drawable.ic_menu_language,
                ) {
                    findNavController().navigate(R.id.action_dictSettingsFragment_to_userDictFragment)
                }

                addPreference(
                    "个性化短语",
                    "自定义快捷码与常用短语映射",
                    R.drawable.ic_menu_phrases,
                ) {
                    findNavController().navigate(R.id.action_dictSettingsFragment_to_phrasesSettingsFragment)
                }
            }

            CardPreferenceStyler.stylePreferenceScreen(this)
        }
    }
}
