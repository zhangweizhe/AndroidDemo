package com.example.androiddemo.layoutmanager

import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.example.androiddemo.BuildConfig
import kotlin.math.min

class PagerGridLayoutManager: RecyclerView.LayoutManager() {

    private val DEBUG = BuildConfig.DEBUG

    private var mItemWidth = 0
    private var mItemHeight = 0

    private var mColumn = 3
    private var mRow = 2
    private var mOnePageSize = mColumn * mRow

    /**
     * 计算多出来的宽度，因为在均分的时候，存在除不尽的情况，要减去多出来的这部分大小，一般也就为几px
     * 不减去的话，会导致翻页计算不触发
     *
     * @see #onMeasure(RecyclerView.Recycler, RecyclerView.State, int, int)
     */
    private var mDiffWidth = 0
    /**
     * 计算多出来的高度，因为在均分的时候，存在除不尽的情况，要减去多出来的这部分大小，一般也就为几px
     * 不减去的话，会导致翻页计算不触发
     *
     * @see #onMeasure(RecyclerView.Recycler, RecyclerView.State, int, int)
     */
    private var mDiffHeight = 0

    /**
     * 用于计算锚点坐标
     * [.mShouldReverseLayout] 为false：左上角第一个view的位置
     * [.mShouldReverseLayout] 为true：右上角第一个view的位置
     */
    private val mStartSnapRect: Rect = Rect()

    /**
     * 用于计算锚点坐标
     * [.mShouldReverseLayout] 为false：右下角最后一个view的位置
     * [.mShouldReverseLayout] 为true：左上角最后一个view的位置
     */
    private val mEndSnapRect: Rect = Rect()

    private val NO_PAGER_COUNT = 0
    private var mPagerCount = NO_PAGER_COUNT

    private val NO_ITEM = 0
    private var mCurrentPagerIndex = NO_ITEM

    private var mRecyclerView: RecyclerView? = null


    /**
     * 用于保存一些状态
     */
    private var mLayoutState: LayoutState? = null

