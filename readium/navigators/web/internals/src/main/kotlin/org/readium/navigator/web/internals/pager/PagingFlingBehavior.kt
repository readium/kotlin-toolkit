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

package org.readium.navigator.web.internals.pager

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.absoluteValue
import timber.log.Timber

public interface PagingLayoutInfo {
    public val orientation: Orientation
    public val density: Density
    public val positionThresholdFraction: Float get() =
        with(density) {
            val minThreshold = minOf(DefaultPositionThreshold.toPx(), pageSize / 2f)
            minThreshold / pageSize.toFloat()
        }
    public val pageSize: Int
    public val pageSpacing: Int
    public val upDownDifference: Offset
    public val reverseLayout: Boolean
    public val visiblePageOffsets: List<Int>
    public val canScrollForward: Boolean
    public val canScrollBackward: Boolean
}

/**
 * A [snapFlingBehavior] that will snap pages to the start of the layout. One can use the
 * given parameters to control how the snapping animation will happen.
 * @see snapFlingBehavior for more information
 * on what which parameter controls in the overall snapping animation.
 * @param layoutInfo The [PagingLayoutInfo] that describes the pager layout.
 * @param snapAnimationSpec The animation spec used to finally snap to the position. This
 * animation will be often used in 2 cases: 1) There was enough space to an approach animation,
 * the Pager will use [snapAnimationSpec] in the last step of the animation to settle the page
 * into position. 2) There was not enough space to run the approach animation.
 * @param snapPositionalThreshold If the fling has a low velocity (e.g. slow scroll),
 * this fling behavior will use this snap threshold in order to determine if the pager should
 * snap back or move forward. Use a number between 0 and 1 as a fraction of the page size that
 * needs to be scrolled before the Pager considers it should move to the next page.
 * For instance, if snapPositionalThreshold = 0.35, it means if this pager is scrolled with a
 * slow velocity and the Pager scrolls more than 35% of the page size, then will jump to the
 * next page, if not it scrolls back.
 * Note that any fling that has high enough velocity will *always* move to the next page
 * in the direction of the fling.
 *
 * @return An instance of [FlingBehavior] that will perform Snapping to the next page by
 * default.
 */
@Composable
public fun pagingFlingBehavior(
    layoutInfo: PagingLayoutInfo,
    decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
    snapAnimationSpec: AnimationSpec<Float> = spring(
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = Int.VisibilityThreshold.toFloat()
    ),
    @FloatRange(from = 0.0, to = 1.0) snapPositionalThreshold: Float = 0.5f,
): TargetedFlingBehavior {
    require(snapPositionalThreshold in 0f..1f) {
        "snapPositionalThreshold should be a number between 0 and 1. " +
            "You've specified $snapPositionalThreshold"
    }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    return remember(
        layoutInfo,
        decayAnimationSpec,
        snapAnimationSpec,
        density,
        layoutDirection
    ) {
        val snapLayoutInfoProvider =
            SnapLayoutInfoProvider(
                layoutInfo,
            ) { flingVelocity, lowerBound, upperBound ->
                calculateFinalSnappingBound(
                    pagingLayoutInfo = layoutInfo,
                    layoutDirection = layoutDirection,
                    snapPositionalThreshold = snapPositionalThreshold,
                    flingVelocity = flingVelocity,
                    lowerBoundOffset = lowerBound,
                    upperBoundOffset = upperBound
                )
            }
        snapFlingBehavior(
            snapLayoutInfoProvider = snapLayoutInfoProvider,
            decayAnimationSpec = decayAnimationSpec,
            snapAnimationSpec = snapAnimationSpec
        )
    }
}

private fun SnapLayoutInfoProvider(
    pagingLayoutInfo: PagingLayoutInfo,
    calculateFinalSnappingBound: (Float, Float, Float) -> Float,
): SnapLayoutInfoProvider {
    return object : SnapLayoutInfoProvider {

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

            Timber.d("Snapping to=$finalDistance")

            return if (finalDistance.isFinite()) {
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
            Timber.d("Calculating Snapping Bounds")
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

/**
 * Given two possible bounds that this Pager can settle in represented by [lowerBoundOffset] and
 * [upperBoundOffset], this function will decide which one of them it will settle to.
 */
@SuppressLint("BinaryOperationInTimber")
@OptIn(ExperimentalFoundationApi::class)
private fun calculateFinalSnappingBound(
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
    Timber.d(
        "isLtrDragging=${pagingLayoutInfo.isLtrDragging()} " +
            "isForward=$isForward " +
            "layoutDirection=$layoutDirection"
    )
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

    Timber.d(
        "\nfinalSnappingItem=$finalSnappingItem" +
            "\nlower=$lowerBoundOffset" +
            "\nupper=$upperBoundOffset"
    )

    return when (finalSnappingItem) {
        FinalSnappingItem.ClosestItem -> {
            if (offsetFromSnappedPositionOverflow.absoluteValue > snapPositionalThreshold) {
                // If we crossed the threshold, go to the next bound
                Timber.d("Crossed Snap Positional Threshold")
                if (isForward) upperBoundOffset else lowerBoundOffset
            } else {
                // if we haven't crossed the threshold. but scrolled minimally, we should
                // bound to the previous bound
                if (abs(offsetFromSnappedPosition) >=
                    abs(pagingLayoutInfo.positionThresholdFraction)
                ) {
                    Timber.d("Crossed Positional Threshold Fraction")
                    if (isForward) lowerBoundOffset else upperBoundOffset
                } else {
                    // if we haven't scrolled minimally, settle for the closest bound
                    Timber.d("Snap To Closest")
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

internal val DefaultPositionThreshold = 56.dp

@JvmInline
private value class FinalSnappingItem(
    @Suppress("unused") private val value: Int,
) {
    companion object {

        val ClosestItem: FinalSnappingItem = FinalSnappingItem(0)

        val NextItem: FinalSnappingItem = FinalSnappingItem(1)

        val PreviousItem: FinalSnappingItem = FinalSnappingItem(2)
    }
}

private fun Density.calculateFinalSnappingItem(velocity: Float): FinalSnappingItem {
    return if (velocity.absoluteValue < MinFlingVelocityDp.toPx()) {
        FinalSnappingItem.ClosestItem
    } else {
        if (velocity > 0) FinalSnappingItem.NextItem else FinalSnappingItem.PreviousItem
    }
}

private val MinFlingVelocityDp = 400.dp

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
