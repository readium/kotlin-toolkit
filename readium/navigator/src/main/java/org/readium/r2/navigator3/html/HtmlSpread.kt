package org.readium.r2.navigator3.html

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import org.readium.r2.navigator.audiobook.withBaseUrl
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import timber.log.Timber
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
@Composable
internal fun HtmlSpread(
    publication: Publication,
    link: Link,
    isPaginated: Boolean,
    state: HtmlSpreadState,
    onTap: ((Offset) -> Unit)?,
    onDoubleTap: ((Offset) -> Unit)?
) {
    require(isPaginated)

    @Suppress("NAME_SHADOWING")
    val state by rememberUpdatedState(state)

    val density = LocalDensity.current.density

    AndroidView(
        modifier = Modifier
            .fillMaxSize(),
        factory = { context ->
            val webView = RelaxedWebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                onScrollChangeListener = object: RelaxedWebView.OnScrollChangeListener {
                    override fun onScrollChange(view: View, scrollX: Int, scrollY: Int,
                        oldScrollX: Int, oldScrollY: Int) {
                        state.horizontalScrollData.value =
                            state.horizontalScrollData.value?.copy(offset = scrollX)
                        state.verticalScrollData.value =
                            state.verticalScrollData.value?.copy(offset = scrollY)
                    }
                }
            }

            val onSizeChanged: (Int, Int) -> Unit = { contentWidth, contentHeight ->
                Timber.v("onSizeChanged $contentWidth $contentHeight")
                state.horizontalScrollData.value =
                    HtmlSpreadState.ScrollData(
                        offset = webView.horizontalScrollOffset,
                        range = (contentWidth * density).roundToInt(),
                        extent = state.viewportSize.width
                    )
                state.verticalScrollData.value =
                    HtmlSpreadState.ScrollData(
                        offset = webView.verticalScrollOffset,
                        range = (contentHeight * density).roundToInt(),
                        extent = state.viewportSize.height
                    )
            }
            val jsReceiver = JavaScriptReceiver(state.viewportSize, onTap, onDoubleTap, onSizeChanged)

            val jsExecutor = JavaScriptExecutor()
            jsExecutor.webView = webView

            val connection = WebViewConnection.create(
                webView = webView,
                publication = publication,
                jsReceiver = jsReceiver,
                jsExecutor = jsExecutor

            )

            try {
                val url = link.withBaseUrl("http://127.0.0.1/publication").href
                connection.openURL(url)
                connection.executeJS {
                    doNothing()
                }
            } catch (e: WebViewLoadException) {
                // Do nothing
            }
            webView
        },
        update = { webView ->
            state.horizontalScrollData.value
                ?.let { if (it.offset != webView.scrollX) webView.scrollX = it.offset }
        }
    )
}