    init {
        mLayoutState = LayoutState()
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT)
    }

    override fun onAttachedToWindow(view: RecyclerView?) {
        super.onAttachedToWindow(view)
        mRecyclerView = view
    }

    override fun onMeasure(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        widthSpec: Int,
        heightSpec: Int
    ) {
        val widthMode = View.MeasureSpec.getMode(widthSpec)
        val heightMode = View.MeasureSpec.getMode(heightSpec)
        if (widthMode != View.MeasureSpec.EXACTLY || heightMode != View.MeasureSpec.EXACTLY) {
            throw IllegalStateException("RecyclerView's width and height must be exactly")
        }
        val widthSize = View.MeasureSpec.getSize(widthSpec)
        val heightSize = View.MeasureSpec.getSize(heightSpec)

        val realWidth = widthSize - paddingStart - paddingEnd
        val realHeight = heightSize - paddingTop - paddingBottom
        // 均分宽高
        mItemWidth = realWidth / mColumn
        mItemHeight = realHeight / mRow

        mDiffWidth = realWidth - mItemWidth * mColumn
        mDiffHeight = realHeight - mItemHeight * mRow

        if (DEBUG) {
            Log.d(TAG,
                "onMeasure-originalWidthSize: $widthSize,originalHeightSize: $heightSize,diffWidth: $mDiffWidth,diffHeight: $mDiffHeight,mItemWidth: $mItemWidth,mItemHeight: $mItemHeight,mStartSnapRect:$mStartSnapRect,mEndSnapRect:$mEndSnapRect"
            )
        }

        super.onMeasure(recycler, state, widthSpec, heightSpec)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (DEBUG) {
            Log.d(TAG, "onLayoutChildren: $state")
        }

        if (itemCount == 0) {
            removeAndRecycleAllViews(recycler)
            setPagerCount(NO_PAGER_COUNT)
            setCurrentPagerIndex(NO_ITEM)
            return
        }

        if (state.isPreLayout) {
            // TODO: preLayout 是啥
            return
        }
        // 流程：1）计算第一个和最后一个view的位置
        // 左上角第一个 View 的位置
        mStartSnapRect.set(paddingStart, paddingTop, paddingStart + mItemWidth, paddingTop + mItemHeight)
        // 右下角第一个 View 的位置
        mEndSnapRect.set(width - paddingEnd - mItemWidth, height - paddingBottom - mItemHeight, width - paddingEnd, height - paddingBottom)

        // 流程：2）计算总页数
        // 计算总页数
        var pageCount = itemCount / mOnePageSize
        if (itemCount % mOnePageSize != 0) {// 最后一页没有铺满的场景
            pageCount++
        }

        // 流程：3）计算最后一页需要补充的空间
        // 计算需要补充空间
        mLayoutState?.replenishDelta = 0
        if (pageCount > 1) {
            // 超过一夜，计算补充空间距离
            val remain = itemCount % mOnePageSize
            var replenish = 0
            if (remain != 0) {// 超过一页，最后一页无法铺满（remain != 0）
                // i+1表示最后一页的行数，如2行3列，remain=4, i=4/3=1，即最后一页有2行
                val i = remain / mColumn
                val k = remain % mColumn
                replenish = if (i == 0) {
                        // i==0，最后一页只有一行，要计算需要补充的空间
                    (mColumn - k) * mItemWidth
                }else{
                    0
                }
            }
            mLayoutState?.replenishDelta = replenish
        }

        mLayoutState?.mRecycle = false
        mLayoutState?.mLayoutDirection = LayoutState.LAYOUT_END
        mLayoutState?.mAvailable = getEnd()
        mLayoutState?.mScrollingOffset = LayoutState.SCROLLING_OFFSET_NaN

        // 流程：4）计算pageIndex
        var pagerIndex = mCurrentPagerIndex
        pagerIndex = if (pagerIndex == NO_ITEM) {
            0
        }else {
            // 取上次PagerIndex和最大MaxPagerIndex中最小值。
            min(pagerIndex, getMaxPagerIndex())
        }

        val firstView = if (!isIdle() && childCount != 0) {
            // 滑动中的更新状态
            getChildClosestToStart()
        }else {
            // 没有子View或者不在滑动状态
            null
        }

        // 计算首个位置的偏移量，主要是为了方便 child layout，计算出目标位置的上一个位置的坐标
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        if (firstView == null) {
            // 从左上角开始布局
            mLayoutState?.mCurrentPosition = pagerIndex * mOnePageSize

            // TODO: 这样计算 bottom right 的意义？
            bottom = height - paddingBottom
            right = paddingStart
        }else {
            val position = getPosition(firstView)
            mLayoutState?.mCurrentPosition = position
            val rect = mLayoutState?.mOffsetRect ?: Rect()

            getDecoratedBoundsWithMargins(firstView, rect)
            if (isNeedMoveToNextSpan(position)) {
                // 为了方便计算
                bottom = height - paddingBottom
                right = rect.left
            }else {
                bottom = rect.top
                right = rect.right
            }
            // 追加额外的滑动空间
            val scrollingOffset = getDecoratedLeft(firstView)
            mLayoutState!!.mAvailable -= scrollingOffset
        }
        top = bottom - mItemHeight
        left = right - mItemWidth
        mLayoutState?.setOffsetRect(left, top, right, bottom)
        
        if (DEBUG) {
            Log.i(TAG, "onLayoutChildren: childCount: $childCount, recycler.scrapList.size: ${recycler.scrapList.size}, mLayoutState.replenishDelta: ${mLayoutState?.replenishDelta}")
        }
        // 回收views
        detachAndScrapAttachedViews(recycler)
        // 填充views
        fill(recycler, state)
        if (DEBUG) {
            Log.i(TAG, "onLayoutChildren: childCount:" + childCount + ",recycler.scrapList.size:" + recycler.scrapList.size + ",mLayoutState.replenishDelta:" + mLayoutState?.replenishDelta);
        }

        if (firstView == null) {
            // 移动状态不更新页数和页码
            setPagerCount(pageCount)
            setCurrentPagerIndex(pagerIndex)
        }
    }

    override fun findViewByPosition(position: Int): View? {
        if (childCount != 0) {
            val firstChild = getPosition(getChildAt(0)!!)
            val viewPosition = position - firstChild
            if (viewPosition in 0 until childCount) {
                val child = getChildAt(viewPosition)
                if (getPosition(child!!) == position) {
                    return child
                }
            }
        }
        return super.findViewByPosition(position)
    }

    /**
     * 填充布局
     */
    private fun fill(recycler: Recycler, state: RecyclerView.State) {
        
    }

    /**
     * @param position
     * @return 是否需要换到下一行
     */
    private fun isNeedMoveToNextSpan(position: Int): Boolean {
        val surplus = position % mOnePageSize
        val rowIndex: Int = surplus / mColumn
        //是否在最后一行
        return rowIndex == 0
    }


    private fun getChildClosestToStart(): View? {
        return getChildAt(0)
    }

    /**
     * @return 当前Recycler是否是静止状态
     */
    private fun isIdle(): Boolean {
        return mRecyclerView == null || mRecyclerView?.scrollState == RecyclerView.SCROLL_STATE_IDLE
    }

    /**
     * @param position position
     * @return 获取当前position所在页下标
     */
    fun getPagerIndexByPosition(position: Int): Int {
        return position / mOnePageSize
    }

    /**
     * @return 获取最大页数
     */
    fun getMaxPagerIndex(): Int {
        return getPagerIndexByPosition(itemCount - 1)
    }

    /**
     * 设置总页数
     *
     * @param pagerCount
     */
    private fun setPagerCount(pagerCount: Int) {
        if (mPagerCount == pagerCount) {
            return
        }
        mPagerCount = pagerCount
    }

    /**
     * 设置当前页码
     *
     * @param pagerIndex 页码
     */
    private fun setCurrentPagerIndex(pagerIndex: Int) {
        if (mCurrentPagerIndex === pagerIndex) {
            return
        }
        val prePagerIndex: Int = mCurrentPagerIndex
        mCurrentPagerIndex = pagerIndex
    }

    private fun getEnd(): Int {
        return getRealWidth()
    }

    private fun getRealWidth(): Int {
        return width - paddingStart - paddingEnd
    }

    protected class LayoutState {
        /**
         * 可填充的View空间大小
         */
        var mAvailable = 0

        /**
         * 是否需要回收View
         */
        var mRecycle = false
        var mCurrentPosition = 0

        /**
         * 布局的填充方向
         * 值为 [.LAYOUT_START] or [.LAYOUT_END]
         */
        var mLayoutDirection = 0

        /**
         * 在滚动状态下构造布局状态时使用。
         * 它应该设置我们可以在不创建新视图的情况下进行滚动量。
         * 有效的视图回收需要设置
         */
        var mScrollingOffset = 0

        /**
         * 开始绘制的坐标位置
         */
        val mOffsetRect = Rect()

        /**
         * 最近一次的滑动数量
         */
        var mLastScrollDelta = 0

        /**
         * 需要补充滑动的距离
         */
        var replenishDelta = 0
        fun setOffsetRect(left: Int, top: Int, right: Int, bottom: Int) {
            mOffsetRect[left, top, right] = bottom
        }

        fun next(recycler: Recycler): View {
            return recycler.getViewForPosition(mCurrentPosition)
        }

        fun hasMore(state: RecyclerView.State): Boolean {
            return mCurrentPosition >= 0 && mCurrentPosition < state.itemCount
        }

        /**
         * @param currentPosition 当前的位置
         * @param orientation     方向
         * @param rows            行数
         * @param columns         列数
         * @param state           状态
         * @return 下一个位置
         */
        fun getNextPosition(
            currentPosition: Int,
            orientation: Int,
            rows: Int,
            columns: Int,
            state: RecyclerView.State
        ): Int {
            var position: Int
            val onePageSize = rows * columns

            val surplus = currentPosition % onePageSize
            //水平滑动
            //向后追加item
            if (surplus == onePageSize - 1) {
                //一页的最后一个位置
                position = currentPosition + 1
            } else {
                //在第几列
                val columnsIndex = currentPosition % columns
                //在第几行
                val rowIndex = surplus / columns
                //是否在最后一行
                val isLastRow = rowIndex == rows - 1
                if (isLastRow) {
                    position = currentPosition - rowIndex * columns + 1
                } else {
                    position = currentPosition + columns
                    if (position >= state.itemCount) {
                        //越界了
                        if (columnsIndex != columns - 1) {
                            //如果不是最后一列，计算换行位置
                            position = currentPosition - rowIndex * columns + 1
                        }
                    }
                }
            }
            return position
        }

        /**
         * @param currentPosition 当前的位置
         * @param orientation     方向
         * @param rows            行数
         * @param columns         列数
         * @param state           状态
         * @return 上一个位置
         */
        fun getPrePosition(
            currentPosition: Int,
            orientation: Int,
            rows: Int,
            columns: Int,
            state: RecyclerView.State?
        ): Int {
            val position: Int
            val onePageSize = rows * columns
            val surplus = currentPosition % onePageSize
            //水平滑动
            //向前追加item
            position = if (surplus == 0) {
                //一页的第一个位置
                currentPosition - 1
            } else {
                //在第几行
                val rowIndex = surplus / columns
                //是否在第一行
                val isFirstRow = rowIndex == 0
                if (isFirstRow) {
                    currentPosition - 1 + (rows - 1) * columns
                } else {
                    currentPosition - columns
                }
            }
            return position
        }

        companion object {
            const val LAYOUT_START = -1
            const val LAYOUT_END = 1
            const val SCROLLING_OFFSET_NaN = Int.MIN_VALUE
        }
    }

    companion object {
        private const val TAG = "PagerGridLayoutManager"
    }
}