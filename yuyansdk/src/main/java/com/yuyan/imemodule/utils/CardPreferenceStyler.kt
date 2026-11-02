package com.yuyan.imemodule.utils

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
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
import com.yuyan.imemodule.view.widget.seekbar.SignSeekBar

object CardPreferenceStyler {

    val expandedStates = mutableMapOf<String, Boolean>()

    fun stylePreferenceScreen(screen: PreferenceScreen) {
        val currentGroup = mutableListOf<Preference>()

        fun styleCurrentGroup() {
            if (currentGroup.isEmpty()) return
            val count = currentGroup.size
            for (i in 0 until count) {
                val pref = currentGroup[i]
                val isFirst = (i == 0)
                val isLast = (i == count - 1)
                styleSingleItem(pref, isFirst, isLast)
            }
            currentGroup.clear()
        }

        for (i in 0 until screen.preferenceCount) {
            val p = screen.getPreference(i)
            if (p is PreferenceCategory) {
                styleCurrentGroup()
                p.layoutResource = R.layout.sdk_item_category_card
                styleGroup(p)
            } else if (p is PreferenceGroup) {
                styleCurrentGroup()
                styleGroup(p)
            } else {
                if (p.isVisible) {
                    currentGroup.add(p)
                }
            }
        }
        styleCurrentGroup()
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
            if (pref !is TestInputBoxPreference) {
                pref.layoutResource = when {
                    isFirst && isLast -> R.layout.sdk_item_preference_card_single
                    isFirst -> R.layout.sdk_item_preference_card_top
                    isLast -> R.layout.sdk_item_preference_card_bottom
                    else -> R.layout.sdk_item_preference_card_middle
                }
            }
        }
    }
}

interface ICardPreferenceItem {
    var isFirstInGroup: Boolean
    var isLastInGroup: Boolean

    fun bindCardViewHolder(holder: PreferenceViewHolder, pref: Preference) {
        val context = holder.itemView.context
        val cardRoot = holder.findViewById(R.id.card_item_root)
        val cardContent = holder.findViewById(R.id.card_item_content)
        val iconBox = holder.findViewById(R.id.icon_box)
        val iconView = holder.findViewById(android.R.id.icon) as? ImageView
        val arrowRight = holder.findViewById(R.id.arrow_right) as? ImageView

        val group = pref.parent
        val (isFirst, isLast) = if (group != null) {
            val visibleItems = (0 until group.preferenceCount)
                .map { group.getPreference(it) }
                .filter { it !is PreferenceCategory && it.isVisible }
            val index = visibleItems.indexOf(pref)
            if (index >= 0) {
                Pair(index == 0, index == visibleItems.size - 1)
            } else {
                Pair(isFirstInGroup, isLastInGroup)
            }
        } else {
            Pair(isFirstInGroup, isLastInGroup)
        }

        if (cardRoot != null) {
            val bgRes = when {
                isFirst && isLast -> R.drawable.sdk_bg_settings_card_single
                isFirst -> R.drawable.sdk_bg_settings_card_top
                isLast -> R.drawable.sdk_bg_settings_card_bottom
                else -> R.drawable.sdk_bg_settings_card_middle
            }
            cardRoot.setBackgroundResource(bgRes)
            cardRoot.clipToOutline = true
            cardRoot.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND

            if (cardContent != null) {
                cardContent.clipToOutline = true
                cardContent.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            }
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
            arrowRight.setImageResource(R.drawable.ic_arrow_right_pretty)
            arrowRight.setColorFilter(android.graphics.Color.parseColor("#9CA3AF"))
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
        layoutResource = R.layout.sdk_item_preference_card_single
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
        layoutResource = R.layout.sdk_item_preference_card_single
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

    private val prefKey: String get() = key ?: title?.toString() ?: ""

    var isExpanded: Boolean
        get() = CardPreferenceStyler.expandedStates[prefKey] ?: false
        set(value) { CardPreferenceStyler.expandedStates[prefKey] = value }

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onClick() {
        isExpanded = !isExpanded
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)

        val inlineContainer = holder.findViewById(R.id.inline_expand_container) as? android.widget.LinearLayout
        val arrowRight = holder.findViewById(R.id.arrow_right) as? ImageView

        if (arrowRight != null) {
            arrowRight.isVisible = true
            arrowRight.animate().rotation(if (isExpanded) 90f else 0f).setDuration(150).start()
        }

        if (inlineContainer != null) {
            inlineContainer.removeAllViews()
            if (isExpanded && entries != null && entryValues != null) {
                inlineContainer.isVisible = true
                val currentVal = value
                entries.forEachIndexed { index, entryText ->
                    val entryVal = entryValues[index].toString()
                    val isSelected = (entryVal == currentVal)

                    val itemTv = android.widget.TextView(context).apply {
                        text = if (isSelected) "✓  $entryText" else "    $entryText"
                        textSize = 14f
                        val textColor = if (isSelected) {
                            ContextCompat.getColor(context, R.color.settings_icon_tint)
                        } else {
                            ContextCompat.getColor(context, R.color.settings_title_text)
                        }
                        setTextColor(textColor)
                        typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                        setPadding(0, 20, 0, 20)
                        isClickable = true
                        isFocusable = true
                        val typedVal = android.util.TypedValue()
                        if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedVal, true) && typedVal.resourceId != 0) {
                            background = ContextCompat.getDrawable(context, typedVal.resourceId)
                        }
                        setOnClickListener {
                            if (callChangeListener(entryVal)) {
                                value = entryVal
                            }
                            isExpanded = false
                            notifyChanged()
                        }
                    }
                    inlineContainer.addView(itemTv)
                }
            } else {
                inlineContainer.isVisible = false
            }
        }
    }
}

