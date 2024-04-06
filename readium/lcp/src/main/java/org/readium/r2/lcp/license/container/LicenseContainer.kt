/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

import android.content.Context
import java.io.File
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.format.EpubSpecification
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.LcpLicenseSpecification
import org.readium.r2.shared.util.resource.Resource

private val LICENSE_IN_EPUB = Url("META-INF/license.lcpl")!!
private val LICENSE_IN_RPF = Url("license.lcpl")!!

/**
 * Encapsulates the read/write access to the packaged License Document (eg. in an EPUB container,
 * or a standalone LCPL file).
 */
internal interface LicenseContainer {
    fun read(): ByteArray
}

internal interface WritableLicenseContainer : LicenseContainer {
    fun write(license: LicenseDocument)
}

internal fun createLicenseContainer(
    file: File,
    formatSpecification: FormatSpecification
): WritableLicenseContainer =
    when {
        formatSpecification.conformsTo(EpubSpecification) -> FileZipLicenseContainer(
            file.path,
            LICENSE_IN_EPUB
        )
        formatSpecification.conformsTo(LcpLicenseSpecification) -> LcplLicenseContainer(file)
        // Assuming it's a Readium WebPub package (e.g. audiobook, LCPDF, etc.) as a fallback
        else -> FileZipLicenseContainer(file.path, LICENSE_IN_RPF)
    }

internal fun createLicenseContainer(
    context: Context,
    asset: Asset
): LicenseContainer =
    when (asset) {
        is ResourceAsset -> createLicenseContainer(asset.resource, asset.format.specification)
        is ContainerAsset -> createLicenseContainer(
            context,
            asset.container,
            asset.format.specification
        )
    }

internal fun createLicenseContainer(
    resource: Resource,
    formatSpecification: FormatSpecification
): LicenseContainer {
    if (!formatSpecification.conformsTo(LcpLicenseSpecification)) {
        throw LcpException(LcpError.Container.OpenFailed)
    }

    return resource.sourceUrl?.toFileUrl()?.toFile()
        ?.let { LcplLicenseContainer(it) }
        ?: LcplResourceLicenseContainer(resource)
}

internal fun createLicenseContainer(
    context: Context,
    container: Container<Resource>,
    formatSpecification: FormatSpecification
): LicenseContainer {
    val licensePath = when {
        formatSpecification.conformsTo(EpubSpecification) -> LICENSE_IN_EPUB
        // Assuming it's a Readium WebPub package (e.g. audiobook, LCPDF, etc.) as a fallback
        else -> LICENSE_IN_RPF
    }

    container.sourceUrl?.toFileUrl()?.let {
        return FileZipLicenseContainer(it.path, licensePath)
    }

    container.sourceUrl?.toContentUrl()?.let {
        return ContentZipLicenseContainer(context, container, licensePath)
    }

    return ContainerLicenseContainer(container, licensePath)
}
