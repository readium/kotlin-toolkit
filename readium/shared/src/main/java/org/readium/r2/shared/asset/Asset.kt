/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.asset

import org.readium.r2.shared.util.mediatype.MediaType

/**
 * An asset which is either a single resource or a container that holds multiple resources.
 */
sealed class Asset {

    /**
     * Name of the asset, e.g. a filename.
     */
    abstract val name: String

    /**
     * Media type of the asset.
     */
    abstract val mediaType: MediaType

    /**
     * Type of the asset source.
     */
    abstract val assetType: AssetType

    /**
     * Releases in-memory resources related to this asset.
     */
    abstract suspend fun close()

    /**
     * A single resource asset.
     *
     * @param name Name of the asset.
     * @param mediaType Media type of the asset.
     * @param resource Opened resource to access the asset.
     */
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

    /**
     * A container asset providing access to several resources.
     *
     * @param name Name of the asset.
     * @param mediaType Media type of the asset.
     * @param exploded If this container is an exploded or packaged container.
     * @param container Opened container to access asset resources.
     */
    class Container(
        override val name: String,
        override val mediaType: MediaType,
        exploded: Boolean,
        val container: org.readium.r2.shared.resource.Container
    ) : Asset() {

        override val assetType: AssetType =
            if (exploded)
                AssetType.Directory
            else
                AssetType.Archive

        override suspend fun close() {
            container.close()
        }
    }
}
