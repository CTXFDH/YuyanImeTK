package com.kingzcheung.xime.speech

import androidx.annotation.Keep

@Keep
object AsrNative {
    init {
        try {
            System.loadLibrary("onnxruntime")
            System.loadLibrary("asr_jni")
        } catch (_: Throwable) {}
    }

    external fun nativeCreate(
        encoder: String,
        decoder: String,
        joiner: String,
        tokens: String
    ): Long

    external fun nativeReset(handle: Long)

    external fun nativeAcceptPcm(handle: Long, pcm: ByteArray)

    external fun nativeGetPartial(handle: Long): String

    external fun nativeFinalize(handle: Long): String

    external fun nativeRelease(handle: Long)
}