open class CardEditTextIntPreference(context: Context) : EditTextIntPreference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
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

    private val prefKey: String get() = key ?: title?.toString() ?: ""

    var isExpanded: Boolean
        get() = CardPreferenceStyler.expandedStates[prefKey] ?: false
        set(value) { CardPreferenceStyler.expandedStates[prefKey] = value }

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onClick() {
        isExpanded = !isExpanded
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)

        val inlineContainer = holder.findViewById(R.id.inline_expand_container) as? android.widget.LinearLayout
        val arrowRight = holder.findViewById(R.id.arrow_right) as? ImageView
        val summaryTv = holder.findViewById(android.R.id.summary) as? android.widget.TextView

        if (arrowRight != null) {
            arrowRight.isVisible = true
            arrowRight.animate().rotation(if (isExpanded) 90f else 0f).setDuration(150).start()
        }

        if (inlineContainer != null) {
            inlineContainer.removeAllViews()
            if (isExpanded) {
                inlineContainer.isVisible = true

                val rowLayout = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 12, 0, 8)
                }

                val accentColor = ContextCompat.getColor(context, R.color.settings_icon_tint)
                val iconBoxBgColor = ContextCompat.getColor(context, R.color.settings_icon_box_bg)

                val btnDefault = android.widget.TextView(context).apply {
                    text = "默认"
                    textSize = 13f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(accentColor)
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 16, 32, 16)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 32f
                        setColor(iconBoxBgColor)
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        default?.let { defVal ->
                            if (callChangeListener(defVal)) {
                                persistInt(defVal)
                                isExpanded = false
                                notifyChanged()
                            }
                        }
                    }
                }

                val currentVal = getPersistedInt(min)
                val countStep = (max - min).coerceAtLeast(1)
                val seekBar = SignSeekBar(context).apply {
                    getConfigBuilder()
                        .min(min.toFloat())
                        .max(max.toFloat())
                        .progress(currentVal.toFloat())
                        .sectionCount(countStep)
                        .trackSize(2)
                        .secondTrackSize(2)
                        .trackColor(ContextCompat.getColor(context, R.color.settings_divider))
                        .secondTrackColor(accentColor)
                        .thumbColor(accentColor)
                        .thumbRadius(8)
                        .thumbRadiusOnDragging(10)
                        .showThumbShadow(false)
                        .touchToSeek()
                        .build()

                    setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
                        override fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                            if (fromUser) {
                                summaryTv?.text = "$progress $unit"
                                persistInt(progress)
                                callChangeListener(progress)
                            }
                        }
                        override fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float) {
                            persistInt(progress)
                            callChangeListener(progress)
                        }
                        override fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
                    })
                }

                rowLayout.addView(btnDefault, android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = 16
                })

                rowLayout.addView(seekBar, android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ))

                inlineContainer.addView(rowLayout)
            } else {
                inlineContainer.isVisible = false
            }
        }
    }
}

