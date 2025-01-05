/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.util

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

internal class WebViewClient(
    private val webViewServer: WebViewServer,
    private val onPageFinishedDelegate: (() -> Unit)? = null,
) : android.webkit.WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        return webViewServer.shouldInterceptRequest(request)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK)) {
            WebViewCompat.postVisualStateCallback(view, 0) {
                onPageFinishedDelegate?.invoke()
            }
        }
    }
}
