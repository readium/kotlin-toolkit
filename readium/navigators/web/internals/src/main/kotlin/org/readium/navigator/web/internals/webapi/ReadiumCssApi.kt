package org.readium.navigator.web.internals.webapi

import android.webkit.WebView

public class ReadiumCssApi(
    private val webView: WebView,
) {

    public fun setProperties(properties: Map<String, String?>) {
        val values = buildString {
            append("[")
            for ((k, v) in properties.entries) {
                append("""["$k", "${v.orEmpty()}"],""")
            }
            append("]")
        }
        val script = "readiumcss.setProperties(new Map($values));"
        webView.evaluateJavascript(script) {}
    }
}
