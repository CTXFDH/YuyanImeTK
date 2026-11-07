package com.kingzcheung.xime.speech

/**
 * JNI bridge matching compiled libasr_jni.so native method signatures.
 */
object AsrNative {
    init {
        try {
            System.loadLibrary("asr_jni")
        } catch (e: Throwable) {
            android.util.Log.e("AsrNative", "Failed to load asr_jni library", e)
        }
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
