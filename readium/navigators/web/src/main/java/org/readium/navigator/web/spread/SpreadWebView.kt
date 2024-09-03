package org.readium.navigator.web.spread

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webview.WebView
import org.readium.navigator.web.webview.WebViewState

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun PageWebView(
    state: WebViewState,
    client: WebViewClient
) {
    WebView(
        modifier = Modifier
            .fillMaxSize(),
        state = state,
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
        }
    )
}
