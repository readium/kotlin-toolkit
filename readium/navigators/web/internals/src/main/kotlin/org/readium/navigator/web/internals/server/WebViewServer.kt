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
import org.readium.r2.shared.util.toAbsoluteUrl

/**
 * Serves the publication resources and application assets in the EPUB navigator web views.
 */
public class WebViewServer(
    private val application: Application,
    private val container: Container<Resource>,
    private val mediaTypes: Map<Url, MediaType>,
    private val baseUrl: AbsoluteUrl? = null,
    private val errorPage: RelativeUrl,
    private val htmlInjector: (Resource, MediaType) -> Resource,
    servedAssets: List<String>,
    private val onResourceLoadFailed: (Url, ReadError) -> Unit,
) {
    public companion object {
        public const val PACKAGE_HOSTNAME: String = "readium_package"
        public const val ASSETS_HOSTNAME: String = "readium_assets"

        public val packageBaseHref: AbsoluteUrl = AbsoluteUrl("https://$PACKAGE_HOSTNAME/")!!
        public val assetsBaseHref: AbsoluteUrl = AbsoluteUrl("https://$ASSETS_HOSTNAME/")!!

        public fun assetUrl(path: String): AbsoluteUrl? =
            Url.fromDecodedPath(path)?.let { assetsBaseHref.resolve(it) }
    }

    /**
     * Gets the url the given [href] is being served at.
     */
    public fun hrefToServedUrl(href: Url): AbsoluteUrl =
        when (href) {
            is AbsoluteUrl ->
                href
            is RelativeUrl ->
                (baseUrl ?: packageBaseHref).resolve(href)
        }

    /**
     * Gets the resource of the resource targeted by [url].
     */
    public fun servedUrlToHref(url: AbsoluteUrl): Url? {
        val href = when (url.host) {
            PACKAGE_HOSTNAME -> {
                urlFromContainer(packageBaseHref.relativize(url))
            }

            else -> {
                urlFromContainer(url)
                    ?: baseUrl?.relativize(url)?.let { relativeUrl ->
                        urlFromContainer(relativeUrl)
                    }
            }
        } ?: return null

        val hrefWithFragment = url.fragment?.let { href.addFragment(it) } ?: href

        // Fragment must be kept as it might be relevant to the caller.
        // For the rest of the url, we return precisely the version in the manifest.
        return hrefWithFragment
    }

    /**
     * Serves the requests of the navigator web views.
     *
     * https://readium_package/ serves the packaged resources.
     * https://readium_assets/ serves the application assets.
     */
    public fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
        val path = request.url.path ?: return null
        val hostname = request.url.host ?: return null
        val requestUrl = request.url.toAbsoluteUrl() ?: return null
        val range = HttpHeaders(request.requestHeaders).range

        return when (hostname) {
            ASSETS_HOSTNAME -> {
                if (isServedAsset(path.removePrefix("/"))) {
                    // Request is for a known asset.
                    assetsLoader.shouldInterceptRequest(request.url)
                        ?.apply { allowCors() }
                } else {
                    val error = ReadError.Decoding(
                        "Attempted to load an unknown asset from $requestUrl"
                    )
                    onResourceLoadFailed(requestUrl, error)
                    serveErrorResponse() // Request is for an unknown asset.
                }
            }

            else -> { // Request is for a publication resource
                servePublicationResourceWithHref(
                    // Drop anchor because it is meant to be interpreted by the client.
                    href = servedUrlToHref(requestUrl) ?: requestUrl.removeFragment(),
                    range = range,
                )
            }
        }
    }

    /**
     * Returns a new [Resource] to serve the given [href] in the publication.
     *
     * If the [Resource] is an HTML document, injects the required JavaScript and CSS files.
     */
    private fun servePublicationResourceWithHref(
        href: Url,
        range: HttpRange?,
    ): WebResourceResponse {
        val mediaType = mediaTypes[href]

        return servePublicationResourceWithHref(
            href = href,
            mediaType = mediaType,
            range = range,
        )
    }

    private fun urlFromContainer(url: Url): Url? {
        return container.entries.firstOrNull { entry ->
            entry.isEquivalent(
                url.removeFragment()
            )
        } ?: container.entries.firstOrNull { entry ->
            entry.isEquivalent(
                url.removeFragment().removeQuery()
            )
        }
    }

    private fun servePublicationResourceWithHref(
        href: Url,
        mediaType: MediaType?,
        range: HttpRange?,
    ): WebResourceResponse {
        var resource = container[href]
            ?.fallback {
                onResourceLoadFailed(href, it)
                errorResource()
            } ?: run {
            val error = ReadError.Decoding(
                "Resource not found at $href in publication."
            )
            onResourceLoadFailed(href, error)
            return serveErrorResponse()
        }

        mediaType
            ?.takeIf { it.isHtml }
            ?.let {
                resource = htmlInjector(resource, it)
            }

        return serveResource(resource, range, mediaType)
    }

    private fun serveResource(
        resource: Resource,
        range: HttpRange?,
        mediaType: MediaType?,
    ): WebResourceResponse {
        val headers = mutableMapOf(
            "Accept-Ranges" to "bytes"
        )

        val stream = resource.asInputStream()
        if (range == null) {
            return WebResourceResponse(
                mediaType?.toString(),
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
                mediaType?.toString(),
                null,
                206,
                "Partial Content",
                headers,
                stream
            )
        }
    }

    /**
     * Allow the response to be consumed by publication documents served
     * from any origin, including the package domain.
     */
    private fun WebResourceResponse.allowCors() {
        responseHeaders = responseHeaders ?: mutableMapOf()
        responseHeaders["Access-Control-Allow-Origin"] = "*"
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

    private fun serveErrorResponse(): WebResourceResponse {
        return serveResource(errorResource(), null, MediaType.XHTML)
    }

    private fun isServedAsset(path: String): Boolean =
        servedAssetPatterns.any { it.match(path) }

    private val servedAssetPatterns: List<PatternMatcher> =
        servedAssets.map { PatternMatcher(it, PatternMatcher.PATTERN_SIMPLE_GLOB) }

    private val assetsLoader =
        WebViewAssetLoader.Builder()
            .setDomain(ASSETS_HOSTNAME)
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(application))
            .build()
}
