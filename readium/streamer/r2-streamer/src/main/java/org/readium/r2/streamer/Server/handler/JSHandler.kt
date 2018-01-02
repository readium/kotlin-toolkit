package org.readium.r2.streamer.Server.handler

import fi.iki.elonen.router.RouterNanoHTTPD
import org.readium.r2.shared.Link
import org.readium.r2.streamer.Fetcher.Fetcher
import java.io.IOException
import java.io.InputStream
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.Response.IStatus
import fi.iki.elonen.NanoHTTPD.Response.Status
import fi.iki.elonen.router.RouterNanoHTTPD.DefaultHandler
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource

import fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT
import org.readium.r2.streamer.Server.Ressources


class JSHandler : RouterNanoHTTPD.DefaultHandler() {


    override fun getMimeType(): String? {
        return null
    }

    override fun getText(): String {
        return ResponseStatus.FAILURE_RESPONSE
    }

    override fun getStatus(): NanoHTTPD.Response.IStatus {
        return NanoHTTPD.Response.Status.OK
    }

    override fun get(uriResource: RouterNanoHTTPD.UriResource?, urlParams: Map<String, String>?, session: NanoHTTPD.IHTTPSession?): NanoHTTPD.Response {

        val method = session!!.method
        var uri = session.uri

        println("$TAG Method: $method, Url: $uri")

        try {
            val lastSlashIndex = uri.lastIndexOf('/')
            uri = uri.substring(lastSlashIndex + 1, uri.length)
            val resources = uriResource!!.initParameter(Ressources::class.java)
            val x = createResponse(NanoHTTPD.Response.Status.OK, "text/javascript", resources.get(uri))
            return x
        } catch (e: Exception) {
            println(TAG + " Exception " + e.toString())
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
        }

    }

    private fun createResponse(status: NanoHTTPD.Response.Status, mimeType: String, message: String): NanoHTTPD.Response {
        val response = NanoHTTPD.newFixedLengthResponse(status, mimeType, message)
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }

    companion object {
        private val TAG = "ResourceHandler"
    }
}