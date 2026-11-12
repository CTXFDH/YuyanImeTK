
package com.yuyan.imemodule.data.theme

import com.yuyan.imemodule.R

object ThemePreset {

    val MaterialLight = Theme.Builtin(
        name = "MaterialLight",
        isDark = false,
        keyboardResources = 0x0,
        keyboardColor = 0xffeceff1,
        barColor = 0xffe4e7e9,
        keyBackgroundColor = 0xfffbfbfc,
        keyTextColor = 0xff37474f,
        accentKeyBackgroundColor = 0xff5cb5ab,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xffd9dbdd,
        functionKeyBackgroundColor = 0xffc9ced1,
        functionKeyPressHighlightColor = 0x1f000000,
    )

    val MaterialDark = Theme.Builtin(
        name = "MaterialDark",
        isDark = true,
        keyboardResources = 0x0,
        barColor = 0xff21272b,
        keyboardColor = 0xff263238,
        keyBackgroundColor = 0xff404a50,
        keyTextColor = 0xffd9dbdc,
        accentKeyBackgroundColor = 0xff6eaca8,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x33ffffff,
        popupBackgroundColor = 0xff3c474c,
        functionKeyBackgroundColor = 0xff3b464c,
        functionKeyPressHighlightColor = 0x1fffffff,
    )

    val PixelLight = Theme.Builtin(
        name = "PixelLight",
        isDark = false,
        keyboardResources = 0x0,
        barColor = 0xffeeeeee,
        keyboardColor = 0xffE3E4EA,
        keyBackgroundColor = 0xffFCFCFC,
        keyTextColor = 0xff212121,
        accentKeyBackgroundColor = 0xff4285f4,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xffeeeeee,
        functionKeyBackgroundColor = 0xffB7BDC4,
        functionKeyPressHighlightColor = 0x1f000000,
    )

    val PixelDark = Theme.Builtin(
        name = "PixelDark",
        isDark = true,
        keyboardResources = 0x0,
        barColor = 0xff373737,
        keyboardColor = 0xff2d2d2d,
        keyBackgroundColor = 0xff464646,
        keyTextColor = 0xfffafafa,
        accentKeyBackgroundColor = 0xff5e97f6,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x33ffffff,
        popupBackgroundColor = 0xff373737,
        functionKeyBackgroundColor = 0xff4a4a4a,
        functionKeyPressHighlightColor = 0x1fffffff,
    )

    val AMOLEDBlack = Theme.Builtin(
        name = "AMOLEDBlack",
        isDark = true,
        keyboardResources = 0x0,
        barColor = 0xff373737,
        keyboardColor = 0xff000000,
        keyBackgroundColor = 0xff2e2e2e,
        keyTextColor = 0xffffffff,
        accentKeyBackgroundColor = 0xff80cbc4,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x33ffffff,
        popupBackgroundColor = 0xff373737,
        functionKeyBackgroundColor = 0xff727272,
        functionKeyPressHighlightColor = 0x1fffffff,
    )

    val NordLight = Theme.Builtin(
        name = "NordLight",
        isDark = false,
        keyboardResources = 0x0,
        barColor = 0xffE5E9F0,
        keyboardColor = 0xffECEFF4,
        keyBackgroundColor = 0xfffbfbfc,
        keyTextColor = 0xff2E3440,
        accentKeyBackgroundColor = 0xff5E81AC,
        accentKeyTextColor = 0xffECEFF4,
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xffE5E9F0,
        functionKeyBackgroundColor = 0xffD8DEE9,
        functionKeyPressHighlightColor = 0x1f000000,
    )

    val NordDark = Theme.Builtin(
        name = "NordDark",
        isDark = true,
        keyboardResources = 0x0,
        barColor = 0xff434C5E,
        keyboardColor = 0xff2E3440,
        keyBackgroundColor = 0xff4C566A,
        keyTextColor = 0xffECEFF4,
        accentKeyBackgroundColor = 0xff88C0D0,
        accentKeyTextColor = 0xff2E3440,
        keyPressHighlightColor = 0x33ffffff,
        popupBackgroundColor = 0xff434C5E,
        functionKeyBackgroundColor = 0xff4C566A,
        functionKeyPressHighlightColor = 0x1fffffff,
    )

    val Monokai = Theme.Builtin(
        name = "Monokai",
        isDark = true,
        keyboardResources = 0x0,
        barColor = 0xff1f201b,
        keyboardColor = 0xff272822,
        keyBackgroundColor = 0xff33342c,
        keyTextColor = 0xffd6d6d6,
        accentKeyBackgroundColor = 0xffb05279,
        accentKeyTextColor = 0xffd6d6d6,
        keyPressHighlightColor = 0x33ffffff,
        popupBackgroundColor = 0xff1f201b,
        functionKeyBackgroundColor = 0xff373830,
        functionKeyPressHighlightColor = 0x1fffffff,
    )

