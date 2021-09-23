/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import org.readium.r2.lcp.persistence.LcpDao
import org.readium.r2.lcp.persistence.Passphrase

internal class PassphrasesRepository(private val lcpDao: LcpDao) {

    suspend fun passphrase(licenseId: String): String? {
        return lcpDao.passphrase(licenseId)
    }

    suspend fun passphrases(userId: String): List<String> {
        return lcpDao.passphrases(userId)
    }

    suspend fun allPassphrases(): List<String> {
        return lcpDao.allPassphrases()
    }

    suspend fun addPassphrase(
        passphraseHash: String,
        licenseId: String,
        provider: String,
        userId: String?
    ) {
        val transaction = Passphrase(
            licenseId = licenseId,
            provider = provider,
            userId = userId,
            passphrase = passphraseHash
        )
        lcpDao.addPassphrase(transaction)
    }
}
