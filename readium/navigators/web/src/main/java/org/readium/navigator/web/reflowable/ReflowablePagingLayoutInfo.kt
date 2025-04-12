/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import org.readium.navigator.web.pager.PagingLayoutInfo
import timber.log.Timber

internal class ReflowablePagingLayoutInfo(
    private val pagerState: PagerState,
    private val resourceStates: List<ReflowableResourceState>,
    override val density: Density,
) : PagingLayoutInfo {

    override val orientation: Orientation =
        Orientation.Horizontal

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
            val lastResourceState = resourceStates[pagerState.pageCount - 1]
            val scrollController = lastResourceState.scrollController.value ?: return false
            return scrollController.canMoveRight
        }

    override val canScrollBackward: Boolean
        get() = pagerState.layoutInfo.visiblePagesInfo.first()
            .let { pageInfo -> pageInfo.index > 0 || pageInfo.offset < 0 } || run {
            val scrollController = resourceStates[0].scrollController.value ?: return false
            return scrollController.canMoveLeft
        }

    override val visiblePageOffsets: List<Int>
        get() = buildList<Int> {
            val pagerVisible = pagerState.layoutInfo.visiblePagesInfo
                .map { it.index to it.offset }
            val webViewVisible = pagerVisible.map {
                resourceStates[it.first].scrollController.value?.scrollX to
                    resourceStates[it.first].scrollController.value?.maxScrollX
            }
            Timber.d("pager $pagerVisible webview $webViewVisible")

            val pageSize = pageSize
            val firstPage = pagerState.layoutInfo.visiblePagesInfo.first()
            val lastPage = pagerState.layoutInfo.visiblePagesInfo.last()

            if (firstPage.index == lastPage.index) {
                val index = firstPage.index
                val scrollController = resourceStates[index].scrollController.value!!
                add(-scrollController.scrollX % pageSize)
                add(-scrollController.scrollX % pageSize + pageSize)
                Timber.d("visiblePageOffsets if $this")
            } else {
                val firstScrollController = resourceStates[firstPage.index].scrollController.value!!
                val lastScrollController = resourceStates[lastPage.index].scrollController.value!!
                // To get to the left bound, we first need to scroll the last visible page to zero.
                add(lastPage.offset - pageSize - lastScrollController.scrollX)
                // To get to the right bound, we first need to scroll the first visible page
                // as far as possible.
                add(lastPage.offset + firstScrollController.maxScrollX - firstScrollController.scrollX)
                Timber.d("visiblePageOffsets else $this")
            }
        }
}
