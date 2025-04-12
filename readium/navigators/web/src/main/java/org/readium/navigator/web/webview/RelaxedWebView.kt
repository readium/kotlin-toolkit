/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.webview

import android.content.Context
import android.webkit.WebView

/**
 * WebView allowing access to protected fields.
 */
internal class RelaxedWebView(context: Context) : WebView(context) {

    val maxScrollX: Int get() =
        horizontalScrollRange - horizontalScrollExtent

    val maxScrollY: Int get() =
        verticalScrollRange - verticalScrollExtent

    val canScrollRight: Boolean get() =
        scrollX < maxScrollX

    val canScrollLeft: Boolean get() =
        scrollX > 0

    val canScrollTop: Boolean get() =
        scrollY > 0

    val canScrollBottom: Boolean get() =
        scrollY < maxScrollY

    val verticalScrollRange: Int get() =
        computeVerticalScrollRange()

    val horizontalScrollRange: Int get() =
        computeHorizontalScrollRange()

    val verticalScrollExtent: Int get() =
        computeVerticalScrollExtent()

    val horizontalScrollExtent: Int get() =
        computeHorizontalScrollExtent()

    private var nextLayoutListener: (() -> Unit) = {}

    fun setNextLayoutListener(block: () -> Unit) {
        nextLayoutListener = block
    }

    @Suppress("Deprecation")
    @Deprecated("Deprecated in Java")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        nextLayoutListener.invoke()
        nextLayoutListener = {}
    }
}
