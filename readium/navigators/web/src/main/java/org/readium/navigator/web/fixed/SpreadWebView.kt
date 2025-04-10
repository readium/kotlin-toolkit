/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixed

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.DpOffset
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.gestures.NullFling2DBehavior
import org.readium.navigator.web.gestures.Scrollable2DDefaults
import org.readium.navigator.web.gestures.Scrollable2DState
import org.readium.navigator.web.gestures.scrollable2D
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webapi.DocumentStateApi
import org.readium.navigator.web.webapi.GesturesApi
import org.readium.navigator.web.webapi.GesturesListener
import org.readium.navigator.web.webview.RelaxedWebView
import org.readium.navigator.web.webview.WebView
import org.readium.navigator.web.webview.WebViewScrollController
import org.readium.navigator.web.webview.WebViewScrollable2DState
import org.readium.navigator.web.webview.WebViewState
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl

@OptIn(ExperimentalReadiumApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun SpreadWebView(
    pagerState: PagerState,
    spreadIndex: Int,
    state: WebViewState<RelaxedWebView>,
    spreadScrollState: SpreadScrollState,
    client: WebViewClient,
    onScriptsLoaded: () -> Unit,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (AbsoluteUrl, String) -> Unit,
    backgroundColor: Color,
    reverseScrollDirection: Boolean,
) {
    val scrollableState = remember { WebViewScrollable2DState() }

    val flingBehavior = Scrollable2DDefaults.flingBehavior()

    val spreadNestedScrollConnection =
        SpreadNestedScrollConnection(spreadIndex, pagerState, scrollableState, spreadScrollState, flingBehavior)

    val documentStateApi = remember(onScriptsLoaded) {
        DocumentStateApi(
            onScriptsLoadedDelegate = onScriptsLoaded,
            onDocumentLoadedDelegate = {
                state.webView?.apply {
                    post {
                        postVisualStateCallback(
                            0,
                            object : WebView.VisualStateCallback() {
                                override fun onComplete(requestId: Long) {
                                    with(this@apply) {
                                        val scrollController = WebViewScrollController(this)
                                        spreadScrollState.scrollController.value = scrollController
                                    }
                                }
                            }
                        )
                    }
                }
            },
            onDocumentResizedDelegate = {}
        )
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
        state.webView?.let { documentStateApi.registerOnWebView(it) }
        state.webView?.let { gesturesApi.registerOnWebView(it) }
    }

    WebView(
        state = state,
        factory = { RelaxedWebView(it) },
        modifier = Modifier
            .nestedScroll(spreadNestedScrollConnection)
            .scrollable2D(
                state = Scrollable2DState { Offset.Zero },
                reverseDirection = reverseScrollDirection,
                flingBehavior = NullFling2DBehavior()
            )
            .fillMaxSize(),
        client = client,
        onCreated = { webview ->
            scrollableState.webView = webview
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
        },
        onDispose = {
            spreadScrollState.scrollController.value = null
            scrollableState.webView = null
        }
    )
}
