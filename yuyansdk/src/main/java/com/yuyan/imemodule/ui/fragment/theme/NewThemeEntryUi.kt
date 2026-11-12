package com.yuyan.imemodule.ui.fragment.theme

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.utils.DialogStyleUtils
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.styledDrawable
import splitties.views.dsl.core.Ui

class NewThemeEntryUi(override val ctx: Context) : Ui {
    private val isDark = DialogStyleUtils.isDarkUiMode(ctx)

    private val cardBgDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = ctx.dp(16f)
        orientation = GradientDrawable.Orientation.TL_BR
        if (isDark) {
            colors = intArrayOf(
                Color.parseColor("#1B2E24"),
                Color.parseColor("#14211A"),
                Color.parseColor("#0F1713")
            )
            setStroke(DevicesUtils.dip2px(1.5f), Color.parseColor("#2A4838"))
        } else {
            colors = intArrayOf(
                Color.parseColor("#E6F7F0"),
                Color.parseColor("#D8F3E8"),
                Color.parseColor("#CAF0DF")
            )
            setStroke(DevicesUtils.dip2px(1.5f), Color.parseColor("#A3E6CA"))
        }
    }

    private val iconBadgeDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(if (isDark) Color.parseColor("#2610B981") else Color.parseColor("#3307C160"))
    }

    val icon = ImageView(ctx).apply {
        val size = ctx.dp(18)
        layoutParams = FrameLayout.LayoutParams(size, size).apply {
            gravity = Gravity.CENTER
        }
        setImageDrawable(ctx.drawable(R.drawable.ic_menu_plus)?.apply {
            setTint(if (isDark) Color.parseColor("#10B981") else Color.parseColor("#07C160"))
        })
    }

    val iconContainer = FrameLayout(ctx).apply {
        val sz = ctx.dp(38)
        layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
            bottomMargin = ctx.dp(6)
        }
        background = iconBadgeDrawable
        addView(icon)
    }

    val text = TextView(ctx).apply {
        setText(R.string.new_theme)
        textSize = 13.5f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setTextColor(if (isDark) Color.parseColor("#E4E4E7") else Color.parseColor("#18181B"))
    }

    override val root = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        foreground = ctx.styledDrawable(android.R.attr.selectableItemBackground)
        background = cardBgDrawable
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
        elevation = ctx.dp(2f)

        addView(iconContainer)
        addView(text)
    }
}