    /**
     * transparent background with semi-transparent white keys
     */
    val TransparentDark = Theme.Builtin(
        name = "TransparentDark",
        isDark = true,
        keyboardResources = 0x0,
        barColor = 0x4c000000,
        keyboardColor = 0x00000000,
        keyBackgroundColor = 0x4bffffff,
        keyTextColor = 0xffffffff,
        accentKeyBackgroundColor = 0xff5e97f6,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xff373737,
        functionKeyBackgroundColor = 0x4bffffff,
        functionKeyPressHighlightColor = 0x1fffffff,
    )

    /**
     * transparent background with semi-transparent black keys
     */
    val TransparentLight = Theme.Builtin(
        name = "TransparentLight",
        isDark = false,
        keyboardResources = 0x0,
        barColor = 0x26000000,
        keyboardColor = 0x00000000,
        keyBackgroundColor = 0x4bffffff,
        keyTextColor = 0xff000000,
        accentKeyBackgroundColor = 0xff5e97f6,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xffeeeeee,
        functionKeyBackgroundColor = 0x5affffff,
        functionKeyPressHighlightColor = 0x1f000000,
    )

    val CustomRed = Theme.Builtin(
        name = "CustomRed",
        isDark = false,
        keyboardResources = R.drawable.skb_background_red,
        barColor = 0xfffbae44,
        keyboardColor = 0xffeceff1,
        keyBackgroundColor = 0x33fbfbfc,
        keyTextColor = 0xffffffff,
        accentKeyBackgroundColor = 0xffffaaa5,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xfffd3422,
        functionKeyBackgroundColor = 0x5affffff,
        functionKeyPressHighlightColor = 0x1f000000,
    )
    val CustomPink = Theme.Builtin(
        name = "CustomPink",
        isDark = false,
        keyboardResources = R.drawable.skb_background_pink,
        barColor = 0xffd5a3a4,
        keyboardColor = 0xffeceff1,
        keyBackgroundColor = 0x33fbfbfc,
        keyTextColor = 0xff2E3440,
        accentKeyBackgroundColor = 0xff6eaca8,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xffd7c4d0,
        functionKeyBackgroundColor = 0x5affffff,
        functionKeyPressHighlightColor = 0x1f000000,
    )
    val CustomYellow = Theme.Builtin(
        name = "CustomYellow",
        isDark = false,
        keyboardResources = R.drawable.skb_background_yellow,
        barColor = 0xffffecba,
        keyboardColor = 0xffeceff1,
        keyBackgroundColor = 0x33fbfbfc,
        keyTextColor = 0xff2E3440,
        accentKeyBackgroundColor = 0xff6eaca8,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xfff16877,
        functionKeyBackgroundColor = 0x5affffff,
        functionKeyPressHighlightColor = 0x1f000000,
    )
    val CustomCrimson = Theme.Builtin(
        name = "CustomCrimson",
        isDark = true,
        keyboardResources = R.drawable.skb_background_crimso,
        barColor = 0XFF860001,
        keyboardColor = 0xffeceff1,
        keyBackgroundColor = 0x33fbfbfc,
        keyTextColor = 0xffffffff,
        accentKeyBackgroundColor = 0xff6eaca8,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0XFF480603,
        functionKeyBackgroundColor = 0x5affffff,
        functionKeyPressHighlightColor = 0x1f000000,
    )
    val CustomPurple = Theme.Builtin(
        name = "CustomPurple",
        isDark = false,
        keyboardResources = R.drawable.skb_background_purple,
        barColor = 0xffe100ff,
        keyboardColor = 0xffeceff1,
        keyBackgroundColor = 0x33fbfbfc,
        keyTextColor = 0xffffffff,
        accentKeyBackgroundColor = 0xff7F00FF,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xff9f00ff,
        functionKeyBackgroundColor = 0x5affffff,
        functionKeyPressHighlightColor = 0x1f000000,
    )

    val CustomBlue = Theme.Builtin(
        name = "CustomBlue",
        isDark = false,
        keyboardResources = R.drawable.skb_background_blue,
        barColor = 0xff1488cc,
        keyboardColor = 0xffeceff1,
        keyBackgroundColor = 0x33fbfbfc,
        keyTextColor = 0xffffffff,
        accentKeyBackgroundColor = 0xff2196f3,
        accentKeyTextColor = 0xffffffff,
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xff0d47a1,
        functionKeyBackgroundColor = 0x5affffff,
        functionKeyPressHighlightColor = 0x1f000000,
    )

