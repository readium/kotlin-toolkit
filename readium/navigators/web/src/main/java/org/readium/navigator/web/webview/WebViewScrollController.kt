package org.readium.navigator.web.webview

import androidx.compose.foundation.gestures.Orientation
import kotlin.math.ceil
import kotlin.math.floor

internal class WebViewScrollController(
    private val webView: RelaxedWebView,
) {

    val canMoveLeft: Boolean
        get() = webView.scrollX > webView.width / 2 == true

    val canMoveRight: Boolean
        get() = webView.maxScrollX - webView.scrollX > webView.width / 2 == true

    val canMoveTop: Boolean
        get() = webView.scrollY > webView.width / 2 == true

    val canMoveBottom: Boolean
        get() = webView.maxScrollY - webView.scrollY > webView.width / 2 == true

    fun moveLeft() {
        webView.scrollBy(-webView.width, 0)
    }

    fun moveRight() {
        webView.scrollBy(webView.width, 0)
    }

    fun moveTop() {
        webView.scrollBy(0, -webView.width)
    }

    fun moveBottom() {
        webView.scrollBy(0, webView.width)
    }

    fun scrollToProgression(progression: Double, scrollOrientation: Orientation) {
        webView.scrollToProgression(progression, scrollOrientation)
    }

    fun progression(scrollOrientation: Orientation) =
        webView.progression(scrollOrientation)
}

private fun RelaxedWebView.scrollToProgression(progression: Double, scrollOrientation: Orientation) {
    if (scrollOrientation == Orientation.Horizontal) {
        scrollTo(floor(progression * maxScrollX).toInt(), 0)
    } else {
        scrollTo(0, ceil(progression * maxScrollY).toInt())
    }
}

private fun RelaxedWebView.progression(orientation: Orientation) = when (orientation) {
    Orientation.Vertical -> scrollY / maxScrollY.toDouble()
    Orientation.Horizontal -> scrollX / maxScrollX.toDouble()
}
