/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.navigator.epub

import android.app.Application
import android.os.PatternMatcher
import android.webkit.MimeTypeMap
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
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.StringResource
import org.readium.r2.shared.util.resource.fallback
import org.readium.r2.shared.util.toUrl

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
        const val READIUM_PACKAGE_HOSTNAME = "readium_package"
        const val ASSETS_HOSTNAME = "readium_assets"

        val publicationBaseHref = AbsoluteUrl("https://$READIUM_PACKAGE_HOSTNAME/")!!
        val assetsBaseHref = AbsoluteUrl("https://$ASSETS_HOSTNAME/")!!

        fun assetUrl(path: String): Url? =
            Url.fromDecodedPath(path)?.let { assetsBaseHref.resolve(it) }
    }

    /**
     * Serves the requests of the navigator web views.
     *
     * https://readium_package/ serves the publication resources through its fetcher.
     * https://readium_assets/ serves the application assets.
     */
    fun shouldInterceptRequest(request: WebResourceRequest, css: ReadiumCss): WebResourceResponse? {
        val path = request.url.path ?: return null
        val hostname = request.url.host ?: return null
        val requestUrl = request.url.toUrl() ?: return null

        return when (hostname) {
            READIUM_PACKAGE_HOSTNAME -> {
                // Request is for a packaged resource.
                val href = Url.fromDecodedPath(path.removePrefix("/"))
                    ?: return null

                servePublicationResource(
                    href = href,
                    range = HttpHeaders(request.requestHeaders).range,
                    css = css
                )
            }

            ASSETS_HOSTNAME if isServedAsset(path.removePrefix("/")) -> {
                // Request is for a known asset.
                assetsLoader.shouldInterceptRequest(request.url)
            }

            ASSETS_HOSTNAME -> return null // Request is for an unknown asset.

            else -> {
                // Request is for streaming a resource, if the baseUrl an AbsoluteUrl and hostname
                // is not ASSETS_HOSTNAME or READIUM_PACKAGE_HOSTNAME
                val baseUrl = publication.baseUrl as? AbsoluteUrl ?: return null

                // Look up the link in the publication to make sure we have the right resource.
                val link = publicationLinkFromHref(baseUrl.resolve(requestUrl))
                    ?: publicationLinkFromHref(baseUrl.relativize(requestUrl))
                    ?: return null // Link not found in publication, we can't serve this resource.

                servePublicationResource(
                    href = link.href.resolve(),
                    range = HttpHeaders(request.requestHeaders).range,
                    css = css
                )
            }
        }
    }

    /**
     * Returns a new [Resource] to serve the given [href] in the publication.
     *
     * If the [Resource] is an HTML document, injects the required JavaScript and CSS files.
     */
    private fun servePublicationResource(
        href: Url,
        range: HttpRange?,
        css: ReadiumCss
    ): WebResourceResponse {
        val link = publicationLinkFromHref(href)
        // Link not found, create a Link from the href and guess the MediaType
            ?: Link(href = href, mediaType = mediaTypeFromUrl(href))

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

        val stream = resource.asInputStream()
        if (range == null) {
            return WebResourceResponse(
                link.mediaType?.toString(),
                null,
                200,
                "OK",
                headers,
                stream
            )
        } else { // Byte range request
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

    /**
     * Resolve the [MediaType] from a [Url].
     */
    private fun mediaTypeFromUrl(href: Url): MediaType? {
        val ext = MimeTypeMap.getFileExtensionFromUrl(href.normalize().toString()) ?: return null
        val mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: return null

        return MediaType.invoke(mimetype)
    }

    /**
     * Get link in publication from a [Url] and replace href to preserve request query parameters.
     */
    private fun publicationLinkFromHref(href: Url): Link? {
        return publication.linkWithHref(href)
            // Query parameters must be kept as they might be relevant for the fetcher.
            ?.copy(href = Href(href))
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
            .setDomain(assetsBaseHref.host!!)
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(application))
            .build()
}
