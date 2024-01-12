/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.asset

import java.io.File
import org.readium.r2.shared.util.mediatype.MediaType

@Deprecated(
    "Use an `AssetRetriever` to create an `Asset`.",
    ReplaceWith("AssetRetriever().retrieve(file)"),
    DeprecationLevel.ERROR
)
public data class FileAsset(
    public val file: File,
    public val mediaType: MediaType? = null
)
