package com.yuyan.imemodule.adapter

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.data.theme.ThemeManager.activeTheme
import com.yuyan.imemodule.database.entry.SideSymbol
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.editText
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent

class PrefixSettingsAdapter(private val mDatas: MutableList<SideSymbol>, @Suppress("UNUSED_PARAMETER") type: String = "pinyin") : RecyclerView.Adapter<PrefixSettingsAdapter.PrefixSettingsHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrefixSettingsHolder {
        val content = LinearLayout(Launcher.instance.context).apply {
            setPadding(0, dp(5), 0, dp(5))
            add(editText {
                gravity = Gravity.CENTER
                setTextColor(activeTheme.keyTextColor)
                id = R.id.et_prefix_setting_key
            }, lParams(width = 0, weight = 1f, height = matchParent) {
                setMargins(dp(20), 0, dp(20), 0)
            })
            add(editText {
                gravity = Gravity.CENTER
                setTextColor(activeTheme.keyTextColor)
                id = R.id.et_prefix_setting_value
            }, lParams(width = 0, weight = 2f))

            add(ImageView(context).apply {
                setImageResource(R.drawable.ic_menu_menu)
                drawable.setTint(activeTheme.keyTextColor)
            }, lParams(width = 0, weight = 1f, gravity = Gravity.CENTER))
        }
        return PrefixSettingsHolder(content)
    }

    override fun onBindViewHolder(holder: PrefixSettingsHolder, position: Int) {
        holder.keyWatcher?.let { holder.etPrefixKey.removeTextChangedListener(it) }
        holder.valueWatcher?.let { holder.etPrefixValue.removeTextChangedListener(it) }

        if (position in mDatas.indices) {
            holder.etPrefixKey.setText(mDatas[position].symbolKey)
            holder.etPrefixValue.setText(mDatas[position].symbolValue)
        }

        val keyWatcher = holder.etPrefixKey.doOnTextChanged { s, _, _, _ ->
            val key = s.toString()
            val bindPos = holder.bindingAdapterPosition
            if (bindPos in mDatas.indices) {
                mDatas[bindPos].symbolKey = key
            }
        }
        val valueWatcher = holder.etPrefixValue.doOnTextChanged { s, _, _, _ ->
            val value = s.toString()
            val bindPos = holder.bindingAdapterPosition
            if (bindPos in mDatas.indices) {
                mDatas[bindPos].symbolValue = value
            }
        }
        holder.keyWatcher = keyWatcher
        holder.valueWatcher = valueWatcher
    }

    override fun getItemCount(): Int {
        return mDatas.size
    }

    class PrefixSettingsHolder(view: View) : RecyclerView.ViewHolder(view) {
        var etPrefixKey: EditText
        var etPrefixValue: EditText
        var keyWatcher: android.text.TextWatcher? = null
        var valueWatcher: android.text.TextWatcher? = null

        init {
            view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            etPrefixKey = view.findViewById(R.id.et_prefix_setting_key)
            etPrefixValue = view.findViewById(R.id.et_prefix_setting_value)
        }
    }
}