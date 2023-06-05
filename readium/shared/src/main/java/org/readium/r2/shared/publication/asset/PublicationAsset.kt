/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Represents a digital medium (e.g. a file) offering access to a publication.
 *
 * @param name Name of the asset, e.g. a filename.
 * @param mediaType Media type of the asset.
*/
data class PublicationAsset(
    val name: String,
    val mediaType: MediaType,
    val fetcher: Fetcher
)
