package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.yuyan.imemodule.R
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.view.preference.ManagedPreference
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * 软键盘View集装箱
 * 所有软键盘（输入、符号、设置等）父容器View。
 */
@SuppressLint("ViewConstructor")
open class BaseContainer(@JvmField val mContext: Context, @JvmField val inputView: InputView) : ConstraintLayout(mContext) {
    private lateinit var mRightPaddingKey: ManagedPreference.PInt
    private lateinit var mBottomPaddingKey: ManagedPreference.PInt

    /**
     * 更新软键盘布局
     */
    open fun updateSkbLayout() {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = EnvironmentSingleton.instance.skbWidth
        val measuredHeight = EnvironmentSingleton.instance.skbHeight
        val widthMeasure = MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY)
        val heightMeasure = MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasure, heightMeasure)
    }

    /**
     * 设置键盘高度
     */
    @SuppressLint("ClickableViewAccessibility")
    fun setKeyboardHeight() {
        if (findViewById<View>(R.id.iv_keyboard_height_Top) != null) return
        val rootView = LayoutInflater.from(context).inflate(R.layout.layout_ime_keyboard_height_shadow, this, false)
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.addView(rootView, params)
        rootView.findViewById<View>(R.id.ll_keyboard_height_reset).setOnClickListener {
            EnvironmentSingleton.instance.keyBoardHeightRatio = 0.3f
            EnvironmentSingleton.instance.initData()
            KeyboardLoaderUtil.instance.clearKeyboardMap()
            updateSkbLayout()
            inputView.initView(inputView.context)
            requestLayout()
            inputView.requestLayout()
        }
        rootView.findViewById<View>(R.id.ll_keyboard_height_sure).setOnClickListener { removeView(rootView) }
        rootView.findViewById<View>(R.id.iv_keyboard_height_Top).setOnTouchListener { v, event -> onModifyKeyboardHeightEvent(v, event) }
        val internalPrefs = AppPrefs.getInstance().internal
        if (EnvironmentSingleton.instance.keyboardModeFloat) {
            mBottomPaddingKey = if (EnvironmentSingleton.instance.isLandscape) internalPrefs.keyboardBottomPaddingLandscapeFloat
            else internalPrefs.keyboardBottomPaddingFloat
            mRightPaddingKey = if (EnvironmentSingleton.instance.isLandscape) internalPrefs.keyboardRightPaddingLandscapeFloat
            else internalPrefs.keyboardRightPaddingFloat
        } else {
            mBottomPaddingKey = internalPrefs.keyboardBottomPadding
            mRightPaddingKey = internalPrefs.keyboardRightPadding
        }
        rootView.findViewById<View>(R.id.iv_keyboard_move).setOnTouchListener { _, event -> onMoveKeyboardEvent(event) }
    }

    private var initialHeightTouchY = 0f
    private var initialHeightRatio = 0f
    private var initialScreenHeightVertical = 0f

    private fun onModifyKeyboardHeightEvent(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialHeightTouchY = event.rawY
                initialHeightRatio = EnvironmentSingleton.instance.keyBoardHeightRatio
                initialScreenHeightVertical = if (initialHeightRatio > 0f) {
                    EnvironmentSingleton.instance.skbHeight / initialHeightRatio
                } else {
                    EnvironmentSingleton.instance.mScreenHeight.toFloat()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (initialScreenHeightVertical <= 0f) return true
                val dy = event.rawY - initialHeightTouchY
                val deltaRatio = -dy / initialScreenHeightVertical
                val minRatio = if (EnvironmentSingleton.instance.isLandscape) 0.2f else 0.15f
                val maxRatio = if (EnvironmentSingleton.instance.isLandscape) 0.7f else 0.6f
                val newRatio = (initialHeightRatio + deltaRatio).coerceIn(minRatio, maxRatio)

                if (abs(newRatio - EnvironmentSingleton.instance.keyBoardHeightRatio) > 0.001f) {
                    EnvironmentSingleton.instance.keyBoardHeightRatio = newRatio
                    EnvironmentSingleton.instance.initData()
                    KeyboardLoaderUtil.instance.clearKeyboardMap()
                    updateSkbLayout()
                    inputView.initView(inputView.context)
                    requestLayout()
                    inputView.requestLayout()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                v.performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                return true
            }
        }
        return false
    }

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var rightPaddingValue = 0  // 右侧边距
    private var bottomPaddingValue = 0  // 底部边距
    private var mSkbRootHeight = 0  // 键盘高度
    private var mSkbRootWidth = 0  // 键盘宽度

    private fun onMoveKeyboardEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                bottomPaddingValue = mBottomPaddingKey.getValue()
                rightPaddingValue = mRightPaddingKey.getValue()
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                mSkbRootHeight = inputView.mSkbRoot.height
                mSkbRootWidth = inputView.mSkbRoot.width
                inputView.dismissBlurWindow()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx: Float = event.rawX - initialTouchX
                val dy: Float = event.rawY - initialTouchY
                val viewWidth = inputView.width
                val viewHeight = inputView.height
                if (dx.absoluteValue > 10) {
                    rightPaddingValue -= dx.toInt()
                    rightPaddingValue = when {
                        rightPaddingValue < 0 -> 0
                        rightPaddingValue > (viewWidth - mSkbRootWidth) -> (viewWidth - mSkbRootWidth)
                        else -> rightPaddingValue
                    }
                    initialTouchX = event.rawX
                    val targetView = if (EnvironmentSingleton.instance.keyboardModeFloat) inputView else inputView.mSkbRoot
                    targetView.setPadding(targetView.paddingLeft, targetView.paddingTop, rightPaddingValue, targetView.paddingBottom)
                }
                if (dy.absoluteValue > 10) {
                    bottomPaddingValue -= dy.toInt()
                    bottomPaddingValue = when {
                        bottomPaddingValue < 0 -> 0
                        bottomPaddingValue > (viewHeight - mSkbRootHeight) -> (viewHeight - mSkbRootHeight)
                        else -> bottomPaddingValue
                    }
                    initialTouchY = event.rawY
                    val targetView = if (EnvironmentSingleton.instance.keyboardModeFloat) inputView else inputView.mSkbRoot
                    targetView.setPadding(targetView.paddingLeft, targetView.paddingTop, targetView.paddingRight, bottomPaddingValue)
                }
                inputView.updateBlurWindow()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mRightPaddingKey.setValue(rightPaddingValue)
                mBottomPaddingKey.setValue(bottomPaddingValue)
                inputView.updateBlurWindow()
            }
        }
        return false
    }
}
