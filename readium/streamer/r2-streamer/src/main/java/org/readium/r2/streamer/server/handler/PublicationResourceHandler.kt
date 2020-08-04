/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server.handler

import kotlinx.coroutines.runBlocking
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD.MIME_PLAINTEXT
import org.nanohttpd.protocols.http.response.IStatus
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newChunkedResponse
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import org.nanohttpd.router.RouterNanoHTTPD
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceInputStream
import org.readium.r2.shared.format.MediaType
import org.readium.r2.streamer.BuildConfig.DEBUG
import org.readium.r2.streamer.server.ServingFetcher
import timber.log.Timber
import java.io.IOException
import java.io.InputStream


class PublicationResourceHandler : RouterNanoHTTPD.DefaultHandler() {

    override fun getMimeType(): String? {
        return null
    }

    override fun getText(): String {
        return ResponseStatus.FAILURE_RESPONSE
    }

    override fun getStatus(): IStatus {
        return Status.OK
    }

    override fun get(uriResource: RouterNanoHTTPD.UriResource, urlParams: Map<String, String>,
                     session: IHTTPSession): Response = runBlocking {

        if (DEBUG) Timber.v("Method: ${session.method}, Uri: ${session.uri}")
        val fetcher = uriResource.initParameter(ServingFetcher::class.java)

        val href = getHref(session)
        val resource = fetcher.get(href)

        try {
            serveResponse(session, resource)
        } catch(e: Resource.Error) {
            Timber.e(e)
            responseFromFailure(e)
                .also { resource.close() }
        } catch (e: Exception) {
            if (DEBUG) Timber.e(e)
            newFixedLengthResponse(Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
                .also { resource.close() }
        }
    }

    private suspend fun serveResponse(session: IHTTPSession, resource: Resource): Response {
        // Depending on the nature of the Response, either resource is closed here,
        // or the responsibility of closing it is forwarded to the created ResourceInputStream.
        // In the latter case, NanoHTTPd will close it at the end of the transmission.

        var rangeRequest: String? = session.headers["range"]
        val mimeType = (resource.link().mediaType ?: MediaType.BINARY).toString()

        // Calculate etag
        val etag = Integer.toHexString(resource.hashCode()) //FIXME: Is this working?

        // Support skipping:
        var startFrom: Long = 0
        var endAt: Long = -1
        if (rangeRequest != null) {
            if (rangeRequest.startsWith("bytes=")) {
                rangeRequest = rangeRequest.substring("bytes=".length)
                val minus = rangeRequest.indexOf('-')
                try {
                    if (minus > 0) {
                        startFrom = java.lang.Long.parseLong(rangeRequest.substring(0, minus))
                        endAt = java.lang.Long.parseLong(rangeRequest.substring(minus + 1))
                    }
                } catch (ignored: NumberFormatException) {
                }

            }
        }

        // Change return code and add Content-Range header when skipping is requested
        return if (rangeRequest != null && startFrom >= 0) {
            val dataLength = resource.length().getOrThrow()
            if (endAt < 0) {
                endAt = dataLength - 1
            }

            if (startFrom >= dataLength) {
                createResponse(Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "")
                    .apply {
                        addHeader("Content-Range", "bytes 0-0/$dataLength")
                        addHeader("ETag", etag)
                    }.also {
                       resource.close()
                    }

            } else {
                createResponse(Status.PARTIAL_CONTENT, mimeType, ResourceInputStream(resource, startFrom..endAt))
                    .apply {
                        addHeader("Content-Range", "bytes 0-0/$dataLength")
                        addHeader("ETag", etag)
                    }
            }
        } else {
            if (etag == session.headers["if-none-match"])
                createResponse(Status.NOT_MODIFIED, mimeType, "")
                    .also {
                        resource.close()
                    }
            else {
                createResponse(Status.OK, mimeType, ResourceInputStream(resource))
                    .apply {
                        addHeader("ETag", etag)
                    }
            }
        }
    }

    private fun createResponse(status: Status, mimeType: String, data: InputStream): Response {
        val response = newChunkedResponse(status, mimeType, data)
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }

    private fun createResponse(status: Status, mimeType: String, message: String): Response {
        val response = newFixedLengthResponse(status, mimeType, message)
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }

    private fun getHref(session: IHTTPSession): String {
        val path = session.uri
        val offset = path.indexOf("/", 0)
        val startIndex = path.indexOf("/", offset + 1)
        val filePath = path.substring(startIndex)

        return if (session.queryParameterString.isNullOrBlank())
            filePath
        else
            "${filePath}?${session.queryParameterString}"
    }

    private fun responseFromFailure(error: Resource.Error): Response {
        val status = when(error) {
            is Resource.Error.NotFound -> Status.NOT_FOUND
            is Resource.Error.Forbidden -> Status.FORBIDDEN
            is Resource.Error.Unavailable -> Status.SERVICE_UNAVAILABLE
            is Resource.Error.Other -> Status.INTERNAL_ERROR
            is Resource.Error.BadRequest -> Status.BAD_REQUEST
        }
        return newFixedLengthResponse(status, mimeType, ResponseStatus.FAILURE_RESPONSE)
    }

}