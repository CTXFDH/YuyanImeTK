package com.yuyan.imemodule.prefs.behavior

import com.yuyan.imemodule.view.preference.ManagedPreference

enum class VoiceInputTriggerMode {
    HOLD_TO_TALK,
    CLICK_TO_TOGGLE;

    companion object : ManagedPreference.StringLikeCodec<VoiceInputTriggerMode> {
        override fun decode(raw: String): VoiceInputTriggerMode =
            entries.firstOrNull { it.name == raw } ?: HOLD_TO_TALK
    }
}
