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
import timber.log.Timber

internal class LoggingNestedScrollConnection(
    private val delegateNestedScrollConnection: NestedScrollConnection,
) : NestedScrollConnection {

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        Timber.d("onPostFling consumed $consumed, available $available")
        return delegateNestedScrollConnection.onPostFling(consumed, available)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        Timber.d("onPostScroll consumed, $consumed, available $available")
        return delegateNestedScrollConnection.onPostScroll(consumed, available, source)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        Timber.d("onPreFling available $available")
        return delegateNestedScrollConnection.onPreFling(available)
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        Timber.d("onPreScroll available $available")
        return delegateNestedScrollConnection.onPreScroll(available, source)
    }
}
