package org.readium.r2.navigator.epub

import android.app.Application
import android.content.res.AssetManager
import android.os.PatternMatcher
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceInputStream
import org.readium.r2.shared.fetcher.StringResource
import org.readium.r2.shared.fetcher.fallback
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.http.HttpHeaders
import org.readium.r2.shared.util.http.HttpRange
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Serves the publication resources and application assets in the EPUB navigator web views.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class WebViewServer(
    private val application: Application,
    private val publication: Publication,
    servedAssets: List<String>,
) {
    companion object {
        val publicationBaseHref = "https://readium/publication/"
        val assetsBaseHref = "https://readium/assets/"

        fun assetUrl(path: String): String =
            Href(path, baseHref = assetsBaseHref).percentEncodedString
    }

    private val assetManager: AssetManager = application.assets

    /**
     * Serves the requests of the navigator web views.
     *
     * https://readium/publication/ serves the publication resources through its fetcher.
     * https://readium/assets/ serves the application assets.
     */
    fun shouldInterceptRequest(request: WebResourceRequest, css: ReadiumCss): WebResourceResponse? {
        if (request.url.host != "readium") return null
        val path = request.url.path ?: return null

        return when {
            path.startsWith("/publication/") -> {
                servePublicationResource(
                    href = path.removePrefix("/publication"),
                    range = HttpHeaders(request.requestHeaders).range,
                    css = css
                )
            }
            path.startsWith("/assets/") && isServedAsset(path.removePrefix("/assets/")) -> {
                assetsLoader.shouldInterceptRequest(request.url)
            }
            else -> null
        }
    }

    /**
     * Returns a new [Resource] to serve the given [href] in the publication.
     *
     * If the [Resource] is an HTML document, injects the required JavaScript and CSS files.
     */
    private fun servePublicationResource(href: String, range: HttpRange?, css: ReadiumCss): WebResourceResponse {
        val link = publication.linkWithHref(href)
            // Query parameters must be kept as they might be relevant for the fetcher.
            ?.copy(href = href)
            ?: Link(href = href)

        var resource = publication.get(link)
            .fallback { errorResource(link, error = it) }
        if (link.mediaType.isHtml) {
            resource = resource.injectHtml(publication, css, baseHref = assetsBaseHref)
        }

        val headers = mutableMapOf(
            "Accept-Ranges" to "bytes",
        )

        if (range == null) {
            return WebResourceResponse(link.type, null, 200, "OK", headers, ResourceInputStream(resource))

        } else { // Byte range request
            val stream = ResourceInputStream(resource)
            val length = stream.available()
            val longRange = range.toLongRange(length.toLong())
            headers["Content-Range"] = "bytes ${longRange.first}-${longRange.last}/$length"
            // Content-Length will automatically be filled by the WebView using the Content-Range header.
//            headers["Content-Length"] = (longRange.last - longRange.first + 1).toString()
            return WebResourceResponse(link.type, null, 206, "Partial Content", headers, stream)
        }
    }

    private fun errorResource(link: Link, error: Resource.Exception): Resource =
        StringResource(link.copy(type = MediaType.XHTML.toString())) {
            withContext(Dispatchers.IO) {
                assetManager
                    .open("readium/error.xhtml").bufferedReader()
                    .use { it.readText() }
                    .replace("\${error}", error.getUserMessage(application))
                    .replace("\${href}", link.href)
            }
        }

    private fun isServedAsset(path: String): Boolean =
        servedAssetPatterns.any { it.match(path) }

    private val servedAssetPatterns: List<PatternMatcher> =
        servedAssets.map { PatternMatcher(it, PatternMatcher.PATTERN_SIMPLE_GLOB) }

    private val assetsLoader =
        WebViewAssetLoader.Builder()
            .setDomain("readium")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(application))
            .build()
}