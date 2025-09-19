/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webview

import android.content.Context
import android.graphics.Rect
import android.view.ActionMode
import android.view.View
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

    private var actionModeCallback: ActionMode.Callback? = null

    public fun setCustomSelectionActionModeCallback(callback: ActionMode.Callback?) {
        actionModeCallback = callback
    }

    @Suppress("Deprecation")
    @Deprecated("Deprecated in Java")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        nextLayoutListener.invoke()
        nextLayoutListener = {}
    }

    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
        val customCallback = actionModeCallback
            ?: return super.startActionMode(callback)

        val parent = parent ?: return null
        return parent.startActionModeForChild(this, customCallback)
    }

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        val customCallback = actionModeCallback
            ?: return super.startActionMode(callback, type)

        val parent = parent ?: return null
        val wrapper = Callback2Wrapper(
            customCallback,
            callback2 = callback as? ActionMode.Callback2
        )
        return parent.startActionModeForChild(this, wrapper, type)
    }
}

private class Callback2Wrapper(
    val callback: ActionMode.Callback,
    val callback2: ActionMode.Callback2?,
) : ActionMode.Callback by callback, ActionMode.Callback2() {

    override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) =
        callback2?.onGetContentRect(mode, view, outRect)
            ?: super.onGetContentRect(mode, view, outRect)
}
