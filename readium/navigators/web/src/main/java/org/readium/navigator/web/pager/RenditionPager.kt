/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

@Composable
internal fun RenditionPager(
    modifier: Modifier = Modifier,
    state: PagerState,
    scrollDispatcher: ScrollDispatcher,
    orientation: Orientation,
    reverseLayout: Boolean,
    beyondViewportPageCount: Int,
    key: ((index: Int) -> Any)? = null,
    pageContent: @Composable PagerScope.(Int) -> Unit,
) {
    // A nested scroll connection is the only way to get separate drag and fling events
    // from the scrollable modifiers. So we catch all events in the "post phase" and pass them to
    // the provided ScrollDispatcher for processing.
    val scrollDispatcherNestedScrollConnection =
        ScrollDispatcherNestedScrollConnection(scrollDispatcher)

    val modifier = modifier
        .nestedScroll(scrollDispatcherNestedScrollConnection)

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
            reverseLayout = reverseLayout,
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
            reverseLayout = reverseLayout,
            pageNestedScrollConnection = pageNestedScrollConnection,
            pageContent = pageContent
        )
    }
}

private object NullNestedScrollConnection : NestedScrollConnection

private class ScrollDispatcherNestedScrollConnection(
    private val scrollDispatcher: ScrollDispatcher,
) : NestedScrollConnection {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source == NestedScrollSource.UserInput) {
            scrollDispatcher.onScroll(available)
        }

        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        scrollDispatcher.onFling(available)
        return available
    }
}

private object NullTargetedFlingBehavior : TargetedFlingBehavior {

    override suspend fun ScrollScope.performFling(
        initialVelocity: Float,
        onRemainingDistanceUpdated: (Float) -> Unit,
    ): Float {
        return initialVelocity
    }
}
