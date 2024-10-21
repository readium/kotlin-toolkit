package org.readium.navigator.web.webapi

import android.content.res.AssetManager
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.navigator.web.layout.DoubleViewportSpread
import org.readium.navigator.web.util.DisplayArea
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl

@OptIn(ExperimentalReadiumApi::class)
internal class FixedDoubleApi(
    private val webView: WebView
) {

    companion object {

        suspend fun getPageContent(assetManager: AssetManager, assetsUrl: AbsoluteUrl): String =
            withContext(Dispatchers.IO) {
                assetManager.open("readium/navigators/web/fixed-double-index.html")
                    .bufferedReader()
                    .use { it.readText() }
                    .replace("{{ASSETS_URL}}", assetsUrl.toString())
            }
    }

    fun loadSpread(spread: DoubleViewportSpread) {
        val leftUrl = spread.leftPage?.let { WebViewServer.publicationBaseHref.resolve(it.href) }
        val rightUrl = spread.rightPage?.let { WebViewServer.publicationBaseHref.resolve(it.href) }
        val argument = buildList {
            leftUrl?.let { add("left: `$it`") }
            rightUrl?.let { add("right: `$it`") }
        }.joinToString(separator = ", ", prefix = "{ ", postfix = " }")
        webView.evaluateJavascript("doubleArea.loadSpread($argument);") {}
    }

    fun setDisplayArea(displayArea: DisplayArea) {
        val (width, height) = displayArea.viewportSize
        val top = displayArea.safeDrawingPadding.top
        val right = displayArea.safeDrawingPadding.right
        val bottom = displayArea.safeDrawingPadding.bottom
        val left = displayArea.safeDrawingPadding.left
        val script = "doubleArea.setViewport($width, $height, $top, $right, $bottom, $left);"
        webView.evaluateJavascript(script) {}
    }

    fun setFit(fit: Fit) {
        val script = "doubleArea.setFit(`${fit.value}`);"
        webView.evaluateJavascript(script) {}
    }
}