    // --- 5 套全新亮色主题 ---
    val MintSummer = Theme.Builtin(
        name = "薄荷夏日",
        isDark = false,
        keyboardResources = 0,
        barColor = 0xffDCEDC8.toInt(),
        keyboardColor = 0xffE8F5E9.toInt(),
        keyBackgroundColor = 0xffFFFFFF.toInt(),
        keyTextColor = 0xff2E7D32.toInt(),
        accentKeyBackgroundColor = 0xff10B981.toInt(),
        accentKeyTextColor = 0xffFFFFFF.toInt(),
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xffC8E6C9.toInt(),
        functionKeyBackgroundColor = 0xffC8E6C9.toInt(),
        functionKeyPressHighlightColor = 0x1f000000,
        toolbarTextColor = 0xff1B5E20.toInt(),
        toolbarActiveIconColor = 0xff10B981.toInt(),
    )

    val SunsetGlow = Theme.Builtin(
        name = "日落晚霞",
        isDark = false,
        keyboardResources = 0,
        barColor = 0xffFFE0B2.toInt(),
        keyboardColor = 0xffFFF3E0.toInt(),
        keyBackgroundColor = 0xffFFFFFF.toInt(),
        keyTextColor = 0xffE65100.toInt(),
        accentKeyBackgroundColor = 0xffFF6F00.toInt(),
        accentKeyTextColor = 0xffFFFFFF.toInt(),
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xffFFCC80.toInt(),
        functionKeyBackgroundColor = 0xffFFCC80.toInt(),
        functionKeyPressHighlightColor = 0x1f000000,
        toolbarTextColor = 0xffBF360C.toInt(),
        toolbarActiveIconColor = 0xffFF6F00.toInt(),
    )

    val SeaSaltCheese = Theme.Builtin(
        name = "海盐芝士",
        isDark = false,
        keyboardResources = 0,
        barColor = 0xffB2EBF2.toInt(),
        keyboardColor = 0xffE0F7FA.toInt(),
        keyBackgroundColor = 0xffFFFDE7.toInt(),
        keyTextColor = 0xff006064.toInt(),
        accentKeyBackgroundColor = 0xff00ACC1.toInt(),
        accentKeyTextColor = 0xffFFFFFF.toInt(),
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xff80DEEA.toInt(),
        functionKeyBackgroundColor = 0xff80DEEA.toInt(),
        functionKeyPressHighlightColor = 0x1f000000,
        toolbarTextColor = 0xff004D40.toInt(),
        toolbarActiveIconColor = 0xff00ACC1.toInt(),
    )

    val QuietVanilla = Theme.Builtin(
        name = "静谧香草",
        isDark = false,
        keyboardResources = 0,
        barColor = 0xffEFEBE9.toInt(),
        keyboardColor = 0xffF5F5DC.toInt(),
        keyBackgroundColor = 0xffFFFFFF.toInt(),
        keyTextColor = 0xff4E342E.toInt(),
        accentKeyBackgroundColor = 0xff8D6E63.toInt(),
        accentKeyTextColor = 0xffFFFFFF.toInt(),
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xffD7CCC8.toInt(),
        functionKeyBackgroundColor = 0xffD7CCC8.toInt(),
        functionKeyPressHighlightColor = 0x1f000000,
        toolbarTextColor = 0xff3E2723.toInt(),
        toolbarActiveIconColor = 0xff8D6E63.toInt(),
    )

    val VioletHyacinth = Theme.Builtin(
        name = "紫罗兰风信子",
        isDark = false,
        keyboardResources = 0,
        barColor = 0xffE1BEE7.toInt(),
        keyboardColor = 0xffF3E5F5.toInt(),
        keyBackgroundColor = 0xffFFFFFF.toInt(),
        keyTextColor = 0xff4A148C.toInt(),
        accentKeyBackgroundColor = 0xff8E24AA.toInt(),
        accentKeyTextColor = 0xffFFFFFF.toInt(),
        keyPressHighlightColor = 0x1f000000,
        popupBackgroundColor = 0xffCE93D8.toInt(),
        functionKeyBackgroundColor = 0xffCE93D8.toInt(),
        functionKeyPressHighlightColor = 0x1f000000,
        toolbarTextColor = 0xff4A148C.toInt(),
        toolbarActiveIconColor = 0xff8E24AA.toInt(),
    )

