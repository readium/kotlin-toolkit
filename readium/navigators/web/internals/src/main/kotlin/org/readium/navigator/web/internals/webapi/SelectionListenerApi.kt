/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webapi

import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

public interface SelectionListener {

    public fun onSelectionStart()

    public fun onSelectionEnd()
}

public class DelegatingSelectionListener(
    private val onSelectionStartDelegate: () -> Unit,
    private val onSelectionEndDelegate: () -> Unit,
) : SelectionListener {

    override fun onSelectionStart() {
        onSelectionStartDelegate()
    }

    override fun onSelectionEnd() {
        onSelectionEndDelegate()
    }
}

public class SelectionListenerApi(
    webView: WebView,
    public var listener: SelectionListener? = null,
) {
    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        webView.addJavascriptInterface(this, "selectionListener")
    }

    @android.webkit.JavascriptInterface
    public fun onSelectionStart() {
        coroutineScope.launch {
            listener?.onSelectionStart()
        }
    }

    @android.webkit.JavascriptInterface
    public fun onSelectionEnd() {
        coroutineScope.launch {
            listener?.onSelectionEnd()
        }
    }
}
