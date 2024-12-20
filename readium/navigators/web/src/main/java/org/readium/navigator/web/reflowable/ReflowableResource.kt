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
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Velocity
import androidx.core.view.postDelayed
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlin.Float
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.css.Layout
import org.readium.navigator.web.css.RsProperties
import org.readium.navigator.web.css.UserProperties
import org.readium.navigator.web.gestures.Fling2DBehavior
import org.readium.navigator.web.gestures.Scroll2DScope
import org.readium.navigator.web.gestures.scrollable2D
import org.readium.navigator.web.util.DisplayArea
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webapi.CssApi
import org.readium.navigator.web.webapi.GesturesApi
import org.readium.navigator.web.webapi.GesturesListener
import org.readium.navigator.web.webapi.InitializationApi
import org.readium.navigator.web.webview.RelaxedWebView
import org.readium.navigator.web.webview.WebView
import org.readium.navigator.web.webview.WebViewLayoutInfoProvider
import org.readium.navigator.web.webview.WebViewScrollable2DState
import org.readium.navigator.web.webview.rememberWebViewState
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import pagingFlingBehavior

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
internal fun ReflowableResource(
    href: Url,
    publicationBaseUrl: AbsoluteUrl,
    webViewClient: WebViewClient,
    displayArea: DisplayArea,
    reverseLayout: Boolean,
    scroll: Boolean,
    userProperties: UserProperties,
    rsProperties: RsProperties,
    layout: Layout,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (Url, String) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val webViewState =
            rememberWebViewState(
                url = publicationBaseUrl.resolve(href).toString()
            )

        val scriptsLoaded = remember(webViewState.webView) {
            mutableStateOf(false)
        }

        val contentIsLaidOut =
            remember(webViewState.webView) { mutableStateOf(false) }

        val initializationApi = remember(webViewState.webView) {
            InitializationApi(
                onScriptsLoadedDelegate = {
                    scriptsLoaded.value = true
                },
                onDocumentLoadedDelegate = {
                    webViewState.webView?.apply {
                        post {
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK)) {
                                WebViewCompat.postVisualStateCallback(this, 0) {
                                    contentIsLaidOut.value = true
                                }
                            } else {
                                // On older devices, there's no reliable way to guarantee the page is fully laid out.
                                // As a workaround, we run a dummy JavaScript, then wait for a short delay before
                                // assuming it's ready.
                                evaluateJavascript("true") {
                                    postDelayed(500) { contentIsLaidOut.value = true }
                                }
                            }
                        }
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
                    onTap(TapEvent(offset))
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

        val scrollableState = remember { WebViewScrollable2DState() }

        val reflowableNestedScrollConnection =
            if (scroll) {
                ScrollReflowableNestedScrollConnection(scrollableState)
            } else {
                ReflowableNestedScrollConnection(scrollableState)
            }

        val density = LocalDensity.current

        val scrollOrientation = if (scroll) Orientation.Vertical else Orientation.Horizontal

        val flingBehavior =
            if (scroll) {
                null
            } else {
                (webViewState.webView as? RelaxedWebView)
                    ?.let {
                        pagingFlingBehavior(
                            WebViewLayoutInfoProvider(
                                density,
                                scrollOrientation,
                                reverseLayout,
                                displayArea,
                                it
                            )
                        )
                    }
                    ?.toFling2DBehavior(orientation = scrollOrientation)
            }

        key(layout) {
            WebView(
                modifier = Modifier
                    .scrollable2D(
                        enabled = contentIsLaidOut.value,
                        state = scrollableState,
                        flingBehavior = flingBehavior,
                        reverseDirection = !reverseLayout,
                        orientation = scrollOrientation
                    )
                    .fillMaxSize()
                    .nestedScroll(reflowableNestedScrollConnection),
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
                    if (!scroll) {
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
                }
            )
        }
    }
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
