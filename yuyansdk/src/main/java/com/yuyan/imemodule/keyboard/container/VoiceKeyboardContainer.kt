package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.voice.AsrModelManager
import com.yuyan.imemodule.voice.SpeechRecognitionManager
import splitties.dimensions.dp

/**
 * 1:1 极简纯净复刻曦码输入法 (Xime) 风格的语音识别键盘界面
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class VoiceKeyboardContainer(context: Context?, inputView: InputView) : InputBaseContainer(context, inputView) {

    private val validContext = context ?: Launcher.instance.context
    private val speechManager = SpeechRecognitionManager(validContext)

    private var tvSubtitle: TextView? = null
    private var rainbowWaveView: RainbowWaveView? = null
    private var tvDomeInstruction: TextView? = null
    private var bottomDomeView: View? = null

    init {
        initXimeVoiceLayout()
        initSpeechManager()
    }

    override fun updateStates() {
        // 语音专属界面无标准 TextKeyboard 视图
    }

    private fun initXimeVoiceLayout() {
        removeAllViews()
        val theme = ThemeManager.activeTheme
        val isDark = theme.isDark
        val mainBgColor = if (isDark) Color.parseColor("#20262C") else Color.parseColor("#E6ECF2")
        val circleBtnBgColor = if (isDark) Color.parseColor("#2D353F") else Color.parseColor("#FFFFFF")
        val textColor = if (isDark) Color.parseColor("#E5E7EB") else Color.parseColor("#1F2937")
        val subTextColor = if (isDark) Color.parseColor("#9CA3AF") else Color.parseColor("#6B7280")

        val rootLayout = LinearLayout(validContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(mainBgColor)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // 1. 顶部 Header (仅保留居中模型名称字幕)
        val headerBar = RelativeLayout(validContext).apply {
            setPadding(dp(16), dp(12), dp(16), dp(4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40))
        }

        val isOfflineModelReady = AsrModelManager.isModelFilesPresent(validContext)
        val modelTitle = if (isOfflineModelReady) "中文 Zipformer int8" else "中文 语音转文本"
        tvSubtitle = TextView(validContext).apply {
            text = modelTitle
            textSize = 14f
            setTextColor(subTextColor)
            gravity = Gravity.CENTER
            val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            layoutParams = lp
        }

        headerBar.addView(tvSubtitle)
        rootLayout.addView(headerBar)

        // 2. 中间控制区域 (撤销/退格圆形按钮 + 曦码彩虹彩籽波形胶囊 + 发送/上屏圆形按钮)
        val centerRow = LinearLayout(validContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64))
        }

        // 撤销/退格按钮 (↶) - 纯圆正圆形按钮
        val btnUndo = ImageView(validContext).apply {
            setImageResource(R.drawable.ic_menu_reset)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(circleBtnBgColor)
            }
            drawable?.mutate()?.setTint(textColor)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dp(54), dp(54)).apply {
                rightMargin = dp(16)
            }
            setOnClickListener {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                inputView.responseKeyEvent(SoftKey(code = KeyEvent.KEYCODE_DEL, label = "⌫"))
            }
        }

        // 曦码彩虹彩籽动态胶囊条
        rainbowWaveView = RainbowWaveView(validContext).apply {
            layoutParams = LinearLayout.LayoutParams(dp(190), dp(50))
        }

        // 发送/上屏按钮 (↑) - 纯圆正圆形按钮
        val btnSend = ImageView(validContext).apply {
            setImageResource(R.drawable.ic_arrow_up_pretty)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(circleBtnBgColor)
            }
            drawable?.mutate()?.setTint(textColor)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dp(54), dp(54)).apply {
                leftMargin = dp(16)
            }
            setOnClickListener {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                inputView.responseKeyEvent(SoftKey(code = InputModeSwitcher.USER_KEYCODE_RETURN, label = "返回"))
            }
        }

        centerRow.addView(btnUndo)
        centerRow.addView(rainbowWaveView)
        centerRow.addView(btnSend)
        rootLayout.addView(centerRow)

        // 3. 底部 1:1 巨型半圆弧拱形触摸区域 (曦码 Dome 弧边设计)
        val domeContainer = FrameLayout(validContext).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val domeBgColor = if (isDark) Color.parseColor("#2C343E") else Color.parseColor("#F2F5F8")
        bottomDomeView = View(validContext).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    dp(160f).toFloat(), dp(160f).toFloat(), // Top-Left
                    dp(160f).toFloat(), dp(160f).toFloat(), // Top-Right
                    0f, 0f,                                  // Bottom-Right
                    0f, 0f                                   // Bottom-Left
                )
                setColor(domeBgColor)
            }
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        tvDomeInstruction = TextView(validContext).apply {
            text = "按住说话"
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            gravity = Gravity.CENTER
            val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                bottomMargin = dp(12)
            }
            layoutParams = lp
        }

        // 底部两侧辅助图标 (左下角折叠 ∨, 右下角键盘 ⌨)
        val bottomIconsRow = RelativeLayout(validContext).apply {
            setPadding(dp(24), 0, dp(24), dp(12))
            val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM
            }
            layoutParams = lp
        }

        val ivBottomClose = ImageView(validContext).apply {
            setImageResource(R.drawable.ic_arrow_down_pretty)
            drawable?.mutate()?.setTint(subTextColor)
            alpha = 0.5f
            val lp = RelativeLayout.LayoutParams(dp(20), dp(20)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            layoutParams = lp
            setOnClickListener {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                inputView.mSkbCandidatesBarView.visibility = View.VISIBLE
                KeyboardManager.instance.switchKeyboardToMain()
            }
        }

        val ivBottomKeyboard = ImageView(validContext).apply {
            setImageResource(R.drawable.ic_menu_keyboard)
            drawable?.mutate()?.setTint(subTextColor)
            alpha = 0.5f
            val lp = RelativeLayout.LayoutParams(dp(20), dp(20)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            layoutParams = lp
            setOnClickListener {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                inputView.mSkbCandidatesBarView.visibility = View.VISIBLE
                KeyboardManager.instance.switchKeyboardToMain()
            }
        }

        bottomIconsRow.addView(ivBottomClose)
        bottomIconsRow.addView(ivBottomKeyboard)

        domeContainer.addView(bottomDomeView)
        domeContainer.addView(tvDomeInstruction)
        domeContainer.addView(bottomIconsRow)

        // 全弧面长按手势监听器 (按住说话，松开结束)
        domeContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    DevicesUtils.tryPlayKeyDown()
                    DevicesUtils.tryVibrate(this)
                    updateVoiceRecordingState(isRecording = true)
                    speechManager.startListening()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    updateVoiceRecordingState(isRecording = false)
                    speechManager.stopListening()
                    true
                }
                else -> false
            }
        }

        rootLayout.addView(domeContainer)
        addView(rootLayout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun updateVoiceRecordingState(isRecording: Boolean) {
        val theme = ThemeManager.activeTheme
        val isDark = theme.isDark
        val activeDomeBg = if (isDark) Color.parseColor("#3B4552") else Color.parseColor("#DEE5EE")
        val idleDomeBg = if (isDark) Color.parseColor("#2C343E") else Color.parseColor("#F2F5F8")

        tvDomeInstruction?.text = if (isRecording) "松开结束" else "按住说话"

        bottomDomeView?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(
                dp(160f).toFloat(), dp(160f).toFloat(),
                dp(160f).toFloat(), dp(160f).toFloat(),
                0f, 0f,
                0f, 0f
            )
            setColor(if (isRecording) activeDomeBg else idleDomeBg)
        }
    }

    private fun initSpeechManager() {
        speechManager.listener = object : SpeechRecognitionManager.Listener {
            override fun onStateChanged(state: SpeechRecognitionManager.State) {
                when (state) {
                    SpeechRecognitionManager.State.LISTENING -> {
                        updateVoiceRecordingState(true)
                    }
                    SpeechRecognitionManager.State.PROCESSING, SpeechRecognitionManager.State.IDLE, SpeechRecognitionManager.State.ERROR -> {
                        updateVoiceRecordingState(false)
                        rainbowWaveView?.setAmplitude(0f)
                    }
                }
            }

            override fun onAmplitudeChanged(amplitude: Float) {
                rainbowWaveView?.setAmplitude(amplitude)
            }

            override fun onPartialResult(text: String) {
                if (text.isNotEmpty()) {
                    tvSubtitle?.text = text
                    // 边说边流式实时写进当前聊天框/编辑框！
                    inputView.responseKeyEvent(SoftKey(label = text))
                }
            }

            override fun onFinalResult(text: String) {
                if (text.isNotEmpty()) {
                    tvSubtitle?.text = text
                    // 松手瞬间最终确认并直接写进当前聊天框/编辑框！
                    inputView.commitClipboardText(text)
                }
            }

            override fun onError(errorMsg: String) {
                tvSubtitle?.text = "⚠️ $errorMsg"
            }
        }
    }

    override fun updateSkbLayout() {
        // 进入语音专属键盘时保持顶栏常显
        inputView.mSkbCandidatesBarView.visibility = View.VISIBLE
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        speechManager.destroy()
        inputView.mSkbCandidatesBarView.visibility = View.VISIBLE
    }

    /**
     * 1:1 复刻曦码彩虹彩籽动画胶囊条 (RainbowWaveView)
     */
    private class RainbowWaveView(context: Context) : View(context) {
        private var amplitude = 0f

        // 7 颗曦码彩虹圆籽色彩配置 (粉红、橙、金黄、浅绿、薄荷绿、天蓝、紫罗兰)
        private val rainbowColors = intArrayOf(
            Color.parseColor("#FF8A80"),
            Color.parseColor("#FFA726"),
            Color.parseColor("#FFCA28"),
            Color.parseColor("#81C784"),
            Color.parseColor("#4DB6AC"),
            Color.parseColor("#64B5F6"),
            Color.parseColor("#BA68C8")
        )

        private val paints = rainbowColors.map { colorVal ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorVal
                style = Paint.Style.FILL
            }
        }

        fun setAmplitude(amp: Float) {
            this.amplitude = amp.coerceIn(0f, 1f)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            val theme = ThemeManager.activeTheme
            val capsuleBgColor = if (theme.isDark) Color.parseColor("#2D353F") else Color.parseColor("#EBF0F5")

            // 1. 绘制底座平滑胶囊框
            val capsuleRect = RectF(0f, 0f, w, h)
            val capsuleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = capsuleBgColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(capsuleRect, h / 2f, h / 2f, capsuleBg)

            // 2. 绘制中间 7 颗动态波纹彩籽 (Rainbow Pills)
            val count = rainbowColors.size
            val centerY = h / 2f
            val maxDotHeight = h * 0.45f
            val minDotHeight = dp(6f).toFloat()

            val totalPadding = w * 0.22f
            val availableW = w - totalPadding * 2
            val spacing = availableW / (count - 1)

            for (i in 0 until count) {
                val cx = totalPadding + i * spacing
                val sineFactor = kotlin.math.sin((i + 1) * Math.PI / (count + 1)).toFloat()
                val currentHeight = (minDotHeight + (maxDotHeight - minDotHeight) * amplitude * sineFactor).coerceAtLeast(minDotHeight)
                val dotWidth = (currentHeight * 0.65f).coerceIn(dp(6f).toFloat(), dp(12f).toFloat())

                val rect = RectF(cx - dotWidth / 2f, centerY - currentHeight / 2f, cx + dotWidth / 2f, centerY + currentHeight / 2f)
                canvas.drawRoundRect(rect, dotWidth / 2f, dotWidth / 2f, paints[i])
            }
        }
    }
}
