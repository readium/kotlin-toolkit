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

class LCPContentProtection(
    private val lcpService: LCPService,
    private val lcpAuthenticating: LCPAuthenticating
) : ContentProtection {

    override suspend fun open(
        file: File,
        fetcher: Fetcher,
        allowUserInteraction: Boolean,
        credentials: String?,
        sender: Any?,
        onAskCredentials: OnAskCredentials?
    ): Try<ContentProtection.ProtectedFile, Publication.OpeningError>? {

        val isProtectedWithLcp = when (file.format()) {
            Format.EPUB -> fetcher.get("/META-INF/license.lcpl").use { it.length().isSuccess }
            else -> fetcher.get("/license.lcpl").use { it.length().isSuccess }
        }

        if (!isProtectedWithLcp)
            return null

        val license = lcpService
            .retrieveLicense(file,  lcpAuthenticating.takeIf { allowUserInteraction })

        val error = when {
            license == null -> "The credentials prompt was cancelled."
            license.isFailure -> license.exceptionOrNull()!!.errorDescription
            else -> null
        }

        val serviceFactory = LCPContentProtectionService
            .createFactory(license?.getOrNull(), error)

        val protectedFile = ContentProtection.ProtectedFile(
            file = file,
            fetcher = TransformingFetcher(fetcher, LCPDecryptor(license?.getOrNull())::transform),
            onCreateServices = { _, _, servicesBuilder ->
                servicesBuilder.apply {
                    contentProtectionServiceFactory = serviceFactory
                }
            }
        )

        return Try.success(protectedFile)
    }
}
