package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.voice.SpeechRecognitionManager
import splitties.dimensions.dp

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class VoiceKeyboardContainer(context: Context?, inputView: InputView) : InputBaseContainer(context, inputView) {

    private val speechManager = SpeechRecognitionManager(context!!)
    private var waveformView: WaveformView? = null
    private var tvLiveText: TextView? = null
    private var tvStatus: TextView? = null
    private var ivMicBtn: ImageView? = null

    init {
        initVoiceLayout()
        initSpeechManager()
    }

    private fun initVoiceLayout() {
        removeAllViews()
        val theme = ThemeManager.activeTheme
        val accentColor = theme.toolbarActiveIconColor
        val keyBgColor = theme.keyBackgroundColor
        val keyTextColor = theme.keyTextColor

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // 1. 实时流式文本框与状态框
        val previewCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12f).toFloat()
                setColor(keyBgColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64)).apply {
                bottomMargin = dp(8)
            }
        }

        tvLiveText = TextView(context).apply {
            text = "正在准备语音识别..."
            textSize = 15f
            setTextColor(keyTextColor)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        previewCard.addView(tvLiveText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        rootLayout.addView(previewCard)

        // 2. 波形动画展示层
        waveformView = WaveformView(context!!).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(24)).apply {
                bottomMargin = dp(8)
            }
        }
        rootLayout.addView(waveformView)

        // 3. 中央控制区域 (麦克风巨型按钮 + 辅助修字按钮)
        val centerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(90)).apply {
                bottomMargin = dp(8)
            }
        }

        // 左侧标点修字面板
        val leftPunctuationCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(70), LinearLayout.LayoutParams.MATCH_PARENT)
        }
        leftPunctuationCol.addView(createPunctuationButton("，"), LinearLayout.LayoutParams(dp(60), dp(36)).apply { bottomMargin = dp(4) })
        leftPunctuationCol.addView(createPunctuationButton("。"), LinearLayout.LayoutParams(dp(60), dp(36)))

        // 中央巨型麦克风按钮
        val micFrame = FrameLayout(context!!).apply {
            layoutParams = LinearLayout.LayoutParams(dp(80), dp(80)).apply {
                leftMargin = dp(16)
                rightMargin = dp(16)
            }
        }

        ivMicBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_menu_voice)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentColor)
            }
            drawable?.mutate()?.setTint(Color.WHITE)
            isClickable = true
            isFocusable = true

            setOnClickListener {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                if (speechManager.currentState == SpeechRecognitionManager.State.LISTENING) {
                    speechManager.stopListening()
                } else {
                    speechManager.startListening()
                }
            }
        }
        micFrame.addView(ivMicBtn, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // 右侧退格/换行控制
        val rightControlCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(70), LinearLayout.LayoutParams.MATCH_PARENT)
        }
        rightControlCol.addView(createActionButton("⌫") {
            inputView.responseKeyEvent(SoftKey(code = android.view.KeyEvent.KEYCODE_DEL, label = "⌫"))
        }, LinearLayout.LayoutParams(dp(60), dp(36)).apply { bottomMargin = dp(4) })

        rightControlCol.addView(createActionButton("↵") {
            inputView.responseKeyEvent(SoftKey(code = InputModeSwitcher.USER_KEYCODE_RETURN, label = "返回"))
        }, LinearLayout.LayoutParams(dp(60), dp(36)))

        centerRow.addView(leftPunctuationCol)
        centerRow.addView(micFrame)
        centerRow.addView(rightControlCol)
        rootLayout.addView(centerRow)

        // 4. 底部状态与返回打字主键盘按钮
        val bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(36))
        }

        val btnBackToKeyboard = TextView(context).apply {
            text = "↩ 返回键盘"
            textSize = 13f
            setTextColor(keyTextColor)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16f).toFloat()
                setColor(keyBgColor)
            }
            setOnClickListener {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                speechManager.cancelListening()
                KeyboardManager.instance.switchKeyboardToMain()
            }
        }

        tvStatus = TextView(context).apply {
            text = "🟢 点击开始语音输入"
            textSize = 12f
            setTextColor(keyTextColor)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        bottomBar.addView(btnBackToKeyboard)
        bottomBar.addView(tvStatus)
        rootLayout.addView(bottomBar)

        addView(rootLayout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun createPunctuationButton(symbol: String): View {
        val theme = ThemeManager.activeTheme
        return TextView(context).apply {
            text = symbol
            textSize = 16f
            setTextColor(theme.keyTextColor)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8f).toFloat()
                setColor(theme.keyBackgroundColor)
            }
            setOnClickListener {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                inputView.responseKeyEvent(SoftKey(label = symbol))
            }
        }
    }

    private fun createActionButton(label: String, onClick: () -> Unit): View {
        val theme = ThemeManager.activeTheme
        return TextView(context).apply {
            text = label
            textSize = 15f
            setTextColor(theme.keyTextColor)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8f).toFloat()
                setColor(theme.keyBackgroundColor)
            }
            setOnClickListener {
                DevicesUtils.tryPlayKeyDown()
                DevicesUtils.tryVibrate(this)
                onClick()
            }
        }
    }

    private fun initSpeechManager() {
        speechManager.listener = object : SpeechRecognitionManager.Listener {
            override fun onStateChanged(state: SpeechRecognitionManager.State) {
                when (state) {
                    SpeechRecognitionManager.State.LISTENING -> {
                        tvStatus?.text = "🔴 正在倾听中..."
                        tvLiveText?.text = "请说话..."
                    }
                    SpeechRecognitionManager.State.PROCESSING -> {
                        tvStatus?.text = "🟡 正在识别处理..."
                    }
                    SpeechRecognitionManager.State.IDLE -> {
                        tvStatus?.text = "🟢 语音就绪，点击说话"
                        waveformView?.setAmplitude(0f)
                    }
                    SpeechRecognitionManager.State.ERROR -> {
                        tvStatus?.text = "⚠️ 识别出错，请重试"
                        waveformView?.setAmplitude(0f)
                    }
                }
            }

            override fun onAmplitudeChanged(amplitude: Float) {
                waveformView?.setAmplitude(amplitude)
            }

            override fun onPartialResult(text: String) {
                tvLiveText?.text = text
            }

            override fun onFinalResult(text: String) {
                tvLiveText?.text = text
                if (text.isNotEmpty()) {
                    inputView.responseKeyEvent(SoftKey(label = text))
                }
            }

            override fun onError(errorMsg: String) {
                tvStatus?.text = "⚠️ $errorMsg"
            }
        }
    }

    override fun updateSkbLayout() {
        speechManager.startListening()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        speechManager.destroy()
    }

    private class WaveformView(context: Context) : View(context) {
        private var amplitude = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ThemeManager.activeTheme.toolbarActiveIconColor
            style = Paint.Style.FILL
        }

        fun setAmplitude(amp: Float) {
            this.amplitude = amp.coerceIn(0f, 1f)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val barCount = 15
            val width = width.toFloat()
            val height = height.toFloat()
            if (width <= 0 || height <= 0) return

            val spacing = dp(4f).toFloat()
            val barWidth = (width - spacing * (barCount - 1)) / barCount
            val centerY = height / 2f

            for (i in 0 until barCount) {
                val factor = kotlin.math.sin((i + 1) * Math.PI / (barCount + 1)).toFloat()
                val barHeight = (height * 0.15f + height * 0.75f * amplitude * factor).coerceAtLeast(dp(3f).toFloat())
                val left = i * (barWidth + spacing)
                val rect = RectF(left, centerY - barHeight / 2f, left + barWidth, centerY + barHeight / 2f)
                canvas.drawRoundRect(rect, dp(2f).toFloat(), dp(2f).toFloat(), paint)
            }
        }
    }
}