    // --- 5 套全新暗色主题 ---
    val NightAurora = Theme.Builtin(
        name = "暗夜极光",
        isDark = true,
        keyboardResources = 0,
        barColor = 0xff1E293B.toInt(),
        keyboardColor = 0xff121A21.toInt(),
        keyBackgroundColor = 0xff1E2D3D.toInt(),
        keyTextColor = 0xffE2E8F0.toInt(),
        accentKeyBackgroundColor = 0xff00F5D4.toInt(),
        accentKeyTextColor = 0xff0F172A.toInt(),
        keyPressHighlightColor = 0x33ffffff,
        popupBackgroundColor = 0xff1E293B.toInt(),
        functionKeyBackgroundColor = 0xff27374D.toInt(),
        functionKeyPressHighlightColor = 0x1fffffff,
        toolbarTextColor = 0xff00F5D4.toInt(),
        toolbarActiveIconColor = 0xff00F5D4.toInt(),
    )

    val BlackGoldLuxury = Theme.Builtin(
        name = "黑金尊享",
        isDark = true,
        keyboardResources = 0,
        barColor = 0xff222222.toInt(),
        keyboardColor = 0xff181818.toInt(),
        keyBackgroundColor = 0xff2A2A2A.toInt(),
        keyTextColor = 0xffF5D061.toInt(),
        accentKeyBackgroundColor = 0xffD4AF37.toInt(),
        accentKeyTextColor = 0xff121212.toInt(),
        keyPressHighlightColor = 0x33ffffff,
        popupBackgroundColor = 0xff222222.toInt(),
        functionKeyBackgroundColor = 0xff333333.toInt(),
        functionKeyPressHighlightColor = 0x1fffffff,
        toolbarTextColor = 0xffF5D061.toInt(),
        toolbarActiveIconColor = 0xffD4AF37.toInt(),
    )

    val CosmicPurple = Theme.Builtin(
        name = "深空幻紫",
        isDark = true,
        keyboardResources = 0,
        barColor = 0xff241442.toInt(),
        keyboardColor = 0xff1A0B2E.toInt(),
        keyBackgroundColor = 0xff2D1954.toInt(),
        keyTextColor = 0xffF3E8FF.toInt(),
        accentKeyBackgroundColor = 0xffB5179E.toInt(),
        accentKeyTextColor = 0xffFFFFFF.toInt(),
        keyPressHighlightColor = 0x33ffffff,
        popupBackgroundColor = 0xff241442.toInt(),
        functionKeyBackgroundColor = 0xff3C226B.toInt(),
        functionKeyPressHighlightColor = 0x1fffffff,
        toolbarTextColor = 0xffE9D5FF.toInt(),
        toolbarActiveIconColor = 0xffF72585.toInt(),
    )

    val MonochromeGraphite = Theme.Builtin(
        name = "灰阶复古",
        isDark = true,
        keyboardResources = 0,
        barColor = 0xff282D2A.toInt(),
        keyboardColor = 0xff1F2421.toInt(),
        keyBackgroundColor = 0xff363C38.toInt(),
        keyTextColor = 0xffDCE1DE.toInt(),
        accentKeyBackgroundColor = 0xff495852.toInt(),
        accentKeyTextColor = 0xffFFFFFF.toInt(),
        keyPressHighlightColor = 0x33ffffff,
        popupBackgroundColor = 0xff282D2A.toInt(),
        functionKeyBackgroundColor = 0xff2D3430.toInt(),
        functionKeyPressHighlightColor = 0x1fffffff,
        toolbarTextColor = 0xffDCE1DE.toInt(),
        toolbarActiveIconColor = 0xffA3B18A.toInt(),
    )

    val AbyssalOcean = Theme.Builtin(
        name = "深海幽蓝",
        isDark = true,
        keyboardResources = 0,
        barColor = 0xff1C2541.toInt(),
        keyboardColor = 0xff0B132B.toInt(),
        keyBackgroundColor = 0xff202C59.toInt(),
        keyTextColor = 0xffE0E1DD.toInt(),
        accentKeyBackgroundColor = 0xff3A86FF.toInt(),
        accentKeyTextColor = 0xffFFFFFF.toInt(),
        keyPressHighlightColor = 0x33ffffff,
        popupBackgroundColor = 0xff1C2541.toInt(),
        functionKeyBackgroundColor = 0xff1F2D5A.toInt(),
        functionKeyPressHighlightColor = 0x1fffffff,
        toolbarTextColor = 0xff60A5FA.toInt(),
        toolbarActiveIconColor = 0xff3A86FF.toInt(),
    )
}