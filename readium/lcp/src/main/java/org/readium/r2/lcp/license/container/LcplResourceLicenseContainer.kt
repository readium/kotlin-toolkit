/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

import kotlinx.coroutines.runBlocking
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.resource.Resource

/**
 * Access a License Document stored in an LCP License Document file (LCPL) readable through a
 * [Resource].
 */
internal class LcplResourceLicenseContainer(private val resource: Resource) : LicenseContainer {

    override fun read(): ByteArray =
        runBlocking {
            resource.read()
                .getOrElse { throw LcpException(LcpError.Container.OpenFailed) }
        }
}
