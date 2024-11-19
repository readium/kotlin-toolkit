/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.navigator.epub

import android.app.Application
import android.os.PatternMatcher
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.asInputStream
import org.readium.r2.shared.util.http.HttpHeaders
import org.readium.r2.shared.util.http.HttpRange
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.StringResource
import org.readium.r2.shared.util.resource.fallback

/**
 * Serves the publication resources and application assets in the EPUB navigator web views.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class WebViewServer(
    private val application: Application,
    private val publication: Publication,
    servedAssets: List<String>,
    private val disableSelectionWhenProtected: Boolean,
    private val onResourceLoadFailed: (Url, ReadError) -> Unit,
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
        val urlWithoutAnchor = href.removeFragment()

        var resource = publication
            .get(urlWithoutAnchor)
            ?.fallback {
                onResourceLoadFailed(urlWithoutAnchor, it)
                errorResource()
            } ?: run {
            val error = ReadError.Decoding(
                "Resource not found at $urlWithoutAnchor in publication."
            )
            onResourceLoadFailed(urlWithoutAnchor, error)
            errorResource()
        }

        link.mediaType
            ?.takeIf { it.isHtml }
            ?.let {
                resource = resource.injectHtml(
                    publication,
                    mediaType = it,
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
                resource.asInputStream()
            )
        } else { // Byte range request
            val stream = resource.asInputStream()
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
    private fun errorResource(): Resource =
        StringResource {
            withContext(Dispatchers.IO) {
                Try.success(
                    application.assets
                        .open("readium/error.xhtml")
                        .bufferedReader()
                        .use { it.readText() }
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
