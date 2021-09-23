/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import org.readium.r2.testapp.databinding.SectionHeaderBinding

class SectionDecoration(
    private val context: Context,
    private val listener: Listener
) : RecyclerView.ItemDecoration() {

    interface Listener {
        fun isStartOfSection(itemPos: Int): Boolean
        fun sectionTitle(itemPos: Int): String
    }

    private lateinit var headerView: View
    private lateinit var sectionTitleView: TextView

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val pos = parent.getChildAdapterPosition(view)
        initHeaderViewIfNeeded(parent)
        if (listener.sectionTitle(pos) != "" && listener.isStartOfSection(pos)) {
            sectionTitleView.text = listener.sectionTitle(pos)
            fixLayoutSize(headerView, parent)
            outRect.top = headerView.height
        }
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
        initHeaderViewIfNeeded(parent)

        val children = parent.children.toList()
        children.forEach { child ->
            val pos = parent.getChildAdapterPosition(child)
            if (pos != NO_POSITION && listener.sectionTitle(pos) != "" &&
                (listener.isStartOfSection(pos) || isTopChild(child, children))) {
                sectionTitleView.text = listener.sectionTitle(pos)
                fixLayoutSize(headerView, parent)
                drawHeader(c, child, headerView)
            }
        }
    }

    private fun initHeaderViewIfNeeded(parent: RecyclerView) {
        if (::headerView.isInitialized) return
        SectionHeaderBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        ).apply {
            headerView = root
            sectionTitleView = header
        }
    }

    private fun fixLayoutSize(v: View, parent: ViewGroup) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)
        val childWidth = ViewGroup.getChildMeasureSpec(widthSpec, parent.paddingStart + parent.paddingEnd, v.layoutParams.width)
        val childHeight = ViewGroup.getChildMeasureSpec(heightSpec, parent.paddingTop + parent.paddingBottom, v.layoutParams.height)
        v.measure(childWidth, childHeight)
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)
    }

    private fun drawHeader(c: Canvas, child: View, headerView: View) {
        c.run {
            save()
            translate(0F, maxOf(0, child.top - headerView.height).toFloat())
            headerView.draw(this)
            restore()
        }
    }

    private fun isTopChild(child: View, children: List<View>): Boolean {
        var tmp = child.top
        children.forEach { c ->
            tmp = minOf(c.top, tmp)
        }
        return child.top == tmp
    }
}
