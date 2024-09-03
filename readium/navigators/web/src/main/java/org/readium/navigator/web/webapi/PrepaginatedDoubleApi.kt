package org.readium.navigator.web.webapi

import android.content.res.AssetManager
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.navigator.web.layout.Spread
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl

@OptIn(ExperimentalReadiumApi::class)
internal class PrepaginatedDoubleApi(
    private val webView: WebView
) {

    companion object {

        suspend fun getPageContent(assetManager: AssetManager, assetsUrl: AbsoluteUrl): String =
            withContext(Dispatchers.IO) {
                assetManager.open("readium/navigators/web/prepaginated-double-index.html")
                    .bufferedReader()
                    .use { it.readText() }
                    .replace("{{ASSETS_URL}}", assetsUrl.toString())
            }
    }

    fun loadSpread(spread: Spread.Double) {
        val leftUrl = spread.leftPage?.let { WebViewServer.publicationBaseHref.resolve(it) }
        val rightUrl = spread.rightPage?.let { WebViewServer.publicationBaseHref.resolve(it) }
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
