package org.readium.r2.shared.publication.asset

import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.AssetDescription

data class PublicationSource(
    val url: Url,
    val description: AssetDescription
)
