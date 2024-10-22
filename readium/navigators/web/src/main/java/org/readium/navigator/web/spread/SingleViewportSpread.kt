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
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.common.LinkContext
import org.readium.navigator.common.TapEvent
import org.readium.navigator.web.layout.SingleViewportSpread
import org.readium.navigator.web.util.DisplayArea
import org.readium.navigator.web.util.WebViewClient
import org.readium.navigator.web.webapi.FixedSingleApi
import org.readium.navigator.web.webview.LoadingState
import org.readium.navigator.web.webview.rememberWebViewStateWithHTMLData
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

@Composable
internal fun SingleViewportSpread(
    onTap: (TapEvent) -> Unit,
    onLinkActivated: (Url, LinkContext?) -> Unit,
    state: SingleSpreadState,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        propagateMinConstraints = true
    ) {
        val webViewState = rememberWebViewStateWithHTMLData(
            data = state.htmlData,
            baseUrl = state.publicationBaseUrl.toString()
        )

        val layoutApi = remember(webViewState.webView, webViewState.loadingState) {
            webViewState.webView
                ?.takeIf { webViewState.loadingState is LoadingState.Finished }
                ?.let {
                    FixedSingleApi(it)
                }
        }

        layoutApi?.let { api ->
            LaunchedEffect(api) {
                snapshotFlow {
                    state.fit.value
                }.onEach {
                    api.setFit(it)
                }.launchIn(this)

                snapshotFlow {
                    state.displayArea.value
                }.onEach {
                    api.setDisplayArea(it)
                }.launchIn(this)

                api.loadSpread(state.spread)
            }
        }

        SpreadWebView(
            state = webViewState,
            client = state.webViewClient,
            onTap = onTap,
            onLinkActivated = { url, context ->
                onLinkActivated(
                    state.publicationBaseUrl.relativize(url),
                    context
                )
            },
            backgroundColor = backgroundColor
        )
    }
}

internal class SingleSpreadState(
    val htmlData: String,
    val publicationBaseUrl: AbsoluteUrl,
    val webViewClient: WebViewClient,
    val spread: SingleViewportSpread,
    val fit: State<Fit>,
    val displayArea: State<DisplayArea>
) {
    val url: AbsoluteUrl =
        publicationBaseUrl.resolve(spread.page.href)
}
