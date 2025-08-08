/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.internals.pager

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.coroutineScope
import org.readium.navigator.common.Overflow
import org.readium.navigator.web.internals.gestures.Scroll2DScope
import org.readium.navigator.web.internals.gestures.Scrollable2DState
import org.readium.navigator.web.internals.util.toLayoutDirection
import org.readium.navigator.web.internals.util.toOrientation
import org.readium.navigator.web.internals.webview.WebViewScrollController
import org.readium.r2.shared.ExperimentalReadiumApi
import timber.log.Timber

public interface PageScrollState {

    public val scrollController: MutableState<WebViewScrollController?>
}

public class RenditionScrollState(
    private val pagerState: PagerState,
    private val pageStates: List<PageScrollState>,
    private val overflow: State<Overflow>,
) : Scrollable2DState {

    private val orientation: Orientation get() =
        overflow.value.axis.toOrientation()

    private val direction: LayoutDirection get() =
        overflow.value.readingProgression.toLayoutDirection()

    private val reverseLayout get() =
        orientation == Orientation.Horizontal && direction == LayoutDirection.Rtl

    /*
     * To ease the reasoning, this function applies reverse scrolling:
     * - a positive delta (finger moved to the right) makes the viewport scrolling left
     * - a negative delta (finger moved to the left) makes the viewport scrolling right
     */
    private fun dispatchRawDelta(available: Float): Float {
        Timber.d("scrollBy available $available")
        Timber.d("visiblePages ${pagerState.layoutInfo.visiblePagesInfo.map { it.index to it.offset }}")
        var deltaLeft = available

        val firstPage = pagerState.layoutInfo.visiblePagesInfo.first()

        val lastPage = pagerState.layoutInfo.visiblePagesInfo.last()

        // This should not be needed if all WebViews are properly initialized at suitable scroll
        // positions and any discontinuous move through the publication adjusts those positions.
        if (firstPage == lastPage) {
            // Set the page that will become visible to the right scroll position.
            when {
                available > 0 -> {
                    val prevPage = pageOnTheLeftOrTop(firstPage.index)
                    prevPage?.let {
                        val success = scrollWebviewToEdge(prevPage, max = true)
                        if (!success) return available
                    }
                }
                else -> {
                    val nextPage = pageOnTheRightOrBottom(firstPage.index)
                    nextPage?.let {
                        val success = scrollWebviewToEdge(nextPage, max = false)
                        if (!success) return available
                    }
                }
            }
        }

        var firstTargetPage = when {
            available >= 0 -> lastPage
            else -> firstPage
        }

        var secondTargetPage = when {
            available >= 0 -> firstPage
            else -> lastPage
        }

        if (reverseLayout) {
            val temp = firstTargetPage
            firstTargetPage = secondTargetPage
            secondTargetPage = temp
        }

        val consumedInFirst = consumeInWebview(firstTargetPage.index, deltaLeft)
        deltaLeft -= consumedInFirst
        Timber.d("consumed $consumedInFirst in ${firstTargetPage.index}")

        val pageSize = pagerState.layoutInfo.pageSize.toFloat()

        val availableForPager = when {
            firstPage.index == lastPage.index ->
                deltaLeft.fastCoerceIn(-pageSize, pageSize)
            reverseLayout ->
                deltaLeft.fastCoerceIn(firstPage.offset.toFloat(), lastPage.offset.toFloat())
            else ->
                deltaLeft.fastCoerceIn(-lastPage.offset.toFloat(), -firstPage.offset.toFloat())
        }

        val consumedInPager = pagerState.dispatchRawDelta(
            delta = availableForPager.reverseIfNeeded(reverseLayout)
        ).reverseIfNeeded(reverseLayout)

        deltaLeft -= consumedInPager
        Timber.d("consumed $consumedInPager in pager")

        val consumedInSecond = consumeInWebview(secondTargetPage.index, deltaLeft)
        deltaLeft -= consumedInSecond
        Timber.d("consumed $consumedInSecond in ${secondTargetPage.index}")

        Timber.d("scrollBy left $deltaLeft")

        return when (deltaLeft) {
            0f -> available
            available -> 0f
            else -> dispatchRawDelta(deltaLeft)
        }
    }

    private fun scrollWebviewToEdge(targetPage: Int, max: Boolean): Boolean {
        val scrollController = pageStates[targetPage].scrollController.value
            ?: return false
        Timber.d("scrolling to edge $targetPage max = $max $orientation $direction")
        if (max) {
            scrollController.scrollToMax(orientation)
        } else {
            scrollController.scrollToMin(orientation)
        }
        return true
    }

    private fun consumeInWebview(targetPage: Int, available: Float): Float {
        val scrollController = pageStates[targetPage].scrollController.value
            ?: return available // WebView is not ready, consume everything.

        return -scrollController.scrollBy(-available.mainAxisOffset).mainAxisValue
    }

    private fun pageOnTheLeftOrTop(index: Int): Int? =
        if (reverseLayout) {
            (index + 1).takeIf { it < pageStates.size }
        } else {
            (index - 1).takeIf { it >= 0 }
        }

    private fun pageOnTheRightOrBottom(index: Int): Int? =
        if (reverseLayout) {
            (index - 1).takeIf { it >= 0 }
        } else {
            (index + 1).takeIf { it < pageStates.size }
        }

    private fun Float.reverseIfNeeded(reverseLayout: Boolean): Float =
        if (reverseLayout) this else -this

    public fun onDocumentResized(index: Int) {
        val firstPage = pagerState.layoutInfo.visiblePagesInfo.first()

        val lastPage = pagerState.layoutInfo.visiblePagesInfo.last()

        if (firstPage == lastPage ||
            firstPage.index != index && direction == LayoutDirection.Ltr ||
            lastPage.index != index && direction == LayoutDirection.Rtl
        ) {
            return
        }

        val scrollController = pageStates[index].scrollController.value!!
        val scrolled = scrollController.scrollToMax(orientation)
        if (scrolled > 0) {
            dispatchRawDelta(scrolled.toFloat())
        }
    }

    private val Offset.mainAxisValue: Float get() = when (orientation) {
        Orientation.Vertical -> y
        Orientation.Horizontal -> x
    }

    private val Float.mainAxisOffset: Offset
        get() = when (orientation) {
            Orientation.Vertical -> Offset(0f, this)
            Orientation.Horizontal -> Offset(this, 0f)
        }

    private val scrollScope: Scroll2DScope = object : Scroll2DScope {
        override fun scrollBy(pixels: Offset): Offset {
            val coercedPixels = Offset(
                x = if (pixels.x.isNaN()) 0f else pixels.x,
                y = if (pixels.y.isNaN()) 0f else pixels.y
            )
            if (coercedPixels == Offset.Zero) return Offset.Zero
            val delta = coercedPixels.mainAxisValue.reverseIfNeeded(reverseLayout)
            val consumed = dispatchRawDelta(delta).reverseIfNeeded(reverseLayout).mainAxisOffset
            return consumed
        }
    }

    private val scrollMutex = MutatorMutex()

    private val isScrollingState = mutableStateOf(false)

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend Scroll2DScope.() -> Unit,
    ): Unit = coroutineScope {
        pagerState.scroll(scrollPriority) {
            scrollMutex.mutateWith(scrollScope, scrollPriority) {
                isScrollingState.value = true
                try {
                    block()
                } finally {
                    isScrollingState.value = false
                }
            }
        }
    }

    override fun dispatchRawDelta(delta: Offset): Offset {
        return dispatchRawDelta(delta.mainAxisValue).mainAxisOffset
    }

    override val isScrollInProgress: Boolean
        get() = isScrollingState.value || pagerState.isScrollInProgress
}
