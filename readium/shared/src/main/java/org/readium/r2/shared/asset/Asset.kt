/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.asset

import org.readium.r2.shared.format.Format
import org.readium.r2.shared.resource.Resource as SharedResource

/**
 * An asset which is either a single resource or a container that holds multiple resources.
 */
public sealed class Asset {

    /**
     * Type of the asset source.
     */
    public abstract val type: AssetType

    /**
     * Media format of the asset.
     */
    public abstract val format: Format

    /**
     * Releases in-memory resources related to this asset.
     */
    public abstract suspend fun close()

    /**
     * A single resource asset.
     *
     * @param format Media format of the asset.
     * @param resource Opened resource to access the asset.
     */
    public class Resource(
        override val format: Format,
        public val resource: SharedResource
    ) : Asset() {

        override val type: AssetType =
            AssetType.Resource

        override suspend fun close() {
            resource.close()
        }
    }

    /**
     * A container asset providing access to several resources.
     *
     * @param format Media format of the asset.
     * @param exploded If this container is an exploded or packaged container.
     * @param container Opened container to access asset resources.
     */
    public class Container(
        override val format: Format,
        exploded: Boolean,
        public val container: org.readium.r2.shared.resource.Container
    ) : Asset() {

        override val type: AssetType =
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
