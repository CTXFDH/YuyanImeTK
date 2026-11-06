package com.yuyan.imemodule.candidate

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.yuyan.imemodule.adapter.CandidatesBarAdapter
import com.yuyan.imemodule.callback.CandidateViewListener
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.keyboard.container.CandidatesContainer
import com.yuyan.imemodule.manager.layout.CustomLinearLayoutManager
import com.yuyan.imemodule.service.DictDecoder
import com.yuyan.imemodule.singleton.EnvironmentSingleton.Companion.instance
import splitties.dimensions.dp

/**
 * 候选词集装箱
 */
class FloatCandidateBar(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private var mFloatCandidateBarWidth: Int = 0

    private lateinit var mCvListener: CandidateViewListener // 候选词视图监听器
    private lateinit var mCandidatesDataContainer: LinearLayout //候选词视图
    private lateinit var mComposingTagContainer: LinearLayout // 悬浮拼音胶囊容器 View
    private lateinit var mComposingView: TextView // 组成字符串的View，用于显示输入的拼音。
    private lateinit var mRVCandidates: RecyclerView    //候选词列表
    private lateinit var mCandidatesAdapter: CandidatesBarAdapter
    private lateinit var candidatesData: LinearLayout //候选词视图
    private var activeCandNo:Int = 0

    fun initialize(cvListener: CandidateViewListener) {
        mCvListener = cvListener
        mFloatCandidateBarWidth = instance.mScreenWidth - dp(40)
        initCandidateView()
    }

    // 初始化候选词界面
    private fun initCandidateView() {
        if(!::mCandidatesDataContainer.isInitialized) {
            mCandidatesDataContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
            }
            mComposingTagContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                visibility = GONE
                setPadding(dp(8), dp(2), dp(8), dp(2))
            }
            mComposingView = TextView(context).apply {
                includeFontPadding = false
                isSingleLine = true
            }
            mComposingTagContainer.addView(mComposingView)

            candidatesData = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            mRVCandidates = RecyclerView(context).apply {
                itemAnimator = null
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                layoutManager =
                    CustomLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, reverseLayout = false)
            }
            mCandidatesAdapter = CandidatesBarAdapter(context)
            mCandidatesAdapter.setOnItemClickLitener { _: RecyclerView.Adapter<*>?, _: View?, position: Int ->
                mCvListener.onClickChoice(position)
            }
            mRVCandidates.setAdapter(mCandidatesAdapter)
            mRVCandidates.addOnScrollListener(object : OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        DictDecoder.activeCandidateBar =
                            layoutManager.findLastVisibleItemPosition()
                        val itemCount = recyclerView.adapter?.itemCount
                        if ((KeyboardManager.instance.currentContainer !is CandidatesContainer) && (itemCount != null) && (DictDecoder.activeCandidateBar >= itemCount - 1)) {
                            DictDecoder.nextPageCandidates
                        }
                    }
                }
            })

            val tagLp = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(10)
                topMargin = dp(2)
                bottomMargin = dp(1)
            }
            mCandidatesDataContainer.addView(mComposingTagContainer, tagLp)
            mCandidatesDataContainer.addView(candidatesData, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
            this.addView(mCandidatesDataContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        } else {
            if (mRVCandidates.parent != null) (mRVCandidates.parent as ViewGroup).removeView(mRVCandidates)
        }
        candidatesData.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        candidatesData.removeAllViews()
        candidatesData.addView(mRVCandidates, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 1f))
        mCandidatesAdapter.notifyChanged()
    }

    /**
     * 显示候选词
     */
    fun showCandidates() {
        val composingText = DictDecoder.composingStrForDisplay
        if (::mComposingTagContainer.isInitialized) {
            if (composingText.isNotBlank()) {
                mComposingView.text = composingText
                mComposingTagContainer.visibility = VISIBLE
            } else {
                mComposingTagContainer.visibility = GONE
            }
        }
        if (DictDecoder.isCandidatesEmpty) {
            this.visibility = GONE
        } else {
            if (DictDecoder.candidateSize > DictDecoder.activeCandidateBar) mRVCandidates.layoutManager?.scrollToPosition(DictDecoder.activeCandidateBar)
            this.visibility = VISIBLE
        }
        activeCandNo = 0
        mCandidatesAdapter.activeCandidates(activeCandNo)
        mCandidatesAdapter.notifyChanged()
    }

    /**
     * 更新激活的候选词
     */
    fun updateActiveCandidateNo(keyCode: Int) {
        if (!DictDecoder.isCandidatesEmpty) {
            when(keyCode){
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if(--activeCandNo <= 0) activeCandNo = 0
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if(++activeCandNo > DictDecoder.candidateSize) activeCandNo = DictDecoder.candidateSize
                }
            }
            mCandidatesAdapter.activeCandidates(activeCandNo)
            mCandidatesAdapter.notifyChanged()
            mRVCandidates.layoutManager?.scrollToPosition(if(activeCandNo - 1 > 0) activeCandNo - 1 else 0 )
        }
    }

    /**
     * 获取激活的候选词
     */
    fun getActiveCandNo():Int {
        return if(activeCandNo > 0) activeCandNo - 1 else 0
    }

    /**
     * 是否操作选词
     */
    fun isActiveCand():Boolean {
        return activeCandNo > 0
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMeasure = MeasureSpec.makeMeasureSpec(instance.heightForCandidatesArea, MeasureSpec.EXACTLY)
        val widthMeasure = MeasureSpec.makeMeasureSpec(mFloatCandidateBarWidth, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasure, heightMeasure)
    }

    // 刷新主题
    @Suppress("UNUSED_PARAMETER")
    fun updateTheme(textColor: Int) {
        initCandidateView()
        val activeTheme = ThemeManager.activeTheme
        if (::mComposingTagContainer.isInitialized) {
            val tagBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14f)
                if (activeTheme.isDark) {
                    setColor(0xE62A2C30.toInt())
                    setStroke(dp(1), 0x40FFFFFF.toInt())
                } else {
                    setColor(0xF5EFF1F5.toInt())
                    setStroke(dp(1), 0x20000000.toInt())
                }
            }
            mComposingTagContainer.background = tagBg
        }
        if (::mComposingView.isInitialized) {
            mComposingView.setTextColor(activeTheme.toolbarTextColor)
            mComposingView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        }
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ThemeManager.activeTheme.keyboardColor)
            cornerRadius = 20f
        }
        background = drawable
        mCandidatesAdapter.notifyChanged()
    }
}
