/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.pager

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

internal class ScrollPagerNestedScrollConnection(
    private val state: PagerState,
    private val flingBehavior: TargetedFlingBehavior,
    private val orientation: Orientation,
) : NestedScrollConnection {

    private val PagerState.firstVisibleOffset get() =
        layoutInfo.visiblePagesInfo.first().offset

    private val PagerState.lastVisibleOffset get() =
        layoutInfo.visiblePagesInfo.last().offset

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (state.layoutInfo.visiblePagesInfo.size <= 1) {
            return Offset.Zero
        }

        // rounding and drag only
        if (source != NestedScrollSource.UserInput || abs(state.currentPageOffsetFraction) < 1e-6) {
            return Offset.Zero
        }

        val delta = if (orientation == Orientation.Horizontal) available.x else available.y

        val minBound = -(state.layoutInfo.pageSize + state.firstVisibleOffset).toFloat()

        val maxBound = (state.layoutInfo.pageSize - state.lastVisibleOffset).toFloat()

        val coerced = delta.coerceIn(minBound, maxBound)
        // dispatch and return reversed as usual
        val consumed = -state.dispatchRawDelta(-coerced)

        check(state.layoutInfo.visiblePagesInfo.size == 1 || abs(consumed - available.y) < 1)
        return Offset(
            x = if (orientation == Orientation.Horizontal) consumed else available.x,
            y = if (orientation == Orientation.Vertical) consumed else available.y
        )
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.layoutInfo.visiblePagesInfo.size > 1) {
            var remaining: Float = available.y
            state.scroll(scrollPriority = MutatePriority.Default) {
                with(flingBehavior) {
                    remaining = -performFling(-available.y)
                }
            }

            if ((available.y - remaining).isNaN()) {
                return available
            }

            return Velocity(0f, available.y - remaining)
        }

        return Velocity.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val consumedY = -state.dispatchRawDelta(-available.y)
        return Offset(0f, consumedY)
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        var remaining = available.y

        state.scroll(scrollPriority = MutatePriority.Default) {
            with(flingBehavior) {
                remaining = -performFling(-available.y)
            }
        }

        if ((available.y - remaining).isNaN()) {
            return available
        }

        return Velocity(available.x, available.y - remaining)
    }
}
