/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.webapi

import android.webkit.WebView
import org.readium.navigator.web.css.RsProperties
import org.readium.navigator.web.css.UserProperties
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
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
