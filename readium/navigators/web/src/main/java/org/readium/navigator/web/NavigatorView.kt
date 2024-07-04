package org.readium.navigator.web

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.readium.navigator.web.util.WebContent
import org.readium.navigator.web.util.WebView
import org.readium.navigator.web.util.WebViewServer
import org.readium.navigator.web.util.WebViewState
import org.readium.r2.shared.ExperimentalReadiumApi

@SuppressLint("SetJavaScriptEnabled")
@ExperimentalReadiumApi
@Composable
public fun NavigatorView(
    state: NavigatorState
) {
    LazyRow {
        items(state.spreads.value) {
            when (it) {
                is LayoutResolver.Spread.Double ->
                    throw NotImplementedError()
                is LayoutResolver.Spread.Single -> {
                    val url = WebViewServer.publicationBaseHref.resolve(it.url).toString()
                    val webContent = WebContent.Url(url)
                    val webViewState = WebViewState(webContent)
                    WebView(
                        modifier = Modifier.fillParentMaxSize(),
                        state = webViewState,
                        client = state.webViewClient,
                        onCreated = { webview ->
                            webview.settings.javaScriptEnabled = true
                            webview.settings.setSupportZoom(true)
                            webview.settings.builtInZoomControls = true
                            webview.settings.displayZoomControls = false
                            webview.settings.loadWithOverviewMode = true
                            webview.settings.useWideViewPort = true
                        }
                    )
                }
            }
        }
    }
}
