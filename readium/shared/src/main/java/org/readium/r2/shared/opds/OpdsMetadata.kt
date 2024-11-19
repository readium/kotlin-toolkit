/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.opds

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.Instant

public data class OpdsMetadata(
    val title: String,
    val numberOfItems: Int? = null,
    val itemsPerPage: Int? = null,
    val currentPage: Int? = null,
    val modified: Instant? = null,
    val position: Int? = null,
    val rdfType: String? = null,
) {
    @InternalReadiumApi
    public data class Builder(
        var title: String,
        var numberOfItems: Int? = null,
        var itemsPerPage: Int? = null,
        var currentPage: Int? = null,
        var modified: Instant? = null,
        var position: Int? = null,
        var rdfType: String? = null,
    ) {
        public fun build(): OpdsMetadata =
            OpdsMetadata(
                title = title,
                numberOfItems = numberOfItems,
                itemsPerPage = itemsPerPage,
                currentPage = currentPage,
                modified = modified,
                position = position,
                rdfType = rdfType
            )
    }
}
