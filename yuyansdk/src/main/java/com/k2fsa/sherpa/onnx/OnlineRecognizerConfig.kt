package com.k2fsa.sherpa.onnx

import androidx.annotation.Keep

@Keep
class FeatureConfig(
    @JvmField var sampleRate: Int = 16000,
    @JvmField var featureDim: Int = 80
)

@Keep
class OnlineTransducerModelConfig(
    @JvmField var encoder: String = "",
    @JvmField var decoder: String = "",
    @JvmField var joiner: String = "",
    @JvmField var tokens: String = ""
)

@Keep
class OnlineParaformerModelConfig(
    @JvmField var encoder: String = "",
    @JvmField var decoder: String = ""
)

@Keep
class OnlineZipformer2CtcModelConfig(
    @JvmField var model: String = ""
)

@Keep
class OnlineModelConfig(
    @JvmField var transducer: OnlineTransducerModelConfig = OnlineTransducerModelConfig(),
    @JvmField var paraformer: OnlineParaformerModelConfig = OnlineParaformerModelConfig(),
    @JvmField var zipformer2Ctc: OnlineZipformer2CtcModelConfig = OnlineZipformer2CtcModelConfig(),
    @JvmField var tokens: String = "",
    @JvmField var numThreads: Int = 2,
    @JvmField var debug: Boolean = false,
    @JvmField var provider: String = "cpu",
    @JvmField var modelType: String = "zipformer"
)

@Keep
class OnlineRecognizerConfig(
    @JvmField var featConfig: FeatureConfig = FeatureConfig(),
    @JvmField var modelConfig: OnlineModelConfig = OnlineModelConfig(),
    @JvmField var decodingMethod: String = "greedy_search",
    @JvmField var maxActivePaths: Int = 4,
    @JvmField var enableEndpoint: Boolean = false,
    @JvmField var rule1MinTrailingSilence: Float = 2.4f,
    @JvmField var rule2MinTrailingSilence: Float = 1.2f,
    @JvmField var rule3MinUtteranceLength: Float = 20.0f,
    @JvmField var hotwordsFile: String = "",
    @JvmField var hotwordsScore: Float = 1.5f
)
