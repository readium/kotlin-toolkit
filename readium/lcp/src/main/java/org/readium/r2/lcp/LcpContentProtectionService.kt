/*
 * Module: r2-lcp-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.publication.services.ContentProtectionService

public class LcpContentProtectionService(
    public val license: LcpLicense?,
    override val error: LcpError?,
) : ContentProtectionService {

    override val isRestricted: Boolean = license == null

    override val credentials: String? = null

    override val rights: ContentProtectionService.UserRights = license
        ?: ContentProtectionService.UserRights.AllRestricted

    override val scheme: ContentProtection.Scheme = ContentProtection.Scheme.Lcp

    override fun close() {
        license?.close()
    }

    public companion object {

        public fun createFactory(
            license: LcpLicense?,
            error: LcpError?,
        ): (
            Publication.Service.Context,
        ) -> LcpContentProtectionService =
            { LcpContentProtectionService(license, error) }
    }
}

/**
 * Returns the [LcpLicense] if the [Publication] is protected by LCP and the license is opened.
 */
public val Publication.lcpLicense: LcpLicense?
    get() = findService(LcpContentProtectionService::class)?.license
