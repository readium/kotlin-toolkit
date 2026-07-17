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
import org.readium.r2.shared.publication.Link

public data class Facet(
    val title: String,
    val metadata: OpdsMetadata = OpdsMetadata(title = title),
    val links: List<Link> = emptyList(),
) {
    @InternalReadiumApi
    public data class Builder(
        val title: String,
        val metadata: OpdsMetadata.Builder = OpdsMetadata.Builder(title = title),
        val links: MutableList<Link> = mutableListOf(),
    ) {
        public fun build(): Facet =
            Facet(
                title = title,
                metadata = metadata.build(),
                links = links.toList()
            )
    }
}
