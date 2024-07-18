package org.readium.navigator.web.util

import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt
import org.readium.navigator.web.gestures.DefaultScrollable2DState
import org.readium.navigator.web.gestures.Scrollable2DState
import org.readium.navigator.web.webview.RelaxedWebView
import timber.log.Timber

internal class WebViewScrollable2DState private constructor(
    private val webViewDeltaDispatcher: WebViewDeltaDispatcher
) : Scrollable2DState by DefaultScrollable2DState(webViewDeltaDispatcher::onDelta) {

    constructor() : this(WebViewDeltaDispatcher())

    var webView: RelaxedWebView?
        get() =
            webViewDeltaDispatcher.webView
        set(value) {
            webViewDeltaDispatcher.webView = value
        }
}

private class WebViewDeltaDispatcher {

    var webView: RelaxedWebView? = null

    fun onDelta(delta: Offset): Offset {
        val webViewNow = webView ?: return Offset.Zero

        val currentX = webViewNow.scrollX
        val currentY = webViewNow.scrollY
        val maxX = webViewNow.horizontalScrollRange - webViewNow.horizontalScrollExtent
        val maxY = webViewNow.verticalScrollRange - webViewNow.verticalScrollExtent

        Timber.d("WebViewDeltaDispatcher delta $delta")
        Timber.d("WebViewDeltaDispatcher currentScrollX $currentX")
        Timber.d("WebViewDeltaDispatcher scrollRangeX ${webViewNow.horizontalScrollRange}")
        Timber.d("WebViewDeltaDispatcher scrollExtentX ${webViewNow.horizontalScrollExtent}")
        Timber.d("WebViewDeltaDispatcher currentScrollY $currentY")
        Timber.d("WebViewDeltaDispatcher currentScrollY $currentY")

        val newX = (currentX - delta.x).coerceIn(0f, maxX.toFloat())
        val newY = (currentY - delta.y).coerceIn(0f, maxY.toFloat())
        webViewNow.scrollTo(newX.roundToInt(), newY.roundToInt())
        // webViewNow.scrollBy(-delta.x.toInt(), delta.y.toInt())

        Timber.d("WebViewDeltaDispatcher newScrollX ${webViewNow.scrollX}")
        Timber.d("WebViewDeltaDispatcher newScrollY ${webViewNow.scrollY}")

        val consumedX = (currentX - webViewNow.scrollX).toFloat()
        val consumedY = (currentY - webViewNow.scrollY).toFloat()
        val consumed = Offset(consumedX, consumedY)
        Timber.d("WebViewDeltaDispatcher consumed $consumed")
        return consumed
    }
}
