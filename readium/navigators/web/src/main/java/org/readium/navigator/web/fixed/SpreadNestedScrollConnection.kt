/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixed

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import org.readium.navigator.web.gestures.Fling2DBehavior
import org.readium.navigator.web.webview.WebViewScrollable2DState

internal class SpreadNestedScrollConnection(
    private val spreadIndex: Int,
    private val pagerState: PagerState,
    private val webviewState: WebViewScrollable2DState,
    private val scrollController: SpreadScrollState,
    private var flingBehavior: Fling2DBehavior,
) : NestedScrollConnection {

    var consumedHere: Boolean = false

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput) {
            return Offset.Zero
        }

        if (!pagerShowsOnlyThisSpread()) {
            // Let the main dispatcher scroll only horizontally.
            return Offset.Zero
        }

        val scrollController = scrollController.scrollController.value
            ?: return available

        consumedHere = true

        return -scrollController.scrollBy(-available)
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        var velocityLeft = available

        // The main dispatcher will do a fling in any case. We restrain ourselves from
        // doing one here if that of the main dispatcher will be significant to prevent
        // strange visual behaviors.
        if (consumedHere && pagerShowsOnlyThisSpread()) {
            webviewState.scroll {
                velocityLeft = with(flingBehavior) { -performFling(-velocityLeft) }
            }
        }

        consumedHere = false

        return Velocity(
            x = if ((available.x - velocityLeft.x).isNaN()) available.x else available.x - velocityLeft.x,
            y = if ((available.y - velocityLeft.y).isNaN()) available.y else available.y - velocityLeft.y
        )
    }

    private fun pagerShowsOnlyThisSpread(): Boolean {
        val visiblePages = pagerState.layoutInfo.visiblePagesInfo
        val otherPages = visiblePages.filter { it.index != spreadIndex }
        val mostlyThis = otherPages.all { abs(it.offset) > 0.95 * pagerState.layoutInfo.pageSize }
        if (mostlyThis) {
            pagerState.requestScrollToPage(spreadIndex)
        }
        return mostlyThis
    }
}
