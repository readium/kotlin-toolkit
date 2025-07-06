/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webapi

import android.webkit.WebView
import org.readium.navigator.web.internals.server.WebViewServer
import org.readium.r2.shared.util.Url

public class FixedSingleInitializationApi(
    private val webView: WebView,
) {
    public fun loadResource(href: Url) {
        val resourceUrl = WebViewServer.publicationBaseHref.resolve(href)
        val urlAsJsLiteral = resourceUrl.toString().toJavaScriptLiteral()
        val script = "singleInitialization.loadResource($urlAsJsLiteral);"
        webView.evaluateJavascript(script) {}
    }
}

public class FixedDoubleInitializationApi(
    private val webView: WebView,
) {
    public fun loadSpread(leftHref: Url?, rightHref: Url?) {
        val leftUrl = leftHref?.let { WebViewServer.publicationBaseHref.resolve(it) }
        val rightUrl = rightHref?.let { WebViewServer.publicationBaseHref.resolve(it) }
        val argument = buildList {
            leftUrl?.let { add("left: ${it.toString().toJavaScriptLiteral()}") }
            rightUrl?.let { add("right: ${it.toString().toJavaScriptLiteral()}") }
        }.joinToString(separator = ", ", prefix = "{ ", postfix = " }")
        webView.evaluateJavascript("doubleInitialization.loadSpread($argument);") {}
    }
}
