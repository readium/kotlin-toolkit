/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.http

import kotlin.test.Test
import kotlin.test.assertEquals
import org.readium.r2.shared.util.AbsoluteUrl

class HttpRequestTest {

    private val url = AbsoluteUrl("https://example.com/api")!!

    @Test
    fun appendQueryParameterChangesTheUrl() {
        val request = HttpRequest(url) {
            appendQueryParameter("query", "book title")
            appendQueryParameter("nullable", null)
            appendQueryParameter("page", "2")
        }
        assertEquals(
            "https://example.com/api?query=book%20title&page=2",
            request.url.toString()
        )
    }

    @Test
    fun buildWithoutQueryParametersKeepsTheUrlUntouched() {
        val request = HttpRequest(url) {}
        assertEquals(url, request.url)
    }

    @Test
    fun setRangeProducesARangeHeader() {
        assertEquals(
            listOf("bytes=10-"),
            HttpRequest(url) { setRange(10L..-1L) }.headers["Range"]
        )
        assertEquals(
            listOf("bytes=10-20"),
            HttpRequest(url) { setRange(10L..20L) }.headers["Range"]
        )
        assertEquals(
            listOf("bytes=0-0"),
            HttpRequest(url) { setRange(0L..0L) }.headers["Range"]
        )
    }

    @Test
    fun setPostFormEncodesTheBody() {
        val request = HttpRequest(url) {
            setPostForm(
                mapOf(
                    "user name" to "café&co",
                    "empty" to null
                )
            )
        }

        assertEquals(HttpRequest.Method.POST, request.method)
        assertEquals(
            listOf("application/x-www-form-urlencoded"),
            request.headers["Content-Type"]
        )
        assertEquals(
            "user name=caf%C3%A9%26co&empty=",
            (request.body as HttpRequest.Body.Bytes).bytes.decodeToString()
        )
    }

    @Test
    fun buildUponPreservesEverything() {
        val request = HttpRequest(
            url = url,
            method = HttpRequest.Method.PUT,
            headers = mapOf("X-Test" to listOf("1", "2")),
            extras = mapOf("key" to "value"),
            allowUserInteraction = true
        )

        val copy = request.copy {}

        assertEquals(request.url, copy.url)
        assertEquals(request.method, copy.method)
        assertEquals(request.headers, copy.headers)
        assertEquals(request.extras, copy.extras)
        assertEquals(request.allowUserInteraction, copy.allowUserInteraction)
    }
}
