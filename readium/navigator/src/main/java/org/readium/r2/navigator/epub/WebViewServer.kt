/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import android.app.Application
import android.os.PatternMatcher
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.AccessException
import org.readium.r2.shared.util.data.BlobInputStream
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.http.HttpHeaders
import org.readium.r2.shared.util.http.HttpRange
import org.readium.r2.shared.util.resource.Resource

/**
 * Serves the publication resources and application assets in the EPUB navigator web views.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class WebViewServer(
    private val application: Application,
    private val publication: Publication,
    servedAssets: List<String>,
    private val disableSelectionWhenProtected: Boolean,
    private val onResourceLoadFailed: (Url, ReadError) -> Unit
) {
    companion object {
        val publicationBaseHref = AbsoluteUrl("https://readium/publication/")!!
        val assetsBaseHref = AbsoluteUrl("https://readium/assets/")!!

        fun assetUrl(path: String): Url? =
            Url.fromDecodedPath(path)?.let { assetsBaseHref.resolve(it) }
    }

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
                val href = Url.fromDecodedPath(path.removePrefix("/publication/"))
                    ?: return null

                servePublicationResource(
                    href = href,
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
    private fun servePublicationResource(href: Url, range: HttpRange?, css: ReadiumCss): WebResourceResponse {
        val link = publication.linkWithHref(href)
            // Query parameters must be kept as they might be relevant for the fetcher.
            ?.copy(href = Href(href))
            ?: Link(href = href)

        // Drop anchor because it is meant to be interpreted by the client.
        val linkWithoutAnchor = link.copy(
            href = Href(href.removeFragment())
        )

        var resource = publication.get(linkWithoutAnchor)
        // FIXME: report loading errors through Navigator.Listener.onResourceLoadingFailed
        // .fallback { errorResource(link, error = it) }
        if (link.mediaType?.isHtml == true) {
            resource = resource.injectHtml(
                publication,
                css,
                baseHref = assetsBaseHref,
                disableSelectionWhenProtected = disableSelectionWhenProtected
            )
        }

        val headers = mutableMapOf(
            "Accept-Ranges" to "bytes"
        )

        if (range == null) {
            return WebResourceResponse(
                link.mediaType?.toString(),
                null,
                200,
                "OK",
                headers,
                BlobInputStream(resource, ::AccessException)
            )
        } else { // Byte range request
            val stream = BlobInputStream(resource, ::AccessException)
            val length = stream.available()
            val longRange = range.toLongRange(length.toLong())
            headers["Content-Range"] = "bytes ${longRange.first}-${longRange.last}/$length"
            // Content-Length will automatically be filled by the WebView using the Content-Range header.
            // headers["Content-Length"] = (longRange.last - longRange.first + 1).toString()
            // Weirdly, the WebView will call itself stream.skip to skip to the requested range.
            return WebResourceResponse(
                link.mediaType?.toString(),
                null,
                206,
                "Partial Content",
                headers,
                stream
            )
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
