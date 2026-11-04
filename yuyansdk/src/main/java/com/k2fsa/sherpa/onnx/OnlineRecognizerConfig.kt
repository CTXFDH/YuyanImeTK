package com.k2fsa.sherpa.onnx

import androidx.annotation.Keep

@Keep
data class FeatureConfig(
    val sampleRate: Int = 16000,
    val featureDim: Int = 80
)

@Keep
data class OnlineTransducerModelConfig(
    val encoder: String = "",
    val decoder: String = "",
    val joiner: String = "",
    val tokens: String = ""
)

@Keep
data class OnlineModelConfig(
    val transducer: OnlineTransducerModelConfig = OnlineTransducerModelConfig(),
    val numThreads: Int = 2,
    val provider: String = "cpu",
    val modelType: String = "zipformer"
)

@Keep
data class OnlineRecognizerConfig(
    val featConfig: FeatureConfig = FeatureConfig(),
    val modelConfig: OnlineModelConfig = OnlineModelConfig(),
    val decodingMethod: String = "greedy_search",
    val maxActivePaths: Int = 4,
    val enableEndpoint: Boolean = true,
    val rule1MinTrailingSilence: Float = 2.4f,
    val rule2MinTrailingSilence: Float = 1.2f,
    val rule3MinUtteranceLength: Float = 20.0f
)
