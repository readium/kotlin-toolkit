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
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.util.use

/**
 * Access to a License Document stored in a [Fetcher].
 */
internal class FetcherLicenseContainer(
    private val fetcher: Fetcher,
    private val entryPath: String,
) : LicenseContainer {

    override fun read(): ByteArray =
        runBlocking {
            fetcher.get(entryPath).use { resource ->
                resource.read()
                    .mapFailure { it.toLcpException() }
                    .getOrThrow()
            }
        }

    private fun Resource.Exception.toLcpException() =
        when (this) {
            is Resource.Exception.NotFound -> LcpException.Container.FileNotFound(entryPath)
            else -> LcpException.Container.ReadFailed(entryPath)
        }

    override fun write(license: LicenseDocument) {
        throw LcpException.Container.WriteFailed(entryPath)
    }
}
