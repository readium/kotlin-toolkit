/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server.handler

import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.IStatus
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import org.nanohttpd.router.RouterNanoHTTPD

import org.readium.r2.streamer.fetcher.Fetcher

import java.io.IOException


class ManifestHandler : RouterNanoHTTPD.DefaultHandler() {

    override fun getMimeType(): String {
        return "application/webpub+json"
    }

    override fun getText(): String {
        return ResponseStatus.FAILURE_RESPONSE
    }

    override fun getStatus(): IStatus {
        return Status.OK
    }

    override fun get(uriResource: RouterNanoHTTPD.UriResource?, urlParams: Map<String, String>?, session: IHTTPSession?): Response {
        try {

            val fetcher = uriResource!!.initParameter(Fetcher::class.java)

            val objectMapper = ObjectMapper()
            val json = objectMapper.writeValueAsString(fetcher.publication)

            return newFixedLengthResponse(status, mimeType, json)

        } catch (e: JsonGenerationException) {
            println(TAG + " JsonGenerationException | JsonMappingException " + e.toString())
            return newFixedLengthResponse(Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
        } catch (e: JsonMappingException) {
            println(TAG + " JsonGenerationException | JsonMappingException " + e.toString())
            return newFixedLengthResponse(Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
        } catch (e: IOException) {
            println(TAG + " IOException " + e.toString())
            return newFixedLengthResponse(Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
        }

    }

    companion object {
        private const val TAG = "ManifestHandler"
    }
}