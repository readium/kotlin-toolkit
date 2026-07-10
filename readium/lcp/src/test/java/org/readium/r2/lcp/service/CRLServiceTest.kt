/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.service

import android.content.Context
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.HttpResponse
import org.readium.r2.shared.util.http.HttpStatus
import org.readium.r2.shared.util.http.HttpStreamResponse
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CRLServiceTest {

    class TestHttpClient : HttpClient {
        var streamCallCount = 0
            private set

        override suspend fun stream(request: HttpRequest): Try<HttpStreamResponse, org.readium.r2.shared.util.http.HttpError> {
            streamCallCount++
            return Try.success(
                HttpStreamResponse(
                    HttpResponse(
                        request = HttpRequest(AbsoluteUrl("http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl")!!),
                        url = AbsoluteUrl("http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl")!!,
                        statusCode = HttpStatus.Success,
                        headers = emptyMap(),
                        mediaType = null
                    ),
                    ByteArrayInputStream(ByteArray(0))
                )
            )
        }
    }

    @Test
    fun `retrieve returns local CRL if not expired`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences(
            "org.readium.r2.lcp",
            Context.MODE_PRIVATE
        )

        val activeDate = (Clock.System.now() - 2.days).toString()
        preferences.edit().putString(CRLService.CRL_KEY, "local_crl").apply()
        preferences.edit().putString(CRLService.DATE_KEY, activeDate).apply()

        val httpClient = TestHttpClient()
        val service = CRLService(httpClient = httpClient, context = context)

        val result = service.retrieve()

        assertEquals(expected = "local_crl", actual = result)

        // Ensure network wasn't called
        assertEquals(0, httpClient.streamCallCount)
    }

    @Test
    fun `retrieve fetches from network if local CRL is expired`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences(
            "org.readium.r2.lcp",
            Context.MODE_PRIVATE
        )

        val expiredDate = (Clock.System.now() - 8.days).toString()
        preferences.edit().putString(CRLService.CRL_KEY, "old_crl").apply()
        preferences.edit().putString(CRLService.DATE_KEY, expiredDate).apply()

        val httpClient = TestHttpClient()
        val service = CRLService(httpClient = httpClient, context = context)

        service.retrieve()

        // Verify network fetch occurred because the previous one was expired
        assertEquals(1, httpClient.streamCallCount)
    }

    @Test
    fun `retrieve fetches from network if local CRL does not exist`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val httpClient = TestHttpClient()
        val service = CRLService(httpClient = httpClient, context = context)

        service.retrieve()

        assertEquals(1, httpClient.streamCallCount)
    }
}
