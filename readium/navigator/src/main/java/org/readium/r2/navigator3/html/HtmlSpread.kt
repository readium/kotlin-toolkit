package org.readium.r2.navigator3.html

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.common.util.concurrent.MoreExecutors
import org.readium.r2.navigator.extensions.withBaseUrl
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

    val widthDp = with(LocalDensity.current) {
        state.viewportSize.width.toDp()
    }

    val jsExecutor = remember {
        JavaScriptExecutor()
    }

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
                jsExecutor.webView = this

                onScrollChangeListener = object: RelaxedWebView.OnScrollChangeListener {
                    override fun onScrollChange(
                        view: View,
                        scrollX: Int,
                        scrollY: Int,
                        oldScrollX: Int,
                        oldScrollY: Int
                    ) {

                    }
                }
            }

            val onSizeChanged: (Int, Int) -> Unit = { contentWidth, contentHeight ->
                Timber.d("$link onDocumentReady")
                Timber.v("onDocumentReady ${link.href} ${webView.horizontalScrollRange} ${webView.horizontalScrollExtent} ${webView.horizontalScrollOffset}")
                //state.canScrollLeft = webView.canScrollHorizontally(-1)
                state.canScrollRight = contentWidth.dp > widthDp
                state.horizontalRange.value = (contentWidth * density).roundToInt()
                Timber.d("contentWidth $contentWidth viewportWidth ${widthDp.value}")
                //state.canScrollLeft = webView.canScrollHorizontally(-1)
                //state.canScrollRight = webView.canScrollHorizontally(1)
            }
            val jsReceiver = JavaScriptReceiver(state.viewportSize, onTap, onDoubleTap, onSizeChanged)

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
                }.addListener(
                    {
                        Timber.v("listener ${link.href} $state")
                        Timber.d("webview progress ${link.href} ${webView.progress}")
                        Timber.d("listener canScrollLeft ${link.href} ${state.canScrollLeft}")
                        Timber.d("listener canScrollRight ${link.href} ${state.canScrollRight}")
                        Timber.v("listener ${link.href} $webView $webView")
                        state.canScrollLeft = webView.canScrollHorizontally(-1)
                        state.canScrollRight = webView.canScrollHorizontally(1)
                        //(state.offset.value + viewportSize.width) < webView.horizontalScrollRange
                        Timber.v("listener ${link.href} ${webView.horizontalScrollRange} ${webView.horizontalScrollExtent} ${webView.horizontalScrollOffset}")
                        Timber.d("listener canScrollLeft ${link.href} ${state.canScrollLeft}")
                        Timber.d("listener canScrollRight ${link.href} ${state.canScrollRight}")


                    },
                    MoreExecutors.directExecutor()
                )
            } catch (e: WebViewLoadException) {
                // Do nothing
            }
            webView
        },
        update = { webView ->

            //Timber.v("${webView.horizontalScrollRange} ${webView.horizontalScrollExtent} ${webView.horizontalScrollOffset}")

            Timber.v("update ${link.href} $state")

            Timber.d("canScrollLeft ${link.href} ${state.canScrollLeft}")
            Timber.d("canScrollRight ${link.href} ${state.canScrollRight}")
            Timber.v("update${link.href} ${webView.horizontalScrollRange} ${webView.horizontalScrollExtent} ${webView.horizontalScrollOffset}")
            Timber.v("Scrolling to ${link.href} ${state.offset.value}")


            when {
                state.pendingProgression.value != null -> {
                    state.horizontalRange.value?.let { documentWidth ->
                        val offset = (documentWidth * state.pendingProgression.value!!).roundToInt()
                        val value = offset + 1
                        val newOffset = (value + - (value % webView.horizontalScrollExtent))
                        state.pendingProgression.value = null
                        webView.scrollX = newOffset
                        state.offset.value = newOffset
                    }
                }
                webView.scrollX != state.offset.value -> {
                    webView.scrollX = state.offset.value
                    state.canScrollLeft = webView.canScrollHorizontally(-1)
                    state.canScrollRight =  webView.canScrollHorizontally(1)
                    Timber.d("canScrollLeft ${link.href} ${state.canScrollLeft}")
                    Timber.d("canScrollRight ${link.href} ${state.canScrollRight}")
                    Timber.v("update ${link.href} ${webView.horizontalScrollRange} ${webView.horizontalScrollExtent} ${webView.horizontalScrollOffset}")

                }
            }
        }
    )
}
