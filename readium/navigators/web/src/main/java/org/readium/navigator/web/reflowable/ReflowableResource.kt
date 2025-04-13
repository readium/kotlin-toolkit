/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.reflowable

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.zIndex
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.css.ReadiumCssLayout
import org.readium.navigator.web.css.RsProperties
import org.readium.navigator.web.css.UserProperties
import org.readium.navigator.web.gestures.NullFling2DBehavior
import org.readium.navigator.web.gestures.Scrollable2DState
import org.readium.navigator.web.gestures.scrollable2D
import org.readium.navigator.web.util.AbsolutePaddingValues
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.util.absolutePadding
import org.readium.navigator.web.webapi.CssApi
import org.readium.navigator.web.webapi.DelegatingGesturesListener
import org.readium.navigator.web.webapi.DocumentStateApi
import org.readium.navigator.web.webapi.GesturesApi
import org.readium.navigator.web.webview.RelaxedWebView
import org.readium.navigator.web.webview.WebView
import org.readium.navigator.web.webview.WebViewScrollController
import org.readium.navigator.web.webview.rememberWebViewState
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
internal fun ReflowableResource(
    resourceState: ReflowableResourceState,
    pagerState: PagerState,
    publicationBaseUrl: AbsoluteUrl,
    webViewClient: WebViewClient,
    backgroundColor: Color,
    padding: AbsolutePaddingValues,
    reverseLayout: Boolean,
    scroll: Boolean,
    orientation: Orientation,
    userProperties: UserProperties,
    rsProperties: RsProperties,
    readiumCssLayout: ReadiumCssLayout,
    enableScroll: Boolean,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (Url, String) -> Unit,
    onProgressionChange: (Double) -> Unit,
    onDocumentResized: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val webViewState =
            rememberWebViewState<RelaxedWebView>(
                url = publicationBaseUrl.resolve(resourceState.href).toString()
            )

        val orientationState =
            rememberUpdatedState(orientation)

        val scriptsLoaded =
            remember(webViewState.webView) { mutableStateOf(false) }

        val contentIsLaidOut =
            remember(webViewState.webView) { mutableStateOf(false) }

        val scrollableState =
            remember { Scrollable2DState { Offset.Zero } }

        val documentStateApi = remember(webViewState.webView) {
            DocumentStateApi(
                onScriptsLoadedDelegate = {
                    scriptsLoaded.value = true
                },
                onDocumentLoadedAndSizedDelegate = {
                    webViewState.webView?.apply {
                        requestLayout()
                        setNextLayoutListener {
                            val scrollController = WebViewScrollController(this)
                            scrollController.moveToProgression(
                                progression = resourceState.progression,
                                scroll = scroll,
                                orientation = orientationState.value
                            )
                            resourceState.scrollController.value = scrollController
                            setOnScrollChangeListener { view, scrollX, scrollY, oldScrollX, oldScrollY ->
                                onProgressionChange(scrollController.progression(orientationState.value))
                            }
                            contentIsLaidOut.value = true
                        }
                    }
                },
                onDocumentResizedDelegate = {
                    onDocumentResized.invoke()
                }
            )
        }

        LaunchedEffect(webViewState.webView, documentStateApi) {
            webViewState.webView?.let { documentStateApi.registerOnWebView(it) }
        }

        val cssApi = remember(webViewState.webView, scriptsLoaded.value) {
            webViewState.webView
                .takeIf { scriptsLoaded.value }
                ?.let { CssApi(it) }
        }

        LaunchedEffect(cssApi, rsProperties, userProperties) {
            cssApi?.setProperties(userProperties, rsProperties)
        }

        val gesturesApi = remember(onTap, onLinkActivated) {
            GesturesApi(
                DelegatingGesturesListener(
                    onTapDelegate = { offset ->
                        val shiftedOffset = DpOffset(
                            x = offset.x + padding.left,
                            y = offset.y + padding.top
                        )
                        onTap(TapEvent(shiftedOffset))
                    },
                    onLinkActivatedDelegate = { href, outerHtml ->
                        onLinkActivated(publicationBaseUrl.relativize(href), outerHtml)
                    }
                )
            )
        }

        LaunchedEffect(webViewState.webView, gesturesApi) {
            webViewState.webView?.let { gesturesApi.registerOnWebView(it) }
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

        key(readiumCssLayout) {
            WebView(
                modifier = Modifier
                    .scrollable2D(
                        enabled = enableScroll,
                        state = scrollableState,
                        flingBehavior = NullFling2DBehavior(),
                        reverseDirection = !reverseLayout,
                        orientation = orientationState.value
                    )
                    .fillMaxSize()
                    .absolutePadding(padding),
                state = webViewState,
                factory = { RelaxedWebView(it) },
                client = webViewClient,
                onCreated = { webview ->
                    webview.settings.javaScriptEnabled = true
                    webview.settings.setSupportZoom(false)
                    webview.settings.builtInZoomControls = false
                    webview.settings.displayZoomControls = false
                    webview.settings.loadWithOverviewMode = false
                    webview.settings.useWideViewPort = false
                    webview.isVerticalScrollBarEnabled = false
                    webview.isHorizontalScrollBarEnabled = false
                    webview.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    // Prevents vertical scrolling towards blank space.
                    // See https://github.com/readium/readium-css/issues/158
                    webview.setOnTouchListener(object : View.OnTouchListener {
                        @SuppressLint("ClickableViewAccessibility")
                        override fun onTouch(view: View, event: MotionEvent): Boolean {
                            return orientationState.value == Orientation.Horizontal &&
                                event.action == MotionEvent.ACTION_MOVE
                        }
                    })
                },
                onDispose = {
                    resourceState.scrollController.value = null
                }
            )
        }
    }
}
