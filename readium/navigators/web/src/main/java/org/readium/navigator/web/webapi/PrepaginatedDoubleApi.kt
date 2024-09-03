package org.readium.navigator.web.webapi

import android.webkit.WebView
import org.readium.navigator.web.LayoutResolver
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
internal class PrepaginatedDoubleApi(
    private val webView: WebView
) {

    fun loadSpread(spread: LayoutResolver.Spread.Double) {
        val leftUrl = spread.left?.let { WebViewServer.publicationBaseHref.resolve(it) }
        val rightUrl = spread.right?.let { WebViewServer.publicationBaseHref.resolve(it) }
        webView.evaluateJavascript("layout.loadSpread({ left: `$leftUrl`, right: `$rightUrl`});") {}
    }

    fun setViewport(width: Float, height: Float) {
        webView.evaluateJavascript("layout.setViewport($width, $height, 0, 0, 0, 0);") {}
    }

    fun setFit(fit: Fit) {
        val script = "layout.setFit(`${fit.value}`);"
        webView.evaluateJavascript(script) {}
    }
}
