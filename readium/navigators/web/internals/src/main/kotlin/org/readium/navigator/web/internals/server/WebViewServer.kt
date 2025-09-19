/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.web.internals.server

import android.app.Application
import android.os.PatternMatcher
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.asInputStream
import org.readium.r2.shared.util.http.HttpHeaders
import org.readium.r2.shared.util.http.HttpRange
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.StringResource
import org.readium.r2.shared.util.resource.fallback

/**
 * Serves the publication resources and application assets in the EPUB navigator web views.
 */
public class WebViewServer(
    private val application: Application,
    private val container: Container<Resource>,
    private val mediaTypes: Map<Url, MediaType>,
    private val errorPage: RelativeUrl,
    private val htmlInjector: (Resource, MediaType) -> Resource,
    servedAssets: List<String>,
    private val onResourceLoadFailed: (Url, ReadError) -> Unit,
) {
    public companion object {
        public val publicationBaseHref: AbsoluteUrl = AbsoluteUrl("https://readium/publication/")!!
        public val assetsBaseHref: AbsoluteUrl = AbsoluteUrl("https://readium/assets/")!!

        public fun assetUrl(path: String): AbsoluteUrl? =
            Url.fromDecodedPath(path)?.let { assetsBaseHref.resolve(it) }
    }

    /**
     * Serves the requests of the navigator web view    s.
     *
     * https://readium/publication/ serves the publication resources through its fetcher.
     * https://readium/assets/ serves the application assets.
     */
    public fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
        if (request.url.host != "readium") return null
        val path = request.url.path ?: return null

        return when {
            path.startsWith("/publication/") -> {
                val href = Url.fromDecodedPath(path.removePrefix("/publication/"))
                    // Drop anchor because it is meant to be interpreted by the client.
                    // Query parameters must be kept as they might be relevant for the container.
                    ?.removeFragment()
                    ?: return null

                servePublicationResource(
                    href = href,
                    range = HttpHeaders(request.requestHeaders).range
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
    private fun servePublicationResource(href: Url, range: HttpRange?): WebResourceResponse {
        var resource = container[href]
            ?.fallback {
                onResourceLoadFailed(href, it)
                errorResource()
            } ?: run {
            val error = ReadError.Decoding(
                "Resource not found at $href in publication."
            )
            onResourceLoadFailed(href, error)
            errorResource()
        }

        val mediaType = mediaTypes[href]

        mediaType
            ?.takeIf { it.isHtml }
            ?.let {
                resource = htmlInjector(resource, it)
            }

        val headers = mutableMapOf(
            "Accept-Ranges" to "bytes"
        )

        if (range == null) {
            return WebResourceResponse(
                mediaType?.toString(),
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
                mediaType?.toString(),
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
                        .open(errorPage.toString())
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
