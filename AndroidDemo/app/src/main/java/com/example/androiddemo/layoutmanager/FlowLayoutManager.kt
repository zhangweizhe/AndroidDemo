package com.example.androiddemo.layoutmanager

import androidx.recyclerview.widget.RecyclerView
import java.lang.Math.max

class FlowLayoutManager: RecyclerView.LayoutManager() {

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT)
    }

    var verticalOffset = 0
    var firstVisiblePosition = 0
    var lastVisiblePosition = 0

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (itemCount == 0) {
            // 没有item，回收ViewHolder
            detachAndScrapAttachedViews(recycler)
            return
        }

        detachAndScrapAttachedViews(recycler)

        // 初始化
        verticalOffset = 0
        firstVisiblePosition = 0
        lastVisiblePosition = itemCount

        fill(recycler, state, 0)

    }

    private fun fill(recycler: RecyclerView.Recycler, state: RecyclerView.State, dy: Int) {
        var topOffset = paddingTop
        var leftOffset = paddingLeft
        var maxHeightCurLine = 0
        var minPosition = firstVisiblePosition
        lastVisiblePosition = itemCount - 1
        for (i in minPosition..lastVisiblePosition) {
            val child = recycler.getViewForPosition(i)
            addView(child)
            measureChildWithMargins(child, 0, 0)
            val childDecoratedMeasuredWidth = getDecoratedMeasuredWidth(child)
            val childDecoratedMeasuredHeight = getDecoratedMeasuredHeight(child)
            if (leftOffset + childDecoratedMeasuredWidth <= getHorizontalSpace()) {
                // 当前行还放得下
                layoutDecoratedWithMargins(child, leftOffset, topOffset, leftOffset + childDecoratedMeasuredWidth, topOffset + childDecoratedMeasuredHeight)
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
                if (topOffset - dy > height - paddingBottom) {
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
    }

    private fun getHorizontalSpace(): Int {
        return width - paddingLeft - paddingRight
    }

    private fun getVerticalSpace(): Int {
        return height - paddingTop - paddingBottom
    }
}