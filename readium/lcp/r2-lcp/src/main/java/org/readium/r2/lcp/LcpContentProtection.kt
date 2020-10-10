/*
 * Module: r2-lcp-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.TransformingFetcher
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.publication.OnAskCredentials
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.contentProtectionServiceFactory
import org.readium.r2.shared.util.File
import org.readium.r2.shared.util.Try

internal class LcpContentProtection(
    private val lcpService: LcpService,
    private val authentication: LcpAuthenticating
) : ContentProtection {

    override suspend fun open(
        file: File,
        fetcher: Fetcher,
        allowUserInteraction: Boolean,
        credentials: String?,
        sender: Any?,
        onAskCredentials: OnAskCredentials?
    ): Try<ContentProtection.ProtectedFile, Publication.OpeningException>? {

        val isProtectedWithLcp = when (file.format()) {
            Format.EPUB -> fetcher.get("/META-INF/license.lcpl").use { it.length().isSuccess }
            else -> fetcher.get("/license.lcpl").use { it.length().isSuccess }
        }

        if (!isProtectedWithLcp)
            return null

        val license = lcpService
            .retrieveLicense(file.file,  authentication, allowUserInteraction, sender)
    
        val error = when {
            license == null -> null
            license.isFailure -> license.exceptionOrNull()!!
            else -> null
        }

        val serviceFactory = LcpContentProtectionService
            .createFactory(license?.getOrNull(), error)

        val protectedFile = ContentProtection.ProtectedFile(
            file = file,
            fetcher = TransformingFetcher(fetcher, LcpDecryptor(license?.getOrNull())::transform),
            onCreatePublication = {
                servicesBuilder.contentProtectionServiceFactory = serviceFactory
            }
        )

        return Try.success(protectedFile)
    }

}
