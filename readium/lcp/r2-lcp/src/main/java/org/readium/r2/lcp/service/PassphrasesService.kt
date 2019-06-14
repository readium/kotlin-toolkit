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
import kotlinx.coroutines.runBlocking
import org.readium.lcp.sdk.Lcp
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.public.LCPAuthenticatedLicense
import org.readium.r2.lcp.public.LCPAuthenticating
import org.readium.r2.lcp.public.LCPAuthenticationReason

class PassphrasesService(private val repository: PassphrasesRepository) {

    fun request(license: LicenseDocument, authentication: LCPAuthenticating?, completion: (String?) -> Unit) {
        runBlocking {
            val candidates = this@PassphrasesService.possiblePassphrasesFromRepository(license)
            val passphrase = try {
                Lcp().findOneValidPassphrase(license.json.toString(), candidates.toTypedArray())
            } catch (e: Exception) {
                null
            }
            when {
                passphrase != null -> completion(passphrase)
                authentication != null -> this@PassphrasesService.authenticate(license, LCPAuthenticationReason.passphraseNotFound, authentication) {
                    completion(it)
                }
                else -> completion(null)
            }
        }
    }

    private fun authenticate(license: LicenseDocument, reason: LCPAuthenticationReason, authentication: LCPAuthenticating, completion: (String?) -> Unit) {
        val authenticatedLicense = LCPAuthenticatedLicense(document = license)
        authentication.requestPassphrase(authenticatedLicense, reason) { clearPassphrase ->
            clearPassphrase?.let {
                val hashedPassphrase = HASH.sha256(clearPassphrase)
                try {
                    val passphrase = Lcp().findOneValidPassphrase(license.json.toString(), listOf(hashedPassphrase).toTypedArray())
                    this.repository.addPassphrase(passphrase, license.id, license.provider, license.user.id)
                    completion(passphrase)
                } catch (e: Exception) {
                    authenticate(license, LCPAuthenticationReason.invalidPassphrase, authentication, completion)
                }
            } ?: run {
                completion(null)
            }
        }
    }

    private fun possiblePassphrasesFromRepository(license: LicenseDocument): List<String> {
        val passphrases: MutableList<String> = mutableListOf()
        val licensePassphrase = repository.passphrase(license.id)
        if (licensePassphrase != null) {
            passphrases.add(licensePassphrase)
        }
        val userId = license.user.id
        if (userId != null) {
            val userPassphrases = repository.passphrases(userId)
            passphrases.addAll(userPassphrases)
        }
        return passphrases
    }
}
