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

    init {
        voiceHandler.initialize()

        val composeView = ComposeView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setContent {
                val isDark = ThemeManager.activeTheme.isDark
                VoiceKeyboardLayout(
                    isDarkTheme = isDark,
                    recognitionState = voiceState,
                    amplitude = voiceAmplitude,
                    spectrum = voiceSpectrum,
                    onDeleteClick = {
                        inputView.imeService.sendCombinationKeyEvents(KeyEvent.KEYCODE_DEL)
                    },
                    onSendClick = {
                        voiceHandler.sealPendingForSend()
                        inputView.imeService.sendEnterKeyEvent()
                    },
                    onStartVoice = {
                        voiceHandler.startRecognition()
                    },
                    onStopVoice = {
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        voiceHandler.release()
    }
}
