/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun NavigatorPager(
    modifier: Modifier = Modifier,
    state: PagerState,
    reverseLayout: Boolean,
    beyondViewportPageCount: Int = 2,
    key: ((index: Int) -> Any)? = null,
    pageContent: @Composable PagerScope.(Int) -> Unit,
) {
    val flingBehavior = PagerDefaults.flingBehavior(
        state = state,
        pagerSnapDistance = PagerSnapDistance.atMost(0)
    )

    HorizontalPager(
        modifier = modifier,
        // Pages must intercept all scroll gestures so the pager moves
        // only through the PagerNestedScrollConnection.
        userScrollEnabled = false,
        state = state,
        beyondViewportPageCount = beyondViewportPageCount,
        reverseLayout = reverseLayout,
        flingBehavior = flingBehavior,
        key = key,
        pageNestedScrollConnection =
        PagerNestedScrollConnection(
            state,
            flingBehavior,
            Orientation.Horizontal
        ),
        pageContent = pageContent
    )
}
