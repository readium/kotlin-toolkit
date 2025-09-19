/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout.spread

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import org.readium.navigator.web.internals.gestures.Fling2DBehavior
import org.readium.navigator.web.internals.pager.PageScrollState

internal class SpreadNestedScrollConnection(
    private val pagerState: PagerState,
    private val resourceStates: List<PageScrollState>,
    private val flingBehavior: Fling2DBehavior,
) : NestedScrollConnection {

    var consumedHere: Boolean = false

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput) {
            return Offset.Zero
        }

        if (!pagerShowsOnlyOneSpread()) {
            // Let the main dispatcher scroll only horizontally.
            return Offset.Zero
        }

        val scrollController = resourceStates[pagerState.currentPage].scrollController.value
            ?: return available

        consumedHere = true

        val consumed = -scrollController.scrollBy(-available)

        // Let the main dispatcher consume what's left on the horizontal axis.
        return consumed
    }

    override suspend fun onPreFling(
        available: Velocity,
    ): Velocity {
        var velocityLeft = available

        // The main dispatcher will do a fling in any case. We restrain ourselves from
        // doing one here if that of the main dispatcher will be significant to prevent
        // strange visual behaviors.
        if (consumedHere && pagerShowsOnlyOneSpread()) {
            val scrollController = resourceStates[pagerState.currentPage].scrollController.value
                ?: return available
            scrollController.scroll {
                velocityLeft = with(flingBehavior) { -performFling(-velocityLeft) }
            }
        }

        consumedHere = false

        return Velocity(
            x = if ((available.x - velocityLeft.x).isNaN()) available.x else available.x - velocityLeft.x,
            y = if ((available.y - velocityLeft.y).isNaN()) available.y else available.y - velocityLeft.y
        )
    }

    private fun pagerShowsOnlyOneSpread(): Boolean {
        val visiblePages = pagerState.layoutInfo.visiblePagesInfo
        val otherPages = visiblePages.filter { it.index != pagerState.currentPage }
        val mostlyCurrent = otherPages.all { abs(it.offset) > 0.95 * pagerState.layoutInfo.pageSize }
        if (mostlyCurrent) {
            pagerState.requestScrollToPage(pagerState.currentPage)
        }
        return mostlyCurrent
    }
}
