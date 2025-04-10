/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixed

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import org.readium.navigator.web.pager.PageScrollState
import org.readium.navigator.web.snapping.PagingLayoutInfo

internal class FixedPagingLayoutInfo(
    private val pagerState: PagerState,
    private val pageStates: List<PageScrollState>,
    override val orientation: Orientation,
    override val density: Density,
) : PagingLayoutInfo {

    override val pageSize: Int get() =
        pagerState.layoutInfo.pageSize

    override val pageSpacing: Int get() =
        0

    override val upDownDifference: Offset
        get() = Offset.Zero

    override val reverseLayout: Boolean get() =
        false

    override val canScrollForward: Boolean
        get() = pagerState.layoutInfo.visiblePagesInfo.last()
            .let { pageInfo -> pageInfo.index < pagerState.pageCount - 1 || pageInfo.offset > 0 } || run {
            val lastResourceState = pageStates[pagerState.pageCount - 1]
            val scrollController = lastResourceState.scrollController.value ?: return false
            return scrollController.canMoveRight
        }

    override val canScrollBackward: Boolean
        get() = pagerState.layoutInfo.visiblePagesInfo.first()
            .let { pageInfo -> pageInfo.index > 0 || pageInfo.offset < 0 } || run {
            val scrollController = pageStates[0].scrollController.value ?: return false
            return scrollController.canMoveLeft
        }

    override val visiblePageOffsets: List<Int>
        get() = pagerState.layoutInfo.visiblePagesInfo.map { it.offset }
}
