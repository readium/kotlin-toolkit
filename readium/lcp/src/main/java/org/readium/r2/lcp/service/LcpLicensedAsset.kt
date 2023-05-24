/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.service

import java.io.File
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

data class LcpLicensedAsset(
    val url: Url,
    override val mediaType: MediaType,
    val licenseFile: File,
    val license: LcpLicense?,
    override val fetcher: Fetcher
) : PublicationAsset {

    override val name: String =
        url.file
}
