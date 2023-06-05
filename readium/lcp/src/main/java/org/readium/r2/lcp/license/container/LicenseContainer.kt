/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

private const val LICENSE_IN_EPUB = "META-INF/license.lcpl"

private const val LICENSE_IN_RPF = "license.lcpl"

/**
 * Encapsulates the read/write access to the packaged License Document (eg. in an EPUB container,
 * or a standalone LCPL file).
 */
internal interface LicenseContainer {
    fun read(): ByteArray
    fun write(license: LicenseDocument)
}

internal suspend fun createLicenseContainer(
    filepath: String,
    mediaTypes: List<String> = emptyList(),
    mediaTypeRetriever: MediaTypeRetriever
): LicenseContainer {
    val mediaType = mediaTypeRetriever.ofFile(filepath, mediaTypes = mediaTypes, fileExtensions = emptyList())
        ?: throw LcpException.Container.OpenFailed
    return createLicenseContainer(filepath, mediaType)
}

internal fun createLicenseContainer(
    filepath: String,
    mediaType: MediaType
): LicenseContainer =
    when (mediaType) {
        MediaType.EPUB -> ZIPLicenseContainer(filepath, LICENSE_IN_EPUB)
        MediaType.LCP_LICENSE_DOCUMENT -> LCPLLicenseContainer(filepath)
        // Assuming it's a Readium WebPub package (e.g. audiobook, LCPDF, etc.) as a fallback
        else -> ZIPLicenseContainer(filepath, LICENSE_IN_RPF)
    }

internal fun createLicenseContainer(
    fetcher: Fetcher,
    mediaType: MediaType,
): LicenseContainer {
    val licensePath = when (mediaType) {
        MediaType.EPUB -> LICENSE_IN_EPUB.addPrefix("/")
        // Assuming it's a Readium WebPub package (e.g. audiobook, LCPDF, etc.) as a fallback
        else -> LICENSE_IN_RPF.addPrefix("/")
    }
    return FetcherLicenseContainer(fetcher, licensePath)
}

internal fun createLicenseContainer(
    container: Container,
    mediaType: MediaType,
): LicenseContainer {
    val licensePath = when (mediaType) {
        MediaType.EPUB -> LICENSE_IN_EPUB.addPrefix("/")
        // Assuming it's a Readium WebPub package (e.g. audiobook, LCPDF, etc.) as a fallback
        else -> LICENSE_IN_RPF.addPrefix("/")
    }
    return ContainerLicenseContainer(container, licensePath)
}
