/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.pager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

internal interface ScrollDispatcher {

    fun onScroll(available: Offset): Offset

    suspend fun onFling(available: Velocity): Velocity
}

internal class DelegatingNestedScrollConnection(
    private val scrollDispatcher: ScrollDispatcher,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput) {
            scrollDispatcher.onScroll(available)
        }

        return available
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        scrollDispatcher.onFling(available)
        return available
    }
}
