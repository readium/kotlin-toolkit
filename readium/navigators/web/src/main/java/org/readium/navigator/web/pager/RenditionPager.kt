/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll

@Composable
internal fun RenditionPager(
    modifier: Modifier = Modifier,
    state: PagerState,
    scrollDispatcher: ScrollDispatcher? = null,
    orientation: Orientation,
    reverseLayout: Boolean,
    beyondViewportPageCount: Int = 2,
    key: ((index: Int) -> Any)? = null,
    pageContent: @Composable PagerScope.(Int) -> Unit,
) {
    val flingBehavior = /*PagerDefaults.flingBehavior(
        state = state,
        pagerSnapDistance = PagerSnapDistance.atMost(0)
    ) */ object : TargetedFlingBehavior {
        override suspend fun ScrollScope.performFling(
            initialVelocity: Float,
            onRemainingDistanceUpdated: (Float) -> Unit,
        ): Float {
            return initialVelocity
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {
    } /*PagerNestedScrollConnection(
        state,
        flingBehavior,
        orientation
    )*/

    val updatedPageContent: @Composable PagerScope.(Int) -> Unit = { index ->
        Box(
            modifier = Modifier, // .nestedScroll(PageNestedScrollConnection(index, state)),
            propagateMinConstraints = true
        ) {
            pageContent(index)
        }
    }

    val delegatingNestedScrollConnection =
        scrollDispatcher?.let { DelegatingNestedScrollConnection(it) }

    if (orientation == Orientation.Horizontal) {
        HorizontalPager(
            modifier = modifier.let { modifier -> delegatingNestedScrollConnection?.let { modifier.nestedScroll(it) } ?: modifier },
            // Pages must intercept all scroll gestures so the pager moves
            // only through the PagerNestedScrollConnection.
            userScrollEnabled = false,
            state = state,
            beyondViewportPageCount = beyondViewportPageCount,
            reverseLayout = reverseLayout,
            flingBehavior = flingBehavior,
            key = key,
            pageNestedScrollConnection = nestedScrollConnection,
            pageContent = updatedPageContent
        )
    } else {
        VerticalPager(
            modifier = modifier.let { modifier -> delegatingNestedScrollConnection?.let { modifier.nestedScroll(it) } ?: modifier },
            // Pages must intercept all scroll gestures so the pager moves
            // only through the PagerNestedScrollConnection.
            userScrollEnabled = false,
            state = state,
            beyondViewportPageCount = beyondViewportPageCount,
            reverseLayout = reverseLayout,
            flingBehavior = flingBehavior,
            key = key,
            pageNestedScrollConnection = nestedScrollConnection,
            pageContent = updatedPageContent
        )
    }
}
