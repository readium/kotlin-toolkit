/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import com.mcxiaoke.koi.HASH
import org.readium.r2.lcp.LcpAuthenticating
import org.readium.r2.lcp.license.model.LicenseDocument

internal class PassphrasesService(private val repository: PassphrasesRepository) {

    suspend fun request(license: LicenseDocument, authentication: LcpAuthenticating?, allowUserInteraction: Boolean, sender: Any?): String? {
        val candidates = this@PassphrasesService.possiblePassphrasesFromRepository(license)
        val passphrase = try {
            LcpClient.findOneValidPassphrase(license.json.toString(), candidates)
        } catch (e: Exception) {
            null
        }
        return when {
            passphrase != null -> passphrase
            authentication != null -> this@PassphrasesService.authenticate(license, LcpAuthenticating.AuthenticationReason.PassphraseNotFound, authentication, allowUserInteraction, sender)
            else -> null
        }
    }

    private suspend fun authenticate(license: LicenseDocument, reason: LcpAuthenticating.AuthenticationReason, authentication: LcpAuthenticating, allowUserInteraction: Boolean, sender:Any?): String? {
        val authenticatedLicense = LcpAuthenticating.AuthenticatedLicense(document = license)
        val clearPassphrase = authentication.retrievePassphrase(authenticatedLicense, reason, allowUserInteraction, sender)
            ?: return null
        val hashedPassphrase = HASH.sha256(clearPassphrase)
        val passphrases = mutableListOf(hashedPassphrase)
        // Note: The C++ LCP lib crashes if we provide a passphrase that is not a valid
        // SHA-256 hash. So we check this beforehand.
        if (sha256Regex.matches(clearPassphrase)) {
            passphrases.add(clearPassphrase)
        }

        return try {
            val passphrase = LcpClient.findOneValidPassphrase(license.json.toString(), passphrases)
            addPassphrase(passphrase, true, license.id, license.provider, license.user.id)
            passphrase
        } catch (e: Exception) {
            authenticate(license, LcpAuthenticating.AuthenticationReason.InvalidPassphrase, authentication, allowUserInteraction, sender)
        }
    }

    suspend fun addPassphrase(
        passphrase: String,
        hashed: Boolean,
        licenseId: String,
        provider: String,
        userId: String?
    ) {
        val hashedPassphrase = if (hashed) passphrase else HASH.sha256(passphrase)
        this.repository.addPassphrase(hashedPassphrase, licenseId, provider, userId)
    }

    private suspend fun possiblePassphrasesFromRepository(license: LicenseDocument): List<String> {
        val passphrases: MutableSet<String> = linkedSetOf()
        val licensePassphrase = repository.passphrase(license.id)
        if (licensePassphrase != null) {
            passphrases.add(licensePassphrase)
        }
        val userId = license.user.id
        if (userId != null) {
            val userPassphrases = repository.passphrases(userId)
            passphrases.addAll(userPassphrases)
        }
        passphrases.addAll(repository.allPassphrases())
        return passphrases.toList()
    }

    companion object {
        private val sha256Regex = "^([a-f0-9]{64})$".toRegex(RegexOption.IGNORE_CASE)
    }

}
