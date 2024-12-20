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

    private var consumedCrossAxis = false

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
        val consumed = -state.dispatchRawDelta(-coerced)

        return Offset(
            x = if (orientation == Orientation.Horizontal) consumed else available.x,
            y = if (orientation == Orientation.Vertical) consumed else available.y
        )
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.layoutInfo.visiblePagesInfo.size > 1) {
            val availableDir =
                if (orientation == Orientation.Horizontal) available.x else available.y

            var remaining: Float = availableDir
            state.scroll(scrollPriority = MutatePriority.Default) {
                with(flingBehavior) {
                    remaining = -performFling(-availableDir)
                }
            }

            val consumed =
                if ((availableDir - remaining).isNaN()) {
                    availableDir
                } else {
                    availableDir - remaining
                }

            return Velocity(
                x = if (orientation == Orientation.Horizontal) consumed else 0f,
                y = if (orientation == Orientation.Vertical) consumed else 0f
            )
        }

        return Velocity.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (orientation == Orientation.Horizontal && abs(consumed.y) > 0 ||
            orientation == Orientation.Vertical && abs(consumed.x) > 0
        ) {
            consumedCrossAxis = true
            return Offset.Zero
        }

        val availableDir =
            if (orientation == Orientation.Horizontal) available.x else available.y

        val consumed = -state.dispatchRawDelta(-availableDir)
        return Offset(
            x = if (orientation == Orientation.Horizontal) consumed else 0f,
            y = if (orientation == Orientation.Vertical) consumed else 0f
        )
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        if (consumedCrossAxis) {
            consumedCrossAxis = false
            return Velocity.Zero
        }

        val availableDir =
            if (orientation == Orientation.Horizontal) available.x else available.y

        var remaining = availableDir
        state.scroll(scrollPriority = MutatePriority.Default) {
            with(flingBehavior) {
                remaining = -performFling(-availableDir)
            }
        }

        val consumed =
            if ((availableDir - remaining).isNaN()) {
                availableDir
            } else {
                availableDir - remaining
            }

        return Velocity(
            x = if (orientation == Orientation.Horizontal) consumed else available.x,
            y = if (orientation == Orientation.Vertical) consumed else available.y
        )
    }
}
