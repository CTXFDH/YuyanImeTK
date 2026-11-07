package com.yuyan.imemodule.speech

object AsrNative {
    fun nativeCreate(encoder: String, decoder: String, joiner: String, tokens: String): Long {
        return try {
            com.kingzcheung.xime.speech.AsrNative.nativeCreate(encoder, decoder, joiner, tokens)
        } catch (e: Throwable) {
            android.util.Log.e("AsrNative", "nativeCreate failed in wrapper", e)
            0L
        }
    }

    fun nativeReset(handle: Long) =
        com.kingzcheung.xime.speech.AsrNative.nativeReset(handle)

    fun nativeAcceptPcm(handle: Long, pcm: ByteArray) =
        com.kingzcheung.xime.speech.AsrNative.nativeAcceptPcm(handle, pcm)

    fun nativeGetPartial(handle: Long): String =
        com.kingzcheung.xime.speech.AsrNative.nativeGetPartial(handle)

    fun nativeFinalize(handle: Long): String =
        com.kingzcheung.xime.speech.AsrNative.nativeFinalize(handle)

    fun nativeRelease(handle: Long) =
        com.kingzcheung.xime.speech.AsrNative.nativeRelease(handle)
}
