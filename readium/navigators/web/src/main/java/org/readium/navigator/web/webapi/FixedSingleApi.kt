/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.webapi

import android.content.res.AssetManager
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.navigator.web.layout.SingleViewportSpread
import org.readium.navigator.web.util.DisplayArea
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.util.AbsoluteUrl

internal class FixedSingleApi(
    private val webView: WebView,
) {

    companion object {

        suspend fun getPageContent(assetManager: AssetManager, assetsUrl: AbsoluteUrl): String =
            withContext(Dispatchers.IO) {
                assetManager.open("readium/navigators/web/fixed-single-index.html")
                    .bufferedReader()
                    .use { it.readText() }
                    .replace("{{ASSETS_URL}}", assetsUrl.toString())
            }
    }

    fun loadSpread(spread: SingleViewportSpread) {
        val resourceUrl = WebViewServer.publicationBaseHref.resolve(spread.page.href)
        val script = "singleArea.loadResource(`$resourceUrl`);"
        webView.evaluateJavascript(script) {}
    }

    fun setDisplayArea(displayArea: DisplayArea) {
        val width = displayArea.viewportSize.width.value
        val height = displayArea.viewportSize.height.value
        val top = displayArea.safeDrawingPadding.top.value
        val right = displayArea.safeDrawingPadding.right.value
        val bottom = displayArea.safeDrawingPadding.bottom.value
        val left = displayArea.safeDrawingPadding.left.value
        val script = "singleArea.setViewport($width, $height, $top, $right, $bottom, $left);"
        webView.evaluateJavascript(script) {}
    }

    fun setFit(fit: Fit) {
        val script = "singleArea.setFit(`${fit.value}`);"
        webView.evaluateJavascript(script) {}
    }
}
