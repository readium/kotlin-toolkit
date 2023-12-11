/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.util.Error

internal class FallbackContentProtectionService(
    override val scheme: ContentProtection.Scheme,
    override val name: String
) : ContentProtectionService {

    override val isRestricted: Boolean = true
    override val credentials: String? = null
    override val rights = ContentProtectionService.UserRights.AllRestricted
    override val error: Error = SchemeNotSupportedError(scheme, name)

    private class SchemeNotSupportedError(
        val scheme: ContentProtection.Scheme,
        val name: String
    ) : Error {

        override val message: String = "$name DRM scheme is not supported."

        override val cause: Error? = null
    }

    companion object {

        fun createFactory(
            scheme: ContentProtection.Scheme,
            name: String
        ): (Publication.Service.Context) -> ContentProtectionService =
            { FallbackContentProtectionService(scheme, name) }
    }
}
