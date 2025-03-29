/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.webview

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign
import org.readium.navigator.web.gestures.DefaultScrollable2DState
import org.readium.navigator.web.gestures.Scrollable2DState
import org.readium.r2.shared.util.Url
import timber.log.Timber

internal class WebViewScrollable2DState private constructor(
    private val webViewDeltaDispatcher: WebViewDeltaDispatcher,
) : Scrollable2DState by DefaultScrollable2DState(webViewDeltaDispatcher::onDelta) {

    constructor(href: Url? = null) : this(WebViewDeltaDispatcher(href))

    var webView: RelaxedWebView?
        get() =
            webViewDeltaDispatcher.webView
        set(value) {
            webViewDeltaDispatcher.webView = value
        }

    val canMoveLeft: Boolean get() =
        webView?.let { it.scrollX > it.width / 2 } == true

    val canMoveRight: Boolean get() =
        webView?.let { (it.maxScrollX - it.scrollX) > it.width / 2 } == true

    val canMoveTop: Boolean get() =
        webView?.let { it.scrollY > it.width / 2 } == true

    val canMoveBottom: Boolean get() =
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

private class WebViewDeltaDispatcher(private val href: Url? = null) {

    var webView: RelaxedWebView? = null

    fun onDelta(delta: Offset): Offset {
        val webViewNow = webView ?: return Offset.Zero

        val currentX = webViewNow.scrollX
        val currentY = webViewNow.scrollY
        val maxX = webViewNow.maxScrollX
        val maxY = webViewNow.maxScrollY

        Timber.d("Dispatching $delta to Webview $href current $currentX max $maxX")

        // Consume slightly more than delta if we have to because
        // we don't want the pager to consume any rounding error
        val newX = (currentX + sign(delta.x) * ceil(abs(delta.x))).toInt().coerceIn(0, maxX)
        val newY = (currentY + sign(delta.y) * ceil(abs(delta.y))).toInt().coerceIn(0, maxY)
        webViewNow.scrollTo(newX, newY)

        // Fake that we never consume more than delta
        val consumedX = -(currentX - webViewNow.scrollX).toFloat().coerceAbsAtMost(abs(delta.x))
        val consumedY = -(currentY - webViewNow.scrollY).toFloat().coerceAbsAtMost(abs(delta.y))
        val consumed = Offset(consumedX, consumedY)

        return consumed
    }

    private fun Float.coerceAbsAtMost(maxValue: Float): Float {
        require(maxValue >= 0)
        return if (this > 0) {
            this.coerceAtMost(maxValue)
        } else {
            this.coerceAtLeast(-maxValue)
        }
    }
}
