/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package org.readium.r2.lcp.license

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.readium.r2.lcp.fakes.FakeLcpDao
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.service.LicensesRepository

class LicenseTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkObject(LicenseValidation.Companion)
        every { LicenseValidation.observe(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(LicenseValidation.Companion)
        Dispatchers.resetMain()
    }

    private class FixedRightsDao(
        private val copies: Int?,
        private val prints: Int?,
    ) : FakeLcpDao() {
        override fun copiesLeftFlow(licenseId: String): Flow<Int?> = flowOf(copies)
        override fun printsLeftFlow(licenseId: String): Flow<Int?> = flowOf(prints)
    }

    private suspend fun license(copiesLeft: Int? = null, printsLeft: Int? = null): License =
        License(
            documents = mockk {
                every { license } returns mockk<LicenseDocument> {
                    every { id } returns "license-id"
                }
            },
            validation = mockk(relaxed = true),
            licenses = LicensesRepository(FixedRightsDao(copiesLeft, printsLeft)),
            device = mockk(),
            httpClient = mockk()
        )

    @Test
    fun `canCopy allows text up to the exact allowance`() = runTest {
        val license = license(copiesLeft = 10)
        assertTrue(license.canCopy("a".repeat(9)))
        assertTrue(license.canCopy("a".repeat(10)))
        assertFalse(license.canCopy("a".repeat(11)))
    }

    @Test
    fun `canCopy allows any text when the allowance is unlimited`() = runTest {
        val license = license(copiesLeft = null)
        assertTrue(license.canCopy("a".repeat(10_000)))
    }

    @Test
    fun `canCopy forbids non-empty text when the allowance is exhausted`() = runTest {
        val license = license(copiesLeft = 0)
        assertFalse(license.canCopy("a"))
    }

    @Test
    fun `canPrint allows page counts up to the exact allowance`() = runTest {
        val license = license(printsLeft = 10)
        assertTrue(license.canPrint(9))
        assertTrue(license.canPrint(10))
        assertFalse(license.canPrint(11))
    }

    @Test
    fun `canPrint allows any page count when the allowance is unlimited`() = runTest {
        val license = license(printsLeft = null)
        assertTrue(license.canPrint(10_000))
    }

    @Test
    fun `canPrint forbids printing when the allowance is exhausted`() = runTest {
        val license = license(printsLeft = 0)
        assertFalse(license.canPrint(1))
    }
}
