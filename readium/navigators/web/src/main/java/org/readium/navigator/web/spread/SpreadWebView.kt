/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.spread

import android.annotation.SuppressLint
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.DpOffset
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webapi.GesturesApi
import org.readium.navigator.web.webapi.GesturesListener
import org.readium.navigator.web.webapi.InitializationApi
import org.readium.navigator.web.webview.WebView
import org.readium.navigator.web.webview.WebViewScrollable2DState
import org.readium.navigator.web.webview.WebViewState
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl

@OptIn(ExperimentalReadiumApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun SpreadWebView(
    state: WebViewState,
    client: WebViewClient,
    onScriptsLoaded: () -> Unit,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (AbsoluteUrl, String) -> Unit,
    backgroundColor: Color,
) {
    val scrollableState = remember { WebViewScrollable2DState() }

    val spreadNestedScrollConnection = SpreadNestedScrollConnection(scrollableState)

    val initializationApi = remember(onScriptsLoaded) {
        InitializationApi(onScriptsLoaded)
    }

    val gesturesApi = remember(onTap) {
        val listener = object : GesturesListener {
            override fun onTap(offset: DpOffset) {
                onTap(TapEvent(offset))
            }

            override fun onLinkActivated(href: AbsoluteUrl, outerHtml: String) {
                onLinkActivated(href, outerHtml)
            }
        }
        GesturesApi(listener)
    }

    LaunchedEffect(state.webView) {
        state.webView?.let { initializationApi.registerOnWebView(it) }
        state.webView?.let { gesturesApi.registerOnWebView(it) }
    }

    WebView(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(spreadNestedScrollConnection),
        state = state,
        client = client,
        scrollableState = scrollableState,
        onCreated = { webview ->
            webview.settings.javaScriptEnabled = true
            webview.settings.setSupportZoom(true)
            webview.settings.builtInZoomControls = true
            webview.settings.displayZoomControls = false
            webview.settings.loadWithOverviewMode = true
            webview.settings.useWideViewPort = true
            webview.isVerticalScrollBarEnabled = false
            webview.isHorizontalScrollBarEnabled = false
            webview.setBackgroundColor(backgroundColor.toArgb())
            webview.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    )
}
