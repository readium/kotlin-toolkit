package org.readium.navigator.web.spread

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webview.WebView
import org.readium.navigator.web.webview.WebViewScrollable2DState
import org.readium.navigator.web.webview.WebViewState

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun SpreadWebView(
    state: WebViewState,
    client: WebViewClient
) {
    val scrollableState = remember { WebViewScrollable2DState() }

    val spreadNestedScrollConnection = SpreadNestedScrollConnection(scrollableState)

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
        }
    )
}
