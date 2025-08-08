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

public class DelegatingReflowableApiStateListener(
    private val onCssApiAvailableDelegate: () -> Unit,
    private val onSelectionApiAvailableDelegate: () -> Unit,
    private val onDecorationApiAvailableDelegate: () -> Unit,
) : ReflowableApiStateListener {

    override fun onCssApiAvailable() {
        this.onCssApiAvailableDelegate()
    }

    override fun onSelectionApiAvailable() {
        this.onSelectionApiAvailableDelegate()
    }

    override fun onDecorationApiAvailable() {
        this.onDecorationApiAvailableDelegate()
    }
}

public interface ReflowableApiStateListener {

    public fun onCssApiAvailable()

    public fun onSelectionApiAvailable()

    public fun onDecorationApiAvailable()
}

public class ReflowableApiStateApi(
    webView: WebView,
    private val listener: ReflowableApiStateListener,
) {
    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        webView.addJavascriptInterface(this, "reflowableApiState")
    }

    @android.webkit.JavascriptInterface
    public fun onCssApiAvailable() {
        coroutineScope.launch {
            listener.onCssApiAvailable()
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
