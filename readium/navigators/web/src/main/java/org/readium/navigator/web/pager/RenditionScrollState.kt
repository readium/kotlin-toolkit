/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.pager

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import kotlinx.coroutines.coroutineScope
import org.readium.navigator.web.gestures.Scroll2DScope
import org.readium.navigator.web.gestures.Scrollable2DState
import org.readium.navigator.web.webview.WebViewScrollController
import timber.log.Timber

internal interface PageScrollState {

    val scrollController: MutableState<WebViewScrollController?>
}

internal class RenditionScrollState(
    private val pagerState: PagerState,
    private val pageStates: List<PageScrollState>,
    internal var pagerOrientation: Orientation,
) : Scrollable2DState {

    /*
     * To favour natural reasoning, this function applies reverse scrolling:
     * - a positive delta makes scrolling left
     * - a negative delta makes scrolling right
     */
    private fun rawScrollBy(available: Float): Float {
        Timber.d("scrollBy available $available")
        var deltaLeft = available

        val firstPage = pagerState.layoutInfo.visiblePagesInfo.first()

        val lastPage = pagerState.layoutInfo.visiblePagesInfo.last()

        if (firstPage == lastPage) {
            // Set the page that will become visible to the right scroll position.
            when {
                available > 0 -> {
                    val prevPage = (firstPage.index - 1).takeIf { it >= 0 }
                    prevPage?.let {
                        val success = scrollWebviewToEdge(prevPage, end = true)
                        if (!success) return available
                    }
                }
                else -> {
                    val nextPage = (firstPage.index + 1).takeIf { it < pageStates.size }
                    nextPage?.let {
                        val success = scrollWebviewToEdge(nextPage, end = false)
                        if (!success) return available
                    }
                }
            }
        }

        val firstTargetPage = when {
            available >= 0 -> lastPage
            else -> firstPage
        }

        val secondTargetPage = when {
            available >= 0 -> firstPage
            else -> lastPage
        }

        val consumedInFirst = consumeInWebview(firstTargetPage.index, deltaLeft)
        deltaLeft -= consumedInFirst
        Timber.d("consumed $consumedInFirst in ${firstTargetPage.index}")

        val availableForPager =
            if (firstPage.index == lastPage.index) {
                when {
                    deltaLeft > 0 ->
                        deltaLeft.fastCoerceAtMost(pagerState.layoutInfo.pageSize.toFloat())
                    deltaLeft < 0 ->
                        deltaLeft.coerceAtLeast(-pagerState.layoutInfo.pageSize.toFloat())
                    else ->
                        0f
                }
            } else {
                when {
                    deltaLeft > 0 -> {
                        deltaLeft.fastCoerceAtMost(-firstPage.offset.toFloat())
                    }
                    deltaLeft < 0 -> {
                        deltaLeft.fastCoerceAtLeast(-lastPage.offset.toFloat())
                    }
                    else ->
                        0f
                }
            }

        val consumedInPager = -pagerState.dispatchRawDelta(-availableForPager)
        deltaLeft -= consumedInPager
        Timber.d("consumed $consumedInPager in pager")

        val consumedInSecond = consumeInWebview(secondTargetPage.index, deltaLeft)
        deltaLeft -= consumedInSecond
        Timber.d("consumed $consumedInSecond in ${secondTargetPage.index}")

        Timber.d("scrollBy left $deltaLeft")

        return when (deltaLeft) {
            0f -> available
            available -> 0f
            else -> rawScrollBy(deltaLeft)
        }
    }

    private fun scrollWebviewToEdge(targetPage: Int, end: Boolean): Boolean {
        val scrollController = pageStates[targetPage].scrollController.value
            ?: return false
        scrollController.moveToProgression(
            progression = if (end) 1.0 else 0.0,
            snap = false,
            orientation = pagerOrientation
        )
        return true
    }

    private fun consumeInWebview(targetPage: Int, available: Float): Float {
        val scrollController = pageStates[targetPage].scrollController.value
            ?: return available // WebView is not ready, consume everything.

        return -scrollController.scrollBy(-available.mainAxisOffset).mainAxisValue
    }

    fun onDocumentResized(index: Int) {
        val firstPage = pagerState.layoutInfo.visiblePagesInfo.first()

        val lastPage = pagerState.layoutInfo.visiblePagesInfo.last()

        if (firstPage == lastPage || firstPage.index != index) {
            return
        }

        val scrollController = pageStates[index].scrollController.value!!
        val scrolled = scrollController.scrollToEnd(pagerOrientation)
        if (scrolled > 0) {
            rawScrollBy(scrolled.toFloat())
        }
    }

    private val Offset.mainAxisValue: Float get() = when (pagerOrientation) {
        Orientation.Vertical -> y
        Orientation.Horizontal -> x
    }

    private val Float.mainAxisOffset: Offset
        get() = when (pagerOrientation) {
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
            val delta = -rawScrollBy(-coercedPixels.mainAxisValue).mainAxisOffset
            return delta
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
        return -rawScrollBy(-delta.mainAxisValue).mainAxisOffset
    }

    override val isScrollInProgress: Boolean
        get() = isScrollingState.value || pagerState.isScrollInProgress
}
