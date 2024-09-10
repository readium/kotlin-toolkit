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

internal class PagerNestedConnection(
    private val state: PagerState,
    private val flingBehavior: TargetedFlingBehavior,
    private val orientation: Orientation
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

        check(state.layoutInfo.visiblePagesInfo.size == 1 || abs(consumed - available.x) < 1)
        return Offset(
            x = if (orientation == Orientation.Horizontal) consumed else available.x,
            y = if (orientation == Orientation.Vertical) consumed else available.y
        )
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return Velocity.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val consumedX = -state.dispatchRawDelta(-available.x)
        return Offset(consumedX, 0f)
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
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
