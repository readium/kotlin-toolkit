package org.readium.r2.navigator2.view

import android.content.Context
import androidx.recyclerview.widget.LinearSmoothScroller

internal class NavigatorSmoothScroller(
    context: Context,
    target: Int
) : LinearSmoothScroller(context) {
    init {
        targetPosition = target
    }
    override fun getHorizontalSnapPreference(): Int {
        return SNAP_TO_START
    }

    override fun getVerticalSnapPreference(): Int {
        return SNAP_TO_START
    }
}