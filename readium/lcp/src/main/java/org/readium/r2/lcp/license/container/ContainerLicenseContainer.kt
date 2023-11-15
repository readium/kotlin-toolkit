/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.container

import kotlinx.coroutines.runBlocking
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ClosedContainer
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.resource.ResourceEntry

/**
 * Access to a License Document stored in a read-only container.
 */
internal class ContainerLicenseContainer(
    private val container: ClosedContainer<ResourceEntry>,
    private val entryUrl: Url
) : LicenseContainer {

    override fun read(): ByteArray {
        return runBlocking {
            val resource = container.get(entryUrl)
                ?: throw LcpException.Container.FileNotFound(entryUrl)

            resource.read()
                .mapFailure {
                    LcpException.Container.ReadFailed(entryUrl)
                }
                .getOrThrow()
        }
    }
}
