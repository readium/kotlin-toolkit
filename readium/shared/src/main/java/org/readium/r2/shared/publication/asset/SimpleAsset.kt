/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.util.mediatype.MediaType

data class SimpleAsset(
    override val name: String,
    override val mediaType: MediaType,
    override val fetcher: Fetcher
) : PublicationAsset
