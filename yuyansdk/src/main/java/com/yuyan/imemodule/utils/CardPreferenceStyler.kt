package com.yuyan.imemodule.utils

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import com.yuyan.imemodule.R
import com.yuyan.imemodule.view.preference.DialogSeekBarPreference
import com.yuyan.imemodule.view.preference.EditTextIntPreference
import com.yuyan.imemodule.view.preference.ImeSwitchPreference
import com.yuyan.imemodule.view.preference.TwinSeekBarPreference

object CardPreferenceStyler {

    fun stylePreferenceScreen(screen: PreferenceScreen) {
        for (i in 0 until screen.preferenceCount) {
            val p = screen.getPreference(i)
            if (p is PreferenceCategory) {
                p.layoutResource = R.layout.sdk_item_category_card
                styleGroup(p)
            } else if (p is PreferenceGroup) {
                styleGroup(p)
            } else {
                styleSingleItem(p, isFirst = true, isLast = true)
            }
        }
    }

    fun styleGroup(group: PreferenceGroup) {
        val visibleItems = mutableListOf<Preference>()
        for (i in 0 until group.preferenceCount) {
            val item = group.getPreference(i)
            if (item.isVisible) {
                visibleItems.add(item)
            }
        }
        val count = visibleItems.size
        for (i in 0 until count) {
            val pref = visibleItems[i]
            val isFirst = (i == 0)
            val isLast = (i == count - 1)
            styleSingleItem(pref, isFirst, isLast)
        }
    }

    private fun styleSingleItem(pref: Preference, isFirst: Boolean, isLast: Boolean) {
        if (pref is ICardPreferenceItem) {
            pref.isFirstInGroup = isFirst
            pref.isLastInGroup = isLast
        }
    }
}

interface ICardPreferenceItem {
    var isFirstInGroup: Boolean
    var isLastInGroup: Boolean

    fun bindCardViewHolder(holder: PreferenceViewHolder, pref: Preference) {
        val context = holder.itemView.context
        val cardRoot = holder.findViewById(R.id.card_item_root)
        val cardDivider = holder.findViewById(R.id.card_divider)
        val iconBox = holder.findViewById(R.id.icon_box)
        val iconView = holder.findViewById(android.R.id.icon) as? ImageView
        val arrowRight = holder.findViewById(R.id.arrow_right) as? ImageView

        if (cardRoot != null) {
            val bgRes = when {
                isFirstInGroup && isLastInGroup -> R.drawable.sdk_bg_settings_card_single
                isFirstInGroup -> R.drawable.sdk_bg_settings_card_top
                isLastInGroup -> R.drawable.sdk_bg_settings_card_bottom
                else -> R.drawable.sdk_bg_settings_card_middle
            }
            cardRoot.setBackgroundResource(bgRes)
        }

        if (cardDivider != null) {
            cardDivider.isVisible = !isLastInGroup
        }

        if (iconBox != null) {
            if (pref.icon != null) {
                iconBox.isVisible = true
                iconView?.setImageDrawable(pref.icon)
                iconView?.setColorFilter(ContextCompat.getColor(context, R.color.settings_icon_tint))
            } else {
                iconBox.isVisible = false
            }
        }

        if (arrowRight != null) {
            val widgetFrame = holder.findViewById(android.R.id.widget_frame) as? ViewGroup
            val hasWidget = widgetFrame != null && widgetFrame.isVisible && widgetFrame.childCount > 0
            val isAction = (pref.intent != null || pref.onPreferenceClickListener != null || pref.fragment != null)
            arrowRight.isVisible = isAction && !hasWidget
        }
    }
}

open class CardPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        layoutResource = R.layout.sdk_item_preference_card
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }
}

open class CardSwitchPreference(context: Context) : ImeSwitchPreference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        layoutResource = R.layout.sdk_item_preference_card
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }
}

open class CardListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ListPreference(context, attrs, defStyleAttr), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        layoutResource = R.layout.sdk_item_preference_card
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }
}

open class CardEditTextIntPreference(context: Context) : EditTextIntPreference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        layoutResource = R.layout.sdk_item_preference_card
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }
}

open class CardDialogSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DialogSeekBarPreference(context, attrs, defStyleAttr), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        layoutResource = R.layout.sdk_item_preference_card
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }
}

open class CardTwinSeekBarPreference(context: Context) : TwinSeekBarPreference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        layoutResource = R.layout.sdk_item_preference_card
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }
}
