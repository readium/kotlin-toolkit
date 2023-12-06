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
import org.readium.r2.shared.util.mediatype.MediaType
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
    mediaType: MediaType
): WritableLicenseContainer =
    when (mediaType) {
        MediaType.EPUB -> FileZipLicenseContainer(file.path, LICENSE_IN_EPUB)
        MediaType.LCP_LICENSE_DOCUMENT -> LcplLicenseContainer(file)
        // Assuming it's a Readium WebPub package (e.g. audiobook, LCPDF, etc.) as a fallback
        else -> FileZipLicenseContainer(file.path, LICENSE_IN_RPF)
    }

internal fun createLicenseContainer(
    context: Context,
    asset: Asset
): LicenseContainer =
    when (asset) {
        is ResourceAsset -> createLicenseContainer(asset.resource, asset.mediaType)
        is ContainerAsset -> createLicenseContainer(context, asset.container, asset.mediaType)
    }

internal fun createLicenseContainer(
    resource: Resource,
    mediaType: MediaType
): LicenseContainer {
    if (mediaType != MediaType.LCP_LICENSE_DOCUMENT) {
        throw LcpException(LcpError.Container.OpenFailed)
    }

    return when {
        resource.sourceUrl?.isFile == true ->
            LcplLicenseContainer(resource.sourceUrl!!.toFile()!!)
        else ->
            LcplResourceLicenseContainer(resource)
    }
}

internal fun createLicenseContainer(
    context: Context,
    container: Container<Resource>,
    mediaType: MediaType
): LicenseContainer {
    val licensePath = when (mediaType) {
        MediaType.EPUB -> LICENSE_IN_EPUB
        // Assuming it's a Readium WebPub package (e.g. audiobook, LCPDF, etc.) as a fallback
        else -> LICENSE_IN_RPF
    }

    return when {
        container.sourceUrl?.isFile == true ->
            FileZipLicenseContainer(container.sourceUrl!!.path!!, licensePath)
        container.sourceUrl?.isContent == true ->
            ContentZipLicenseContainer(context, container, licensePath)
        else ->
            ContainerLicenseContainer(container, licensePath)
    }
}
