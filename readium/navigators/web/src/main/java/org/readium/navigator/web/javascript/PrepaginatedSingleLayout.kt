package org.readium.navigator.web.javascript

import android.webkit.WebView
import org.readium.navigator.web.LayoutResolver
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
internal class PrepaginatedSingleLayout {

    fun WebView.loadSpread(spread: LayoutResolver.Spread.Single) {
        val resourceUrl = WebViewServer.publicationBaseHref.resolve(spread.value)
        evaluateJavascript("layout.loadResource(`$resourceUrl`);") {}
    }

    fun WebView.setViewport(width: Float, height: Float) {
        evaluateJavascript("layout.setViewport($width, $height, 0, 0, 0, 0);") {}
    }
}
