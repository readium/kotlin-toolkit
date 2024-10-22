package org.readium.navigator.web.spread

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.DpOffset
import org.readium.navigator.common.LinkContext
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webapi.GesturesApi
import org.readium.navigator.web.webapi.GesturesListener
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
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (AbsoluteUrl, LinkContext?) -> Unit,
    backgroundColor: Color
) {
    val scrollableState = remember { WebViewScrollable2DState() }

    val spreadNestedScrollConnection = SpreadNestedScrollConnection(scrollableState)

    val gesturesApi = remember(onTap) {
        val listener = object : GesturesListener {
            override fun onTap(offset: DpOffset) {
                onTap(TapEvent(offset))
            }

            override fun onLinkActivated(href: AbsoluteUrl) {
                onLinkActivated(href, null)
            }
        }
        GesturesApi(listener)
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
