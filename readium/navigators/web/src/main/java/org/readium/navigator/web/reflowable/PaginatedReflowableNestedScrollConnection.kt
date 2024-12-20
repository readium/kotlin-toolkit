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

internal class PaginatedReflowableNestedScrollConnection(
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

        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return Velocity(0f, available.y)
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
