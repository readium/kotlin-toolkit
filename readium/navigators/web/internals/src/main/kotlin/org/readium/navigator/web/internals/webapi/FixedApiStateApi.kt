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
import timber.log.Timber

public class DelegatingFixedApiStateListener(
    private val onInitializationApiAvailableDelegate: () -> Unit,
    private val onAreaApiAvailableDelegate: () -> Unit,
    private val onSelectionApiAvailableDelegate: () -> Unit,
    private val onDecorationApiAvailableDelegate: () -> Unit,
) : FixedApiStateListener {

    override fun onInitializationApiAvailable() {
        this.onInitializationApiAvailableDelegate()
    }

    override fun onAreaApiAvailable() {
        this.onAreaApiAvailableDelegate()
    }

    override fun onSelectionApiAvailable() {
        this.onSelectionApiAvailableDelegate()
    }

    override fun onDecorationApiAvailable() {
        this.onDecorationApiAvailableDelegate()
    }
}

public interface FixedApiStateListener {

    public fun onInitializationApiAvailable()

    public fun onAreaApiAvailable()

    public fun onSelectionApiAvailable()

    public fun onDecorationApiAvailable()
}

public class FixedApiStateApi(
    webView: WebView,
    private val listener: FixedApiStateListener,
) {
    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        webView.addJavascriptInterface(this, "fixedApiState")
    }

    @android.webkit.JavascriptInterface
    public fun onInitializationApiAvailable() {
        coroutineScope.launch {
            listener.onInitializationApiAvailable()
        }
    }

    @android.webkit.JavascriptInterface
    public fun onAreaApiAvailable() {
        coroutineScope.launch {
            listener.onAreaApiAvailable()
        }
    }

    @android.webkit.JavascriptInterface
    public fun onSelectionApiAvailable() {
        Timber.d("onselectionApiAvailable")
        coroutineScope.launch {
            listener.onSelectionApiAvailable()
        }
    }

    @android.webkit.JavascriptInterface
    public fun onDecorationApiAvailable() {
        coroutineScope.launch {
            listener.onDecorationApiAvailable()
        }
    }
}
