/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.spread

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import org.readium.navigator.web.webview.RelaxedWebView
import org.readium.navigator.web.webview.WebViewScrollable2DState

internal class SpreadNestedScrollConnection(
    private val webviewState: WebViewScrollable2DState,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val webViewNow = webviewState.webView ?: return Offset.Zero

        // For some reason, scrollX can vary by 1 or 2 pixels without any call to scrollTo.
        val webViewCannotScrollHorizontally =
            (webViewNow.scrollX < 3 && available.x > 0) ||
                ((webViewNow.maxScrollX - webViewNow.scrollX) < 3 && available.x < 0)

        if (webViewCannotScrollHorizontally) {
            snapWebview(webViewNow)
        }

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
        snapWebview(webViewNow)
        return Velocity.Zero
    }

    private fun snapWebview(webview: RelaxedWebView) {
        if ((webview.maxScrollX - webview.scrollX) < 15) {
            webview.scrollTo(webview.maxScrollX, webview.scrollY)
        } else if (webview.scrollX in (0 until 15)) {
            webview.scrollTo(0, webview.scrollY)
        }
    }
}
