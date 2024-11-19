/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.webapi

import android.webkit.WebView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.readium.r2.shared.util.AbsoluteUrl

internal interface GesturesListener {

    fun onTap(offset: DpOffset)
    fun onLinkActivated(href: AbsoluteUrl, outerHtml: String)
}

internal class GesturesApi(
    private val listener: GesturesListener,
) {

    fun registerOnWebView(webView: WebView) {
        webView.addJavascriptInterface(this, "gestures")
    }

    @android.webkit.JavascriptInterface
    fun onTap(eventJson: String) {
        val tapEvent = Json.decodeFromString<JsonTapEvent>(eventJson)
        listener.onTap(DpOffset(tapEvent.x.dp, tapEvent.y.dp))
    }

    @android.webkit.JavascriptInterface
    fun onLinkActivated(href: String, outerHtml: String) {
        val url = AbsoluteUrl(href) ?: return
        listener.onLinkActivated(url, outerHtml)
    }
}

@Serializable
private data class JsonTapEvent(
    val x: Float,
    val y: Float,
)
