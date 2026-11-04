package com.k2fsa.sherpa.onnx

import android.util.Log
import androidx.annotation.Keep

@Keep
class OnlineRecognizer(val config: OnlineRecognizerConfig) {

    private var ptr: Long = 0L

    init {
        try {
            loadNativeLibraries()
            ptr = newOnlineRecognizer(config)
        } catch (e: Throwable) {
            Log.e("X_ASR_NATIVE", "newOnlineRecognizer failed: ${e.message}", e)
            ptr = 0L
        }
    }

    fun isNativeReady(): Boolean = ptr != 0L

    fun createStream(): OnlineStream? {
        if (ptr == 0L) return null
        return try {
            val streamPtr = createOnlineStream(ptr)
            if (streamPtr != 0L) OnlineStream(streamPtr) else null
        } catch (e: Throwable) {
            Log.e("X_ASR_NATIVE", "createOnlineStream failed: ${e.message}", e)
            null
        }
    }

    fun isReady(stream: OnlineStream): Boolean {
        if (ptr == 0L || stream.ptr == 0L) return false
        return try {
            isReady(ptr, stream.ptr)
        } catch (_: Throwable) {
            false
        }
    }

    fun decode(stream: OnlineStream) {
        if (ptr == 0L || stream.ptr == 0L) return
        try {
            decode(ptr, stream.ptr)
        } catch (e: Throwable) {
            Log.e("X_ASR_NATIVE", "decode failed: ${e.message}", e)
        }
    }

    fun getResult(stream: OnlineStream): OnlineRecognizerResult {
        if (ptr == 0L || stream.ptr == 0L) return OnlineRecognizerResult()

        return try {
            val res = getResult(ptr, stream.ptr)
            when (res) {
                is OnlineRecognizerResult -> res
                is Array<*> -> {
                    val t = res.getOrNull(0) as? String ?: ""
                    OnlineRecognizerResult(text = t)
                }
                is String -> OnlineRecognizerResult(text = res)
                else -> OnlineRecognizerResult()
            }
        } catch (e: Throwable) {
            Log.e("X_ASR_NATIVE", "getResult JNI call failed: ${e.message}", e)
            OnlineRecognizerResult()
        }
    }

    fun isEndpoint(stream: OnlineStream): Boolean {
        if (ptr == 0L || stream.ptr == 0L) return false
        return try {
            isEndpoint(ptr, stream.ptr)
        } catch (_: Throwable) {
            false
        }
    }

    fun reset(stream: OnlineStream) {
        if (ptr == 0L || stream.ptr == 0L) return
        try {
            reset(ptr, stream.ptr)
        } catch (_: Throwable) {}
    }

    fun release() {
        if (ptr != 0L) {
            try {
                deleteOnlineRecognizer(ptr)
            } catch (_: Throwable) {}
            ptr = 0L
        }
    }

    protected fun finalize() {
        release()
    }

    private external fun newOnlineRecognizer(config: OnlineRecognizerConfig): Long
    private external fun deleteOnlineRecognizer(ptr: Long)
    private external fun createOnlineStream(ptr: Long): Long
    private external fun isReady(ptr: Long, streamPtr: Long): Boolean
    private external fun decode(ptr: Long, streamPtr: Long)
    private external fun getResult(ptr: Long, streamPtr: Long): Any?
    private external fun isEndpoint(ptr: Long, streamPtr: Long): Boolean
    private external fun reset(ptr: Long, streamPtr: Long)

    companion object {
        private var librariesLoaded = false

        fun loadNativeLibraries() {
            if (librariesLoaded) return
            try {
                System.loadLibrary("onnxruntime")
                System.loadLibrary("sherpa-onnx-c-api")
                System.loadLibrary("sherpa-onnx-jni")
                librariesLoaded = true
            } catch (e: Throwable) {
                Log.e("X_ASR_NATIVE", "Failed to load Sherpa-onnx native libraries: ${e.message}", e)
            }
        }

        init {
            loadNativeLibraries()
        }
    }
}
