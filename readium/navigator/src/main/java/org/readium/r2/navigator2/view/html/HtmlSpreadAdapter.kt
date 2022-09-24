package org.readium.r2.navigator2.view.html

import android.graphics.PointF
import android.net.Uri
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import org.readium.r2.navigator.R
import org.readium.r2.navigator2.view.ResourceAdapter
import org.readium.r2.navigator2.view.SpreadAdapter
import org.readium.r2.navigator2.view.image.ImageResourceAdapter
import org.readium.r2.navigator3.html.HtmlInjector
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Href

class HtmlSpreadAdapter(
    override val links: List<Link>,
    baseUrl: String,
    private val publication: Publication
) : SpreadAdapter {

    val baseUrl: String =
        baseUrl.removeSuffix("/") + "/"

    /**
     * Generates the URL to the given publication link.
     */
    fun urlTo(link: Link): String =
        with(link) {
            // Already an absolute URL?
            if (Uri.parse(href).scheme != null) {
                href
            } else {
                Href(
                    href = href.removePrefix("/"),
                    baseHref = baseUrl
                ).percentEncodedString
            }
        }

    override fun bind(view: View) {
        check(view is FrameLayout)
        val webView = view.findViewById<WebView>(R.id.r2_item_webview_webview)
        //val url = links.first().withBaseUrl("http://127.0.0.1/publication").href
        val url = urlTo(links.first())
        val htmlInjector = HtmlInjector(publication)
        //webView.webViewClient = WebViewClient(publication, htmlInjector)
        webView.loadUrl(url)
    }

    override fun unbind(view: View) {
        check(view is FrameLayout)
        val webView = view.findViewById<WebView>(R.id.r2_item_webview_webview)
        webView.loadUrl("about:blank")
    }

    override fun scrollForLocations(locations: Locator.Locations, view: View): PointF {
        return PointF(0f, 0f)
    }

    override fun resourceAdapters(view: View): List<ResourceAdapter> {
        return links.map {
            ImageResourceAdapter(it, view)
        }
    }
}