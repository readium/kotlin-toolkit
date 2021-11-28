package org.readium.r2.navigator2.view.html

import android.graphics.PointF
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import org.readium.r2.navigator.R
import org.readium.r2.navigator.extensions.withBaseUrl
import org.readium.r2.navigator2.view.ResourceAdapter
import org.readium.r2.navigator2.view.SpreadAdapter
import org.readium.r2.navigator2.view.image.ImageResourceAdapter
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

class HtmlSpreadAdapter(
    override val links: List<Link>,
    private val baseUrl: String
) : SpreadAdapter {

    override fun bind(view: View) {
        check(view is FrameLayout)
        val webView = view.findViewById<WebView>(R.id.r2_item_webview_webview)
        val url = links.first().withBaseUrl(baseUrl).href
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