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
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Velocity
import kotlin.math.ceil
import kotlin.math.floor
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.css.Layout
import org.readium.navigator.web.css.RsProperties
import org.readium.navigator.web.css.UserProperties
import org.readium.navigator.web.gestures.Fling2DBehavior
import org.readium.navigator.web.gestures.Scroll2DScope
import org.readium.navigator.web.gestures.scrollable2D
import org.readium.navigator.web.util.AbsolutePaddingValues
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.util.absolutePadding
import org.readium.navigator.web.webapi.CssApi
import org.readium.navigator.web.webapi.GesturesApi
import org.readium.navigator.web.webapi.GesturesListener
import org.readium.navigator.web.webapi.InitializationApi
import org.readium.navigator.web.webview.RelaxedWebView
import org.readium.navigator.web.webview.WebView
import org.readium.navigator.web.webview.WebViewLayoutInfoProvider
import org.readium.navigator.web.webview.WebViewScrollController
import org.readium.navigator.web.webview.WebViewScrollable2DState
import org.readium.navigator.web.webview.rememberWebViewState
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import pagingFlingBehavior

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
internal fun ReflowableResource(
    resourceState: ReflowableResourceState,
    pagerState: PagerState,
    publicationBaseUrl: AbsoluteUrl,
    webViewClient: WebViewClient,
    viewportSize: DpSize,
    backgroundColor: Color,
    padding: AbsolutePaddingValues,
    reverseLayout: Boolean,
    scroll: Boolean,
    verticalText: Boolean,
    userProperties: UserProperties,
    rsProperties: RsProperties,
    layout: Layout,
    initialProgression: Double,
    stickToInitialProgression: Boolean,
    enableScroll: Boolean,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (Url, String) -> Unit,
    onScrollChanged: (Double) -> Unit,
) {
    val scrollOrientation = when {
        verticalText -> Orientation.Horizontal
        scroll -> Orientation.Vertical
        else -> Orientation.Horizontal
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val webViewState =
            rememberWebViewState<RelaxedWebView>(
                url = publicationBaseUrl.resolve(resourceState.href).toString()
            )

        val scriptsLoaded = remember(webViewState.webView) {
            mutableStateOf(false)
        }

        val contentIsLaidOut =
            remember(webViewState.webView) { mutableStateOf(false) }

        val initializationApi = remember(webViewState.webView, stickToInitialProgression) {
            InitializationApi(
                onScriptsLoadedDelegate = {
                    scriptsLoaded.value = true
                },
                onDocumentLoadedDelegate = {
                    webViewState.webView?.apply {
                        post {
                            postVisualStateCallback(
                                0,
                                object : WebView.VisualStateCallback() {
                                    override fun onComplete(requestId: Long) {
                                        contentIsLaidOut.value = true
                                    }
                                }
                            )
                        }
                    }
                },
                onDocumentResizedDelegate = {
                    resourceState.scrollController.value = webViewState.webView?.let { WebViewScrollController(it) }
                    if (stickToInitialProgression) {
                        webViewState.webView?.scrollToProgression(initialProgression, scrollOrientation)
                    }
                }
            )
        }

        val cssApi = remember(webViewState.webView, scriptsLoaded.value) {
            webViewState.webView
                .takeIf { scriptsLoaded.value }
                ?.let { CssApi(it) }
        }

        cssApi?.let { api ->
            LaunchedEffect(rsProperties, userProperties) {
                api.setProperties(userProperties, rsProperties)
            }
        }

        val gesturesApi = remember(onTap) {
            val listener = object : GesturesListener {
                override fun onTap(offset: DpOffset) {
                    val shiftedOffset = DpOffset(
                        x = offset.x + padding.left,
                        y = offset.y + padding.top
                    )
                    onTap(TapEvent(shiftedOffset))
                }

                override fun onLinkActivated(href: AbsoluteUrl, outerHtml: String) {
                    onLinkActivated(publicationBaseUrl.relativize(href), outerHtml)
                }
            }
            GesturesApi(listener)
        }

        LaunchedEffect(webViewState.webView) {
            webViewState.webView?.let { initializationApi.registerOnWebView(it) }
            webViewState.webView?.let { gesturesApi.registerOnWebView(it) }
        }

        val scrollableState = remember { WebViewScrollable2DState(resourceState.href) }

        val density = LocalDensity.current

        LaunchedEffect(contentIsLaidOut, contentIsLaidOut.value, scrollableState.webView) {
            scrollableState.webView
                .takeIf { contentIsLaidOut.value }
                ?.let { webview ->
                    webview.scrollToProgression(initialProgression, scrollOrientation)
                    webview.setOnScrollChangeListener { view, scrollX, scrollY, oldScrollX, oldScrollY ->
                        onScrollChanged(webview.progression(scrollOrientation))
                    }
                }
        }

        val webViewViewport = DpSize(
            width = viewportSize.width - padding.left - padding.right,
            height = viewportSize.height - padding.top - padding.bottom
        )

        val flingBehavior =
            if (scroll) {
                null
            } else {
                webViewState.webView
                    ?.let {
                        pagingFlingBehavior(
                            WebViewLayoutInfoProvider(
                                density,
                                scrollOrientation,
                                reverseLayout,
                                webViewViewport,
                                it
                            )
                        )
                    }
                    ?.toFling2DBehavior(orientation = scrollOrientation)
            }

        val resourceScrollConnection =
            ResourceNestedScrollConnection(pagerState, scrollableState, scrollOrientation)

        key(layout) {
            WebView(
                modifier = Modifier
                    // Detect taps on padding
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (it.x >= 0 && it.y >= 0) {
                                    val offset = DpOffset(x = it.x.toDp(), y = it.y.toDp())
                                    val event = TapEvent(offset)
                                    onTap(event)
                                }
                            }
                        )
                    }.nestedScroll(resourceScrollConnection)
                    .scrollable2D(
                        enabled = enableScroll,
                        state = scrollableState,
                        flingBehavior = flingBehavior,
                        reverseDirection = !reverseLayout,
                        orientation = scrollOrientation
                    )
                    .fillMaxSize()
                    .background(backgroundColor)
                    .absolutePadding(padding),
                state = webViewState,
                factory = { RelaxedWebView(it) },
                client = webViewClient,
                onCreated = { webview ->
                    scrollableState.webView = webview
                    webview.settings.javaScriptEnabled = true
                    webview.settings.setSupportZoom(false)
                    webview.settings.builtInZoomControls = false
                    webview.settings.displayZoomControls = false
                    webview.settings.loadWithOverviewMode = false
                    webview.settings.useWideViewPort = false
                    webview.isVerticalScrollBarEnabled = false
                    webview.isHorizontalScrollBarEnabled = false
                    webview.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    if (verticalText || !scroll) {
                        // Prevents vertical scrolling towards blank space.
                        // See https://github.com/readium/readium-css/issues/158
                        webview.setOnTouchListener(object : View.OnTouchListener {
                            @SuppressLint("ClickableViewAccessibility")
                            override fun onTouch(view: View, event: MotionEvent): Boolean {
                                return (event.action == MotionEvent.ACTION_MOVE)
                            }
                        })
                    }
                },
                onDispose = {
                    scrollableState.webView = null
                    resourceState.scrollController.value = null
                }
            )
        }
    }
}