open class CardTwinSeekBarPreference(context: Context) : TwinSeekBarPreference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = false

    private val prefKey: String get() = key ?: title?.toString() ?: ""

    var isExpanded: Boolean
        get() = CardPreferenceStyler.expandedStates[prefKey] ?: false
        set(value) { CardPreferenceStyler.expandedStates[prefKey] = value }

    init {
        layoutResource = R.layout.sdk_item_preference_card_single
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onClick() {
        isExpanded = !isExpanded
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)

        val inlineContainer = holder.findViewById(R.id.inline_expand_container) as? android.widget.LinearLayout
        val arrowRight = holder.findViewById(R.id.arrow_right) as? ImageView
        val summaryTv = holder.findViewById(android.R.id.summary) as? android.widget.TextView

        if (arrowRight != null) {
            arrowRight.isVisible = true
            arrowRight.animate().rotation(if (isExpanded) 90f else 0f).setDuration(150).start()
        }

        if (inlineContainer != null) {
            inlineContainer.removeAllViews()
            if (isExpanded) {
                inlineContainer.isVisible = true

                val accentColor = ContextCompat.getColor(context, R.color.settings_icon_tint)
                val iconBoxBgColor = ContextCompat.getColor(context, R.color.settings_icon_box_bg)

                val countStep = (max - min).coerceAtLeast(1)

                // 1. Primary SeekBar Row
                val rowLayout1 = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 12, 0, 8)
                }

                val btnDefault1 = android.widget.TextView(context).apply {
                    text = label.ifEmpty { "默认" }
                    textSize = 13f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(accentColor)
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 16, 32, 16)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 32f
                        setColor(iconBoxBgColor)
                    }
                }

                val seekBar1 = SignSeekBar(context).apply {
                    getConfigBuilder()
                        .min(min.toFloat())
                        .max(max.toFloat())
                        .progress(value.toFloat())
                        .sectionCount(countStep)
                        .trackSize(2)
                        .secondTrackSize(2)
                        .trackColor(ContextCompat.getColor(context, R.color.settings_divider))
                        .secondTrackColor(accentColor)
                        .thumbColor(accentColor)
                        .thumbRadius(8)
                        .thumbRadiusOnDragging(10)
                        .showThumbShadow(false)
                        .touchToSeek()
                        .build()

                    setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
                        override fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                            if (fromUser) {
                                preferenceDataStore?.putInt(key, progress) ?: sharedPreferences?.edit { putInt(key, progress) }
                                summaryTv?.text = "$progress $unit / $secondaryValue $unit"
                                callChangeListener(progress to secondaryValue)
                            }
                        }
                        override fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float) {}
                        override fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
                    })
                }

                rowLayout1.addView(btnDefault1, android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = 16
                })
                rowLayout1.addView(seekBar1, android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ))

                // 2. Secondary SeekBar Row
                val rowLayout2 = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 8, 0, 12)
                }

                val btnDefault2 = android.widget.TextView(context).apply {
                    text = secondaryLabel.ifEmpty { "横屏" }
                    textSize = 13f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(accentColor)
                    gravity = android.view.Gravity.CENTER
                    setPadding(32, 16, 32, 16)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 32f
                        setColor(iconBoxBgColor)
                    }
                }

                val seekBar2 = SignSeekBar(context).apply {
                    getConfigBuilder()
                        .min(min.toFloat())
                        .max(max.toFloat())
                        .progress(secondaryValue.toFloat())
                        .sectionCount(countStep)
                        .trackSize(2)
                        .secondTrackSize(2)
                        .trackColor(ContextCompat.getColor(context, R.color.settings_divider))
                        .secondTrackColor(accentColor)
                        .thumbColor(accentColor)
                        .thumbRadius(8)
                        .thumbRadiusOnDragging(10)
                        .showThumbShadow(false)
                        .touchToSeek()
                        .build()

                    setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
                        override fun onProgressChanged(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                            if (fromUser) {
                                preferenceDataStore?.putInt(secondaryKey, progress) ?: sharedPreferences?.edit { putInt(secondaryKey, progress) }
                                summaryTv?.text = "$value $unit / $progress $unit"
                                callChangeListener(value to progress)
                            }
                        }
                        override fun getProgressOnActionUp(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float) {}
                        override fun getProgressOnFinally(signSeekBar: SignSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
                    })
                }

                rowLayout2.addView(btnDefault2, android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = 16
                })
                rowLayout2.addView(seekBar2, android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ))

                inlineContainer.addView(rowLayout1)
                inlineContainer.addView(rowLayout2)
            } else {
                inlineContainer.isVisible = false
            }
        }
    }
}

open class TestInputBoxPreference(context: Context) : Preference(context), ICardPreferenceItem {
    override var isFirstInGroup: Boolean = false
    override var isLastInGroup: Boolean = true

    init {
        layoutResource = R.layout.sdk_item_test_input_box
        isIconSpaceReserved = false
        isSingleLineTitle = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        bindCardViewHolder(holder, this)
    }
}
