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
import org.readium.r2.shared.publication.Publication

public data class Group(
    val title: String,
    val metadata: OpdsMetadata = OpdsMetadata(title = title),
    val links: List<Link> = emptyList(),
    val publications: List<Publication> = emptyList(),
    val navigation: List<Link> = emptyList(),
) {
    @InternalReadiumApi
    public data class Builder(
        val title: String,
        val metadata: OpdsMetadata.Builder = OpdsMetadata.Builder(title = title),
        val links: MutableList<Link> = mutableListOf(),
        val publications: MutableList<Publication> = mutableListOf(),
        val navigation: MutableList<Link> = mutableListOf(),
    ) {
        public fun build(): Group =
            Group(
                title = title,
                metadata = metadata.build(),
                links = links.toList(),
                publications = publications.toList(),
                navigation = navigation.toList()
            )
    }
}
