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
import org.readium.r2.shared.util.RelativeUrl
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
import org.readium.r2.shared.util.toAbsoluteUrl

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
        const val PACKAGE_HOSTNAME = "readium_package"
        const val ASSETS_HOSTNAME = "readium_assets"

        val packageBaseHref = AbsoluteUrl("https://$PACKAGE_HOSTNAME/")!!
        val assetsBaseHref = AbsoluteUrl("https://$ASSETS_HOSTNAME/")!!

        fun assetUrl(path: String): Url? =
            Url.fromDecodedPath(path)?.let { assetsBaseHref.resolve(it) }
    }

    /**
     * Gets the url the given [link] is being served at.
     */
    fun linkToServedUrl(link: Link): AbsoluteUrl =
        when (val url = link.url()) {
            is AbsoluteUrl ->
                url
            is RelativeUrl ->
                (publication.baseUrl ?: packageBaseHref).resolve(link.url())
        }

    /**
     * Gets a link to the resource targeted by [url].
     */
    fun servedUrlToLink(url: AbsoluteUrl): Link? {
        val link = when (url.host) {
            PACKAGE_HOSTNAME -> {
                val href = packageBaseHref.relativize(url)
                publication.linkWithHref(href)
            }
            else -> {
                publication.linkWithHref(url)
                    ?: publication.baseUrl?.relativize(url)
                        ?.let { relativeUrl ->
                            publication.linkWithHref(relativeUrl)
                        }
            }
        } ?: return null

        val hrefWithFragment = link.url().let { linkUrl ->
            url.fragment?.let { linkUrl.addFragment(it) } ?: linkUrl
        }

        // Fragment must be kept as it might be relevant to the caller.
        // For the rest of the url, we return precisely the version in the manifest.
        return link.copy(href = Href(hrefWithFragment))
    }

    /**
     * Serves the requests of the navigator web views.
     *
     * https://readium_package/ serves the publication resources through its container.
     * https://readium_assets/ serves the application assets.
     */
    fun shouldInterceptRequest(request: WebResourceRequest, css: ReadiumCss): WebResourceResponse? {
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
                servePublicationResourceWithUrl(
                    // Drop anchor because it is meant to be interpreted by the client.
                    url = requestUrl.removeFragment(),
                    range = range,
                    css = css
                )
            }
        }
    }

    /**
     * Returns a new [Resource] to serve the given [url] in the publication.
     *
     * If the [Resource] is an HTML document, injects the required JavaScript and CSS files.
     */
    private fun servePublicationResourceWithUrl(
        url: AbsoluteUrl,
        range: HttpRange?,
        css: ReadiumCss,
    ): WebResourceResponse {
        val link = servedUrlToLink(url)

        val mediaType = link?.mediaType
            ?: mediaTypeFromUrl(url)

        val href = link?.url() ?: url // Just in case some resource is not in the manifest

        return servePublicationResourceWithHref(
            href = href,
            mediaType = mediaType,
            range = range,
            css = css
        )
    }

    private fun servePublicationResourceWithHref(
        href: Url,
        mediaType: MediaType?,
        range: HttpRange?,
        css: ReadiumCss,
    ): WebResourceResponse {
        var resource = publication
            .get(href)
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

        // Only inject html when the profile is EPUB
        if (publication.conformsTo(Publication.Profile.EPUB)) {
            mediaType
                ?.takeIf { it.isHtml }
                ?.let {
                    resource = resource.injectHtml(
                        publication,
                        mediaType = it,
                        css,
                        assetsBaseHref,
                        disableSelectionWhenProtected
                    )
                }
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
     * Resolve the [MediaType] from an [Url].
     */
    private fun mediaTypeFromUrl(href: Url): MediaType? {
        val ext = MimeTypeMap.getFileExtensionFromUrl(href.normalize().toString()) ?: return null
        val mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: return null

        return MediaType.invoke(mimetype)
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

    /**
     * Allow the response to be consumed by publication documents served
     * from any origin, including the package domain.
     */
    private fun WebResourceResponse.allowCors() {
        responseHeaders = responseHeaders ?: mutableMapOf()
        responseHeaders["Access-Control-Allow-Origin"] = "*"
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
