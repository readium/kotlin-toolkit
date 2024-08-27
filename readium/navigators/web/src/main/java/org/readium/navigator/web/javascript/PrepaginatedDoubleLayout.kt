package org.readium.navigator.web.javascript

import android.webkit.WebView
import org.readium.navigator.web.LayoutResolver
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
internal class PrepaginatedDoubleLayout {

    fun WebView.loadSpread(spread: LayoutResolver.Spread.Double) {
        val leftUrl = spread.left?.let { WebViewServer.publicationBaseHref.resolve(it) }
        val rightUrl = spread.right?.let { WebViewServer.publicationBaseHref.resolve(it) }
        evaluateJavascript("layout.loadSpread({ left: `$leftUrl`, right: `$rightUrl`});") {}
    }

    fun WebView.setViewport(width: Float, height: Float) {
        evaluateJavascript("layout.setViewport($width, $height, 0, 0, 0, 0);") {}
    }
}
