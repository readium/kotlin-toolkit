package org.readium.navigator.web.webapi

import android.webkit.WebView
import org.readium.navigator.web.LayoutResolver
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
internal class PrepaginatedSingleApi(
    private val webView: WebView
) {
    fun loadSpread(spread: LayoutResolver.Spread.Single) {
        val resourceUrl = WebViewServer.publicationBaseHref.resolve(spread.value)
        val script = "layout.loadResource(`$resourceUrl`);"
        webView.evaluateJavascript(script) {}
    }

    fun setViewport(width: Float, height: Float) {
        val script = "layout.setViewport($width, $height, 0, 0, 0, 0);"
        webView.evaluateJavascript(script) {}
    }

    fun setFit(fit: Fit) {
        val script = "layout.setFit(`${fit.value}`);"
        webView.evaluateJavascript(script) {}
    }
}
