/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

internal sealed class MetadataItem {

    abstract val id: String?
    abstract val children: List<MetadataItem>
    abstract val refines: String?

    data class Link(
        override val id: String?,
        override val refines: String?,
        override val children: List<MetadataItem> = emptyList(),
        val href: String,
        val rels: Set<String>,
        val mediaType: String?,
        val properties: List<String> = emptyList(),
    ) : MetadataItem()

    data class Meta(
        override val id: String?,
        override val refines: String? = null,
        override val children: List<MetadataItem> = emptyList(),
        val property: String,
        val value: String,
        val lang: String,
        val scheme: String? = null,
    ) : MetadataItem()
}

internal fun List<MetadataItem>.metasWithProperty(property: String) = this
    .filterIsInstance(MetadataItem.Meta::class.java)
    .filter { it.property == property }

internal fun List<MetadataItem>.firstWithProperty(property: String) = this
    .filterIsInstance(MetadataItem.Meta::class.java)
    .firstOrNull { it.property == property }

internal fun List<MetadataItem>.firstWithRel(rel: String) = this
    .filterIsInstance(MetadataItem.Link::class.java)
    .firstOrNull { it.rels.contains(rel) }

internal fun List<MetadataItem>.firstValue(property: String) = this
    .firstWithProperty(property)?.value

internal fun List<MetadataItem>.takeAllWithProperty(property: String) =
    partition { it is MetadataItem.Meta && it.property == property }

internal fun List<MetadataItem>.takeFirstWithProperty(property: String) =
    removeFirstOrNull { it is MetadataItem.Meta && it.property == property }

internal fun List<MetadataItem>.takeAllWithRel(rel: String) =
    partition { it is MetadataItem.Link && it.rels.contains(rel) }

internal fun List<MetadataItem>.takeFirstWithRel(rel: String) =
    removeFirstOrNull { it is MetadataItem.Link && it.rels.contains(rel) }

internal fun <T> List<T>.removeFirstOrNull(predicate: (T) -> Boolean): Pair<T?, List<T>> {
    var consumed: T? = null
    val remaining: MutableList<T> = mutableListOf()
    for (element: T in this) {
        if (predicate(element)) {
            consumed = element
        } else {
            remaining.add(element)
        }
    }
    return consumed to remaining
}
