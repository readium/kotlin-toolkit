package org.readium.navigator.web.webview

internal class WebViewScrollController(
    private val webView: RelaxedWebView,
) {

    val canMoveLeft: Boolean
        get() =
            webView?.let { it.scrollX > it.width / 2 } == true

    val canMoveRight: Boolean
        get() =
            webView?.let { (it.maxScrollX - it.scrollX) > it.width / 2 } == true

    val canMoveTop: Boolean
        get() =
            webView?.let { it.scrollY > it.width / 2 } == true

    val canMoveBottom: Boolean
        get() =
            webView?.let { (it.maxScrollY - it.scrollY) > it.width / 2 } == true

    fun moveLeft() {
        webView?.let { it.scrollBy(-it.width, 0) }
    }

    fun moveRight() {
        webView?.let { it.scrollBy(it.width, 0) }
    }

    fun moveTop() {
        webView?.let { it.scrollBy(0, -it.width) }
    }

    fun moveBottom() {
        webView?.let { it.scrollBy(0, it.width) }
    }
}
