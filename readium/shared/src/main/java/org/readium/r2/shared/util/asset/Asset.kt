/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import org.readium.r2.shared.util.data.ClosedContainer
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
        public val resource: org.readium.r2.shared.util.resource.Resource
    ) : Asset() {

        override suspend fun close() {
            resource.close()
        }
    }

    /**
     * A container asset providing access to several resources.
     *
     * @param mediaType Media type of the asset.
     * @param containerType Media type of the container.
     * @param container Opened container to access asset resources.
     */
    public class Container(
        override val mediaType: MediaType,
        public val containerType: MediaType,
        public val container: ClosedContainer<org.readium.r2.shared.util.resource.Resource>
    ) : Asset() {

        override suspend fun close() {
            container.close()
        }
    }
}
