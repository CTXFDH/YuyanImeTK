package com.yuyan.imemodule.prefs

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.view.HapticFeedbackConstants
import android.view.View
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.utils.audioManager
import com.yuyan.imemodule.utils.vibrator

object InputFeedbacks {

    private val soundOnKeyPress by AppPrefs.getInstance().internal.soundOnKeyPress
    private val vibrationAmplitude by AppPrefs.getInstance().internal.vibrationAmplitude

    private val soundEffectStandardPref = AppPrefs.getInstance().internal.soundEffectStandard
    private val soundEffectSpaceBarPref = AppPrefs.getInstance().internal.soundEffectSpaceBar
    private val soundEffectDeletePref = AppPrefs.getInstance().internal.soundEffectDelete
    private val soundEffectReturnPref = AppPrefs.getInstance().internal.soundEffectReturn

    private val vibrator = Launcher.instance.context.vibrator
    private val hasAmplitudeControl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()
    private val audioManager = Launcher.instance.context.audioManager

    private val soundPool: SoundPool by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    private val soundMap = mutableMapOf<String, Int>()

    private fun loadSoundFromAssets(fileName: String): Int {
        return soundMap.getOrPut(fileName) {
            try {
                val afd = Launcher.instance.context.assets.openFd("sound/$fileName")
                soundPool.load(afd, 1)
            } catch (e: Exception) {
                0
            }
        }
    }

    fun hapticFeedback(view: View) {
        val duration = when (vibrationAmplitude) {
            0 -> {
                @Suppress("DEPRECATION")
                val flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, flags)
                0L
            }
            1 -> 0L
            2 -> 5L
            3 -> 30L
            else -> 50L
        }
        if (duration != 0L) {
            if (hasAmplitudeControl) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }

    enum class SoundEffect {
        Standard, SpaceBar, Delete, Return
    }

    fun soundEffect(effect: SoundEffect) {
        val volume = when {
            soundOnKeyPress < 5 -> -1f
            soundOnKeyPress < 10 -> return
            else -> (soundOnKeyPress - 10) / 30f
        }

        val choice = when (effect) {
            SoundEffect.Standard -> soundEffectStandardPref.getValue()
            SoundEffect.SpaceBar -> soundEffectSpaceBarPref.getValue()
            SoundEffect.Delete -> soundEffectDeletePref.getValue()
            SoundEffect.Return -> soundEffectReturnPref.getValue()
        }

        val customFile = when (choice) {
            "微信点击", "微信默认点击" -> "wx_click.wav"
            "微信空格", "微信空格音效" -> "wx_click.wav"
            "微信删除", "微信删除音效" -> "wx_delete.wav"
            "微信回车", "微信回车/动作音效" -> "wx_action.wav"

            "百度红轴" -> "bd_axis_red.ogg"
            "百度青轴" -> "bd_axis_green.ogg"
            "百度茶轴" -> "bd_axis_tea.ogg"
            "百度机械" -> "bd_axis_mechanical.ogg"
            "百度黑莓" -> "bd_device_blackberry.ogg"
            "百度轻敲" -> "bd_device_tap.ogg"
            "百度可爱" -> "bd_style_cute.ogg"
            "百度卡通" -> "bd_style_cartoon.ogg"
            "百度快板" -> "bd_style_allegro.ogg"
            "百度经典" -> "bd_classic_click.ogg"
            "百度动作" -> "bd_classic_func.ogg"
            else -> null
        }

        if (customFile != null) {
            val soundId = loadSoundFromAssets(customFile)
            if (soundId != 0) {
                val playVol = if (volume < 0f) 0.8f else volume
                soundPool.play(soundId, playVol, playVol, 1, 0, 1.0f)
                return
            }
        }

        val fx = when (effect) {
            SoundEffect.Standard -> AudioManager.FX_KEYPRESS_STANDARD
            SoundEffect.SpaceBar -> AudioManager.FX_KEYPRESS_SPACEBAR
            SoundEffect.Delete -> AudioManager.FX_KEYPRESS_DELETE
            SoundEffect.Return -> AudioManager.FX_KEYPRESS_RETURN
        }
        audioManager.playSoundEffect(fx, volume)
    }

}
