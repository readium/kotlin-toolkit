package org.readium.r2.testapp.opds

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue

class GridAutoFitLayoutManager : GridLayoutManager {
    private var mColumnWidth: Int = 0
    private var mColumnWidthChanged = true
    private var mWidthChanged = true
    private var mWidth: Int = 0

    constructor(context: Context, columnWidth: Int) : super(context, 1) {
        setColumnWidth(checkedColumnWidth(context, columnWidth))
    }/* Initially set spanCount to 1, will be changed automatically later. */

    constructor(context: Context, columnWidth: Int, orientation: Int, reverseLayout: Boolean) : super(context, 1, orientation, reverseLayout) {
        setColumnWidth(checkedColumnWidth(context, columnWidth))
    }/* Initially set spanCount to 1, will be changed automatically later. */

    private fun checkedColumnWidth(context: Context, columnWidth: Int): Int {
        var columnWidth = columnWidth
        if (columnWidth <= 0) {
            columnWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sColumnWidth.toFloat(),
                    context.resources.displayMetrics).toInt()
        } else {
            columnWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, columnWidth.toFloat(),
                    context.resources.displayMetrics).toInt()
        }
        return columnWidth
    }

    private fun setColumnWidth(newColumnWidth: Int) {
        if (newColumnWidth > 0 && newColumnWidth != mColumnWidth) {
            mColumnWidth = newColumnWidth
            mColumnWidthChanged = true
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        val width = width
        val height = height

        if (width != mWidth) {
            mWidthChanged = true
            mWidth = width
        }

        if (mColumnWidthChanged && mColumnWidth > 0 && width > 0 && height > 0 || mWidthChanged) {
            val totalSpace: Int
            if (orientation == LinearLayoutManager.VERTICAL) {
                totalSpace = width - paddingRight - paddingLeft
            } else {
                totalSpace = height - paddingTop - paddingBottom
            }
            val spanCount = Math.max(1, totalSpace / mColumnWidth)
            setSpanCount(spanCount)
            mColumnWidthChanged = false
            mWidthChanged = false
        }
        super.onLayoutChildren(recycler, state)
    }

    companion object {
        private val sColumnWidth = 200 // assume cell width of 200dp
    }
}