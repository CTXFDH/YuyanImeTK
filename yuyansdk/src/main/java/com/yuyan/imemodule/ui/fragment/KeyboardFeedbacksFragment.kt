package com.yuyan.imemodule.ui.fragment

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.yuyan.imemodule.R
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.InputFeedbacks
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.view.widget.seekbar.SignSeekBar
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.dsl.appcompat.switch
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.before
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.constraintlayout.topToTopOf
import splitties.views.dsl.core.add
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalLayout
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.core.wrapInScrollView
import splitties.views.gravityCenter
import splitties.views.gravityVerticalCenter

class KeyboardFeedbacksFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = with(requireContext()) {

        val soundOnKeyPress by AppPrefs.getInstance().internal.soundOnKeyPress
        val vibrationAmplitude by AppPrefs.getInstance().internal.vibrationAmplitude

        val soundEffectStandardPref = AppPrefs.getInstance().internal.soundEffectStandard
        val soundEffectSpaceBarPref = AppPrefs.getInstance().internal.soundEffectSpaceBar
        val soundEffectDeletePref = AppPrefs.getInstance().internal.soundEffectDelete
        val soundEffectReturnPref = AppPrefs.getInstance().internal.soundEffectReturn

        val accentColor = styledColor(android.R.attr.colorAccent)
        val grayColor = ContextCompat.getColor(requireContext(), R.color.color_gray)
        val textSecondaryColor = styledColor(android.R.attr.textColorSecondary)
        val primaryTextColor = styledColor(android.R.attr.textColorPrimary)

        val soundSeekBar = SignSeekBar(requireContext())
        soundSeekBar.getConfigBuilder()
            .min(0f).max(40f).progress(soundOnKeyPress.toFloat())
            .sectionCount(4)
            .trackSize(2)
            .secondTrackSize(2)
            .trackColor(grayColor)
            .secondTrackColor(accentColor)
            .thumbColor(accentColor)
            .thumbRadius(6)
            .thumbRadiusOnDragging(8)
            .showThumbShadow(false)
            .showSectionText()
            .sectionTextPosition(SignSeekBar.TextPosition.BELOW_SECTION_MARK)
            .sectionTextColor(textSecondaryColor)
            .sectionTextSize(12)
            .bottomSidesLabels(resources.getStringArray(R.array.sound_labels))
            .autoAdjustSectionMark()
            .touchToSeek()
            .build()

        val vibrateSeekBar = SignSeekBar(requireContext())
        vibrateSeekBar.getConfigBuilder()
            .min(0f).max(4f).progress(vibrationAmplitude.toFloat())
            .sectionCount(4)
            .trackSize(2)
            .secondTrackSize(2)
            .trackColor(grayColor)
            .secondTrackColor(accentColor)
            .thumbColor(accentColor)
            .thumbRadius(6)
            .thumbRadiusOnDragging(8)
            .showThumbShadow(false)
            .showSectionText()
            .sectionTextPosition(SignSeekBar.TextPosition.BELOW_SECTION_MARK)
            .sectionTextColor(textSecondaryColor)
            .sectionTextSize(12)
            .bottomSidesLabels(resources.getStringArray(R.array.vibrate_labels))
            .autoAdjustSectionMark()
            .touchToSeek()
            .build()

        // Tab state and sound configurations
        var selectedTab = 0 // 0: 标准, 1: 空格, 2: 删除, 3: 回车

        val tabsInfo = listOf(
            Triple(
                "标准", soundEffectStandardPref, InputFeedbacks.SoundEffect.Standard to arrayOf(
                    "默认", "微信点击",
                    "百度红轴", "百度青轴", "百度茶轴", "百度机械",
                    "百度黑莓", "百度轻敲", "百度经典",
                    "百度可爱", "百度卡通", "百度快板"
                )
            ),
            Triple(
                "空格", soundEffectSpaceBarPref, InputFeedbacks.SoundEffect.SpaceBar to arrayOf(
                    "默认", "微信空格",
                    "百度红轴", "百度青轴", "百度茶轴", "百度机械",
                    "百度黑莓", "百度经典"
                )
            ),
            Triple(
                "删除", soundEffectDeletePref, InputFeedbacks.SoundEffect.Delete to arrayOf(
                    "默认", "微信删除",
                    "百度红轴", "百度青轴", "百度茶轴", "百度机械",
                    "百度黑莓", "百度经典"
                )
            ),
            Triple(
                "回车", soundEffectReturnPref, InputFeedbacks.SoundEffect.Return to arrayOf(
                    "默认", "微信回车", "百度动作",
                    "百度红轴", "百度青轴", "百度茶轴", "百度机械",
                    "百度黑莓"
                )
            )
        )

