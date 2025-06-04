/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webview

import android.content.Context
import android.webkit.WebView

/**
 * WebView allowing access to some protected fields.
 */
public class RelaxedWebView(context: Context) : WebView(context) {

    public val maxScrollX: Int get() =
        horizontalScrollRange - horizontalScrollExtent

    public val maxScrollY: Int get() =
        verticalScrollRange - verticalScrollExtent

    public val canScrollRight: Boolean get() =
        scrollX < maxScrollX

    public val canScrollLeft: Boolean get() =
        scrollX > 0

    public val canScrollTop: Boolean get() =
        scrollY > 0

    public val canScrollBottom: Boolean get() =
        scrollY < maxScrollY

    public val verticalScrollRange: Int get() =
        computeVerticalScrollRange()

    public val horizontalScrollRange: Int get() =
        computeHorizontalScrollRange()

    public val verticalScrollExtent: Int get() =
        computeVerticalScrollExtent()

    public val horizontalScrollExtent: Int get() =
        computeHorizontalScrollExtent()

    private var nextLayoutListener: (() -> Unit) = {}

    public fun setNextLayoutListener(block: () -> Unit) {
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
