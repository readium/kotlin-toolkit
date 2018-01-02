package org.readium.r2.streamer.Server.handler

import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper

import org.readium.r2.streamer.Fetcher.Fetcher

import java.io.IOException

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.router.RouterNanoHTTPD

class ManifestHandler : RouterNanoHTTPD.DefaultHandler() {

    override fun getMimeType(): String {
        return "application/webpub+json"
    }

    override fun getText(): String {
        return ResponseStatus.FAILURE_RESPONSE
    }

    override fun getStatus(): NanoHTTPD.Response.IStatus {
        return NanoHTTPD.Response.Status.OK
    }

    override fun get(uriResource: RouterNanoHTTPD.UriResource?, urlParams: Map<String, String>?, session: NanoHTTPD.IHTTPSession?): NanoHTTPD.Response {
        try {

            val fetcher = uriResource!!.initParameter(Fetcher::class.java)

            val objectMapper = ObjectMapper()
            val json = objectMapper.writeValueAsString(fetcher.publication)

            return NanoHTTPD.newFixedLengthResponse(status, mimeType, json)

        } catch (e: JsonGenerationException) {
            println(TAG + " JsonGenerationException | JsonMappingException " + e.toString())
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
        } catch (e: JsonMappingException) {
            println(TAG + " JsonGenerationException | JsonMappingException " + e.toString())
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
        } catch (e: IOException) {
            println(TAG + " IOException " + e.toString())
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
        }

    }

    companion object {
        private val TAG = "ManifestHandler"
    }
}