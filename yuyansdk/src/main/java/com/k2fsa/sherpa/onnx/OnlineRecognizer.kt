package com.k2fsa.sherpa.onnx

class OnlineRecognizer(val config: OnlineRecognizerConfig) {

    private var ptr: Long = 0L

    init {
        try {
            System.loadLibrary("sherpa-onnx-jni")
            ptr = newOnlineRecognizer(config)
        } catch (_: Throwable) {
            ptr = 0L
        }
    }

    fun isNativeReady(): Boolean = ptr != 0L

    fun createStream(): OnlineStream? {
        if (ptr == 0L) return null
        return try {
            val streamPtr = createOnlineStream(ptr)
            if (streamPtr != 0L) OnlineStream(streamPtr) else null
        } catch (_: Throwable) {
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
        } catch (_: Throwable) {}
    }

    fun getResult(stream: OnlineStream): OnlineRecognizerResult {
        if (ptr == 0L || stream.ptr == 0L) return OnlineRecognizerResult()
        return try {
            val rawText = getResultText(ptr, stream.ptr) ?: ""
            OnlineRecognizerResult(text = rawText)
        } catch (_: Throwable) {
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
    private external fun getResultText(ptr: Long, streamPtr: Long): String?
    private external fun isEndpoint(ptr: Long, streamPtr: Long): Boolean
    private external fun reset(ptr: Long, streamPtr: Long)

    companion object {
        init {
            try {
                System.loadLibrary("sherpa-onnx-jni")
            } catch (_: Throwable) {}
        }
    }
}
