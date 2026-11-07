package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.service.VoiceRecognitionHandler
import com.yuyan.imemodule.speech.RecognitionState
import com.yuyan.imemodule.ui.keyboard.VoiceKeyboardLayout

@SuppressLint("ViewConstructor")
class VoiceKeyboardContainer(context: Context, inputView: InputView) : BaseContainer(context, inputView) {

    private var voiceState by mutableStateOf(RecognitionState.IDLE)
    private var voiceAmplitude by mutableFloatStateOf(0f)
    private var voiceSpectrum by mutableStateOf(FloatArray(16))

    private val voiceHandler = VoiceRecognitionHandler(
        context = context,
        onStateChanged = { state ->
            voiceState = state.voiceRecognitionState
        },
        getState = { com.yuyan.imemodule.service.VoiceState(voiceRecognitionState = voiceState) },
        getInputConnection = { inputView.imeService.currentInputConnection },
        onVoiceComplete = {},
        onAmplitudeChanged = { amp -> voiceAmplitude = amp },
        onSpectrumChanged = { spec -> voiceSpectrum = spec }
    )

    private var isVoiceActive = false

    init {
        voiceHandler.initialize()

        val composeView = ComposeView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setViewTreeLifecycleOwner(inputView.imeService)
            setViewTreeSavedStateRegistryOwner(inputView.imeService)
            setViewTreeViewModelStoreOwner(inputView.imeService)
            setContent {
                val isDark = ThemeManager.activeTheme.isDark
                VoiceKeyboardLayout(
                    isDarkTheme = isDark,
                    recognitionState = voiceState,
                    amplitude = voiceAmplitude,
                    spectrum = voiceSpectrum,
                    onDeleteClick = {
                        inputView.imeService.deleteSurroundingText(1, 0)
                    },
                    onSendClick = {
                        voiceHandler.sealPendingForSend()
                        inputView.imeService.sendEnterKeyEvent()
                    },
                    onStartVoice = {
                        val ctx = context
                        if (com.hjq.permissions.XXPermissions.isGranted(ctx, com.hjq.permissions.Permission.RECORD_AUDIO)) {
                            isVoiceActive = true
                            voiceHandler.startRecognition()
                        } else {
                            try {
                                val intent = android.content.Intent(ctx, com.yuyan.imemodule.ui.activity.LauncherActivity::class.java).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(intent)
                                android.widget.Toast.makeText(ctx, "使用语音功能前，请先在权限列表中勾选麦克风录音权限", android.widget.Toast.LENGTH_LONG).show()
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(ctx, "请先授予应用麦克风录音权限以开启语音识别", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onStopVoice = {
                        isVoiceActive = false
                        voiceHandler.finishRecognition()
                    },
                    onCursorMove = { direction ->
                        val keyCode = if (direction < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
                        inputView.imeService.sendCombinationKeyEvents(keyCode)
                    }
                )
            }
        }
        addView(composeView)
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        ev?.let { event ->
            val w = width.toFloat()
            val h = height.toFloat()
            if (h > 0 && w > 0 && event.y > h * 0.40f && event.x < w * 0.80f) {
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        val ctx = context
                        if (com.hjq.permissions.XXPermissions.isGranted(ctx, com.hjq.permissions.Permission.RECORD_AUDIO)) {
                            if (!isVoiceActive) {
                                isVoiceActive = true
                                voiceHandler.startRecognition()
                            }
                        }
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        if (isVoiceActive) {
                            isVoiceActive = false
                            voiceHandler.finishRecognition()
                        }
                    }
                }
            } else if (isVoiceActive && (event.actionMasked == android.view.MotionEvent.ACTION_UP || event.actionMasked == android.view.MotionEvent.ACTION_CANCEL)) {
                isVoiceActive = false
                voiceHandler.finishRecognition()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        voiceHandler.release()
    }
}
