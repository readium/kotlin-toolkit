package org.readium.r2.streamer.Server.handler

import android.util.Log
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


class ResourceHandler : DefaultHandler() {

    private val fonts = arrayOf(".woff", ".ttf", ".obf", ".woff2", ".eot", ".otf")

    override fun getMimeType(): String? {
        return null
    }

    override fun getText(): String {
        return ResponseStatus.FAILURE_RESPONSE
    }

    override fun getStatus(): IStatus {
        return Status.OK
    }

    override fun get(uriResource: UriResource?, urlParams: Map<String, String>?, session: IHTTPSession?): Response {
        val method = session!!.method
        val uri = session.uri

        println("$TAG Method: $method, Url: $uri")

        try {
            val fetcher = uriResource!!.initParameter(Fetcher::class.java)
            val offset = uri.indexOf("/", 0)
            val startIndex = uri.indexOf("/", offset + 1)
            val filePath = uri.substring(startIndex + 1)
            val link = fetcher.publication.linkWithHref("/" + filePath)!!
            val mimeType = link.typeLink!!

            // If the content is of type html return the response this is done to
            // skip the check for following font deobfuscation check
            return if (mimeType == "application/xhtml+xml") {
                serveResponse(session, fetcher.dataStream(filePath), mimeType)
            } else serveResponse(session, fetcher.dataStream(filePath), mimeType)

            // ********************
            //  FONT DEOBFUSCATION
            // ********************


            
        } catch (e: Exception) {
            println(TAG + " Exception " + e.toString())
            Log.e(TAG, e.toString())
            return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
        }

    }

    private fun serveResponse(session: IHTTPSession, inputStream: InputStream, mimeType: String): Response {
        var response: Response?
        var rangeRequest: String? = session.headers["range"]

        try {
            // Calculate etag
            val etag = Integer.toHexString(inputStream.hashCode())

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
            val streamLength = inputStream.available().toLong()
            if (rangeRequest != null && startFrom >= 0) {
                if (startFrom >= streamLength) {
                    response = createResponse(Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "")
                    response.addHeader("Content-Range", "bytes 0-0/" + streamLength)
                    response.addHeader("ETag", etag)
                } else {
                    if (endAt < 0) {
                        endAt = streamLength - 1
                    }
                    var newLen = endAt - startFrom + 1
                    if (newLen < 0) {
                        newLen = 0
                    }

                    val dataLen = newLen
                    inputStream.skip(startFrom)

                    response = createResponse(Status.PARTIAL_CONTENT, mimeType, inputStream)
                    response.addHeader("Content-Length", "" + dataLen)
                    response.addHeader("Content-Range", "bytes $startFrom-$endAt/$streamLength")
                    response.addHeader("ETag", etag)
                }
            } else {
                if (etag == session.headers["if-none-match"])
                    response = createResponse(Status.NOT_MODIFIED, mimeType, "")
                else {
                    response = createResponse(Status.OK, mimeType, inputStream)
                    response.addHeader("Content-Length", "" + streamLength)
                    response.addHeader("ETag", etag)
                }
            }
        } catch (ioe: IOException) {
            response = getResponse("Forbidden: Reading file failed")
        } catch (ioe: NullPointerException) {
            response = getResponse("Forbidden: Reading file failed")
        }

        return if (response == null) getResponse("Error 404: File not found") else response
    }

    private fun createResponse(status: Status, mimeType: String, message: InputStream): Response {
        val response = NanoHTTPD.newChunkedResponse(status, mimeType, message)
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }

    private fun createResponse(status: Status, mimeType: String, message: String): Response {
        val response = NanoHTTPD.newFixedLengthResponse(status, mimeType, message)
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }

    private fun getResponse(message: String): Response {
        return createResponse(Status.OK, "text/plain", message)
    }

    private fun isFontFile(file: String): Boolean {
        for (font in fonts) {
            if (file.endsWith(font)) {
                return true
            }
        }
        return false
    }

    companion object {
        private val TAG = "ResourceHandler"
    }
}