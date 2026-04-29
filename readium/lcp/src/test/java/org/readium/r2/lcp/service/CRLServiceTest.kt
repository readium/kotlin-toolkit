/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.service

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.Try
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CRLServiceTest {

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

        val network = mockk<NetworkService>()
        val service = CRLService(network = network, context = context)

        val result = service.retrieve()

        assertEquals(expected = "local_crl", actual = result)

        // Ensure network wasn't called
        coVerify(exactly = 0) {
            network.fetch(
                url = any(),
                method = any(),
                parameters = any(),
                timeout = any(),
                headers = any()
            )
        }
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

        val network = mockk<NetworkService>()
        coEvery {
            network.fetch(
                url = any(),
                method = any(),
                parameters = any(),
                timeout = any(),
                headers = any()
            )
        } returns Try.success(success = ByteArray(size = 0))

        val service = CRLService(network = network, context = context)

        service.retrieve()

        // Verify network fetch occurred because the previous one was expired
        coVerify(exactly = 1) {
            network.fetch(
                url = any(),
                method = any(),
                parameters = any(),
                timeout = any(),
                headers = any()
            )
        }
    }

    @Test
    fun `retrieve fetches from network if local CRL does not exist`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val network = mockk<NetworkService>()
        coEvery {
            network.fetch(
                url = any(),
                method = any(),
                parameters = any(),
                timeout = any(),
                headers = any()
            )
        } returns Try.success(success = ByteArray(size = 0))

        val service = CRLService(network = network, context = context)

        service.retrieve()

        coVerify(exactly = 1) {
            network.fetch(
                url = any(),
                method = any(),
                parameters = any(),
                timeout = any(),
                headers = any()
            )
        }
    }
}
