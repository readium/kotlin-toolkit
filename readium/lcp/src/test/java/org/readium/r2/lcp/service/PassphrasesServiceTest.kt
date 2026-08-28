/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.service

import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.readium.r2.lcp.fakes.FakeLcpDao

class PassphrasesServiceTest {

    @Test
    fun `addPassphrase hashes passphrase before saving when not already hashed`() = runTest {
        val fakeDao = FakeLcpDao()
        val repository = PassphrasesRepository(lcpDao = fakeDao)
        val service = PassphrasesService(repository = repository)

        val clearPassphrase = "secret"
        // SHA-256 of "secret"
        val expectedHash = "2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b"

        service.addPassphrase(
            passphrase = clearPassphrase,
            hashed = false,
            licenseId = "license_1",
            provider = "provider",
            userId = "user_1"
        )

        val saved = fakeDao.addedPassphrases.single()
        assertEquals(expectedHash, saved.passphrase)
        assertEquals("license_1", saved.licenseId)
        assertEquals("provider", saved.provider)
        assertEquals("user_1", saved.userId)
    }

    @Test
    fun `addPassphrase does not hash passphrase if already hashed`() = runTest {
        val fakeDao = FakeLcpDao()
        val repository = PassphrasesRepository(lcpDao = fakeDao)
        val service = PassphrasesService(repository = repository)

        val alreadyHashed = "2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b"

        service.addPassphrase(
            passphrase = alreadyHashed,
            hashed = true,
            licenseId = "license_1",
            provider = "provider",
            userId = "user_1"
        )

        val saved = fakeDao.addedPassphrases.single()
        assertEquals(alreadyHashed, saved.passphrase)
    }
}
