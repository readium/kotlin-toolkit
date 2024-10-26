package org.readium.navigator.web.webapi

import android.webkit.JavascriptInterface
import android.webkit.WebView

internal class InitializationApi(
    private val onScriptsLoadedDelegate: () -> Unit
) {

    fun registerOnWebView(webView: WebView) {
        webView.addJavascriptInterface(this, "initialization")
    }

    @JavascriptInterface
    fun onScriptsLoaded() {
        onScriptsLoadedDelegate.invoke()
    }
}
