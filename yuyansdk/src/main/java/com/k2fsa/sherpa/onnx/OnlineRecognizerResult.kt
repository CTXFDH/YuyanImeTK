package com.k2fsa.sherpa.onnx

import androidx.annotation.Keep

@Keep
class OnlineRecognizerResult {

    @JvmField
    var text: String = ""

    @JvmField
    var tokens: Array<String> = emptyArray()

    @JvmField
    var timestamps: FloatArray = floatArrayOf()

    constructor()

    constructor(text: String) {
        this.text = text
    }

    constructor(text: String, tokens: Array<String>, timestamps: FloatArray) {
        this.text = text
        this.tokens = tokens
        this.timestamps = timestamps
    }
}
