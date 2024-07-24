package org.readium.navigator.web.util

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import timber.log.Timber

internal class PagerNestedConnection(
    private val state: PagerState,
    private val orientation: Orientation
) : NestedScrollConnection {

    private fun Velocity.consumeOnOrientation(orientation: Orientation): Velocity {
        return if (orientation == Orientation.Vertical) {
            copy(x = 0f)
        } else {
            copy(y = 0f)
        }
    }

    private fun Offset.mainAxis(): Float =
        if (orientation == Orientation.Horizontal) this.x else this.y

    private val PagerState.firstVisibleOffset get() =
        layoutInfo.visiblePagesInfo.first().offset

    private val PagerState.lastVisibleOffset get() =
        layoutInfo.visiblePagesInfo.last().offset

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        Timber.d("pagerNestedConnection preScroll available $available")

        if (state.layoutInfo.visiblePagesInfo.size <= 1) {
            Timber.d("pagerNestedConnection only one page $available")
            return Offset.Zero
        }

        // rounding and drag only
        if (source != NestedScrollSource.UserInput || abs(state.currentPageOffsetFraction) < 1e-6) {
            return Offset.Zero
        }

        val delta = if (orientation == Orientation.Horizontal) available.x else available.y

        val minBound = -(state.layoutInfo.pageSize + state.firstVisibleOffset).toFloat()

        val maxBound = (state.layoutInfo.pageSize - state.lastVisibleOffset).toFloat()

        /*
        // find the current and next page (in the direction of dragging)
        val currentPageOffset = state.currentPageOffsetFraction * state.layoutInfo.pageSize
        val pageAvailableSpace = state.layoutInfo.pageSize + state.layoutInfo.pageSpacing
        val nextClosestPageOffset =
            currentPageOffset + pageAvailableSpace * -sign(state.currentPageOffsetFraction)

        val minBound: Float
        val maxBound: Float
        // build min and max bounds in absolute coordinates for nested scroll
        if (state.currentPageOffsetFraction > 0f) {
            minBound = nextClosestPageOffset
            maxBound = currentPageOffset
        } else {
            minBound = currentPageOffset
            maxBound = nextClosestPageOffset
        }


         */

        Timber.d("delta $delta minBound $minBound maxBound $maxBound")

        val coerced = delta.coerceIn(minBound, maxBound)
        // dispatch and return reversed as usual
        val consumed = -state.dispatchRawDelta(-coerced)

        Timber.d("consumed $consumed")

        check(state.layoutInfo.visiblePagesInfo.size == 1 || abs(consumed - available.x) < 1)
        return Offset(
            x = if (orientation == Orientation.Horizontal) consumed else available.x,
            y = if (orientation == Orientation.Vertical) consumed else available.y
        )
    }

    /*override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source == NestedScrollSource.SideEffect && available.mainAxis() != 0f) {
            throw CancellationException()
        }
        return Offset.Zero
    }*/

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return Velocity.Zero
        // return available.consumeOnOrientation(orientation)
    }
}
