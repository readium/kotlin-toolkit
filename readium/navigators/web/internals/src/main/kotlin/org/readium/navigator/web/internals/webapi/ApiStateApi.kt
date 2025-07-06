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

public class DelegatingApiStateListener(
    private val onAreaApiAvailableDelegate: () -> Unit,
    private val onSelectionApiAvailableDelegate: () -> Unit,
    private val onDecorationApiAvailableDelegate: () -> Unit,
) : ApiStateListener {
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

public interface ApiStateListener {

    public fun onAreaApiAvailable()

    public fun onSelectionApiAvailable()

    public fun onDecorationApiAvailable()
}

public class ApiStateApi(
    webView: WebView,
    private val listener: ApiStateListener,
) {
    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        webView.addJavascriptInterface(this, "apiState")
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
