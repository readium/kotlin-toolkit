/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.asset

import org.readium.r2.shared.util.mediatype.MediaType

sealed class Asset {

    /**
     * Name of the asset, e.g. a filename.
     */
    abstract val name: String

    abstract val mediaType: MediaType

    abstract val assetType: AssetType

    abstract suspend fun close()

    class Resource(
        override val name: String,
        override val mediaType: MediaType,
        val resource: org.readium.r2.shared.resource.Resource
    ) : Asset() {

        override val assetType: AssetType =
            AssetType.Resource

        override suspend fun close() {
            resource.close()
        }
    }

    class Container(
        override val name: String,
        override val mediaType: MediaType,
        override val assetType: AssetType,
        val container: org.readium.r2.shared.resource.Container
    ) : Asset() {

        override suspend fun close() {
            container.close()
        }
    }
}
