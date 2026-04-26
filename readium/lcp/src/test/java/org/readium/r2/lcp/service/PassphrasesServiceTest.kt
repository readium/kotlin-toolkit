package org.readium.r2.lcp.service

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PassphrasesServiceTest {

    @Test
    fun `addPassphrase hashes passphrase before saving when not already hashed`() = runTest {
        val repository = mockk<PassphrasesRepository>(relaxed = true)
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

        coVerify {
            repository.addPassphrase(
                passphraseHash = expectedHash,
                licenseId = "license_1",
                provider = "provider",
                userId = "user_1"
            )
        }
    }

    @Test
    fun `addPassphrase does not hash passphrase if already hashed`() = runTest {
        val repository = mockk<PassphrasesRepository>(relaxed = true)
        val service = PassphrasesService(repository = repository)

        val alreadyHashed = "2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b"

        service.addPassphrase(
            passphrase = alreadyHashed,
            hashed = true,
            licenseId = "license_1",
            provider = "provider",
            userId = "user_1"
        )

        coVerify {
            repository.addPassphrase(
                passphraseHash = alreadyHashed,
                licenseId = "license_1",
                provider = "provider",
                userId = "user_1"
            )
        }
    }
}
