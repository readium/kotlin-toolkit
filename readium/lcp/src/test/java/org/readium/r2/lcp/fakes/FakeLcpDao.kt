/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.readium.r2.lcp.persistence.LcpDao
import org.readium.r2.lcp.persistence.License
import org.readium.r2.lcp.persistence.Passphrase

internal class FakeLcpDao : LcpDao {
    val addedPassphrases = mutableListOf<Passphrase>()

    override suspend fun passphrase(licenseId: String): String? = null
    override suspend fun passphrases(userId: String): List<String> = emptyList()
    override suspend fun allPassphrases(): List<String> = emptyList()
    override suspend fun addPassphrase(passphrase: Passphrase) {
        addedPassphrases.add(passphrase)
    }

    override suspend fun exists(licenseId: String): String? = null
    override suspend fun isDeviceRegistered(licenseId: String): Boolean = false
    override suspend fun registerDevice(licenseId: String) {}
    override suspend fun addLicense(license: License) {}

    override suspend fun getCopiesLeft(licenseId: String): Int? = null
    override fun copiesLeftFlow(licenseId: String): Flow<Int?> = flowOf(null)
    override suspend fun setCopiesLeft(quantity: Int, licenseId: String) {}

    override suspend fun getPrintsLeft(licenseId: String): Int? = null
    override fun printsLeftFlow(licenseId: String): Flow<Int?> = flowOf(null)
    override suspend fun setPrintsLeft(quantity: Int, licenseId: String) {}
}
