/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import org.readium.navigator.web.webview.RelaxedWebView
import org.readium.navigator.web.webview.WebViewScrollable2DState

internal class ScrollReflowableNestedScrollConnection(
    private val webviewState: WebViewScrollable2DState,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val webViewNow = webviewState.webView ?: return Offset.Zero

        val webViewCannotScrollVertically =
            (webViewNow.scrollY < 3 && available.y > 0) ||
                ((webViewNow.maxScrollY - webViewNow.scrollY) < 3 && available.y < 0)

        if (webViewCannotScrollVertically) {
            snapWebview(webViewNow)
        }

        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return Velocity(available.x, 0f)
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val webViewNow = webviewState.webView ?: return Velocity.Zero
        snapWebview(webViewNow)
        return Velocity.Zero
    }

    private fun snapWebview(webview: RelaxedWebView) {
        if ((webview.maxScrollY - webview.scrollY) < 15) {
            webview.scrollTo(webview.scrollX, webview.maxScrollY)
        } else if (webview.scrollY in (0 until 15)) {
            webview.scrollTo(webview.scrollX, 0)
        }
    }
}
