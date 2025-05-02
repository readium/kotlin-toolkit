/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixed

import android.annotation.SuppressLint
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webapi.DocumentStateApi
import org.readium.navigator.web.webapi.GesturesApi
import org.readium.navigator.web.webapi.GesturesListener
import org.readium.navigator.web.webview.RelaxedWebView
import org.readium.navigator.web.webview.WebView
import org.readium.navigator.web.webview.WebViewScrollController
import org.readium.navigator.web.webview.WebViewState
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import timber.log.Timber

@OptIn(ExperimentalReadiumApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun SpreadWebView(
    pagerState: PagerState,
    spreadIndex: Int,
    state: WebViewState<RelaxedWebView>,
    spreadScrollState: SpreadScrollState,
    layoutDirection: LayoutDirection,
    progression: Double,
    client: WebViewClient,
    onScriptsLoaded: () -> Unit,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (AbsoluteUrl, String) -> Unit,
    backgroundColor: Color,
) {
    val contentIsLaidOut =
        remember { mutableStateOf(false) }

    LaunchedEffect(state.webView, onTap, onLinkActivated) {
        state.webView?.let { webView ->
            val listener = object : GesturesListener {
                override fun onTap(offset: DpOffset) {
                    onTap(TapEvent(offset))
                }

                override fun onLinkActivated(href: AbsoluteUrl, outerHtml: String) {
                    onLinkActivated(href, outerHtml)
                }
            }

            GesturesApi(webView, listener)
        }
    }

    LaunchedEffect(state.webView, onScriptsLoaded, spreadScrollState, contentIsLaidOut) {
        state.webView?.let { webView ->
            DocumentStateApi(
                webView = webView,
                onScriptsLoadedDelegate = onScriptsLoaded,
                onDocumentLoadedAndSizedDelegate = {
                    Timber.d("spread $spreadIndex onDocumentLoadedAndSized")
                    webView.requestLayout()
                    webView.setNextLayoutListener {
                        val scrollController = WebViewScrollController(webView)
                        scrollController.moveToProgression(
                            progression = progression,
                            snap = true,
                            orientation = Orientation.Horizontal,
                            direction = layoutDirection
                        )
                        spreadScrollState.scrollController.value = scrollController
                        contentIsLaidOut.value = true
                    }
                },
                onDocumentResizedDelegate = {
                    Timber.d("spread $spreadIndex onDocumentResizedAndSized")
                }
            )
        }
    }

    // Hide content before initial position is settled
    if (!contentIsLaidOut.value) {
        Box(
            modifier = Modifier
                .background(backgroundColor)
                .zIndex(1f)
                .fillMaxSize(),
            content = {}
        )
    }

    WebView(
        state = state,
        factory = { RelaxedWebView(it) },
        modifier = Modifier
            .fillMaxSize(),
        client = client,
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
        },
        onDispose = {
            spreadScrollState.scrollController.value = null
            Timber.d("spread disposing $spreadIndex")
        }
    )
}
