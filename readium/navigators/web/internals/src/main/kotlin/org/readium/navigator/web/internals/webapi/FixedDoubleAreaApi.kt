/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webapi

import android.content.res.AssetManager
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.navigator.web.internals.util.DisplayArea
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.util.AbsoluteUrl

public class FixedDoubleAreaApi(
    private val webView: WebView,
) {

    public companion object {

        public suspend fun getPageContent(assetManager: AssetManager, assetsUrl: AbsoluteUrl): String =
            withContext(Dispatchers.IO) {
                assetManager.open("readium/navigator/web/internals/generated/fixed-double-index.html")
                    .bufferedReader()
                    .use { it.readText() }
                    .replace("{{ASSETS_URL}}", assetsUrl.toString())
            }
    }

    public fun setDisplayArea(displayArea: DisplayArea) {
        val width = displayArea.viewportSize.width.value
        val height = displayArea.viewportSize.height.value
        val top = displayArea.safeDrawingPadding.top.value
        val right = displayArea.safeDrawingPadding.right.value
        val bottom = displayArea.safeDrawingPadding.bottom.value
        val left = displayArea.safeDrawingPadding.left.value
        val script = "doubleArea.setViewport($width, $height, $top, $right, $bottom, $left);"
        webView.evaluateJavascript(script) {}
    }

    public fun setFit(fit: Fit) {
        val script = "doubleArea.setFit(`${fit.value}`);"
        webView.evaluateJavascript(script) {}
    }
}
