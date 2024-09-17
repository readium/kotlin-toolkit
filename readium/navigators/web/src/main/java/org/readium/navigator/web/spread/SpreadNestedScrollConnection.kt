package org.readium.navigator.web.spread

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import org.readium.navigator.web.webview.WebViewScrollable2DState

internal class SpreadNestedScrollConnection(
    private val webviewState: WebViewScrollable2DState
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val webViewNow = webviewState.webView ?: return Offset.Zero

        val webViewCannotScrollHorizontally =
            (webViewNow.scrollX < 1 && available.x > 0) ||
                ((webViewNow.maxScrollX - webViewNow.scrollX) < 1 && available.x < 0)

        val isGestureHorizontal =
            (abs(available.y) / abs(available.x)) < 0.58 // tan(Pi/6)

        return if (webViewCannotScrollHorizontally && isGestureHorizontal) {
            // If the gesture is mostly horizontal and the spread has nothing to consume horizontally,
            // we consume everything vertically.
            Offset(0f, available.y)
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val webViewNow = webviewState.webView ?: return Velocity.Zero

        if ((webViewNow.maxScrollX - webViewNow.scrollX) < 15) {
            webViewNow.scrollTo(webViewNow.maxScrollX, webViewNow.scrollY)
        } else if (webViewNow.scrollX in (0 until 15)) {
            webViewNow.scrollTo(0, webViewNow.scrollY)
        }

        return Velocity.Zero
    }
}
