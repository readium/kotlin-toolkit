/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.readium.navigator.web.snapping

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.absoluteValue
import org.readium.navigator.web.webview.DefaultPositionThreshold
import timber.log.Timber

internal interface PagingLayoutInfo {
    val orientation: Orientation
    val density: Density
    val positionThresholdFraction: Float get() =
        with(density) {
            val minThreshold = minOf(DefaultPositionThreshold.toPx(), pageSize / 2f)
            minThreshold / pageSize.toFloat()
        }
    val pageSize: Int
    val pageSpacing: Int
    val upDownDifference: Offset
    val reverseLayout: Boolean
    val visiblePageOffsets: List<Int>
    val canScrollForward: Boolean
    val canScrollBackward: Boolean
}

internal fun SnapLayoutInfoProvider(
    pagingLayoutInfo: PagingLayoutInfo,
    calculateFinalSnappingBound: (Float, Float, Float) -> Float,
): SnapLayoutInfoProvider {
    return object : SnapLayoutInfoProvider {

        fun Float.isValidDistance(): Boolean {
            return this != Float.POSITIVE_INFINITY && this != Float.NEGATIVE_INFINITY
        }

        override fun calculateSnapOffset(velocity: Float): Float {
            Timber.d("Fling calculateSnapOffset $velocity")
            val (lowerBoundOffset, upperBoundOffset) = searchForSnappingBounds()
            Timber.d("Fling lowerBound $lowerBoundOffset upperBound $upperBoundOffset")

            val finalDistance =
                calculateFinalSnappingBound(
                    velocity,
                    lowerBoundOffset,
                    upperBoundOffset
                )

            Timber.d("Fling finalDistance $finalDistance")

            check(
                finalDistance == lowerBoundOffset ||
                    finalDistance == upperBoundOffset ||
                    finalDistance == 0.0f
            ) {
                "Final Snapping Offset Should Be one of $lowerBoundOffset, $upperBoundOffset or 0.0"
            }

            debugLog { "Snapping to=$finalDistance" }

            return if (finalDistance.isValidDistance()) {
                finalDistance
            } else {
                0f
            }
        }

        override fun calculateApproachOffset(
            velocity: Float,
            decayOffset: Float,
        ): Float = 0f

        private fun searchForSnappingBounds(): Pair<Float, Float> {
            debugLog { "Calculating Snapping Bounds" }
            var lowerBoundOffset = Float.NEGATIVE_INFINITY
            var upperBoundOffset = Float.POSITIVE_INFINITY

            pagingLayoutInfo.visiblePageOffsets.fastForEach { offset ->

                // Find page that is closest to the snap position, but before it
                if (offset <= 0 && offset > lowerBoundOffset) {
                    lowerBoundOffset = offset.toFloat()
                }

                // Find page that is closest to the snap position, but after it
                if (offset >= 0 && offset < upperBoundOffset) {
                    upperBoundOffset = offset.toFloat()
                }
            }

            // If any of the bounds is unavailable, use the other.
            if (lowerBoundOffset == Float.NEGATIVE_INFINITY) {
                lowerBoundOffset = upperBoundOffset
            }

            if (upperBoundOffset == Float.POSITIVE_INFINITY) {
                upperBoundOffset = lowerBoundOffset
            }

            // Don't move if we are at the bounds

            val isDragging = pagingLayoutInfo.dragGestureDelta() != 0f

            if (!pagingLayoutInfo.canScrollForward) {
                upperBoundOffset = 0.0f
                // If we can not scroll forward but are trying to move towards the bound, set both
                // bounds to 0 as we don't want to move
                if (isDragging && pagingLayoutInfo.isScrollingForward()) {
                    lowerBoundOffset = 0.0f
                }
            }

            if (!pagingLayoutInfo.canScrollBackward) {
                lowerBoundOffset = 0.0f
                // If we can not scroll backward but are trying to move towards the bound, set both
                // bounds to 0 as we don't want to move
                if (isDragging && !pagingLayoutInfo.isScrollingForward()) {
                    upperBoundOffset = 0.0f
                }
            }
            return lowerBoundOffset to upperBoundOffset
        }
    }
}

