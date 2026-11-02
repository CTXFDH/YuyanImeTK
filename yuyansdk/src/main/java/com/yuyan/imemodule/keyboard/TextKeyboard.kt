@file:Suppress("ALL")

package com.yuyan.imemodule.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import splitties.dimensions.dp
import android.graphics.Paint.FontMetricsInt
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager.activeTheme
import com.yuyan.imemodule.data.theme.ThemeManager.prefs
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.entity.keyboard.SoftKeyToggle
import com.yuyan.imemodule.entity.keyboard.SoftKeyboard
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.service.DictDecoder
import com.yuyan.imemodule.singleton.EnvironmentSingleton.Companion.instance
import kotlin.math.max
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import com.yuyan.imemodule.prefs.behavior.SkbStyleMode
import com.yuyan.imemodule.service.InputDispatcher

/**
 * 软件盘视图
 */
@Suppress("ALL")
open class TextKeyboard(context: Context?) : BaseKeyboardView(context){
    private var mKeyboardChanged = false
    private var mBuffer: Bitmap? = null
    private var mCanvas: Canvas? = null
    private var mNormalKeyTextSize = 0   //正常按键的文本大小
    private var mNormalKeyTextSizeSmall = 0  //正常按键的文本大小(小值)
    private val mPaint: Paint = Paint()   //绘制按键的画笔
    private val mFmi: FontMetricsInt
    private var isKeyBorder = false // 启用按键边框
    private var isKeyShadow = false // 启用按键阴影
    protected lateinit var mActiveTheme: Theme
    private var keyRadius = 0
    private var keyboardFontBold = false
    private var keyboardSymbol = false
    private var keyboardMnemonic = false
    protected var mDirtyRect = Rect()
    private var skbStyleMode: SkbStyleMode = prefs.skbStyleMode.getValue()

    /**
     * 构造方法
     */
    init {
        mPaint.isAntiAlias = true
        mFmi = mPaint.fontMetricsInt
        keyboardFontBold = prefs.keyboardFontBold.getValue()
        keyboardSymbol = prefs.keyboardSymbol.getValue()
        keyboardMnemonic = AppPrefs.getInstance().keyboardSetting.keyboardMnemonic.getValue()
        isKeyBorder = prefs.keyBorder.getValue()
        isKeyShadow = prefs.keyShadow.getValue()
        prefs.keyBorder.registerOnChangeListener { _, value ->
            isKeyBorder = value
            mBuffer = null
            mKeyboardChanged = true
            invalidateView()
            invalidate()
        }
        prefs.keyShadow.registerOnChangeListener { _, value ->
            isKeyShadow = value
            mBuffer = null
            mKeyboardChanged = true
            invalidateView()
            invalidate()
        }
    }

    override fun getKeyIndices(x: Int, y: Int): SoftKey? {
        if (InputModeSwitcher.isTextEditSkb && x < width * 0.52f) {
            val centerX = width * 0.27f
            val centerY = height * 0.39f
            val outerRadius = kotlin.math.min(width * 0.46f, height * 0.68f) / 2f
            val innerRadius = outerRadius * 0.40f

            val dx = x - centerX
            val dy = y - centerY
            val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

            if (dist <= innerRadius) {
                return mSoftKeyboard?.getKeyByCode(InputModeSwitcher.USER_KEYCODE_SELECT_MODE)
            } else if (dist <= outerRadius) {
                val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                val code = when {
                    angle in -135f..-45f -> KeyEvent.KEYCODE_DPAD_UP
                    angle > -45f && angle <= 45f -> KeyEvent.KEYCODE_DPAD_RIGHT
                    angle > 45f && angle <= 135f -> KeyEvent.KEYCODE_DPAD_DOWN
                    else -> KeyEvent.KEYCODE_DPAD_LEFT
                }
                return mSoftKeyboard?.getKeyByCode(code)
            }
        }
        return super.getKeyIndices(x, y)
    }

    /**
     * 设置键盘实体
     *
     * @param softSkb 键盘
     */
    override fun setSoftKeyboard(softSkb: SoftKeyboard) {
        super.setSoftKeyboard(softSkb)
        isKeyBorder = prefs.keyBorder.getValue()
        isKeyShadow = prefs.keyShadow.getValue()
        keyRadius = prefs.keyRadius.getValue()
        mActiveTheme = activeTheme
        mPaint.color = mActiveTheme.keyTextColor
        mKeyboardChanged = true
        invalidateView()
    }

