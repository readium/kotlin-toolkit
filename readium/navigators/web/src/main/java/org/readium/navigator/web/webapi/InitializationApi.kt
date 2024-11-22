/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.webapi

import android.webkit.JavascriptInterface
import android.webkit.WebView

internal class InitializationApi(
    private val onScriptsLoadedDelegate: () -> Unit,
) {

    fun registerOnWebView(webView: WebView) {
        webView.addJavascriptInterface(this, "initialization")
    }

    @JavascriptInterface
    fun onScriptsLoaded() {
        onScriptsLoadedDelegate.invoke()
    }
}
