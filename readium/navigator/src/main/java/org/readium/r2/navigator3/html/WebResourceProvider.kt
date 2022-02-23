package org.readium.r2.navigator3.html

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.extensions.splitAt
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceInputStream
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber
import java.io.File

internal class WebResourceProvider(
    private val publication: Publication,
    private val htmlInjector: HtmlInjector
) {
    private val fallbackMediaType = MediaType.BINARY

    fun processRequest(context: Context, request: WebResourceRequest): WebResourceResponse? {
        Timber.d("Request ${request.url}")

        if (request.url.scheme != "http") {// || request.url.authority != "127.0.0.1") {
            return null
        }

        val path = request.url.path
            ?.takeIf { it.length > 2 }
            ?: return null

        val (location, resource) = path.substring(1).splitAt("/")
        if (resource == null) {
            return null
        }

        val response: WebResourceResponse? = when (location) {
            "publication" -> serveResource(resource)
            "assets" -> serveAsset(context, resource)
            else -> null
        }

        Timber.d("Response for $resource ${response?.mimeType}")

        return response
    }

    private fun serveResource(path: String): WebResourceResponse? {
        Timber.d("serveResource $path")
        val href = "/${path}"
        val link = publication.linkWithHref(href)
            ?: return null
        val resource = transformResource(publication.get(link))
        return WebResourceResponse(link.mediaType.toString(), null, ResourceInputStream(resource))
    }

    private fun transformResource(resource: Resource): Resource {
        return if (publication.type == Publication.TYPE.EPUB) {
            htmlInjector.transform(resource)
        } else {
            resource
        }
    }

    private fun serveAsset(context: Context, path: String): WebResourceResponse? {
        Timber.d("serveAsset $path")
        val stream = tryOrNull { context.assets.open("readium/$path") }
            ?: return null
        val mediaType = runBlocking { MediaType.of(fileExtension = File(path).extension) }
            ?: fallbackMediaType
        val mime = "${mediaType.type}/${mediaType.subtype}"
        return WebResourceResponse(mime, null, stream)
    }
}
