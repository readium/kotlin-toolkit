/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.http

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.network.UnresolvedAddressException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.logging.ReadiumLog
import org.readium.r2.shared.util.logging.ReadiumLogger

class DefaultHttpClientTest {

    private val url = AbsoluteUrl("http://example.com/resource")!!

    private var previousLogger: ReadiumLogger? = null

    @BeforeTest
    fun disableLogging() {
        // The Android Logcat logger is not available in host unit tests.
        previousLogger = ReadiumLog.logger
        ReadiumLog.logger = null
    }

    @AfterTest
    fun restoreLogging() {
        ReadiumLog.logger = previousLogger
    }

    @Test
    fun fetchReadsTheWholeBody() = runTest {
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                respond(
                    content = "hello world",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "text/plain")
                )
            }
        )

        val response = client.fetch(HttpRequest(url)).getOrElse { fail("Unexpected error: $it") }
        assertEquals("hello world", response.body.decodeToString())
        assertEquals(200, response.response.statusCode.code)
        assertEquals("text", response.response.mediaType?.type)
    }

    @Test
    fun streamSupportsForwardRangeReads() = runTest {
        val client = DefaultHttpClient(
            engine = MockEngine {
                respond(content = "0123456789", status = HttpStatusCode.OK)
            }
        )

        val response = client.stream(HttpRequest(url)).getOrElse { fail("$it") }
        val body = response.body

        assertEquals("012", body.read(0L..2L).getOrElse { fail("$it") }.decodeToString())
        // Forward skip.
        assertEquals("56", body.read(5L..6L).getOrElse { fail("$it") }.decodeToString())
        // Backward read is not supported.
        assertIs<ReadError.UnsupportedOperation>(
            body.read(0L..1L).failureOrNull() ?: fail("Expected a failure")
        )
        // Reading past the end returns the remaining bytes.
        assertEquals("789", body.read(7L..100L).getOrElse { fail("$it") }.decodeToString())

        body.close()
    }

    @Test
    fun errorResponseIncludesBodyAndMediaType() = runTest {
        val client = DefaultHttpClient(
            engine = MockEngine {
                respond(
                    content = """{"title": "Not found"}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf("Content-Type", "application/problem+json")
                )
            }
        )

        val error = client.fetch(HttpRequest(url)).failureOrNull()
        val errorResponse = assertIs<HttpError.ErrorResponse>(error)
        assertEquals(404, errorResponse.status.code)
        assertEquals("""{"title": "Not found"}""", errorResponse.body?.decodeToString())
        assertEquals("Not found", errorResponse.problemDetails?.title)
    }

    @Test
    fun failedHeadIsRetriedWithGetToFetchTheErrorBody() = runTest {
        val methods = mutableListOf<String>()
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                methods.add(request.method.value)
                respond(
                    content = if (request.method.value == "HEAD") "" else "error body",
                    status = HttpStatusCode.Unauthorized
                )
            }
        )

        val error = client.stream(
            HttpRequest(url) { method = HttpRequest.Method.HEAD }
        ).failureOrNull()

        val errorResponse = assertIs<HttpError.ErrorResponse>(error)
        assertEquals(401, errorResponse.status.code)
        assertEquals("error body", errorResponse.body?.decodeToString())
        assertEquals(listOf("HEAD", "GET"), methods)
    }

    @Test
    fun sameSchemeRedirectionIsFollowedAutomatically() = runTest {
        val requestedUrls = mutableListOf<String>()
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                requestedUrls.add(request.url.toString())
                if (request.url.encodedPath == "/resource") {
                    respond(
                        content = "",
                        status = HttpStatusCode.Found,
                        headers = headersOf(
                            "Location" to listOf("http://other.com/moved"),
                            "Set-Cookie" to listOf("session=1")
                        )
                    )
                } else {
                    respond(content = "moved content", status = HttpStatusCode.OK)
                }
            }
        )

        val response = client.fetch(HttpRequest(url)).getOrElse { fail("$it") }
        assertEquals("moved content", response.body.decodeToString())
        assertEquals(
            listOf("http://example.com/resource", "http://other.com/moved"),
            requestedUrls
        )
    }

    @Test
    fun redirectionPassesCookiesAlong() = runTest {
        var cookie: String? = null
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                if (request.url.encodedPath == "/resource") {
                    respond(
                        content = "",
                        status = HttpStatusCode.Found,
                        headers = headersOf(
                            "Location" to listOf("http://example.com/moved"),
                            "Set-Cookie" to listOf("session=1")
                        )
                    )
                } else {
                    cookie = request.headers["Cookie"]
                    respond(content = "ok", status = HttpStatusCode.OK)
                }
            }
        )

        client.fetch(HttpRequest(url)).getOrElse { fail("$it") }
        assertEquals("session=1", cookie)
    }

    @Test
    fun redirectionPreservesTheOriginalHeaders() = runTest {
        var headers: Pair<String?, String?>? = null
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                if (request.url.encodedPath == "/resource") {
                    respond(
                        content = "",
                        status = HttpStatusCode.Found,
                        headers = headersOf("Location", "http://example.com/moved")
                    )
                } else {
                    headers = request.headers["Range"] to request.headers["Authorization"]
                    respond(content = "", status = HttpStatusCode.PartialContent)
                }
            }
        )

        client.stream(
            HttpRequest(url) {
                setRange(100L..-1L)
                setHeader("Authorization", "Bearer token")
            }
        ).getOrElse { fail("$it") }.body.close()

        assertEquals("bytes=100-" to "Bearer token", headers)
    }

    @Test
    fun redirectionToAnotherHostDropsCredentialHeadersButKeepsTheOthers() = runTest {
        var headers: Triple<String?, String?, String?>? = null
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                if (request.url.host == "example.com") {
                    respond(
                        content = "",
                        status = HttpStatusCode.Found,
                        headers = headersOf(
                            "Location" to listOf("http://other.com/moved"),
                            "Set-Cookie" to listOf("session=1")
                        )
                    )
                } else {
                    headers = Triple(
                        request.headers["Range"],
                        request.headers["Authorization"],
                        request.headers["Cookie"]
                    )
                    respond(content = "", status = HttpStatusCode.PartialContent)
                }
            }
        )

        client.stream(
            HttpRequest(url) {
                setRange(100L..-1L)
                setHeader("Authorization", "Bearer token")
                setHeader("Cookie", "existing=1")
            }
        ).getOrElse { fail("$it") }.body.close()

        // Range survives; Authorization and Cookie are dropped, and the Set-Cookie of the
        // redirecting response is not forwarded to a different host.
        assertEquals(Triple("bytes=100-", null, null), headers)
    }

    @Test
    fun redirectionJoinsCookiePairsInASingleHeader() = runTest {
        var cookie: String? = null
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                if (request.url.encodedPath == "/resource") {
                    respond(
                        content = "",
                        status = HttpStatusCode.Found,
                        headers = headersOf(
                            "Location" to listOf("http://example.com/moved"),
                            "Set-Cookie" to listOf(
                                "session=1; Path=/; HttpOnly",
                                "theme=dark; Max-Age=3600"
                            )
                        )
                    )
                } else {
                    cookie = request.headers["Cookie"]
                    respond(content = "ok", status = HttpStatusCode.OK)
                }
            }
        )

        client.fetch(
            HttpRequest(url) { setHeader("Cookie", "existing=1") }
        ).getOrElse { fail("$it") }

        assertEquals("existing=1; session=1; theme=dark", cookie)
    }

    @Test
    fun seeOtherRedirectionOfAPostIsFollowedWithAGetWithoutBody() = runTest {
        var redirected: Pair<String, Long?>? = null
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                if (request.url.encodedPath == "/resource") {
                    respond(
                        content = "",
                        status = HttpStatusCode.SeeOther,
                        headers = headersOf("Location", "http://example.com/created")
                    )
                } else {
                    redirected = request.method.value to request.body.contentLength
                    respond(content = "ok", status = HttpStatusCode.OK)
                }
            }
        )

        client.fetch(
            HttpRequest(url) { setPostForm(mapOf("key" to "value")) }
        ).getOrElse { fail("$it") }

        assertEquals("GET", redirected?.first)
        assertTrue((redirected?.second ?: 0L) == 0L)
    }

    @Test
    fun notModifiedIsAResponseNotARedirection() = runTest {
        val client = DefaultHttpClient(
            engine = MockEngine {
                respond(content = "", status = HttpStatusCode.NotModified)
            }
        )

        val response = client.stream(HttpRequest(url)).getOrElse { fail("$it") }
        assertEquals(304, response.response.statusCode.code)
        response.body.close()
    }

    @Test
    fun aClosedClientFailsWithAnHttpError() = runTest {
        val client = DefaultHttpClient(
            engine = MockEngine {
                respond(content = "ok", status = HttpStatusCode.OK)
            }
        )
        client.close()

        val error = client.fetch(HttpRequest(url)).failureOrNull()
        assertIs<HttpError>(error)
    }

    @Test
    fun tooManyRedirectionsFail() = runTest {
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf("Location", "${request.url}/next")
                )
            }
        )

        val error = client.fetch(HttpRequest(url)).failureOrNull()
        assertIs<HttpError.Redirection>(error)
    }

    @Test
    fun crossSchemeRedirectionIsUnsafeAndCancelledByDefault() = runTest {
        val client = DefaultHttpClient(
            engine = MockEngine {
                respond(
                    content = "",
                    status = HttpStatusCode.MovedPermanently,
                    headers = headersOf("Location", "https://example.com/resource")
                )
            }
        )

        val error = client.fetch(HttpRequest(url)).failureOrNull()
        assertIs<HttpError.Redirection>(error)
    }

    @Test
    fun unsafeRedirectionProceedsWhenConfirmedByTheCallback() = runTest {
        var confirmed = false
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                if (request.url.protocol.name == "http") {
                    respond(
                        content = "",
                        status = HttpStatusCode.MovedPermanently,
                        headers = headersOf("Location", "https://example.com/resource")
                    )
                } else {
                    respond(content = "secure content", status = HttpStatusCode.OK)
                }
            },
            callback = object : DefaultHttpClient.Callback {
                override suspend fun onFollowUnsafeRedirect(
                    request: HttpRequest,
                    response: HttpResponse,
                    newRequest: HttpRequest,
                ): HttpTry<HttpRequest> {
                    confirmed = true
                    return Try.success(newRequest)
                }
            }
        )

        val response = client.fetch(HttpRequest(url)).getOrElse { fail("$it") }
        assertEquals("secure content", response.body.decodeToString())
        assertTrue(confirmed)
    }

    @Test
    fun rangeHeaderIsPassedThrough() = runTest {
        var rangeHeader: String? = null
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                rangeHeader = request.headers["Range"]
                respond(
                    content = "23456",
                    status = HttpStatusCode.PartialContent
                )
            }
        )

        val response = client.stream(
            HttpRequest(url) { setRange(2L..6L) }
        ).getOrElse { fail("$it") }
        response.body.close()

        assertEquals("bytes=2-6", rangeHeader)
        assertEquals(206, response.response.statusCode.code)
    }

    @Test
    fun userAgentIsSentUnlessOverridden() = runTest {
        var userAgent: String? = null
        val client = DefaultHttpClient(
            userAgent = "Readium/Test",
            engine = MockEngine { request ->
                userAgent = request.headers["User-Agent"]
                respond(content = "", status = HttpStatusCode.OK)
            }
        )

        client.fetch(HttpRequest(url))
        assertEquals("Readium/Test", userAgent)

        client.fetch(HttpRequest(url) { setHeader("User-Agent", "Custom") })
        assertEquals("Custom", userAgent)
    }

    @Test
    fun engineExceptionsAreMappedToHttpErrors() = runTest {
        val client = DefaultHttpClient(
            engine = MockEngine {
                throw UnresolvedAddressException()
            }
        )

        val error = client.fetch(HttpRequest(url)).failureOrNull()
        assertIs<HttpError.Unreachable>(error)
    }

    @Test
    fun onRecoverRequestCanRetryAFailedRequest() = runTest {
        var attempts = 0
        val client = DefaultHttpClient(
            engine = MockEngine { request ->
                attempts++
                if (request.headers["Authorization"] == null) {
                    respond(content = "", status = HttpStatusCode.Unauthorized)
                } else {
                    respond(content = "authorized", status = HttpStatusCode.OK)
                }
            },
            callback = object : DefaultHttpClient.Callback {
                override suspend fun onRecoverRequest(
                    request: HttpRequest,
                    error: HttpError,
                ): HttpTry<HttpRequest> =
                    if (error is HttpError.ErrorResponse && error.status.code == 401) {
                        Try.success(request.copy { setHeader("Authorization", "Bearer token") })
                    } else {
                        Try.failure(error)
                    }
            }
        )

        val response = client.fetch(HttpRequest(url)).getOrElse { fail("$it") }
        assertEquals("authorized", response.body.decodeToString())
        assertEquals(2, attempts)
    }
}

class HttpExceptionMappingTest {

    @Test
    fun mapsUnresolvedAddress() {
        assertIs<HttpError.Unreachable>(mapHttpException(UnresolvedAddressException()))
    }

    @Test
    fun mapsUnknownExceptionsToIO() {
        assertIs<HttpError.IO>(mapHttpException(Exception("boom")))
    }
}
