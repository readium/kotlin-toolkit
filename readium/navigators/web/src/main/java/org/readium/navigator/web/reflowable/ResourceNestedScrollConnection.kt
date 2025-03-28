package org.readium.navigator.web.reflowable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import org.readium.navigator.web.webview.RelaxedWebView
import org.readium.navigator.web.webview.WebViewScrollable2DState

internal class ResourceNestedScrollConnection(
    private val pagerState: PagerState,
    private val resourceState: WebViewScrollable2DState,
    private val orientation: Orientation,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val webViewNow = resourceState.webView ?: return available
        val current = webViewNow.current
        val max = webViewNow.max

        val availableDir =
            if (orientation == Orientation.Horizontal) available.x else available.y

        val webViewIsGoingToConsume =
            (availableDir > 0 && current > 0) || (availableDir < 0 && current < max)

        return if (webViewIsGoingToConsume) {
            // Ensure the WebView is well positioned before consuming
            val consumed = scrollToCurrentPage()
            Offset(
                x = if (orientation == Orientation.Horizontal) consumed else 0f,
                y = if (orientation == Orientation.Vertical) consumed else 0f
            )
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (orientation == Orientation.Vertical) {
            // Mode scroll, the WebView is not paginated.
            return Velocity.Zero
        }

        val webViewNow = resourceState.webView ?: return available
        val currentX = webViewNow.scrollX
        val maxX = webViewNow.maxScrollX
        val pageSize = webViewNow.width

        // WebView stopped scrolling because reaching a good snap position.
        val webViewCanStillScroll =
            (available.x < 0 && maxX - currentX > pageSize / 2) || (available.x >= 0 && currentX > 1.5 * pageSize)

        return if (webViewCanStillScroll) {
            // Ensure Webview is the only one who's visible
            scrollToCurrentPage()
            // Consume everything left
            available
        } else {
            Velocity.Zero
        }
    }

    private fun scrollToCurrentPage(): Float {
        val currentPageInfo = pagerState.layoutInfo.visiblePagesInfo
            .first { it.index == pagerState.currentPage }

        val delta = -currentPageInfo.offset.toFloat()
        val consumed = -pagerState.dispatchRawDelta(-delta)
        return consumed
    }

    private val RelaxedWebView.current get() =
        if (orientation == Orientation.Horizontal) scrollX else scrollY

    private val RelaxedWebView.max get() =
        if (orientation == Orientation.Horizontal) maxScrollX else maxScrollY
}
