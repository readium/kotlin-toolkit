/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.webapi

import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

internal class DocumentStateApi(
    webView: WebView,
    private val onScriptsLoadedDelegate: () -> Unit,
    private val onDocumentLoadedAndSizedDelegate: () -> Unit,
    private val onDocumentResizedDelegate: () -> Unit,
) {
    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        webView.addJavascriptInterface(this, "documentState")
    }

    @JavascriptInterface
    fun onScriptsLoaded() {
        coroutineScope.launch {
            onScriptsLoadedDelegate.invoke()
        }
    }

    @JavascriptInterface
    fun onDocumentLoadedAndSized() {
        coroutineScope.launch {
            onDocumentLoadedAndSizedDelegate.invoke()
        }
    }

    @JavascriptInterface
    fun onDocumentResized() {
        coroutineScope.launch {
            onDocumentResizedDelegate.invoke()
        }
    }
}
