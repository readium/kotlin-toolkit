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

internal class PagerNestedScrollConnection(
    private val state: PagerState,
    private val flingBehavior: TargetedFlingBehavior,
    private val orientation: Orientation,
) : NestedScrollConnection {

    private var spreadConsumedVertically = false

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

        check(state.layoutInfo.visiblePagesInfo.size == 1 || abs(consumed - available.x) < 1)
        return Offset(
            x = if (orientation == Orientation.Horizontal) consumed else available.x,
            y = if (orientation == Orientation.Vertical) consumed else available.y
        )
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.layoutInfo.visiblePagesInfo.size > 1) {
            state.scroll(scrollPriority = MutatePriority.Default) {
                with(flingBehavior) {
                    performFling(-available.x)
                }
            }

            return available
        }

        return Velocity.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (abs(consumed.y) > 0) {
            spreadConsumedVertically = true
        }

        if (spreadConsumedVertically) {
            return Offset.Zero
        }
        val consumedX = -state.dispatchRawDelta(-available.x)
        return Offset(consumedX, 0f)
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        if (spreadConsumedVertically) {
            spreadConsumedVertically = false
            return Velocity.Zero
        }

        var remaining = available.x

        state.scroll(scrollPriority = MutatePriority.Default) {
            with(flingBehavior) {
                remaining = -performFling(-available.x)
            }
        }

        if ((available.x - remaining).isNaN()) {
            return available
        }

        return Velocity(available.x - remaining, available.y)
    }
}
