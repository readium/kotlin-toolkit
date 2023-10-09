/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import org.readium.r2.shared.util.MapCompanion

public enum class AssetType(public val value: String) {

    /**
     * A simple resource.
     */
    Resource("resource"),

    /**
     * A directory container.
     */
    Directory("directory"),

    /**
     * An archive container.
     */
    Archive("archive");

    public companion object : MapCompanion<String, AssetType>(values(), AssetType::value)
}
