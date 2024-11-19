/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.resource.Resource

/**
 * An asset which is either a single resource or a container that holds multiple resources.
 */
public sealed class Asset : Closeable {

    /**
     * Format of the asset.
     */
    public abstract val format: Format
}

/**
 * A container asset providing access to several resources.
 *
 * @param format Format of the asset.
 * @param container Opened container to access asset resources.
 */
public class ContainerAsset(
    override val format: Format,
    public val container: Container<Resource>,
) : Asset() {

    override fun close() {
        container.close()
    }
}

/**
 * A single resource asset.
 *
 * @param format Format of the asset.
 * @param resource Opened resource to access the asset.
 */
public class ResourceAsset(
    override val format: Format,
    public val resource: Resource,
) : Asset() {

    override fun close() {
        resource.close()
    }
}
