/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout.webapi

import android.webkit.WebView
import org.readium.navigator.web.fixedlayout.layout.DoubleViewportSpread
import org.readium.navigator.web.fixedlayout.layout.SingleViewportSpread
import org.readium.navigator.web.internals.server.WebViewServer
import org.readium.navigator.web.internals.webapi.toJavaScriptLiteral

internal class FixedSingleInitializationApi(
    private val webView: WebView,
) {
    fun loadSpread(spread: SingleViewportSpread) {
        val resourceUrl = WebViewServer.Companion.publicationBaseHref.resolve(spread.page.href)
        val urlAsJsLiteral = resourceUrl.toString().toJavaScriptLiteral()
        val script = "singleInitialization.loadResource($urlAsJsLiteral);"
        webView.evaluateJavascript(script) {}
    }
}

internal class FixedDoubleInitializationApi(
    private val webView: WebView,
) {
    fun loadSpread(spread: DoubleViewportSpread) {
        val leftUrl = spread.leftPage?.let { WebViewServer.publicationBaseHref.resolve(it.href) }
        val rightUrl = spread.rightPage?.let { WebViewServer.publicationBaseHref.resolve(it.href) }
        val argument = buildList {
            leftUrl?.let { add("left: ${it.toString().toJavaScriptLiteral()}") }
            rightUrl?.let { add("right: ${it.toString().toJavaScriptLiteral()}") }
        }.joinToString(separator = ", ", prefix = "{ ", postfix = " }")
        webView.evaluateJavascript("doubleInitialization.loadSpread($argument);") {}
    }
}
