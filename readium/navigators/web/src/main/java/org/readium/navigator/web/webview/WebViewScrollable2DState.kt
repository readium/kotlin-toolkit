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

internal class WebViewScrollable2DState private constructor(
    private val webViewDeltaDispatcher: WebViewDeltaDispatcher,
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
        val maxX = webViewNow.maxScrollX
        val maxY = webViewNow.maxScrollY

        // Consume slightly more than delta si we have to because
        // we don't want the pager to consume any rounding error
        val newX = (currentX - sign(delta.x) * ceil(abs(delta.x))).toInt().coerceIn(0, maxX)
        val newY = (currentY - sign(delta.y) * ceil(abs(delta.y))).toInt().coerceIn(0, maxY)
        webViewNow.scrollTo(newX, newY)

        // Fake that we never consume more than delta
        val consumedX = (currentX - webViewNow.scrollX).toFloat().coerceAbsAtMost(abs(delta.x))
        val consumedY = (currentY - webViewNow.scrollY).toFloat().coerceAbsAtMost(abs(delta.y))
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
