/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import org.readium.navigator.web.gestures.Fling2DBehavior
import org.readium.navigator.web.gestures.Scroll2DScope
import org.readium.navigator.web.webview.WebViewScrollController
import timber.log.Timber

internal interface PageScrollState {

    val scrollController: MutableState<WebViewScrollController?>
}

internal class ScrollDispatcher(
    private val pagerState: PagerState,
    private val resourceStates: List<PageScrollState>,
    internal var pagerOrientation: Orientation,
    internal var flingBehavior: Fling2DBehavior,
) : Scroll2DScope {

    override fun scrollBy(pixels: Offset): Offset {
        return -rawScrollBy(-pixels.mainAxisValue).mainAxisOffset
    }

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
                    val nextPage = (firstPage.index + 1).takeIf { it < resourceStates.size }
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
        val scrollController = resourceStates[targetPage].scrollController.value
            ?: return false
        scrollController.moveToProgression(
            progression = if (end) 1.0 else 0.0,
            snap = false,
            orientation = pagerOrientation
        )
        return true
    }

    private fun consumeInWebview(targetPage: Int, available: Float): Float {
        val scrollController = resourceStates[targetPage].scrollController.value
            ?: return available // WebView is not ready, consume everything.

        return -scrollController.scrollBy(-available.mainAxisOffset).mainAxisValue
    }

    fun onScroll(available: Offset): Offset {
        Timber.d("onScroll ${available.x}")
        return -scrollBy(-available)
    }

    suspend fun onFling(available: Velocity): Velocity {
        Timber.d("onFling ${available.x}")
        var velocityLeft = available
        pagerState.scroll {
            with(flingBehavior) {
                with(this@ScrollDispatcher) {
                    velocityLeft = -performFling(-velocityLeft)
                }
            }
        }

        return Velocity(
            x = if ((available.x - velocityLeft.x).isNaN()) {
                available.x
            } else {
                available.x - velocityLeft.x
            },
            y = if ((available.y - velocityLeft.y).isNaN()) {
                available.y
            } else {
                available.y - velocityLeft.y
            }
        )
    }

    fun onDocumentResized(index: Int) {
        val firstPage = pagerState.layoutInfo.visiblePagesInfo.first()

        val lastPage = pagerState.layoutInfo.visiblePagesInfo.last()

        if (firstPage == lastPage || firstPage.index != index) {
            return
        }

        val scrollController = resourceStates[index].scrollController.value!!
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
}
