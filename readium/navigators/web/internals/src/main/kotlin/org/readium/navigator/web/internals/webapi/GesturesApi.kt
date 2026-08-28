/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webapi

import android.webkit.WebView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.readium.r2.shared.util.AbsoluteUrl
import timber.log.Timber

public class DelegatingGesturesListener(
    private val onTapDelegate: (DpOffset) -> Unit,
    private val onLinkActivatedDelegate: (AbsoluteUrl, String) -> Unit,
    private val onDecorationActivatedDelegate: (String, String, DpRect, DpOffset) -> Unit,
) : GesturesListener {

    override fun onTap(offset: DpOffset) {
        onTapDelegate(offset)
    }

    override fun onLinkActivated(
        href: AbsoluteUrl,
        outerHtml: String,
    ) {
        onLinkActivatedDelegate(href, outerHtml)
    }

    override fun onDecorationActivated(
        id: String,
        group: String,
        rect: DpRect,
        offset: DpOffset,
    ) {
        onDecorationActivatedDelegate(id, group, rect, offset)
    }
}

public interface GesturesListener {

    public fun onTap(offset: DpOffset)

    public fun onLinkActivated(href: AbsoluteUrl, outerHtml: String)

    public fun onDecorationActivated(id: String, group: String, rect: DpRect, offset: DpOffset)
}

public class GesturesApi(
    webView: WebView,
    public var listener: GesturesListener? = null,
) {
    private val coroutineScope: CoroutineScope =
        MainScope()

    init {
        webView.addJavascriptInterface(this, "gestures")
    }

    @android.webkit.JavascriptInterface
    public fun onTap(eventJson: String) {
        coroutineScope.launch {
            val tapEvent = Json.decodeFromString<JsonOffset>(eventJson)
            listener?.onTap(DpOffset(tapEvent.x.dp, tapEvent.y.dp))
        }
    }

    @android.webkit.JavascriptInterface
    public fun onLinkActivated(href: String, outerHtml: String) {
        coroutineScope.launch {
            val url = AbsoluteUrl(href) ?: return@launch
            listener?.onLinkActivated(url, outerHtml)
        }
    }

    @android.webkit.JavascriptInterface
    public fun onDecorationActivated(id: String, group: String, rect: String, offset: String) {
        coroutineScope.launch {
            val jsonRect = Json.decodeFromString<JsonRect>(rect)
            val jsonOffset = Json.decodeFromString<JsonOffset>(offset)
            Timber.d("onDecorationActivated offset ${jsonOffset.x} ${jsonOffset.y}")
            Timber.d("onDecorationActivated rect ${jsonRect.left} ${jsonRect.top}")
            listener?.onDecorationActivated(
                id = id,
                group = group,
                rect = DpRect(
                    left = jsonRect.left.dp,
                    top = jsonRect.top.dp,
                    right = jsonRect.right.dp,
                    bottom = jsonRect.bottom.dp
                ),
                offset = DpOffset(jsonOffset.x.dp, jsonOffset.y.dp)
            )
        }
    }
}
