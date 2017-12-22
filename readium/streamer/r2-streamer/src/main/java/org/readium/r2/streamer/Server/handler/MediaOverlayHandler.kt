package org.readium.r2.streamer.Server.handler

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper

import org.readium.r2.shared.Link
import org.readium.r2.shared.MediaOverlayNode
import org.readium.r2.shared.MediaOverlays
import org.readium.r2.streamer.Fetcher.Fetcher

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.router.RouterNanoHTTPD


class MediaOverlayHandler : RouterNanoHTTPD.DefaultHandler() {

    override fun getText(): String {
        return ResponseStatus.FAILURE_RESPONSE
    }

    override fun getMimeType(): String {
        return "application/webpub+json"
    }

    override fun getStatus(): NanoHTTPD.Response.IStatus {
        return NanoHTTPD.Response.Status.OK
    }

    override fun get(uriResource: RouterNanoHTTPD.UriResource?, urlParams: Map<String, String>?, session: NanoHTTPD.IHTTPSession?): NanoHTTPD.Response {
        val fetcher = uriResource!!.initParameter(Fetcher::class.java)

        if (session!!.parameters.containsKey("resource")) {
            val searchQueryPath = session.parameters["resource"]!!.get(0)
            val spines = fetcher.publication.resources
            val objectMapper = ObjectMapper()
            try {
                val json = objectMapper.writeValueAsString(getMediaOverlay(spines, searchQueryPath))
                return NanoHTTPD.newFixedLengthResponse(status, mimeType, json)
            } catch (e: JsonProcessingException) {
                return NanoHTTPD.newFixedLengthResponse(status, mimeType, ResponseStatus.FAILURE_RESPONSE)
            }

        } else {
            return NanoHTTPD.newFixedLengthResponse(status, mimeType, ResponseStatus.FAILURE_RESPONSE)
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
