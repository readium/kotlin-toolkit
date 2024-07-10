package org.readium.navigator.web.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import timber.log.Timber

internal class LoggingNestedScrollConnection : NestedScrollConnection {

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        Timber.d("onPostFling $consumed $available")
        return super.onPostFling(consumed, available)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        Timber.d("onPostFling $consumed $available $source")
        return super.onPostScroll(consumed, available, source)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        Timber.d("onPreFling $available")
        return super.onPreFling(available)
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        Timber.d("onPreScroll $available $source")
        return super.onPreScroll(available, source)
    }
}