private fun RelaxedWebView.scrollToProgression(progression: Double, scrollOrientation: Orientation) {
    if (scrollOrientation == Orientation.Horizontal) {
        scrollTo(floor(progression * maxScrollX).toInt(), 0)
    } else {
        scrollTo(0, ceil(progression * maxScrollY).toInt())
    }
}

private fun RelaxedWebView.progression(orientation: Orientation) = when (orientation) {
    Orientation.Vertical -> scrollY / maxScrollY.toDouble()
    Orientation.Horizontal -> scrollX / maxScrollX.toDouble()
}

private fun FlingBehavior.toFling2DBehavior(orientation: Orientation) =
    object : Fling2DBehavior {
        override suspend fun Scroll2DScope.performFling(
            initialVelocity: Velocity,
        ): Velocity {
            val scrollScope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float =
                    when (orientation) {
                        Orientation.Vertical -> scrollBy(Offset(0f, pixels)).y
                        Orientation.Horizontal -> scrollBy(Offset(pixels, 0f)).x
                    }
            }

            val velocity =
                when (orientation) {
                    Orientation.Vertical -> initialVelocity.y
                    Orientation.Horizontal -> initialVelocity.x
                }

            val remainingVelocity =
                scrollScope.performFling(velocity)

            return when (orientation) {
                Orientation.Vertical -> Velocity(0f, remainingVelocity)
                Orientation.Horizontal -> Velocity(remainingVelocity, 0f)
            }
        }
    }
