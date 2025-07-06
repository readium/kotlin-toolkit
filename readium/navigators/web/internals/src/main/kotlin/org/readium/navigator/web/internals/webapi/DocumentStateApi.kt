/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webapi

import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

public class DelegatingDocumentApiListener(
    private val onDocumentLoadedAndSizedDelegate: () -> Unit,
    private val onDocumentResizedDelegate: () -> Unit,
) : DocumentStateApiListener {

    override fun onDocumentLoadedAndSized() {
        this.onDocumentLoadedAndSizedDelegate()
    }

    override fun onDocumentResized() {
        this.onDocumentResizedDelegate()
    }
}

public interface DocumentStateApiListener {

    public fun onDocumentLoadedAndSized()

    public fun onDocumentResized()
}

public class DocumentStateApi(
    webView: WebView,
    public var listener: DocumentStateApiListener? = null,
) {
    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        webView.addJavascriptInterface(this, "documentState")
    }

    @JavascriptInterface
    public fun onDocumentLoadedAndSized() {
        coroutineScope.launch {
            checkNotNull(listener).onDocumentLoadedAndSized()
        }
    }

    @JavascriptInterface
    public fun onDocumentResized() {
        coroutineScope.launch {
            checkNotNull(listener).onDocumentResized()
        }
    }
}
