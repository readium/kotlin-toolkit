package org.readium.r2.navigator3.html

import android.os.Build
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import com.google.common.util.concurrent.MoreExecutors
import org.readium.r2.navigator.extensions.withBaseUrl
import org.readium.r2.navigator3.core.util.logConstraints
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

@Composable
internal fun HtmlSpread(
    publication: Publication,
    link: Link,
    isPaginated: Boolean,
    state: HtmlSpreadState,
    onTap: ((Offset) -> Unit)?,
    onDoubleTap: ((Offset) -> Unit)?
) {
    if (isPaginated) {
        HtmlPaginatedResource(publication, link, state, onTap, onDoubleTap)
    } else {
        throw NotImplementedError()
        /*AndroidView(
            factory = { context ->
                WebView(context).apply {
                    isScrollContainer = false
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    loadUrl(link.href)
                }
            },
            update = { webview ->


            }
        )*/
    }

}

@Composable
private fun HtmlPaginatedResource(
    publication: Publication,
    link: Link,
    state: HtmlSpreadState,
    onTap: ((Offset) -> Unit)?,
    onDoubleTap: ((Offset) -> Unit)?
) {
    BoxWithConstraints(
        propagateMinConstraints = true
    ) {
        val jsExecutor = remember {
            JavaScriptExecutor()
        }

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .logConstraints("AndroidView"),
            factory = { context ->
                val webView = WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    jsExecutor.webView = this

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                            state.canScrollLeft = canScrollHorizontally(-1)
                            state.canScrollRight = canScrollHorizontally(1)
                        }
                    }
                }
                val viewportSize = IntSize(constraints.maxWidth, constraints.maxHeight)
                val connection = WebViewConnection.create(
                    webView = webView,
                    publication = publication,
                    jsReceiver = JavaScriptReceiver(viewportSize, onTap, onDoubleTap),
                    jsExecutor = jsExecutor

                )
                try {
                    val url = link.withBaseUrl("http://127.0.0.1/publication").href
                    connection.openURL(url)
                        connection.executeJS {
                            scrollToStart()
                        }

                        .addListener(
                            {
                                webView.postDelayed({
                                    state.canScrollLeft = webView.canScrollHorizontally(-1)
                                    state.canScrollRight = webView.canScrollHorizontally(1)
                                }, 1000)

                            },
                            MoreExecutors.directExecutor()
                        )
                } catch (e: WebViewLoadException) {
                    // Do nothing
                }
                webView
            },
            update = { webView ->
                state.pendingCommands.value.forEach { it ->
                    jsExecutor.it()
                }

                state.pendingCommands.value = emptyList()
            }
        )
    }


}