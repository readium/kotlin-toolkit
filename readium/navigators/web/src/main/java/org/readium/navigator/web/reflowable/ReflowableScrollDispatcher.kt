/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import org.readium.navigator.web.pager.ScrollDispatcher
import timber.log.Timber

internal class ReflowableScrollDispatcher(
    private val pagerState: PagerState,
    private val resourceStates: List<ReflowableResourceState>,
    internal var pagerOrientation: Orientation,
    internal var flingBehavior: FlingBehavior,
) : ScrollDispatcher, ScrollScope {

    override fun scrollBy(available: Float): Float {
        return -rawScrollBy(-available)
    }

    private fun rawScrollBy(available: Float): Float {
        Timber.d("scrollBy available $available")
        var deltaLeft = available

        val firstPage = pagerState.layoutInfo.visiblePagesInfo.first()

        val lastPage = pagerState.layoutInfo.visiblePagesInfo.last()

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

    private fun consumeInWebview(targetPage: Int, available: Float): Float {
        val scrollController = resourceStates[targetPage].scrollController.value
            ?: return available // WebView is not ready, consume everything.

        return -scrollController.scrollBy(-available.mainAxisOffset).mainAxisValue
    }

    override fun onScroll(available: Offset): Offset {
        Timber.d("onScroll ${available.x}")
        val consumed = -scrollBy(-available.mainAxisValue)
        return consumed.mainAxisOffset
    }

    override suspend fun onFling(available: Velocity): Velocity {
        Timber.d("onFling ${available.x}")
        var velocityLeft = available.mainAxisValue
        pagerState.scroll {
            with(flingBehavior) {
                with(this@ReflowableScrollDispatcher) {
                    velocityLeft = -performFling(-velocityLeft)
                }
            }
        }

        return if ((available.mainAxisValue - velocityLeft).isNaN()) {
            available
        } else {
            available - velocityLeft.mainAxisVelocity
        }
    }

    private val Offset.mainAxisValue: Float get() = when (pagerOrientation) {
        Orientation.Vertical -> y
        Orientation.Horizontal -> x
    }

    private val Velocity.mainAxisValue: Float get() = when (pagerOrientation) {
        Orientation.Vertical -> y
        Orientation.Horizontal -> x
    }

    private val Float.mainAxisOffset: Offset get() = when (pagerOrientation) {
        Orientation.Vertical -> Offset(0f, this)
        Orientation.Horizontal -> Offset(this, 0f)
    }

    private val Float.mainAxisVelocity: Velocity get() = when (pagerOrientation) {
        Orientation.Vertical -> Velocity(0f, this)
        Orientation.Horizontal -> Velocity(this, 0f)
    }
}