    /**
     * 刷新按键状态
     */
    fun updateStates() {
        var softKey = mSoftKeyboard?.getKeyByCode(KeyEvent.KEYCODE_ENTER) as? SoftKeyToggle
        softKey?.enableToggleState( if(mService!!.isAddPhrases)4 else InputModeSwitcher.mToggleStates.imeAction)
        softKey = mSoftKeyboard?.getKeyByCode(KeyEvent.KEYCODE_SHIFT_LEFT) as? SoftKeyToggle
        val isEnglishCell = AppPrefs.getInstance().input.abcSearchEnglishCell.getValue()
        softKey?.enableToggleState(InputModeSwitcher.mToggleStates.modifiers + if(isEnglishCell) 3 else 0)
        invalidateView()
    }

    /**
     * 重置主题并清空离屏缓存，确保按键背景色与高亮色毫秒级实时重绘刷新
     */
    open fun setTheme(theme: Theme) {
        isKeyBorder = prefs.keyBorder.getValue()
        isKeyShadow = prefs.keyShadow.getValue()
        keyRadius = prefs.keyRadius.getValue()
        mActiveTheme = theme
        mPaint.color = mActiveTheme.keyTextColor
        mBuffer = null
        mKeyboardChanged = true
        invalidateView()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var measuredWidth = 0
        var measuredHeight = 0
        if (null != mSoftKeyboard) {
            measuredWidth = instance.skbWidth +  paddingLeft + paddingRight
            measuredHeight = instance.skbHeight + paddingTop + paddingBottom
        }
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun invalidateView() {
        requestLayout()
        invalidateKey()
    }

    public override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBuffer = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mDirtyRect.union(0, 0, width, height)
        if (mDrawPending || (mBuffer == null) || mKeyboardChanged) {
            onBufferDraw()
        }
        canvas.drawBitmap(mBuffer!!, 0f, 0f, null)

        // 直接在屏幕顶层 Canvas 绘制黑白相间“蚂蚁选区虚线框”，零延迟！100% 显现圈住！
        selectedKey?.let { selected ->
            val regionKeys = getSelectedRegionKeys(selected)
            drawBlackAndWhiteMarqueeBox(canvas, regionKeys)
        }
    }

    override fun onBufferDraw() {
        if (mBuffer == null || mKeyboardChanged) {
            if (mBuffer == null || mBuffer!!.width != width || mBuffer!!.height != height) {
                val width = max(1.0, width.toDouble()).toInt()
                val height = max(1.0, height.toDouble()).toInt()
                mBuffer = createBitmap(width, height)
                mCanvas = Canvas(mBuffer!!)
            }
            invalidateKey()
            mKeyboardChanged = false
        }
        if (mSoftKeyboard == null) return
        mCanvas!!.withSave {
            val canvas = mCanvas
            canvas?.clipRect(mDirtyRect)
            canvas?.drawColor(0x00000000, PorterDuff.Mode.CLEAR)
            val env = instance
            mNormalKeyTextSize = env.keyTextSize
            mNormalKeyTextSizeSmall = env.keyTextSmallSize
            val keyXMargin = mSoftKeyboard!!.keyXMargin
            val keyYMargin = if(skbStyleMode == SkbStyleMode.Google && InputModeSwitcher.isQwert) mSoftKeyboard!!.keyYMargin * 1.5
                else mSoftKeyboard!!.keyYMargin
            for (softKeys in mSoftKeyboard!!.mKeyRows) {
                for (softKey in softKeys) {
                    canvas?.let {
                        if (InputModeSwitcher.isTextEditSkb) {
                            drawTextEditKey(it, softKey, keyXMargin, keyYMargin.toInt())
                        } else {
                            drawSoftKey(it, softKey, keyXMargin, keyYMargin.toInt())
                        }
                    }
                }
            }
            mCanvas!!
        }
        mDrawPending = false
        mDirtyRect.setEmpty()
    }

    var selectedKey: SoftKey? = null
    var selectSingleMode: Boolean = false // false: 全部输入区按键生效, true: 仅当前按键生效
    val customKeyColorMap = mutableMapOf<String, Int>()

    private fun isEnterKey(softKey: SoftKey): Boolean {
        return softKey.code == KeyEvent.KEYCODE_ENTER ||
                softKey.label == "发送" || softKey.label == "回车" || softKey.label == "Enter"
    }

