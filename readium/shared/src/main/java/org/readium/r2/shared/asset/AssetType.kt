/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.asset

import org.readium.r2.shared.util.MapCompanion

enum class AssetType(val rawValue: String) {

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

    companion object : MapCompanion<String, AssetType>(values(), AssetType::rawValue)
}
