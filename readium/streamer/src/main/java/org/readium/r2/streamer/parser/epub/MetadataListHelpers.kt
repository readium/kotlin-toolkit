/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

internal fun interface MetadataItemsAdapter<T> {

    fun adapt(items: List<MetadataItem>): Pair<T, List<MetadataItem>>
}

internal class MetadataItemsHolder(private var items: List<MetadataItem>) {

    val remainingItems: List<MetadataItem>
        get() = items

    fun <T> adapt(adapter: MetadataItemsAdapter<T>): T =
        adapter.adapt(items).let {
            items = it.second
            it.first
        }
}

/**
 * Return all meta elements with the given [property].
 */
internal fun List<MetadataItem>.metasWithProperty(property: String) = this
    .filterIsInstance(MetadataItem.Meta::class.java)
    .filter { it.property == property }

/**
 * Return the first meta element with the given [property].
 */
internal fun List<MetadataItem>.firstWithProperty(property: String) = this
    .filterIsInstance(MetadataItem.Meta::class.java)
    .firstOrNull { it.property == property }

/**
 * Return the first link element with the given [rel].
 */
internal fun List<MetadataItem>.firstWithRel(rel: String) = this
    .filterIsInstance(MetadataItem.Link::class.java)
    .firstOrNull { it.rels.contains(rel) }

/**
 * Consume all meta elements with any of [properties].
 */
@Suppress("Unchecked_Cast")
internal fun List<MetadataItem>.takeAllWithProperty(vararg properties: String): Pair<List<MetadataItem.Meta>, List<MetadataItem>> =
    partition { it is MetadataItem.Meta && it.property in properties }
        as Pair<List<MetadataItem.Meta>, List<MetadataItem>>

/**
 * Consume the first meta element with any of [properties] and the given [id] if specified.
 */
internal fun List<MetadataItem>.takeFirstWithProperty(vararg properties: String, id: String? = null): Pair<MetadataItem.Meta?, List<MetadataItem>> =
    removeFirstOrNull { it.property in properties && (id == null || it.id == id) }

/**
 * Consume the first link element with given [rel].
 */
internal fun List<MetadataItem>.takeFirstWithRel(rel: String): Pair<MetadataItem.Link?, List<MetadataItem>> =
    removeFirstOrNull { it.rels.contains(rel) }

/**
 * Consume all elements mapped to not null values with [transform].
 */
internal fun <T : Any> List<MetadataItem>.mapTakeNotNull(transform: (MetadataItem) -> T?): Pair<List<T>, List<MetadataItem>> =
    map { it to transform(it) }
        .partition { it.second != null }
        .mapFirst { list -> list.mapNotNull { it.second } }
        .mapSecond { list -> list.map { it.first } }

/**
 * Consume the first element of the list which verifies the given [predicate].
 */
internal inline fun <reified T, reified S : T> List<T>.removeFirstOrNull(predicate: (S) -> Boolean): Pair<S?, List<T>> {
    var consumed: S? = null
    val remaining: MutableList<T> = mutableListOf()
    for (element: T in this) {
        if (consumed == null && element is S && predicate(element)) {
            consumed = element
        } else {
            remaining.add(element)
        }
    }
    return consumed to remaining
}

/**
 * Map the first element of the given pair with [transform] if it is not null.
 */
internal fun <A : Any, B, R : Any> Pair<A?, B>.mapFirstNotNull(transform: (A) -> R?): Pair<R?, B> =
    first?.let { transform(it) } to second

/**
 * Map the first element of the given pair with [transform].
 */
internal fun <A : Any, B, R : Any> Pair<A, B>.mapFirst(transform: (A) -> R): Pair<R, B> =
    transform(first) to second

/**
 * Map the second element of the given pair with [transform].
 */
internal fun <A, B : Any, R : Any> Pair<A, B>.mapSecond(transform: (B) -> R): Pair<A, R> =
    first to transform(second)
