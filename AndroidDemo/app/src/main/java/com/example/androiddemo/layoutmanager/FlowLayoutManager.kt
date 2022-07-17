package com.example.androiddemo.layoutmanager

import android.graphics.Rect
import android.util.Log
import android.util.SparseArray
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import java.lang.Math.max

/**
 * 流式布局 LayoutManager
 * https://blog.csdn.net/zxt0601/article/details/52956504
 */
class FlowLayoutManager: RecyclerView.LayoutManager() {

    init {

    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT)
    }

    var verticalOffset = 0
    var firstVisiblePosition = 0
    var lastVisiblePosition = 0
    val itemRects = SparseArray<Rect>()

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (itemCount == 0) {
            // 没有item，回收ViewHolder
            detachAndScrapAttachedViews(recycler)
            return
        }
        if (childCount == 0 && state.isPreLayout) {//state.isPreLayout()是支持动画的
            Log.i(TAG, "onLayoutChildren: childCount == 0 && state.isPreLayout")
            return;
        }
        detachAndScrapAttachedViews(recycler)

        // 初始化
        verticalOffset = 0
        firstVisiblePosition = 0
        lastVisiblePosition = itemCount

        fill(recycler, state, 0)

    }

    private fun fill(recycler: RecyclerView.Recycler, state: RecyclerView.State, dy: Int): Int {
        var realOffset = dy
        var topOffset = paddingTop
        Log.i(TAG, "fill: childCount=$childCount")
        if (childCount > 0) {
            // 滑动过程中进入这个方法的
            for (i in childCount-1 downTo 0) {
                val childAt = getChildAt(i)!!
                if (realOffset > 0) {
                    Log.i(TAG, "fill: 向上滑动，i=$i")
                    Log.i(TAG, "fill: 向上滑动，realOffset=$realOffset")
                    // 需要回收当前屏幕，上越界的View
                    val decoratedBottom = getDecoratedBottom(childAt)
                    Log.i(TAG, "fill: 向上滑动，decoratedBottom=$decoratedBottom")
                    if (decoratedBottom - realOffset < topOffset) {
                        Log.i(TAG, "fill:  向上滑动，removeAndRecycleView")
                        removeAndRecycleView(childAt, recycler)
                        firstVisiblePosition++
                        continue
                    }
                }else if(realOffset < 0) {
                    Log.i(TAG, "fill: 向下滑动，realOffset=$realOffset")
                    if (getDecoratedTop(childAt) - realOffset > height - paddingBottom) {
                        Log.i(TAG, "fill:  向下滑动，removeAndRecycleView")
                        removeAndRecycleView(childAt, recycler)
                        lastVisiblePosition--
                        continue
                    }
                }
            }
//            detachAndScrapAttachedViews(recycler);
        }

        var leftOffset = paddingLeft
        var maxHeightCurLine = 0
        if (realOffset >= 0) {
            var minPosition = firstVisiblePosition
            lastVisiblePosition = itemCount - 1
            if (childCount > 0) {
                val lastView = getChildAt(childCount - 1)!!
                minPosition = getPosition(lastView)
                topOffset = getDecoratedTop(lastView)
                leftOffset = getDecoratedLeft(lastView)
                maxHeightCurLine = max(maxHeightCurLine, getDecoratedMeasurementVertical(lastView))
            }
            for (i in minPosition..lastVisiblePosition) {
                val child = recycler.getViewForPosition(i)
                addView(child)
                measureChildWithMargins(child, 0, 0)
                val childDecoratedMeasuredWidth = getDecoratedMeasuredWidth(child)
                val childDecoratedMeasuredHeight = getDecoratedMeasuredHeight(child)
                if (leftOffset + getDecoratedMeasurementHorizontal(child) <= getHorizontalSpace()) {
                    // 当前行还放得下
                    layoutDecoratedWithMargins(child, leftOffset, topOffset, leftOffset + getDecoratedMeasurementHorizontal(child), topOffset + getDecoratedMeasurementVertical(child))

                    // 保存Rect，供逆序layout用
                    val rect = Rect(leftOffset, topOffset+verticalOffset, leftOffset+getDecoratedMeasurementHorizontal(child), topOffset + getDecoratedMeasurementVertical(child) + verticalOffset)
                    itemRects.put(i, rect)

                    leftOffset += childDecoratedMeasuredWidth
                    // 更新当前行的最大高度
                    maxHeightCurLine = max(maxHeightCurLine, childDecoratedMeasuredHeight)
                }else {
                    // 当前行放不下了，新起一行
                    leftOffset = paddingLeft
                    topOffset += maxHeightCurLine
                    // 重置当前行的高度
                    maxHeightCurLine = 0

                    // 判断是否超过下边界
                    if (topOffset - realOffset > height - paddingBottom) {
                        // 越界了，回收view
                        removeAndRecycleView(child, recycler)
                        lastVisiblePosition = i - 1
                        break
                    }else {
                        // 没有越界，继续向下布局
                        layoutDecoratedWithMargins(child, leftOffset, topOffset, leftOffset + childDecoratedMeasuredWidth, topOffset + childDecoratedMeasuredHeight)
                        leftOffset += childDecoratedMeasuredWidth
                        maxHeightCurLine = max(maxHeightCurLine, childDecoratedMeasuredHeight)
                    }
                }
            }
        }else {
            Log.i(TAG, "fill: 向下滑动，逆序layout")
            // 正序排列时，保存每个子View的Rect，逆序时，直接拿出来layout
            var maxPos = itemCount - 1
            firstVisiblePosition = 0
            if (childCount > 0) {
                val firstView = getChildAt(0)
                maxPos = getPosition(firstView!!) - 1
            }
            for (i in maxPos downTo  firstVisiblePosition) {
                val rect = itemRects.get(i)
                if (rect.bottom - verticalOffset - realOffset < paddingTop) {
                    firstVisiblePosition = i+1
                    break
                }else {
                    val child = recycler.getViewForPosition(i)
                    addView(child, 0)
                    measureChildWithMargins(child, 0, 0)
                    layoutDecoratedWithMargins(child, rect.left, rect.top - verticalOffset, rect.right, rect.bottom - verticalOffset)
                }
            }
        }
        //添加完后，判断是否已经没有更多的ItemView，并且此时屏幕仍有空白，则需要修正dy
        //添加完后，判断是否已经没有更多的ItemView，并且此时屏幕仍有空白，则需要修正dy
        val lastChild = getChildAt(childCount - 1)
        if (getPosition(lastChild!!) == itemCount - 1) {
            val gap = height - paddingBottom - getDecoratedBottom(lastChild)
            if (gap > 0) {
                realOffset -= gap
            }
        }
        Log.d(TAG, "count= [" + childCount + "]" + ",[recycler.getScrapList().size():" + recycler.scrapList.size + ", dy:" + dy + ",  mVerticalOffset" + verticalOffset + ", ");
        return realOffset
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    /**
     * 手指向下滑动时，[dy]为负，否则为正
     */
    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        Log.d(
            Companion.TAG,
            "scrollVerticallyBy() called with: dy = $dy"
        )
        if (dy == 0 || itemCount == 0) {
            // 位移为0，或者没有子View，不用移动
            return 0
        }
        var realOffset = dy
        if (realOffset + verticalOffset < 0) {
            // 向下滑动，到达上边界
            realOffset = -verticalOffset
        }else if (realOffset > 0) {
            // 向上滑动
            //利用最后一个子View比较修正
            //利用最后一个子View比较修正
            val lastChild = getChildAt(childCount - 1)
            if (getPosition(lastChild!!) == itemCount - 1) {
                val gap = height - paddingBottom - getDecoratedBottom(lastChild)
                realOffset = if (gap > 0) {
                    -gap
                } else if (gap == 0) {
                    0
                } else {
                    Math.min(realOffset, -gap)
                }
            }
        }
        realOffset = fill(recycler, state, realOffset)
        offsetChildrenVertical(-realOffset)
        verticalOffset+=realOffset
        return realOffset
    }

    private fun getHorizontalSpace(): Int {
        return width - paddingLeft - paddingRight
    }

    private fun getVerticalSpace(): Int {
        return height - paddingTop - paddingBottom
    }

    private fun getDecoratedMeasurementVertical(view: View): Int {
        val lp = view.layoutParams as RecyclerView.LayoutParams
        return getDecoratedMeasuredHeight(view) + lp.topMargin + lp.bottomMargin
    }

    private fun getDecoratedMeasurementHorizontal(view: View): Int {
        val lp = view.layoutParams as RecyclerView.LayoutParams
        return getDecoratedMeasuredWidth(view) + lp.leftMargin + lp.rightMargin
    }

    companion object {
        private const val TAG = "FlowLayoutManager"
    }
}