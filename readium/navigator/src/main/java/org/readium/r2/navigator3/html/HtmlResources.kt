package org.readium.r2.navigator3.html

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import org.readium.r2.navigator.extensions.withBaseUrl
import org.readium.r2.navigator3.Overflow
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

@Composable
fun HtmlResource(
    publication: Publication,
    link: Link,
    overflow: Overflow
) {
    if (overflow == Overflow.PAGINATED) {
        HtmlPaginatedResource(publication, link)
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
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val connection = WebViewConnection.create(
                    webView = this,
                    publication = publication,
                    jsReceiver =  JavaScriptReceiver(),

                )
                try {
                    val url = link.withBaseUrl("http://127.0.0.1/publication").href
                    connection.openURL(url)
                } catch (e: WebViewLoadException) {
                    // Do nothing
                }

            }
        },
        update = { webView ->


        }
    )
}