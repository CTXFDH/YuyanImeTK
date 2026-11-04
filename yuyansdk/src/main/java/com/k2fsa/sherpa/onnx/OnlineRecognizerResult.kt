package com.k2fsa.sherpa.onnx

data class OnlineRecognizerResult(
    val text: String = "",
    val tokens: Array<String> = emptyArray(),
    val timestamps: FloatArray = floatArrayOf()
)
