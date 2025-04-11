/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.webview

import android.webkit.WebView

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
