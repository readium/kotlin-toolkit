/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server.handler

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.IStatus
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import org.nanohttpd.router.RouterNanoHTTPD
import org.readium.r2.shared.MediaOverlays
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.publication.Link
import org.readium.r2.streamer.server.ServingFetcher


class MediaOverlayHandler : RouterNanoHTTPD.DefaultHandler() {

    override fun getText(): String {
        return ResponseStatus.FAILURE_RESPONSE
    }

    override fun getMimeType(): String {
        return MediaType.READIUM_WEBPUB_MANIFEST.toString()
    }

    override fun getStatus(): IStatus {
        return Status.OK
    }

    override fun get(uriResource: RouterNanoHTTPD.UriResource?, urlParams: Map<String, String>?, session: IHTTPSession?): Response {
        val fetcher = uriResource!!.initParameter(ServingFetcher::class.java)

        return if (session!!.parameters.containsKey("resource")) {
            val searchQueryPath = session.parameters["resource"]!![0]
            val spines = fetcher.publication.resources
            val objectMapper = ObjectMapper()
            return try {
                val json = objectMapper.writeValueAsString(getMediaOverlay(spines, searchQueryPath))
                newFixedLengthResponse(status, mimeType, json)
            } catch (e: JsonProcessingException) {
                newFixedLengthResponse(status, mimeType, ResponseStatus.FAILURE_RESPONSE)
            }

        } else {
            newFixedLengthResponse(status, mimeType, ResponseStatus.FAILURE_RESPONSE)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getMediaOverlay(spines: List<Link>, searchQueryPath: String): MediaOverlays? {
        // FIXME: This is not supported until the model is properly specified
        return null
//        for (link in spines) {
//            if (link.href.contains(searchQueryPath)) {
//                return link.mediaOverlays
//            }
//        }
//        return MediaOverlays()
    }

}
