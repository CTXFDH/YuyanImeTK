package com.k2fsa.sherpa.onnx

class OnlineStream(var ptr: Long) {

    fun acceptWaveform(samples: FloatArray, sampleRate: Int = 16000) {
        if (ptr == 0L) return
        try {
            acceptWaveform(ptr, samples, sampleRate)
        } catch (_: Throwable) {}
    }

    fun inputFinished() {
        if (ptr == 0L) return
        try {
            inputFinished(ptr)
        } catch (_: Throwable) {}
    }

    fun release() {
        if (ptr != 0L) {
            try {
                deleteOnlineStream(ptr)
            } catch (_: Throwable) {}
            ptr = 0L
        }
    }

    protected fun finalize() {
        release()
    }

    private external fun acceptWaveform(ptr: Long, samples: FloatArray, sampleRate: Int)
    private external fun inputFinished(ptr: Long)
    private external fun deleteOnlineStream(ptr: Long)

    companion object {
        init {
            try {
                System.loadLibrary("sherpa-onnx-jni")
            } catch (_: Throwable) {}
        }
    }
}
