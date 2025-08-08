/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout.spread

import android.annotation.SuppressLint
import android.view.ActionMode
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.internals.server.WebViewClient
import org.readium.navigator.web.internals.webapi.DelegatingDocumentApiListener
import org.readium.navigator.web.internals.webapi.DelegatingGesturesListener
import org.readium.navigator.web.internals.webapi.DocumentStateApi
import org.readium.navigator.web.internals.webapi.GesturesApi
import org.readium.navigator.web.internals.webview.RelaxedWebView
import org.readium.navigator.web.internals.webview.WebView
import org.readium.navigator.web.internals.webview.WebViewScrollController
import org.readium.navigator.web.internals.webview.WebViewState
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
    layoutDirection: LayoutDirection,
    progression: Double,
    client: WebViewClient,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (AbsoluteUrl, String) -> Unit,
    backgroundColor: Color,
    actionModeCallback: ActionMode.Callback?,
    onDecorationActivated: (String, String, DpRect, DpOffset) -> Unit,
) {
    var gesturesApi by remember(state.webView) {
        mutableStateOf<GesturesApi?>(null)
    }

    var documentStateApi by remember(state.webView) {
        mutableStateOf<DocumentStateApi?>(null)
    }

    LaunchedEffect(state.webView) {
        state.webView?.let { webView ->
            gesturesApi = GesturesApi(webView)
            documentStateApi = DocumentStateApi(webView)
        }
    }

    LaunchedEffect(gesturesApi, onTap, onLinkActivated) {
        gesturesApi?.let { gesturesApi ->
            gesturesApi.listener = DelegatingGesturesListener(
                onTapDelegate = { offset ->
                    onTap(TapEvent(offset))
                },
                onLinkActivatedDelegate = { href: AbsoluteUrl, outerHtml: String ->
                    onLinkActivated(href, outerHtml)
                },
                onDecorationActivatedDelegate = {
                        id: String, group: String, rect: DpRect, offset: DpOffset ->
                    onDecorationActivated(id, group, rect, offset)
                }
            )
        }
    }

    var showPlaceholder by remember { mutableStateOf(true) }

    LaunchedEffect(documentStateApi, state.webView, spreadScrollState, showPlaceholder) {
        state.webView?.let { webView ->
            documentStateApi?.let { documentStateApi ->
                documentStateApi.listener = DelegatingDocumentApiListener(
                    onDocumentLoadedAndSizedDelegate = {
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
                            showPlaceholder = false
                        }
                    },
                    onDocumentResizedDelegate = {
                    }
                )
            }
        }
    }

    LaunchedEffect(state.webView, actionModeCallback) {
        state.webView?.setCustomSelectionActionModeCallback(actionModeCallback)
    }

    // Hide content before initial position is settled
    if (showPlaceholder) {
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
        }
    )
}
