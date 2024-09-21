@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.web.spread

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.web.layout.DoubleViewportSpread
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webapi.PrepaginatedDoubleApi
import org.readium.navigator.web.webview.LoadingState
import org.readium.navigator.web.webview.rememberWebViewStateWithHTMLData
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl

@Composable
internal fun DoubleViewportSpread(
    state: DoubleSpreadState
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val webViewState = rememberWebViewStateWithHTMLData(
            data = state.htmlData,
            baseUrl = state.publicationBaseUrl.toString()
        )

        val webApi = remember(webViewState.webView, webViewState.loadingState) {
            webViewState.webView
                ?.takeIf { webViewState.loadingState is LoadingState.Finished }
                ?.let {
                    PrepaginatedDoubleApi(it)
                }
        }

        webApi?.let { api ->
            LaunchedEffect(api) {
                snapshotFlow {
                    state.fit.value
                }.onEach {
                    api.setFit(state.fit.value)
                }.launchIn(this)

                snapshotFlow {
                    state.viewport.value
                }.onEach {
                    val (width, height) = state.viewport.value
                    api.setViewport(width, height)
                }.launchIn(this)

                api.loadSpread(state.spread)
            }
        }

        SpreadWebView(
            state = webViewState,
            client = state.webViewClient
        )
    }
}

internal class DoubleSpreadState(
    val htmlData: String,
    val publicationBaseUrl: AbsoluteUrl,
    val webViewClient: WebViewClient,
    val spread: DoubleViewportSpread,
    val fit: State<Fit>,
    val viewport: State<Size>
) {
    val left: AbsoluteUrl? =
        spread.leftPage?.let { publicationBaseUrl.resolve(it.href) }

    val right: AbsoluteUrl? =
        spread.rightPage?.let { publicationBaseUrl.resolve(it.href) }
}
