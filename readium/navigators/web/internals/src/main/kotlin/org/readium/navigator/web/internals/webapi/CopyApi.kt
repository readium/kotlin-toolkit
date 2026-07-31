/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webapi

import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

public fun interface CopyInterceptionListener {

    /**
     * Called when a copy event was intercepted with the given selected [text].
     *
     * The default clipboard write was prevented; the receiver is responsible for performing
     * the counted copy.
     */
    public fun onCopyIntercepted(text: String)
}

public class CopyListenerApi(
    webView: WebView,
    private val interceptEnabled: Boolean,
    public var listener: CopyInterceptionListener? = null,
) {
    private val coroutineScope: CoroutineScope =
        MainScope()

    /**
     * Whether a user text selection is currently active. Fed from the selection listener's
     * start/end events, and read synchronously from the JavaScript thread.
     */
    @Volatile
    private var isSelecting: Boolean = false

    init {
        webView.addJavascriptInterface(this, "copyListener")
    }

    // The injected scripts feed these synchronously with a SelectionReporter, so the gate cannot
    // lag behind the actual selection.

    @android.webkit.JavascriptInterface
    public fun onSelectionStart() {
        isSelecting = true
    }

    @android.webkit.JavascriptInterface
    public fun onSelectionEnd() {
        isSelecting = false
    }

    /**
     * The single gate deciding whether the JS layer intercepts copy events: the publication must
     * be protected and a user selection must be active. Requiring an active selection raises the
     * bar against arbitrary clipboard writes from publication scripts.
     */
    @android.webkit.JavascriptInterface
    public fun shouldInterceptCopy(): Boolean =
        interceptEnabled && isSelecting

    @android.webkit.JavascriptInterface
    public fun onCopyIntercepted(text: String) {
        coroutineScope.launch {
            listener?.onCopyIntercepted(text)
        }
    }
}
