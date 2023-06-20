/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.readium.r2.shared.UserException
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.ContentProtectionService

internal class FallbackContentProtectionService(
    override val scheme: ContentProtection.Scheme?
) : ContentProtectionService {

    override val isRestricted: Boolean = true
    override val credentials: String? = null
    override val rights = ContentProtectionService.UserRights.AllRestricted
    override val error: UserException = ContentProtection.Exception.SchemeNotSupported(scheme)

    companion object {

        fun createFactory(
            scheme: ContentProtection.Scheme?
        ): (Publication.Service.Context) -> ContentProtectionService =
            { FallbackContentProtectionService(scheme) }
    }
}
