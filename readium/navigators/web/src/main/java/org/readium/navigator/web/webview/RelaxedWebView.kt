package org.readium.navigator.web.webview

import android.content.Context
import android.webkit.WebView

internal class RelaxedWebView(context: Context) : WebView(context) {

    val maxScrollX: Int get() =
        horizontalScrollRange - horizontalScrollExtent

    val maxScrollY: Int get() =
        verticalScrollRange - verticalScrollExtent

    val verticalScrollRange: Int get() =
        computeVerticalScrollRange()

    val horizontalScrollRange: Int get() =
        computeHorizontalScrollRange()

    val verticalScrollExtent: Int get() =
        computeVerticalScrollExtent()

    val horizontalScrollExtent: Int get() =
        computeHorizontalScrollExtent()
}