private fun PagingLayoutInfo.isLtrDragging() = dragGestureDelta() > 0
private fun PagingLayoutInfo.isScrollingForward(): Boolean {
    val reverseScrollDirection = reverseLayout
    return (
        isLtrDragging() && reverseScrollDirection ||
            !isLtrDragging() && !reverseScrollDirection
        )
}

private fun PagingLayoutInfo.dragGestureDelta() = if (orientation == Orientation.Horizontal) {
    upDownDifference.x
} else {
    upDownDifference.y
}

private inline fun debugLog(generateMsg: () -> String) {
    if (debug) {
        println("PagerSnapLayoutInfoProvider: ${generateMsg()}")
    }
}

/**
 * Given two possible bounds that this Pager can settle in represented by [lowerBoundOffset] and
 * [upperBoundOffset], this function will decide which one of them it will settle to.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun calculateFinalSnappingBound(
    pagingLayoutInfo: PagingLayoutInfo,
    layoutDirection: LayoutDirection,
    snapPositionalThreshold: Float,
    flingVelocity: Float,
    lowerBoundOffset: Float,
    upperBoundOffset: Float,
): Float {
    val isForward = if (pagingLayoutInfo.orientation == Orientation.Vertical) {
        pagingLayoutInfo.isScrollingForward()
    } else {
        if (layoutDirection == LayoutDirection.Ltr) {
            pagingLayoutInfo.isScrollingForward()
        } else {
            !pagingLayoutInfo.isScrollingForward()
        }
    }
    debugLog {
        "isLtrDragging=${pagingLayoutInfo.isLtrDragging()} " +
            "isForward=$isForward " +
            "layoutDirection=$layoutDirection"
    }
    // how many pages have I scrolled using a drag gesture.
    val pageSize = pagingLayoutInfo.pageSize
    val offsetFromSnappedPosition =
        if (pageSize == 0) {
            0f
        } else {
            pagingLayoutInfo.dragGestureDelta() / pageSize.toFloat()
        }

    // we're only interested in the decimal part of the offset.
    val offsetFromSnappedPositionOverflow =
        offsetFromSnappedPosition - offsetFromSnappedPosition.toInt().toFloat()

    // If the velocity is not high, use the positional threshold to decide where to go.
    // This is applicable mainly when the user scrolls and lets go without flinging.
    val finalSnappingItem =
        with(pagingLayoutInfo.density) { calculateFinalSnappingItem(flingVelocity) }

    debugLog {
        "\nfinalSnappingItem=$finalSnappingItem" +
            "\nlower=$lowerBoundOffset" +
            "\nupper=$upperBoundOffset"
    }

    return when (finalSnappingItem) {
        FinalSnappingItem.ClosestItem -> {
            if (offsetFromSnappedPositionOverflow.absoluteValue > snapPositionalThreshold) {
                // If we crossed the threshold, go to the next bound
                debugLog { "Crossed Snap Positional Threshold" }
                if (isForward) upperBoundOffset else lowerBoundOffset
            } else {
                // if we haven't crossed the threshold. but scrolled minimally, we should
                // bound to the previous bound
                if (abs(offsetFromSnappedPosition) >=
                    abs(pagingLayoutInfo.positionThresholdFraction)
                ) {
                    debugLog { "Crossed Positional Threshold Fraction" }
                    if (isForward) lowerBoundOffset else upperBoundOffset
                } else {
                    // if we haven't scrolled minimally, settle for the closest bound
                    debugLog { "Snap To Closest" }
                    if (lowerBoundOffset.absoluteValue < upperBoundOffset.absoluteValue) {
                        lowerBoundOffset
                    } else {
                        upperBoundOffset
                    }
                }
            }
        }

        FinalSnappingItem.NextItem -> upperBoundOffset
        FinalSnappingItem.PreviousItem -> lowerBoundOffset
        else -> 0f
    }
}

private val debug: Boolean = false
