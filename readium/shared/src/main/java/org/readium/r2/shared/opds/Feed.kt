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
import org.readium.r2.shared.util.Url

public data class Feed(
    val title: String,
    val type: Int,
    val href: Url,
    val metadata: OpdsMetadata = OpdsMetadata(title = title),
    val links: List<Link> = emptyList(),
    val facets: List<Facet> = emptyList(),
    val groups: List<Group> = emptyList(),
    val publications: List<Publication> = emptyList(),
    val navigation: List<Link> = emptyList(),
    val context: List<String> = emptyList(),
) {
    @InternalReadiumApi
    public data class Builder(
        val title: String,
        val type: Int,
        val href: Url,
        val metadata: OpdsMetadata.Builder = OpdsMetadata.Builder(title = title),
        val links: MutableList<Link> = mutableListOf(),
        val facets: MutableList<Facet.Builder> = mutableListOf(),
        val groups: MutableList<Group.Builder> = mutableListOf(),
        val publications: MutableList<Publication> = mutableListOf(),
        val navigation: MutableList<Link> = mutableListOf(),
        val context: MutableList<String> = mutableListOf(),
    ) {
        public fun build(): Feed =
            Feed(
                title = title,
                type = type,
                href = href,
                metadata = metadata.build(),
                links = links.toList(),
                facets = facets.map { it.build() },
                groups = groups.map { it.build() },
                publications = publications.toList(),
                navigation = navigation.toList(),
                context = context.toList()
            )
    }
}

public data class ParseData(val feed: Feed?, val publication: Publication?, val type: Int)
