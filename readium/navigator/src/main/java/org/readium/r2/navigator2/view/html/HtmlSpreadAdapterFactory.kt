package org.readium.r2.navigator2.view.html

import android.content.Context
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import org.readium.r2.navigator.R
import org.readium.r2.navigator2.view.SpreadAdapter
import org.readium.r2.navigator2.view.SpreadAdapterFactory
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import android.view.MotionEvent

class HtmlSpreadAdapterFactory(
    private val publication: Publication,
    private val baseUrl: String,
) : SpreadAdapterFactory {

    private val webViewClient = object: WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            view.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            view.requestLayout()
        }
    }

    override fun createSpread(links: List<Link>): Pair<SpreadAdapter, List<Link>>? {
        val first = links.first()
        if (!first.mediaType.isHtml) {
            return null
        }

        val spread = HtmlSpreadAdapter(listOf(first), baseUrl, publication)
        return Pair(spread, links.subList(1, links.size))
    }

    override fun createView(context: Context, viewportSize: Size): View {
        val view = View.inflate(context, R.layout.navigator2_item_webview, null)
        check(view is FrameLayout)

        val webView = view.findViewById<WebView>(R.id.r2_item_webview_webview)
        webView.apply {
            isScrollContainer = false
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            webViewClient = this@HtmlSpreadAdapterFactory.webViewClient
        }

        return view
    }
}