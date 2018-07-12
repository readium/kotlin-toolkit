/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.Server.handler

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.IStatus
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import org.nanohttpd.router.RouterNanoHTTPD

import org.readium.r2.shared.Link
import org.readium.r2.shared.MediaOverlayNode
import org.readium.r2.shared.MediaOverlays
import org.readium.r2.streamer.Fetcher.Fetcher



class MediaOverlayHandler : RouterNanoHTTPD.DefaultHandler() {

    override fun getText(): String {
        return ResponseStatus.FAILURE_RESPONSE
    }

    override fun getMimeType(): String {
        return "application/webpub+json"
    }

    override fun getStatus(): IStatus {
        return Status.OK
    }

    override fun get(uriResource: RouterNanoHTTPD.UriResource?, urlParams: Map<String, String>?, session: IHTTPSession?): Response {
        val fetcher = uriResource!!.initParameter(Fetcher::class.java)

        if (session!!.parameters.containsKey("resource")) {
            val searchQueryPath = session.parameters["resource"]!!.get(0)
            val spines = fetcher.publication.resources
            val objectMapper = ObjectMapper()
            try {
                val json = objectMapper.writeValueAsString(getMediaOverlay(spines, searchQueryPath))
                return newFixedLengthResponse(status, mimeType, json)
            } catch (e: JsonProcessingException) {
                return newFixedLengthResponse(status, mimeType, ResponseStatus.FAILURE_RESPONSE)
            }

        } else {
            return newFixedLengthResponse(status, mimeType, ResponseStatus.FAILURE_RESPONSE)
        }
    }

    private fun getMediaOverlay(spines: List<Link>, searchQueryPath: String): MediaOverlays? {
        for (link in spines) {
            if (link.href!!.contains(searchQueryPath)) {
                return link.mediaOverlays
            }
        }
        return MediaOverlays()
    }

    companion object {
        val TAG = MediaOverlayNode::class.java.simpleName
    }
}
