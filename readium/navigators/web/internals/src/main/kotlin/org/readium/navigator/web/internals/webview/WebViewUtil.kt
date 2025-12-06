/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webview

import android.webkit.WebView
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun WebView.invokeOnReadyToBeDrawn(callback: (WebView) -> Unit) {
    post {
        postVisualStateCallback(
            0,
            object : WebView.VisualStateCallback() {
                override fun onComplete(requestId: Long) {
                    callback(this@invokeOnReadyToBeDrawn)
                }
            }
        )
    }
}

public suspend fun WebView.evaluateJavaScriptSuspend(javascript: String): String =
    withContext(Dispatchers.Main) {
        suspendCoroutine { cont ->
            evaluateJavascript(javascript) { result ->
                cont.resume(result)
            }
        }
    }
