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
                        inputView.imeService.sendCombinationKeyEvents(KeyEvent.KEYCODE_DEL)
                    },
                    onSendClick = {
                        voiceHandler.sealPendingForSend()
                        inputView.imeService.sendEnterKeyEvent()
                    },
                    onStartVoice = {
                        val ctx = inputView.context
                        if (com.hjq.permissions.XXPermissions.isGranted(ctx, com.hjq.permissions.Permission.RECORD_AUDIO)) {
                            voiceHandler.startRecognition()
                        } else {
                            com.hjq.permissions.XXPermissions.with(ctx)
                                .permission(com.hjq.permissions.Permission.RECORD_AUDIO)
                                .request { _, allGranted ->
                                    if (allGranted) {
                                        voiceHandler.startRecognition()
                                    } else {
                                        android.widget.Toast.makeText(ctx, "请先授予麦克风录音权限以开启语音识别", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
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
