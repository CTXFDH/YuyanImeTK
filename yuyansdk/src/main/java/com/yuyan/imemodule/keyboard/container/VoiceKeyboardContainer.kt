package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
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

    private var isVoiceActive by mutableStateOf(false)

    private val voiceHandler = VoiceRecognitionHandler(
        context = context,
        onStateChanged = { state ->
            voiceState = state.voiceRecognitionState
            if (state.voiceRecognitionState == RecognitionState.ERROR) {
                isVoiceActive = false
                val isModelReady = com.yuyan.imemodule.speech.AsrModelManager(context).isModelReady()
                val msg = if (isModelReady) "语音引擎启动异常，请重试" else "未导入离线语音模型：请在【设置-方案与数据-语音识别设置】中导入模型包"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        },
        getState = { com.yuyan.imemodule.service.VoiceState(voiceRecognitionState = voiceState) },
        getInputConnection = { inputView.imeService.currentInputConnection },
        onVoiceComplete = { isVoiceActive = false },
        onAmplitudeChanged = { amp -> voiceAmplitude = amp },
        onSpectrumChanged = { spec -> voiceSpectrum = spec }
    )

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
                    isVoicePressed = isVoiceActive,
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
                        startVoiceInternal()
                    },
                    onStopVoice = {
                        stopVoiceInternal()
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

    private fun startVoiceInternal() {
        val ctx = context
        if (com.hjq.permissions.XXPermissions.isGranted(ctx, com.hjq.permissions.Permission.RECORD_AUDIO)) {
            if (!isVoiceActive) {
                isVoiceActive = true
                voiceHandler.startRecognition()
            }
        } else {
            try {
                com.hjq.permissions.XXPermissions.startPermissionActivity(ctx, com.hjq.permissions.Permission.RECORD_AUDIO)
                android.widget.Toast.makeText(ctx, "使用语音识别前，请先开启【麦克风录音权限】", android.widget.Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                android.widget.Toast.makeText(ctx, "请先在系统应用设置中开启语燕输入法的【麦克风权限】", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun stopVoiceInternal() {
        if (isVoiceActive) {
            isVoiceActive = false
            voiceHandler.finishRecognition()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        ev?.let { event ->
            val w = width.toFloat()
            val h = height.toFloat()
            if (h > 0 && w > 0 && event.y > h * 0.40f && event.x < w * 0.80f) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startVoiceInternal()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        stopVoiceInternal()
                    }
                }
            } else if (isVoiceActive && (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL)) {
                stopVoiceInternal()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        voiceHandler.release()
    }
}
