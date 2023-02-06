/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.container

import kotlinx.coroutines.runBlocking
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.fetcher.Fetcher

/**
 * Access to a License Document stored in a [Fetcher].
 */
internal class FetcherLicenseContainer(
    private val fetcher: Fetcher,
    private val entryPath: String,
) : LicenseContainer {

    override fun read(): ByteArray {
        val link = try {
            runBlocking { fetcher.links().first { it.href == entryPath } }
        } catch (e: Exception) {
            throw LcpException.Container.FileNotFound(entryPath)
        }

        return try {
            runBlocking { fetcher.get(link).read().getOrThrow() }
        } catch (e: Exception) {
            throw LcpException.Container.ReadFailed(entryPath)
        }
    }

    override fun write(license: LicenseDocument) {
        throw LcpException.Container.WriteFailed(entryPath)
    }
}
