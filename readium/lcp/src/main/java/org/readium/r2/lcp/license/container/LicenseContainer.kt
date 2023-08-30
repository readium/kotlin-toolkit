/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

import java.io.File
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.mediatype.MediaType

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

internal fun createLicenseContainer(
    file: File,
    mediaType: MediaType
): LicenseContainer =
    when (mediaType) {
        MediaType.EPUB -> ZIPLicenseContainer(file.path, LICENSE_IN_EPUB)
        MediaType.LCP_LICENSE_DOCUMENT -> LCPLLicenseContainer(file.path)
        // Assuming it's a Readium WebPub package (e.g. audiobook, LCPDF, etc.) as a fallback
        else -> ZIPLicenseContainer(file.path, LICENSE_IN_RPF)
    }

internal fun createLicenseContainer(
    asset: Asset
): LicenseContainer =
    when (asset) {
        is Asset.Resource -> createLicenseContainer(asset.resource, asset.mediaType)
        is Asset.Container -> createLicenseContainer(asset.container, asset.mediaType)
    }

internal fun createLicenseContainer(
    resource: Resource,
    mediaType: MediaType
): LicenseContainer {
    if (mediaType != MediaType.LCP_LICENSE_DOCUMENT) {
        throw LcpException.Container.OpenFailed
    }
    return LcplResourceLicenseContainer(resource)
}

internal fun createLicenseContainer(
    container: Container,
    mediaType: MediaType
): LicenseContainer {
    val licensePath = when (mediaType) {
        MediaType.EPUB -> LICENSE_IN_EPUB
        // Assuming it's a Readium WebPub package (e.g. audiobook, LCPDF, etc.) as a fallback
        else -> LICENSE_IN_RPF
    }
    return ContainerLicenseContainer(container, licensePath)
}
