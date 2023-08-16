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
public sealed class Asset {

    /**
     * Media type of the asset.
     */
    public abstract val mediaType: MediaType

    /**
     * Type of the asset source.
     */
    public abstract val assetType: AssetType

    /**
     * Releases in-memory resources related to this asset.
     */
    public abstract suspend fun close()

    /**
     * A single resource asset.
     *
     * @param mediaType Media type of the asset.
     * @param resource Opened resource to access the asset.
     */
    public class Resource(
        override val mediaType: MediaType,
        public val resource: org.readium.r2.shared.resource.Resource
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
     * @param mediaType Media type of the asset.
     * @param exploded If this container is an exploded or packaged container.
     * @param container Opened container to access asset resources.
     */
    public class Container(
        override val mediaType: MediaType,
        exploded: Boolean,
        public val container: org.readium.r2.shared.resource.Container
    ) : Asset() {

        override val assetType: AssetType =
            if (exploded) {
                AssetType.Directory
            } else {
                AssetType.Archive
            }

        override suspend fun close() {
            container.close()
        }
    }
}
