/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

internal class ResourceAdapter(
    private val spine: Spine,
    private val manifest: List<Item>,
    private val encryptionData: Map<Url, Encryption>,
    private val coverId: String?,
    private val durationById: Map<String, Double?>,
) {
    data class Links(
        val readingOrder: List<Link>,
        val resources: List<Link>,
        val mediaOverlays: List<Link>,
    )

    @Suppress("Unchecked_cast")
    private val itemById = manifest
        .filter { it.id != null }
        .associateBy(Item::id) as Map<String, Item>

    private val itemrefByIdref = spine.itemrefs
        .associateBy(Itemref::idref)

    fun adapt(): Links {
        val readingOrderIds = spine.itemrefs.filter { it.linear }.map { it.idref }
        val readingOrder = readingOrderIds.mapNotNull { id ->
            itemById[id]?.let { item ->
                computeLink(
                    item
                )
            }
        }
        val readingOrderAllIds = computeIdsWithFallbacks(readingOrderIds)
        val smilIds = manifest.mapNotNull { it.mediaOverlay }
        val smils = smilIds.mapNotNull { id -> itemById[id]?.let { item -> computeLink(item) } }
        val resourceItems = manifest.filterNot { it.id in readingOrderAllIds || it.id in smilIds }
        val resources = resourceItems.map { computeLink(it) }
        return Links(readingOrder, resources, smils)
    }

    /** Recursively find the ids contained in fallback chains of items with [ids]. */
    private fun computeIdsWithFallbacks(ids: List<String>): Set<String> {
        val fallbackIds: MutableSet<String> = mutableSetOf()
        ids.forEach { fallbackIds.addAll(computeFallbackChain(it)) }
        return fallbackIds
    }

    /** Compute the ids contained in the fallback chain of item with [id]. */
    private fun computeFallbackChain(id: String): Set<String> {
        // The termination has already been checked while computing links
        val ids: MutableSet<String> = mutableSetOf()
        val item = itemById[id]
        item?.id?.let { ids.add(it) }
        item?.fallback?.let { ids.addAll(computeFallbackChain(it)) }
        return ids
    }

    /** Compute a Publication [Link] for an epub [Item] and its fallbacks */
    private fun computeLink(item: Item, fallbackChain: Set<String> = emptySet()): Link {
        val (rels, properties) = computePropertiesAndRels(item, itemrefByIdref[item.id])

        return Link(
            href = Href(item.href),
            mediaType = item.mediaType?.let { MediaType(it) },
            duration = durationById[item.id],
            rels = rels,
            properties = properties,
            alternates = computeAlternates(item, fallbackChain)
        )
    }

    private fun computePropertiesAndRels(item: Item, itemref: Itemref?): Pair<Set<String>, Properties> {
        val properties: MutableMap<String, Any> = mutableMapOf()
        val rels: MutableSet<String> = mutableSetOf()
        val (manifestRels, contains, others) = parseItemProperties(item.properties)
        rels.addAll(manifestRels)
        if (contains.isNotEmpty()) {
            properties["contains"] = contains
        }
        for (other in others) {
            properties[other] = true
        }
        if (itemref != null) {
            properties.putAll(parseItemrefProperties(itemref.properties))
        }

        if (coverId != null && item.id == coverId) rels.add("cover")

        encryptionData[item.href]?.let {
            properties["encrypted"] = it.toJSON().toMap()
        }

        return Pair(rels, Properties(properties))
    }

    /** Compute alternate links for [item], checking for an infinite recursion */
    private fun computeAlternates(item: Item, fallbackChain: Set<String>): List<Link> {
        val fallback = item.fallback?.let { id ->
            if (id in fallbackChain) {
                null
            } else {
                itemById[id]?.let {
                    val updatedChain = if (item.id != null) fallbackChain + item.id else fallbackChain
                    computeLink(it, updatedChain)
                }
            }
        }
        val mediaOverlays = item.mediaOverlay?.let { id ->
            itemById[id]?.let {
                computeLink(it)
            }
        }
        return listOfNotNull(fallback, mediaOverlays)
    }

    private fun parseItemProperties(properties: List<String>): Triple<List<String>, List<String>, List<String>> {
        val rels: MutableList<String> = mutableListOf()
        val contains: MutableList<String> = mutableListOf()
        val others: MutableList<String> = mutableListOf()
        for (property in properties) {
            when (property) {
                Vocabularies.ITEM + "scripted" -> contains.add("js")
                Vocabularies.ITEM + "mathml" -> contains.add("mathml")
                Vocabularies.ITEM + "svg" -> contains.add("svg")
                Vocabularies.ITEM + "xmp-record" -> contains.add("xmp")
                Vocabularies.ITEM + "remote-resources" -> contains.add("remote-resources")
                Vocabularies.ITEM + "nav" -> rels.add("contents")
                Vocabularies.ITEM + "cover-image" -> rels.add("cover")
                else -> others.add(property)
            }
        }
        return Triple(rels, contains, others)
    }

    private fun parseItemrefProperties(properties: List<String>): Map<String, String> {
        val linkProperties: MutableMap<String, String> = mutableMapOf()
        for (property in properties) {
            //  Page
            when (property) {
                Vocabularies.RENDITION + "page-spread-center" -> "center"
                Vocabularies.RENDITION + "page-spread-left",
                Vocabularies.ITEMREF + "page-spread-left",
                -> "left"
                Vocabularies.RENDITION + "page-spread-right",
                Vocabularies.ITEMREF + "page-spread-right",
                -> "right"
                else -> null
            }?.let { linkProperties["page"] = it }
            //  Spread
            when (property) {
                Vocabularies.RENDITION + "spread-none" -> "none"
                Vocabularies.RENDITION + "spread-auto" -> "auto"
                Vocabularies.RENDITION + "spread-landscape" -> "landscape"
                Vocabularies.RENDITION + "spread-portrait",
                Vocabularies.RENDITION + "spread-both",
                -> "both"
                else -> null
            }?.let { linkProperties["spread"] = it }
            //  Layout
            when (property) {
                Vocabularies.RENDITION + "layout-reflowable" -> "reflowable"
                Vocabularies.RENDITION + "layout-pre-paginated" -> "fixed"
                else -> null
            }?.let { linkProperties["layout"] = it }
            //  Orientation
            when (property) {
                Vocabularies.RENDITION + "orientation-auto" -> "auto"
                Vocabularies.RENDITION + "orientation-landscape" -> "landscape"
                Vocabularies.RENDITION + "orientation-portrait" -> "portrait"
                else -> null
            }?.let { linkProperties["orientation"] = it }
            //  Overflow
            when (property) {
                Vocabularies.RENDITION + "flow-auto" -> "auto"
                Vocabularies.RENDITION + "flow-paginated" -> "paginated"
                Vocabularies.RENDITION + "flow-scrolled-continuous",
                Vocabularies.RENDITION + "flow-scrolled-doc",
                -> "scrolled"
                else -> null
            }?.let { linkProperties["overflow"] = it }
        }
        return linkProperties
    }
}
