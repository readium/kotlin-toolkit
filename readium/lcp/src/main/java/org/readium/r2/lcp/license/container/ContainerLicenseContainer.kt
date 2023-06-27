/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.container

import kotlinx.coroutines.runBlocking
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.getOrThrow

/**
 * Access to a License Document stored in a read-only ZIP archive.
 */
internal class ContainerLicenseContainer(
    private val container: Container,
    private val entryPath: String,
) : LicenseContainer {

    override fun read(): ByteArray {
        return runBlocking {
            container
                .entry(entryPath)
                .read()
                .mapFailure {
                    when (it) {
                        is Resource.Exception.NotFound -> LcpException.Container.FileNotFound(entryPath)
                        else -> LcpException.Container.ReadFailed(entryPath)
                    }
                }
                .getOrThrow()
        }
    }

    override fun write(license: LicenseDocument) {
        throw LcpException.Container.WriteFailed(entryPath)
    }
}
