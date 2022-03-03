package org.readium.r2.navigator3

import androidx.compose.ui.unit.IntSize
import org.readium.r2.navigator3.core.viewer.LazyViewerState
import org.readium.r2.shared.util.Try


class NavigatorScope internal constructor(
    private val viewerState: LazyViewerState,
    private val layout: LayoutFactory.Layout
) {
    val viewport: IntSize = layout.viewport

    suspend fun goForward(): Try<Unit, NavigatorState.Exception> {
        val currentItemIndex = viewerState.firstVisibleItemIndex
        val currentSpread = layout.spreadStates[currentItemIndex]
        if (currentSpread.goForward()) {
            return Try.success(Unit)
        }

        if (currentItemIndex + 1 == viewerState.totalItemsCount) {
            return Try.failure(NavigatorState.Exception.InvalidState("Reached end."))
        }
        viewerState.scrollToItem(currentItemIndex + 1)
        layout.spreadStates[currentItemIndex + 1].goBeginning()
        return Try.success(Unit)
    }

    suspend fun goBackward(): Try<Unit, NavigatorState.Exception> {
        val currentItemIndex = viewerState.firstVisibleItemIndex
        val currentSpread = layout.spreadStates[currentItemIndex]
        if (currentSpread.goBackward()) {
            return Try.success(Unit)
        }

        if (currentItemIndex == 0) {
            return Try.failure(NavigatorState.Exception.InvalidState("Reached beginning."))
        }
        viewerState.scrollToItem(currentItemIndex - 1)
        layout.spreadStates[currentItemIndex - 1].goEnd()
        return Try.success(Unit)
    }
}