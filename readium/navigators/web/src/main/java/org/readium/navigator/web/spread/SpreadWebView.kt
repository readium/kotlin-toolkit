package org.readium.navigator.web.spread

import android.annotation.SuppressLint
import android.graphics.PointF
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webapi.GesturesApi
import org.readium.navigator.web.webapi.GesturesListener
import org.readium.navigator.web.webview.WebView
import org.readium.navigator.web.webview.WebViewScrollable2DState
import org.readium.navigator.web.webview.WebViewState
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.util.AbsoluteUrl

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun SpreadWebView(
    state: WebViewState,
    client: WebViewClient,
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (AbsoluteUrl) -> Unit,
    backgroundColor: Color
) {
    val scrollableState = remember { WebViewScrollable2DState() }

    val spreadNestedScrollConnection = SpreadNestedScrollConnection(scrollableState)
    val density = LocalDensity.current

    val gesturesApi = remember(onTap) {
        val listener = object : GesturesListener {
            override fun onTap(point: PointF) {
                onTap(TapEvent(point))
            }

            override fun onLinkActivated(href: AbsoluteUrl) {
                onLinkActivated(href)
            }
        }
        GesturesApi(density, listener)
    }

    LaunchedEffect(state.webView) {
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
        }
    )
}