        fun createCapsuleDrawable(color: Int, radiusDp: Float = 16f): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(radiusDp).toFloat()
                setColor(color)
            }
        }

        val tabsLayout = horizontalLayout {
            weightSum = 4f
        }

        val optionsContainer = verticalLayout {
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12f).toFloat()
                setStroke(dp(1), grayColor)
                setColor(styledColor(android.R.attr.colorBackgroundFloating))
            }
        }

        fun renderTabsAndOptions(enabled: Boolean) {
            tabsLayout.removeAllViews()
            optionsContainer.removeAllViews()

            // 1. Render 4 Tab Buttons
            tabsInfo.forEachIndexed { index, (tabName, _, _) ->
                val isTabSelected = (index == selectedTab)
                val tabBtn = textView {
                    text = tabName
                    textSize = 14f
                    gravity = gravityCenter
                    setPadding(dp(8), dp(8), dp(8), dp(8))

                    val bgCol = when {
                        !enabled -> grayColor
                        isTabSelected -> accentColor
                        else -> ContextCompat.getColor(context, R.color.color_gray)
                    }
                    val textCol = when {
                        !enabled -> textSecondaryColor
                        isTabSelected -> Color.WHITE
                        else -> primaryTextColor
                    }

                    background = createCapsuleDrawable(bgCol, 16f)
                    setTextColor(textCol)
                    isEnabled = enabled

                    setOnClickListener {
                        if (enabled && selectedTab != index) {
                            selectedTab = index
                            renderTabsAndOptions(enabled)
                        }
                    }
                }

                tabsLayout.add(tabBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (index > 0) leftMargin = dp(8)
                })
            }

            // 2. Render Options for currently selected tab
            val (_, currentPref, effectPair) = tabsInfo[selectedTab]
            val (effect, options) = effectPair
            val selectedOptionValue = currentPref.getValue()

            var currentRow: LinearLayout? = null
            options.forEachIndexed { optIndex, optionText ->
                if (optIndex % 2 == 0) {
                    currentRow = horizontalLayout {
                        weightSum = 2f
                    }
                    optionsContainer.add(currentRow!!, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        if (optIndex > 0) topMargin = dp(10)
                    })
                }

                val isOptionSelected = (optionText == selectedOptionValue)
                val optionChip = textView {
                    text = optionText
                    textSize = 13f
                    gravity = gravityCenter
                    setPadding(dp(12), dp(10), dp(12), dp(10))

                    val chipBgCol = when {
                        !enabled -> ContextCompat.getColor(context, R.color.color_gray)
                        isOptionSelected -> accentColor
                        else -> styledColor(android.R.attr.colorBackground)
                    }
                    val chipTextCol = when {
                        !enabled -> textSecondaryColor
                        isOptionSelected -> Color.WHITE
                        else -> primaryTextColor
                    }

                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(16f).toFloat()
                        setColor(chipBgCol)
                        if (!isOptionSelected && enabled) {
                            setStroke(dp(1), grayColor)
                        }
                    }
                    setTextColor(chipTextCol)
                    isEnabled = enabled

                    setOnClickListener {
                        if (enabled) {
                            currentPref.setValue(optionText)
                            renderTabsAndOptions(enabled)

                            when (effect) {
                                InputFeedbacks.SoundEffect.Standard -> DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_A)
                                InputFeedbacks.SoundEffect.SpaceBar -> DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_SPACE)
                                InputFeedbacks.SoundEffect.Delete -> DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_DEL)
                                InputFeedbacks.SoundEffect.Return -> DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_ENTER)
                            }
                        }
                    }
                }

                currentRow?.add(optionChip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (optIndex % 2 == 1) leftMargin = dp(10)
                })
            }

            // Fill remaining weight in row if odd number of items
            if (options.size % 2 == 1 && currentRow != null) {
                val dummyView = View(context)
                currentRow?.add(dummyView, LinearLayout.LayoutParams(0, 0, 1f))
            }
        }

        val isSoundActive = soundOnKeyPress < 5 || soundOnKeyPress > 10
        soundSeekBar.isEnabled = isSoundActive
        renderTabsAndOptions(isSoundActive)

        val soundSwitch = switch {
            isChecked = isSoundActive
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    soundSeekBar.setProgress(0f)
                    AppPrefs.getInstance().internal.soundOnKeyPress.setValue(0)
                    soundSeekBar.isEnabled = true
                    renderTabsAndOptions(true)
                } else {
                    soundSeekBar.setProgress(10f)
                    AppPrefs.getInstance().internal.soundOnKeyPress.setValue(10)
                    soundSeekBar.isEnabled = false
                    renderTabsAndOptions(false)
                }
            }
        }

        soundSeekBar.setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
            override fun onProgressChanged(signSeekBar: SignSeekBar, progress: Int, progressFloat: Float, fromUser: Boolean) {}
            override fun getProgressOnFinally(signSeekBar: SignSeekBar, progress: Int, progressFloat: Float, fromUser: Boolean) {}
            override fun getProgressOnActionUp(signSeekBar: SignSeekBar, progress: Int, progressFloat: Float) {
                AppPrefs.getInstance().internal.soundOnKeyPress.setValue(progress)
                DevicesUtils.tryPlayKeyDown()
            }
        })

        val isVibrationActive = vibrationAmplitude != 1
        vibrateSeekBar.isEnabled = isVibrationActive

        val vibrationSwitch = switch {
            isChecked = isVibrationActive
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    vibrateSeekBar.setProgress(0f)
                    AppPrefs.getInstance().internal.vibrationAmplitude.setValue(0)
                    vibrateSeekBar.isEnabled = true
                } else {
                    vibrateSeekBar.setProgress(1f)
                    AppPrefs.getInstance().internal.vibrationAmplitude.setValue(1)
                    vibrateSeekBar.isEnabled = false
                }
            }
        }

        vibrateSeekBar.setOnProgressChangedListener(object : SignSeekBar.OnProgressChangedListener {
            override fun onProgressChanged(signSeekBar: SignSeekBar, progress: Int, progressFloat: Float, fromUser: Boolean) {}
            override fun getProgressOnActionUp(signSeekBar: SignSeekBar, progress: Int, progressFloat: Float) {}

            override fun getProgressOnFinally(signSeekBar: SignSeekBar, progress: Int, progressFloat: Float, fromUser: Boolean) {
                AppPrefs.getInstance().internal.vibrationAmplitude.setValue(progress)
                DevicesUtils.tryVibrate(signSeekBar)
            }
        })

        val soundTitle = textView {
            setText(R.string.button_sound_volume)
            textSize = 16f
            gravity = gravityVerticalCenter
        }

        val vibrationTitle = textView {
            setText(R.string.button_vibration_amplitude)
            textSize = 16f
            gravity = gravityVerticalCenter
        }

        val soundEffectsCategoryTitle = textView {
            setText(R.string.sound_effect_category)
            textSize = 14f
            setTextColor(accentColor)
            setPadding(dp(24), dp(16), dp(24), dp(8))
        }

        val mainContentLayout = constraintLayout {
            val lineHeight = dp(48)
            val itemMargin = dp(24)
            add(soundTitle, lParams(matchConstraints, lineHeight) {
                topOfParent()
                startOfParent(itemMargin)
                before(soundSwitch)
                above(soundSeekBar)
            })
            add(soundSwitch, lParams(matchConstraints, lineHeight) {
                topToTopOf(soundTitle)
                endOfParent(itemMargin)
            })
            add(soundSeekBar, lParams(width = matchParent, height = wrapContent) {
                below(soundTitle)
                startOfParent(itemMargin)
                endOfParent(itemMargin)
                above(vibrationTitle)
            })

            add(vibrationTitle, lParams(matchConstraints, lineHeight) {
                below(soundSeekBar)
                startOfParent(itemMargin)
                topToTopOf(soundSeekBar, itemMargin)
                above(vibrateSeekBar)
            })
            add(vibrationSwitch, lParams(matchConstraints, lineHeight) {
                topToTopOf(vibrationTitle)
                endOfParent(itemMargin)
            })
            add(vibrateSeekBar, lParams(width = matchParent, height = wrapContent) {
                below(vibrationTitle)
                startOfParent(itemMargin)
                endOfParent(itemMargin)
            })
        }

        val soundEffectsLayout = verticalLayout {
            setPadding(dp(24), dp(8), dp(24), dp(24))
            add(tabsLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            add(optionsContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        }

        val contentLayout = verticalLayout {
            add(mainContentLayout, lParams(matchParent, wrapContent))
            add(soundEffectsCategoryTitle, lParams(matchParent, wrapContent))
            add(soundEffectsLayout, lParams(matchParent, wrapContent))
        }

        contentLayout.wrapInScrollView()
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.setTitle(R.string.keyboard_feedback)
    }

    override fun onStop() {
        super.onStop()
        KeyboardManager.instance.clearKeyboard()
    }
}
