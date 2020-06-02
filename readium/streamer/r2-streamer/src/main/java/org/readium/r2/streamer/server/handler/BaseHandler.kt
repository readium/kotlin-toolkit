/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server.handler

import android.net.Uri
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.IStatus
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Status
import org.nanohttpd.router.RouterNanoHTTPD
import org.readium.r2.shared.format.MediaType
import org.readium.r2.streamer.BuildConfig
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.InputStream

internal abstract class BaseHandler : RouterNanoHTTPD.DefaultHandler() {

    override fun getMimeType(): String? = null
    override fun getText(): String = ""
    override fun getStatus(): IStatus = Status.OK

    override fun get(uriResource: RouterNanoHTTPD.UriResource?, urlParams: Map<String, String>?, session: IHTTPSession?): Response {
        uriResource ?: return notFoundResponse
        session ?: return notFoundResponse

        if (BuildConfig.DEBUG) Timber.v("Method: ${session.method}, URL: ${session.uri}")

        return try {
            val uri = Uri.parse(session.uri)
            handle(resource = uriResource, uri = uri, parameters = urlParams)

        } catch (e: FileNotFoundException) {
            if (BuildConfig.DEBUG) Timber.e( "Server handler error: %s", e.toString())
            notFoundResponse

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Timber.e( "Server handler error: %s", e.toString())
            createErrorResponse(Status.INTERNAL_ERROR)
        }
    }

    abstract fun handle(resource: RouterNanoHTTPD.UriResource, uri: Uri, parameters: Map<String, String>?): Response

    fun createResponse(mediaType: MediaType, body: String): Response =
        createResponse(mediaType, body.toByteArray())

    fun createResponse(mediaType: MediaType, body: ByteArray): Response =
        Response.newFixedLengthResponse(Status.OK, mediaType.toString(), body).apply {
            addHeader("Accept-Ranges", "bytes")
        }

    fun createResponse(mediaType: MediaType, body: InputStream): Response =
        Response.newChunkedResponse(Status.OK, mediaType.toString(), body).apply {
            addHeader("Accept-Ranges", "bytes")
        }

    fun createErrorResponse(status: Status) =
        Response.newFixedLengthResponse(status, "text/html", "")

    val notFoundResponse: Response get() = createErrorResponse(Status.NOT_FOUND)

}
