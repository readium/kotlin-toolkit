package org.readium.navigator.web.util

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

internal class WebViewClient(
    private val webViewServer: WebViewServer
) : android.webkit.WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return webViewServer.shouldInterceptRequest(request)
    }
}
