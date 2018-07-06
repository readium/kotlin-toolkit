package org.readium.r2.streamer.Server.handler


import android.webkit.MimeTypeMap
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.IStatus
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import org.nanohttpd.router.RouterNanoHTTPD
import org.readium.r2.streamer.Server.Fonts
import java.io.InputStream


class FontHandler : RouterNanoHTTPD.DefaultHandler() {

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

    override fun get(uriResource: RouterNanoHTTPD.UriResource?, urlParams: Map<String, String>?, session: IHTTPSession?): Response {

        val method = session!!.method
        var uri = session.uri

        println("$TAG Method: $method, Url: $uri")

        try {
            val lastSlashIndex = uri.lastIndexOf('/')
            uri = uri.substring(lastSlashIndex + 1, uri.length)
            val resources = uriResource!!.initParameter(Fonts::class.java)
            val x = createResponse(Status.OK, getMimeType(uri), resources.get(uri).inputStream())
            return x
        } catch (e: Exception) {
            println(TAG + " Exception " + e.toString())
            return newFixedLengthResponse(Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
        }
    }

    fun getMimeType(url: String): String {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        else {
            when (extension) {
                ".otf" -> return "application/vnd.ms-opentype"
                ".ttf" -> return "application/vnd.ms-truetype"
                // TODO handle other font types
            }
        }
        return "application/vnd.ms-opentype"
    }

    private fun createResponse(status: Status, mimeType: String, message: InputStream): Response {
        val response = Response.newChunkedResponse(status, mimeType, message)
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }

    companion object {
        private val TAG = "FontHandler"
    }
}
