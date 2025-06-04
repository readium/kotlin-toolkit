/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.readium.navigator.web.internals.gestures.Fling2DBehavior
import org.readium.navigator.web.internals.gestures.Scrollable2DState
import org.readium.navigator.web.internals.gestures.scrollable2D

@Composable
public fun RenditionPager(
    modifier: Modifier = Modifier,
    state: PagerState,
    scrollState: Scrollable2DState,
    flingBehavior: Fling2DBehavior,
    orientation: Orientation,
    enableScroll: Boolean = true,
    beyondViewportPageCount: Int,
    key: ((index: Int) -> Any)? = null,
    pageContent: @Composable PagerScope.(Int) -> Unit,
) {
    val modifier = modifier
        .scrollable2D(
            enabled = enableScroll,
            state = scrollState,
            flingBehavior = flingBehavior,
            reverseDirection = orientation == Orientation.Vertical ||
                LocalLayoutDirection.current == LayoutDirection.Ltr
        )

    // Disable built-in pager behavior as it is not suitable.
    val pageNestedScrollConnection = NullNestedScrollConnection

    // Disable scroll detection built-in in the pager as we need 2D gestures in fixed layout.
    val userScrollEnabled = false
    val flingBehavior = NullTargetedFlingBehavior

    if (orientation == Orientation.Horizontal) {
        HorizontalPager(
            state = state,
            modifier = modifier,
            beyondViewportPageCount = beyondViewportPageCount,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            key = key,
            reverseLayout = false,
            pageNestedScrollConnection = pageNestedScrollConnection,
            pageContent = pageContent
        )
    } else {
        VerticalPager(
            state = state,
            modifier = modifier,
            beyondViewportPageCount = beyondViewportPageCount,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            key = key,
            reverseLayout = false,
            pageNestedScrollConnection = pageNestedScrollConnection,
            pageContent = pageContent
        )
    }
}

private object NullNestedScrollConnection : NestedScrollConnection

private object NullTargetedFlingBehavior : TargetedFlingBehavior {

    override suspend fun ScrollScope.performFling(
        initialVelocity: Float,
        onRemainingDistanceUpdated: (Float) -> Unit,
    ): Float {
        return initialVelocity
    }
}
