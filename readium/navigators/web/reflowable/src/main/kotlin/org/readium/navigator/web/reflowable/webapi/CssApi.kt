/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable.webapi

import android.webkit.WebView
import org.readium.navigator.web.reflowable.css.RsProperties
import org.readium.navigator.web.reflowable.css.UserProperties

internal class CssApi(
    private val webView: WebView,
) {

    fun setProperties(userProperties: UserProperties, rsProperties: RsProperties) {
        val properties = rsProperties.toCssProperties() + userProperties.toCssProperties()
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
