
package com.yuyan.imemodule.ui.fragment.theme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.data.theme.displayName
import com.yuyan.imemodule.utils.rippleDrawable
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.centerInParent
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.rightOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent
import splitties.views.imageDrawable
import splitties.views.imageResource
import splitties.views.setPaddingDp

class ThemeThumbnailUi(override val ctx: Context) : Ui {

    enum class State { Normal, Selected, LightMode, DarkMode }

    var onExportClick: (() -> Unit)? = null

    private val bkg = imageView {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private val spaceBar = view(::View)

    private val returnKey = view(::View)

    private val checkMark = imageView {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    val editButton = imageView {
        setPaddingDp(16, 4, 4, 16)
        scaleType = ImageView.ScaleType.FIT_CENTER
        imageResource = R.drawable.ic_menu_edit
    }

    private val themeNameView = textView {
        textSize = 11.5f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setTextColor(android.graphics.Color.WHITE)
        gravity = android.view.Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(android.graphics.Color.parseColor("#B3000000"))
            cornerRadius = dp(4f)
        }
        setPadding(dp(6), dp(2), dp(6), dp(2))
        isVisible = false
    }

    override val root = constraintLayout {
        outlineProvider = ViewOutlineProvider.BOUNDS
        elevation = dp(2f)
        add(bkg, lParams(matchParent, matchParent))
        add(themeNameView, lParams(wrapContent, wrapContent) {
            centerHorizontally()
            bottomOfParent(dp(20))
        })
        add(spaceBar, lParams(height = dp(10)) {
            centerHorizontally()
            bottomOfParent(dp(6))
            matchConstraintPercentWidth = 0.5f
        })
        add(returnKey, lParams(dp(14), dp(14)) {
            rightOfParent(dp(4))
            bottomOfParent(dp(4))
        })
        add(checkMark, lParams(dp(60), dp(60)) {
            centerInParent()
        })
        add(editButton, lParams(dp(44), dp(44)) {
            topOfParent()
            endOfParent()
        })
    }

    fun setTheme(theme: Theme) {
        root.apply {
            foreground = rippleDrawable(theme.keyboardColor)
            setOnLongClickListener {
                if (theme is Theme.Custom) {
                    val popup = android.widget.PopupMenu(ctx, it)
                    popup.menu.add("导出主题 (.yus)")
                    popup.setOnMenuItemClickListener { item ->
                        if (item.title == "导出主题 (.yus)") {
                            onExportClick?.invoke()
                        }
                        true
                    }
                    popup.show()
                    true
                } else false
            }
        }
        val thumbDrawable = if (theme.backgroundImage != null) {
            try {
                val customBg = theme.backgroundImage as? Theme.Custom.CustomBackground
                customBg?.toDrawable(theme.backgroundBlurRadius, reqWidth = 360, reqHeight = 240) ?: theme.backgroundDrawable()
            } catch (_: Exception) {
                theme.backgroundDrawable()
            }
        } else {
            theme.backgroundDrawable()
        }
        thumbDrawable?.colorFilter = null
        bkg.imageDrawable = thumbDrawable
        spaceBar.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = ctx.dp(2f)
            setColor(theme.functionKeyBackgroundColor)
        }
        returnKey.background = ShapeDrawable(OvalShape()).apply {
            paint.color = theme.accentKeyBackgroundColor
        }
        val foregroundTint = ColorStateList.valueOf(theme.keyTextColor)
        themeNameView.text = theme.displayName
        themeNameView.isVisible = true
        editButton.apply {
            visibility = if (theme is Theme.Custom) View.VISIBLE else View.GONE
            background = rippleDrawable(theme.keyBackgroundColor)
            imageTintList = foregroundTint
        }
        checkMark.imageTintList = foregroundTint
    }

    fun setChecked(checked: Boolean) {
        checkMark.isVisible = checked
        checkMark.imageResource = R.drawable.ic_menu_done
    }

    fun setChecked(state: State) {
        checkMark.isVisible = state != State.Normal
        checkMark.imageResource = when (state) {
            State.Normal -> 0
            State.Selected -> R.drawable.ic_menu_done
            State.LightMode -> R.drawable.ic_sharp_light_mode_24
            State.DarkMode -> R.drawable.ic_sharp_mode_night_24
        }
    }
}