/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server.handler


import android.webkit.MimeTypeMap
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.IStatus
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import org.nanohttpd.router.RouterNanoHTTPD
import org.readium.r2.shared.format.MediaType
import org.readium.r2.streamer.BuildConfig.DEBUG
import org.readium.r2.streamer.server.Fonts
import timber.log.Timber
import java.io.InputStream
import java.util.*


class FontHandler : RouterNanoHTTPD.DefaultHandler() {

    override fun getMimeType(): String? {
        return null
    }

    override fun getText(): String {
        return ResponseStatus.FAILURE_RESPONSE
    }

    override fun getStatus(): IStatus {
        return Status.OK
    }

    override fun get(uriResource: RouterNanoHTTPD.UriResource?, urlParams: Map<String, String>?, session: IHTTPSession?): Response {

        val method = session!!.method
        var uri = session.uri

        if (DEBUG) Timber.v("Method: $method, Url: $uri")

        return try {
            val lastSlashIndex = uri.lastIndexOf('/')
            uri = uri.substring(lastSlashIndex + 1, uri.length)
            val resources = uriResource!!.initParameter(Fonts::class.java)
            val x = createResponse(Status.OK, getMimeType(uri), resources.get(uri).inputStream())
            x
        } catch (e: Exception) {
            if (DEBUG) Timber.e( " Exception %s", e.toString())
            newFixedLengthResponse(Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
        }
    }

    private fun getMimeType(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        var mimeType = MediaType.OTF.toString()
        if (extension != null) {
            try {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)!!
            } catch (e: Exception) {
                when (extension.toLowerCase(Locale.ROOT)) {
                    "otf" -> mimeType = MediaType.OTF.toString()
                    "ttf" -> mimeType = MediaType.TTF.toString()
                // TODO handle other font types
                }
            }
        }
        return mimeType
    }

    private fun createResponse(status: Status, mimeType: String, message: InputStream): Response {
        val response = Response.newChunkedResponse(status, mimeType, message)
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }

}