    override fun isSpaceKey(key: SoftKey?): Boolean {
        if (key == null) return false
        return key.code == KeyEvent.KEYCODE_SPACE || key.label == "空格" || key.label == "En" || key.label == "cn" || key.labelSmall == "En" || key.labelSmall == "cn"
    }

    private fun isDeleteKey(softKey: SoftKey): Boolean {
        return softKey.code == KeyEvent.KEYCODE_DEL || softKey.label == "⌫" || softKey.label == "删除"
    }

    private fun isSymbolKey(softKey: SoftKey): Boolean {
        return softKey.code == InputModeSwitcher.USER_KEYCODE_SYMBOL || softKey.label == "符号"
    }

    private fun isNumberKey(softKey: SoftKey): Boolean {
        if (softKey.code == InputModeSwitcher.USER_KEYCODE_NUMBER) return true
        val label = softKey.label
        val cleanLabel = label.trim()
        return cleanLabel == "123" || cleanLabel == "?123" || cleanLabel == "123#" || cleanLabel.contains("123")
    }

    private fun isShiftKey(softKey: SoftKey): Boolean {
        if (softKey.code == KeyEvent.KEYCODE_SHIFT_LEFT ||
            softKey.code == KeyEvent.KEYCODE_SHIFT_RIGHT) return true
        val label = softKey.label
        val lower = label.lowercase()
        return lower.contains("shift") || lower.contains("caps") ||
               label == "⇧" || label == "▲" || label == "⬆" || label == "⇪" || label == "^"
    }

    private fun isClearKey(softKey: SoftKey): Boolean {
        return softKey.code == KeyEvent.KEYCODE_CLEAR || softKey.label == "重输"
    }

    private fun isReturnKey(softKey: SoftKey): Boolean {
        return softKey.code == InputModeSwitcher.USER_KEYCODE_RETURN || softKey.label == "返回"
    }

    private fun isAtKey(softKey: SoftKey): Boolean {
        if (isEnterKey(softKey)) return false
        if (softKey.code == KeyEvent.KEYCODE_AT) return true
        val label = softKey.label
        return label == "@" || label == "@ / 0" || label.startsWith("@")
    }

    private fun isDotKey(softKey: SoftKey): Boolean {
        return softKey.code == KeyEvent.KEYCODE_PERIOD || softKey.label == "."
    }

    private fun isSideSymbolKey(softKey: SoftKey): Boolean {
        if (isAtKey(softKey) || isDotKey(softKey) || isShiftKey(softKey) || isNumberKey(softKey)) return false
        if (softKey.code == InputModeSwitcher.USER_KEYCODE_LEFT_SYMBOL) return true
        if (softKey.heightF > 0.5f) return true
        val sideLabels = arrayOf("，", "。", "？", "！", "～", "、", "；", "：", ",")
        if (softKey.label in sideLabels) return true
        val firstChar = softKey.label.getOrNull(0)
        if (firstChar != null && !firstChar.isLetterOrDigit() && softKey.code < 0) {
            return !isEnterKey(softKey) && !isSpaceKey(softKey) && !isDeleteKey(softKey) && !isSymbolKey(softKey) && !isNumberKey(softKey) && !isClearKey(softKey) && !isReturnKey(softKey) && !isShiftKey(softKey)
        }
        return false
    }

    private fun isAlphaInputKey(softKey: SoftKey): Boolean {
        if (isNumberKey(softKey)) return false // 123 键绝对不是字母输入区！
        return !isEnterKey(softKey) && !isSpaceKey(softKey) && !isDeleteKey(softKey) &&
               !isSymbolKey(softKey) && !isSideSymbolKey(softKey) &&
               !isClearKey(softKey) && !isReturnKey(softKey) && !isAtKey(softKey) && !isShiftKey(softKey) && !isDotKey(softKey)
    }

