/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.util

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

internal class WebViewClient(
    private val webViewServer: WebViewServer,
) : android.webkit.WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        return webViewServer.shouldInterceptRequest(request)
    }
}
