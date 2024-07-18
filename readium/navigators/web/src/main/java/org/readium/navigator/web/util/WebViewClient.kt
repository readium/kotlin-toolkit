package org.readium.navigator.web.util

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import org.readium.navigator.web.webview.AccompanistWebViewClient

internal class WebViewClient(
    private val webViewServer: WebViewServer
) : AccompanistWebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return webViewServer.shouldInterceptRequest(request)
    }
}
