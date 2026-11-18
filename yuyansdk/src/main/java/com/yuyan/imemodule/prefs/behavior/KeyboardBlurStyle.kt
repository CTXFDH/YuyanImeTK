package com.yuyan.imemodule.prefs.behavior

import com.yuyan.imemodule.view.preference.ManagedPreference

enum class KeyboardBlurStyle {
    DEFAULT,          // 默认高斯模糊
    GLASS,            // 玻璃背景
    SOFT_GLASS,       // 柔光玻璃
    HYPEROS_FROST,    // HyperOS 4 清透磨砂
    COLOROS_MATERIAL, // ColorOS 高级材质
    CUSTOM;           // 自定义效果

    companion object : ManagedPreference.StringLikeCodec<KeyboardBlurStyle> {
        override fun decode(raw: String): KeyboardBlurStyle = runCatching { valueOf(raw) }.getOrDefault(DEFAULT)
    }
}
