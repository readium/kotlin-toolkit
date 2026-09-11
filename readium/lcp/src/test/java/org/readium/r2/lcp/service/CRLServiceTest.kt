/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package org.readium.r2.lcp.service

import android.content.Context
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.HttpResponse
import org.readium.r2.shared.util.http.HttpStatus
import org.readium.r2.shared.util.http.HttpStreamResponse
import org.readium.r2.shared.util.http.HttpTry
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CRLServiceTest {

    companion object {
        /**
         * The actual CRL served by http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl, DER-encoded.
         *
         * Its validity dates are not checked when parsing, so this fixture will not expire.
         */
        private val crlBytes: ByteArray =
            CRLServiceTest::class.java.getResourceAsStream("edrlab-ca.crl")!!
                .use { it.readBytes() }

        private val CRL_BASE64: String =
            android.util.Base64.encodeToString(crlBytes, android.util.Base64.NO_WRAP)

        /**
         * The same CRL, encoded the way versions of the toolkit running on API level 25 and below
         * cached it: with line breaks every 76 characters.
         *
         * It is a valid CRL which is not equal to [CRL_BASE64], which makes it a convenient stand-in
         * for a previously cached CRL when checking whether a refresh took place.
         */
        private val STALE_CRL_BASE64: String =
            android.util.Base64.encodeToString(crlBytes, android.util.Base64.DEFAULT)

        /**
         * A captive portal login page, as returned with a 200 status by some Wi-Fi networks.
         */
        private val captivePortalBytes: ByteArray =
            "<html><body>Please buy some Wi-Fi</body></html>".toByteArray()
    }

    class TestHttpClient(private val body: ByteArray) : HttpClient {
        var streamCallCount = 0
            private set

        var lastRequest: HttpRequest? = null
            private set

        override suspend fun stream(request: HttpRequest): HttpTry<HttpStreamResponse> {
            streamCallCount++
            lastRequest = request
            return Try.success(
                HttpStreamResponse(
                    HttpResponse(
                        request = request,
                        url = request.url,
                        statusCode = HttpStatus.Success,
                        headers = emptyMap(),
                        mediaType = null
                    ),
                    ByteArrayInputStream(body)
                )
            )
        }
    }

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val preferences
        get() = context.getSharedPreferences("org.readium.r2.lcp", Context.MODE_PRIVATE)

    private fun saveLocalCrl(crl: String, age: kotlin.time.Duration = 2.days) {
        preferences.edit()
            .putString(CRLService.CRL_KEY, crl)
            .putString(CRLService.DATE_KEY, (Clock.System.now() - age).toString())
            .apply()
    }

    private fun pem(base64: String): String =
        "-----BEGIN X509 CRL-----$base64-----END X509 CRL-----"

    /**
     * Parent of the background work started by the service under test.
     */
    private val serviceJob = SupervisorJob()

    private fun TestScope.createService(httpClient: HttpClient): CRLService =
        CRLService(
            httpClient = httpClient,
            context = context,
            coroutineScope = CoroutineScope(serviceJob + StandardTestDispatcher(testScheduler))
        )

    /**
     * Waits for the background refreshes started by the service.
     *
     * The test scheduler cannot be advanced to them instead, as fetching a response hops onto
     * [Dispatchers.IO].
     */
    private suspend fun awaitBackgroundWork() {
        serviceJob.children.toList().joinAll()
    }

    @Test
    fun `retrieve returns local CRL if not expired`() = runTest {
        saveLocalCrl(pem(CRL_BASE64), age = 2.days)

        val httpClient = TestHttpClient(crlBytes)
        val service = createService(httpClient)

        val result = service.retrieve()

        assertEquals(expected = pem(CRL_BASE64), actual = result)

        // Ensure network wasn't called
        assertEquals(0, httpClient.streamCallCount)
    }

    @Test
    fun `retrieve returns the expired local CRL and refreshes it in the background`() = runTest {
        val staleCrl = pem(STALE_CRL_BASE64)
        saveLocalCrl(staleCrl, age = 8.days)

        val httpClient = TestHttpClient(crlBytes)
        val service = createService(httpClient)

        // Opening a publication is not delayed by the fetch.
        assertEquals(expected = staleCrl, actual = service.retrieve())
        assertEquals(0, httpClient.streamCallCount)

        awaitBackgroundWork()

        // The refreshed CRL is cached for the next opening.
        assertEquals(1, httpClient.streamCallCount)
        assertEquals(pem(CRL_BASE64), preferences.getString(CRLService.CRL_KEY, null))
        assertEquals(expected = pem(CRL_BASE64), actual = service.retrieve())
    }

    @Test
    fun `retrieve keeps the expired local CRL when the background refresh fails`() = runTest {
        val staleCrl = pem(STALE_CRL_BASE64)
        saveLocalCrl(staleCrl, age = 8.days)

        val httpClient = TestHttpClient(captivePortalBytes)
        val service = createService(httpClient)

        assertEquals(expected = staleCrl, actual = service.retrieve())
        awaitBackgroundWork()

        assertEquals(1, httpClient.streamCallCount)
        assertEquals(staleCrl, preferences.getString(CRLService.CRL_KEY, null))
        // The next opening tries again, without failing.
        assertEquals(expected = staleCrl, actual = service.retrieve())
    }

    @Test
    fun `preload caches the CRL`() = runTest {
        val httpClient = TestHttpClient(crlBytes)
        val service = createService(httpClient)

        service.preload()
        awaitBackgroundWork()

        assertEquals(1, httpClient.streamCallCount)
        assertEquals(pem(CRL_BASE64), preferences.getString(CRLService.CRL_KEY, null))
    }

    @Test
    fun `preload does not fetch when the local CRL is not expired`() = runTest {
        saveLocalCrl(pem(CRL_BASE64), age = 2.days)

        val httpClient = TestHttpClient(crlBytes)
        val service = createService(httpClient)

        service.preload()
        awaitBackgroundWork()

        assertEquals(0, httpClient.streamCallCount)
    }

    @Test
    fun `retrieve fetches from network if local CRL does not exist`() = runTest {
        val httpClient = TestHttpClient(crlBytes)
        val service = createService(httpClient)

        val result = service.retrieve()

        assertEquals(1, httpClient.streamCallCount)
        assertEquals(expected = pem(CRL_BASE64), actual = result)
        assertEquals(
            expected = AbsoluteUrl("http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl"),
            actual = httpClient.lastRequest?.url
        )
        assertEquals(HttpRequest.Method.GET, httpClient.lastRequest?.method)
    }

    @Test
    fun `retrieve accepts the CRL served by the EDRLab server`() = runTest {
        val httpClient = TestHttpClient(crlBytes)
        val service = createService(httpClient)

        val result = service.retrieve()

        assertEquals(expected = pem(CRL_BASE64), actual = result)
        // The fetched CRL is cached, and read back as valid on the next call.
        assertEquals(pem(CRL_BASE64), preferences.getString(CRLService.CRL_KEY, null))
        assertEquals(expected = pem(CRL_BASE64), actual = service.retrieve())
        assertEquals(1, httpClient.streamCallCount)
    }

    @Test
    fun `retrieve accepts a cached CRL with line breaks`() = runTest {
        // Versions of the toolkit running on API level 25 and below wrapped the base64 payload at
        // 76 characters, so such CRLs must still be readable.
        val wrapped = android.util.Base64.encodeToString(crlBytes, android.util.Base64.DEFAULT)
        assertTrue(wrapped.contains("\n"))
        saveLocalCrl(pem(wrapped), age = 2.days)

        val httpClient = TestHttpClient(crlBytes)
        val service = createService(httpClient)

        assertEquals(expected = pem(wrapped), actual = service.retrieve())
        assertEquals(0, httpClient.streamCallCount)
    }

    @Test
    fun `retrieve rejects a response which is not a CRL`() = runTest {
        val httpClient = TestHttpClient(captivePortalBytes)
        val service = createService(httpClient)

        assertFailsWith<LcpException> { service.retrieve() }

        assertEquals(null, preferences.getString(CRLService.CRL_KEY, null))
    }

    @Test
    fun `retrieve ignores a cached CRL which is not a CRL`() = runTest {
        // Cached by a previous version of the toolkit, before the response was validated.
        saveLocalCrl(
            pem(android.util.Base64.encodeToString(captivePortalBytes, android.util.Base64.DEFAULT)),
            age = 2.days
        )

        val httpClient = TestHttpClient(crlBytes)
        val service = createService(httpClient)

        service.retrieve()

        assertEquals(1, httpClient.streamCallCount)
        assertEquals(pem(CRL_BASE64), preferences.getString(CRLService.CRL_KEY, null))
    }
}