    private fun darkenColor(color: Int, factor: Float = 0.22f): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1f - factor)).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    private fun drawSoftKey(canvas: Canvas, softKey: SoftKey, keyXMargin: Int, keyYMargin: Int) {
        val left = softKey.mLeft + keyXMargin
        val top = softKey.mTop + keyYMargin
        val right = softKey.mRight - keyXMargin
        val bottom = softKey.mBottom - keyYMargin
        val shadowDepth = dp(2f).toInt().coerceAtLeast(1)

        val keyUniqueKey = softKey.label.ifEmpty { softKey.code.toString() }
        val customColor = customKeyColorMap[keyUniqueKey]

        val override = mActiveTheme.keyCustomOverrides[softKey.label]
            ?: mActiveTheme.keyCustomOverrides[softKey.keyLabel]
            ?: mActiveTheme.keyCustomOverrides[softKey.code.toString()]

        val background = when {
            override?.bgInt() != null -> override.bgInt()!!
            customColor != null -> customColor
            isEnterKey(softKey) -> mActiveTheme.accentKeyBackgroundColor
            isSpaceKey(softKey) -> mActiveTheme.spaceKeyBackgroundColor
            isDeleteKey(softKey) -> mActiveTheme.deleteKeyBackgroundColor
            isClearKey(softKey) -> mActiveTheme.clearKeyBackgroundColor
            isShiftKey(softKey) -> when (InputModeSwitcher.mToggleStates.modifiers) {
                KeyEvent.META_CAPS_LOCK_ON -> mActiveTheme.shiftKeyLockBackgroundColor
                KeyEvent.META_SHIFT_ON -> mActiveTheme.shiftKeyActiveBackgroundColor
                else -> mActiveTheme.shiftKeyBackgroundColor
            }
            isReturnKey(softKey) -> mActiveTheme.returnKeyBackgroundColor
            isAtKey(softKey) -> mActiveTheme.atKeyBackgroundColor
            isDotKey(softKey) -> mActiveTheme.periodKeyBackgroundColor
            isSymbolKey(softKey) -> mActiveTheme.symbolKeyBackgroundColor
            isNumberKey(softKey) -> mActiveTheme.numberKeyBackgroundColor
            isSideSymbolKey(softKey) -> mActiveTheme.sideSymbolKeyBackgroundColor
            else -> mActiveTheme.keyBackgroundColor
        }

        if (softKey.pressed || (mService?.hasSelection == true && softKey.code == InputModeSwitcher.USER_KEYCODE_SELECT_MODE)) {
            val pressColor = when {
                isEnterKey(softKey) -> mActiveTheme.accentKeyPressColor
                isSpaceKey(softKey) -> mActiveTheme.spaceKeyPressHighlightColor
                isDeleteKey(softKey) -> mActiveTheme.deleteKeyPressColor
                isClearKey(softKey) -> mActiveTheme.clearKeyPressColor
                isShiftKey(softKey) -> mActiveTheme.shiftKeyPressColor
                isReturnKey(softKey) -> mActiveTheme.returnKeyPressColor
                isAtKey(softKey) -> mActiveTheme.atKeyPressColor
                isDotKey(softKey) -> mActiveTheme.periodKeyPressColor
                isSymbolKey(softKey) -> mActiveTheme.symbolKeyPressColor
                isNumberKey(softKey) -> mActiveTheme.numberKeyPressColor
                isSideSymbolKey(softKey) -> mActiveTheme.sideSymbolKeyPressColor
                else -> mActiveTheme.keyPressHighlightColor
            }
            val topCap = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = keyRadius.toFloat()
                setColor(pressColor)
                setBounds(left, top + shadowDepth, right, bottom)
            }
            topCap.draw(canvas)
        } else {
            if (isKeyBorder || !isAlphaInputKey(softKey)) {
                if (isKeyShadow) {
                    // 1. 开启按键阴影: 绘制底层 3D 阴影底座 + 顶层按键帽
                    val shadowColor = darkenColor(background, 0.22f)
                    val shadowCap = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = keyRadius.toFloat()
                        setColor(shadowColor)
                        setBounds(left, top + shadowDepth, right, bottom)
                    }
                    shadowCap.draw(canvas)

                    // 2. 绘制顶层按键帽 (向上浮起 shadowDepth 像素)
                    val topCap = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = keyRadius.toFloat()
                        setColor(background)
                        setBounds(left, top, right, bottom - shadowDepth)
                    }
                    topCap.draw(canvas)
                } else {
                    // 2. 关闭按键阴影: 绘制无阴影平实按键帽
                    val flatCap = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = keyRadius.toFloat()
                        setColor(background)
                        setBounds(left, top, right, bottom)
                    }
                    flatCap.draw(canvas)
                }
            }
        }

        val textColor = when {
            override?.textInt() != null -> override.textInt()!!
            isEnterKey(softKey) -> mActiveTheme.accentKeyTextColor
            isSpaceKey(softKey) -> mActiveTheme.spaceKeyTextColor
            isDeleteKey(softKey) -> mActiveTheme.deleteKeyTextColor
            isClearKey(softKey) -> mActiveTheme.clearKeyTextColor
            isShiftKey(softKey) -> mActiveTheme.shiftKeyTextColor
            isReturnKey(softKey) -> mActiveTheme.keyTextColor
            isAtKey(softKey) -> mActiveTheme.atKeyTextColor
            isDotKey(softKey) -> mActiveTheme.periodKeyTextColor
            isSymbolKey(softKey) -> mActiveTheme.symbolKeyTextColor
            isNumberKey(softKey) -> mActiveTheme.numberKeyTextColor
            isSideSymbolKey(softKey) -> mActiveTheme.sideSymbolKeyTextColor
            else -> mActiveTheme.keyTextColor
        }

        val smallSymbolColor = override?.symbolInt() ?: mActiveTheme.keySmallSymbolColor


        var keyLabel = if(skbStyleMode == SkbStyleMode.Google){
            if(InputModeSwitcher.isLower) softKey.keyLabel.lowercase() else softKey.keyLabel
        } else {
            if(InputModeSwitcher.isLower && InputModeSwitcher.isEnglish) softKey.keyLabel.lowercase() else softKey.keyLabel
        }
        val keyLabelSmall = softKey.getmKeyLabelSmall()
        val keyMnemonic = softKey.keyMnemonic
        var keyIcon = if(softKey.code == KeyEvent.KEYCODE_SPACE) {
            ContextCompat.getDrawable(context, R.drawable.sdk_skb_key_space_icon)
        } else if (isShiftKey(softKey)) {
            when (InputModeSwitcher.mToggleStates.modifiers) {
                KeyEvent.META_SHIFT_ON -> ContextCompat.getDrawable(context, R.drawable.shift_on_1_icon)
                KeyEvent.META_CAPS_LOCK_ON -> ContextCompat.getDrawable(context, R.drawable.shift_lock_2_icon)
                else -> ContextCompat.getDrawable(context, R.drawable.shift_off_0_icon)
            }
        } else if(skbStyleMode == SkbStyleMode.Google && softKey.code == InputModeSwitcher.USER_KEYCODE_CURSOR_DIRECTION && !DictDecoder.isCandidatesEmpty) null
            else softKey.keyIcon
        val weightHeigth = softKey.height() / 4f
        if(softKey.code == KeyEvent.KEYCODE_SHIFT_LEFT && InputModeSwitcher.isChinese && !InputDispatcher.isFinish){
            keyLabel = "分词"
            keyIcon = null
        }
        if (keyboardSymbol && !TextUtils.isEmpty(keyLabelSmall)) {
            mPaint.color = smallSymbolColor
            mPaint.typeface = Typeface.DEFAULT
            if(skbStyleMode == SkbStyleMode.Samsung)mPaint.alpha = 128
            mPaint.textSize = mNormalKeyTextSizeSmall.toFloat()
            val x = when(prefs.skbStyleMode.getValue()){
                SkbStyleMode.Yuyan -> softKey.mLeft + (softKey.width() - mPaint.measureText(keyLabelSmall)) / 2.0f
                SkbStyleMode.Samsung -> softKey.mRight - mPaint.measureText(keyLabelSmall) - keyXMargin * 2
                SkbStyleMode.Google -> softKey.mRight - mPaint.measureText(keyLabelSmall) - keyXMargin * 2
            }
            val y = softKey.mTop + weightHeigth * 1.1f
            canvas.drawText(keyLabelSmall, x, y, mPaint)
        }
        if (null != keyIcon) {
            var  intrinsicWidth = keyIcon.intrinsicWidth
            var  intrinsicHeight = keyIcon.intrinsicHeight
            while(softKey.width() < intrinsicWidth || softKey.height() < intrinsicHeight){
                intrinsicWidth /= 2
                intrinsicHeight /= 2
            }
            val marginLeft = (softKey.width() - intrinsicWidth) / 2
            val marginRight = softKey.width() - intrinsicWidth - marginLeft
            val marginTop = if (softKey.code == KeyEvent.KEYCODE_SPACE) {
                ((softKey.height() - intrinsicHeight) / 2 + softKey.height() * 0.12f).toInt()
            } else {
                (softKey.height() - intrinsicHeight) / 2
            }
            val marginBottom = softKey.height() - intrinsicHeight - marginTop
            keyIcon.setTint(textColor)
            keyIcon.setBounds(softKey.mLeft + marginLeft, softKey.mTop + marginTop, softKey.mRight - marginRight, softKey.mBottom - marginBottom)
            keyIcon.draw(canvas)
        } else if (!TextUtils.isEmpty(keyLabel)) { //Label位于中间
            mPaint.color = textColor
            if(keyboardFontBold) mPaint.typeface = Typeface.DEFAULT_BOLD
            mPaint.textSize =  mNormalKeyTextSize.toFloat()
            val x = softKey.mLeft + (softKey.width() - mPaint.measureText(keyLabel)) / 2.0f
            val fontHeight = mFmi.bottom - mFmi.top
            val y = if(keyLabelSmall.isEmpty()) (softKey.mTop + softKey.mBottom) / 2.0f + fontHeight
            else  (softKey.mTop + softKey.mBottom) / 2.0f + fontHeight *1.5f
            canvas.drawText(keyLabel, x, y, mPaint)
        }
        if (keyboardMnemonic && !TextUtils.isEmpty(keyMnemonic)) {  //助记符位于中下方
            mPaint.color = textColor
            mPaint.typeface = Typeface.DEFAULT
            mPaint.textSize = mNormalKeyTextSizeSmall.toFloat() * 0.7f
            val x = softKey.mLeft + (softKey.width() - mPaint.measureText(keyMnemonic)) / 2.0f
            val y = softKey.mTop + weightHeigth * 3 + weightHeigth / 2.0f
            canvas.drawText(keyMnemonic, x, y, mPaint)
        }
    }

    private fun drawChevronArrow(canvas: Canvas, cx: Float, cy: Float, size: Float, angleDegree: Float, color: Int) {
        val arrowPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
            isAntiAlias = true
        }

        val path = android.graphics.Path().apply {
            moveTo(cx - size, cy - size / 2.2f)
            lineTo(cx, cy + size / 2.2f)
            lineTo(cx + size, cy - size / 2.2f)
        }

        canvas.save()
        canvas.rotate(angleDegree, cx, cy)
        canvas.drawPath(path, arrowPaint)
        canvas.restore()
    }

    private fun drawJumpArrow(canvas: Canvas, cx: Float, cy: Float, isStart: Boolean, color: Int) {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_jump_arrow)?.mutate() ?: return
        drawable.setTint(color)
        val iconSize = dp(22f).toInt()
        val left = (cx - iconSize / 2f).toInt()
        val top = (cy - iconSize / 2f).toInt()
        drawable.setBounds(left, top, left + iconSize, top + iconSize)

        canvas.save()
        if (!isStart) {
            // 水平镜像翻转：以 (cx, cy) 为中心进行 (-1f, 1f) 镜像翻转！
            canvas.scale(-1f, 1f, cx, cy)
        }
        drawable.draw(canvas)
        canvas.restore()
    }

    private fun drawRingSectorHighlight(canvas: Canvas, cx: Float, cy: Float, innerR: Float, outerR: Float, startAngle: Float, fillPaint: android.graphics.Paint) {
        val path = android.graphics.Path().apply {
            val outerRect = android.graphics.RectF(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
            val innerRect = android.graphics.RectF(cx - innerR, cy - innerR, cx + innerR, cy + innerR)
            arcTo(outerRect, startAngle, 90f, false)
            arcTo(innerRect, startAngle + 90f, -90f, false)
            close()
        }
        canvas.drawPath(path, fillPaint)
    }

    private fun drawTextEditKey(canvas: Canvas, softKey: SoftKey, keyXMargin: Int, keyYMargin: Int) {
        val centerX = width * 0.27f
        val centerY = height * 0.39f
        val outerRadius = kotlin.math.min(width * 0.46f, height * 0.68f) / 2f
        val innerRadius = outerRadius * 0.40f
        val arrowDist = (outerRadius + innerRadius) / 2f

        val strokePaint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = (height * 0.002f).coerceAtLeast(1.5f)
            color = (mActiveTheme.keyTextColor and 0x00FFFFFF) or 0x44000000
            isAntiAlias = true
        }

        val fillPaint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = android.graphics.Paint().apply {
            color = mActiveTheme.keyTextColor
            typeface = Typeface.DEFAULT_BOLD
            textSize = height * 0.04f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        when (softKey.code) {
            InputModeSwitcher.USER_KEYCODE_SELECT_MODE -> { // 中央内圆 [18%, 29%, 18%, 30%]
                val isSelected = mService?.hasSelection == true || softKey.pressed
                val circleColor = if (isSelected) mActiveTheme.accentKeyBackgroundColor else mActiveTheme.keyBackgroundColor
                fillPaint.color = circleColor
                canvas.drawCircle(centerX, centerY, innerRadius, fillPaint)
                canvas.drawCircle(centerX, centerY, innerRadius, strokePaint)

                // 中央大圆盘外圆外框 [4%, 10%, 46%, 68%]
                canvas.drawCircle(centerX, centerY, outerRadius, strokePaint)

                // 绘制 4 条 45 度斜对角线，将圆盘环形区切割为上下左右 4 个 90 度环形扇区
                for (angle in listOf(45f, 135f, 225f, 315f)) {
                    val rad = Math.toRadians(angle.toDouble())
                    val x1 = (centerX + innerRadius * kotlin.math.cos(rad)).toFloat()
                    val y1 = (centerY + innerRadius * kotlin.math.sin(rad)).toFloat()
                    val x2 = (centerX + outerRadius * kotlin.math.cos(rad)).toFloat()
                    val y2 = (centerY + outerRadius * kotlin.math.sin(rad)).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, strokePaint)
                }

                textPaint.color = if (isSelected) mActiveTheme.accentKeyTextColor else mActiveTheme.keyTextColor
                val fontMetrics = textPaint.fontMetrics
                val textY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText("选择", centerX, textY, textPaint)
            }

            KeyEvent.KEYCODE_DPAD_UP -> { // 上箭头
                val arrowX = centerX
                val arrowY = centerY - arrowDist
                if (softKey.pressed) {
                    fillPaint.color = mActiveTheme.keyPressHighlightColor
                    drawRingSectorHighlight(canvas, centerX, centerY, innerRadius, outerRadius, 225f, fillPaint)
                }
                drawChevronArrow(canvas, arrowX, arrowY, dp(6f), 180f, mActiveTheme.keyTextColor)
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> { // 下箭头
                val arrowX = centerX
                val arrowY = centerY + arrowDist
                if (softKey.pressed) {
                    fillPaint.color = mActiveTheme.keyPressHighlightColor
                    drawRingSectorHighlight(canvas, centerX, centerY, innerRadius, outerRadius, 45f, fillPaint)
                }
                drawChevronArrow(canvas, arrowX, arrowY, dp(6f), 0f, mActiveTheme.keyTextColor)
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> { // 左箭头
                val arrowX = centerX - arrowDist
                val arrowY = centerY
                if (softKey.pressed) {
                    fillPaint.color = mActiveTheme.keyPressHighlightColor
                    drawRingSectorHighlight(canvas, centerX, centerY, innerRadius, outerRadius, 135f, fillPaint)
                }
                drawChevronArrow(canvas, arrowX, arrowY, dp(6f), 90f, mActiveTheme.keyTextColor)
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> { // 右箭头
                val arrowX = centerX + arrowDist
                val arrowY = centerY
                if (softKey.pressed) {
                    fillPaint.color = mActiveTheme.keyPressHighlightColor
                    drawRingSectorHighlight(canvas, centerX, centerY, innerRadius, outerRadius, 315f, fillPaint)
                }
                drawChevronArrow(canvas, arrowX, arrowY, dp(6f), -90f, mActiveTheme.keyTextColor)
            }

            InputModeSwitcher.USER_KEYCODE_MOVE_START, InputModeSwitcher.USER_KEYCODE_MOVE_END -> { // 底部 |＜ 和 ＞| 按钮：增大尺寸，箭头与竖线无缝连结
                val isStart = softKey.code == InputModeSwitcher.USER_KEYCODE_MOVE_START
                val cx = if (isStart) width * 0.10f else width * 0.42f
                val cy = (softKey.mTop + softKey.mBottom) / 2f
                val btnR = dp(21f) // 增大按钮尺寸

                val btnBgColor = if (softKey.pressed) mActiveTheme.keyPressHighlightColor else mActiveTheme.keyBackgroundColor
                fillPaint.color = btnBgColor
                canvas.drawCircle(cx, cy, btnR, fillPaint)
                canvas.drawCircle(cx, cy, btnR, strokePaint)

                drawJumpArrow(canvas, cx, cy, isStart, mActiveTheme.keyTextColor)
            }

            else -> { // 右侧 3x2 网格区按键
                drawSoftKey(canvas, softKey, dp(2f).toInt(), dp(2f).toInt())
            }
        }
    }

    private fun getSelectedRegionKeys(selected: SoftKey): List<SoftKey> {
        if (isShiftKey(selected) || isDeleteKey(selected) || isClearKey(selected) ||
            isReturnKey(selected) || isAtKey(selected) || isDotKey(selected) ||
            isSpaceKey(selected) || isEnterKey(selected) || isSymbolKey(selected) || isNumberKey(selected)) {
            return listOf(selected)
        }

        val keys = mutableListOf<SoftKey>()
        val skb = mSoftKeyboard ?: return keys
        val selectedIsAlpha = isAlphaInputKey(selected)
        val selectedIsSide = isSideSymbolKey(selected)
        val selectedIsDel = isDeleteKey(selected)
        val selectedIsClear = isClearKey(selected)
        val selectedIsShift = isShiftKey(selected)
        val selectedIsReturn = isReturnKey(selected)
        val selectedIsAt = isAtKey(selected)
        val selectedIsDot = isDotKey(selected)
        val selectedIsSym = isSymbolKey(selected)
        val selectedIsNum = isNumberKey(selected)
        val selectedIsSpace = isSpaceKey(selected)
        val selectedIsEnter = isEnterKey(selected)

        for (row in skb.mKeyRows) {
            for (key in row) {
                val isMatch = when {
                    selectedIsAlpha -> isAlphaInputKey(key)
                    selectedIsSide -> isSideSymbolKey(key)
                    selectedIsDel -> isDeleteKey(key)
                    selectedIsClear -> isClearKey(key)
                    selectedIsShift -> isShiftKey(key)
                    selectedIsReturn -> isReturnKey(key)
                    selectedIsAt -> isAtKey(key)
                    selectedIsDot -> isDotKey(key)
                    selectedIsSym -> isSymbolKey(key)
                    selectedIsNum -> isNumberKey(key)
                    selectedIsSpace -> isSpaceKey(key)
                    selectedIsEnter -> isEnterKey(key)
                    else -> key == selected
                }
                if (isMatch) keys.add(key)
            }
        }
        return keys
    }

    private fun drawBlackAndWhiteMarqueeBox(canvas: Canvas, keys: List<SoftKey>) {
        if (keys.isEmpty()) return

        val strokeHalfWidth = dp(1.5f)
        val padding = dp(2.5f)
        val r = (keyRadius.toFloat() + padding).coerceAtLeast(dp(6f))
        val marqueePath = Path()

        val maxAllowedX = width.toFloat() - strokeHalfWidth
        val minAllowedY = strokeHalfWidth
        val maxAllowedY = height.toFloat() - strokeHalfWidth

        // 使用直角矩形合并，彻底消灭按键交叉中心夹角处留下的星形内孔与小黑点！
        for (k in keys) {
            val rectF = android.graphics.RectF(
                (k.mLeft - padding).coerceAtLeast(strokeHalfWidth),
                (k.mTop - padding).coerceAtLeast(minAllowedY),
                (k.mRight + padding).coerceAtMost(maxAllowedX),
                (k.mBottom + padding).coerceAtMost(maxAllowedY),
            )
            val subPath = Path().apply {
                addRect(rectF, Path.Direction.CW)
            }
            marqueePath.op(subPath, Path.Op.UNION)
        }

        val dashWidth = dp(5f)
        val dashGap = dp(5f)

        // 使用 PathEffect.chainPathEffect 为最外廓整体大框架外拐角平滑磨圆
        val cornerEffect = android.graphics.CornerPathEffect(r)

        val blackPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.5f)
            color = Color.BLACK
            pathEffect = android.graphics.ComposePathEffect(
                DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f),
                cornerEffect
            )
            isAntiAlias = true
        }

        val whitePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.5f)
            color = Color.WHITE
            pathEffect = android.graphics.ComposePathEffect(
                DashPathEffect(floatArrayOf(dashWidth, dashGap), dashWidth),
                cornerEffect
            )
            isAntiAlias = true
        }

        canvas.drawPath(marqueePath, blackPaint)
        canvas.drawPath(marqueePath, whitePaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closing()
    }

    override fun closing() {
        super.closing()
        mBuffer = null
        mCanvas = null
    }
}
