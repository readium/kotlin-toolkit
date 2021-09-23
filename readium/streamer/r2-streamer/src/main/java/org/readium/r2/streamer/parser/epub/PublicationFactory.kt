/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.encryption.Encryption

/**
 * Creates a [Publication] model from an EPUB package's document.
 *
 * @param displayOptions iBooks Display Options XML file to use as a fallback for the metadata.
 *        See https://github.com/readium/architecture/blob/master/streamer/parser/metadata.md#epub-2x-9
 */
internal class PublicationFactory(
    private val fallbackTitle: String,
    private val packageDocument: PackageDocument,
    private val navigationData: Map<String, List<Link>> = emptyMap(),
    private val encryptionData: Map<String, Encryption> = emptyMap(),
    private val displayOptions: Map<String, String> = emptyMap()
) {
    private val epubVersion = packageDocument.epubVersion
    private val links = packageDocument.metadata.links
    private val spine = packageDocument.spine
    private val manifest = packageDocument.manifest

    private val pubMetadata = PubMetadataAdapter(
        epubVersion,
        packageDocument.metadata.global,
        fallbackTitle,
        packageDocument.uniqueIdentifierId,
        spine.direction,
        displayOptions
    )

    private val itemMetadata = packageDocument.metadata.refine
        .mapValues { LinkMetadataAdapter(epubVersion, it.value) }

    @Suppress("Unchecked_cast")
    private val itemById = manifest
        .filter { it.id != null }
        .associateBy(Item::id) as Map<String, Item>

    private val itemrefByIdref = spine.itemrefs.associateBy(Itemref::idref)

    fun create(): Manifest {
        // Compute metadata
        val metadata = pubMetadata.metadata()
        val metadataLinks = links.mapNotNull(::mapEpubLink)

        // Compute links
        val readingOrderIds = spine.itemrefs.filter { it.linear }.map { it.idref }
        val readingOrder = readingOrderIds.mapNotNull { itemById[it]?.let { computeLink(it) } }
        val readingOrderAllIds = computeIdsWithFallbacks(readingOrderIds)
        val resourceItems = manifest.filterNot { it.id in readingOrderAllIds }
        val resources = resourceItems.map { computeLink(it) }

        // Compute toc and otherCollections
        val toc = navigationData["toc"].orEmpty()
        val subcollections = navigationData
            .minus("toc")
            .mapKeys {
                when (it.key) {
                    // RWPM uses camel case for the roles
                    // https://github.com/readium/webpub-manifest/issues/53
                    "page-list" -> "pageList"
                    else -> it.key
                }
            }
            .mapValues { listOf(PublicationCollection(links = it.value)) }

        // Build Publication object
        return Manifest(
            metadata = metadata,
            links = metadataLinks,
            readingOrder = readingOrder,
            resources = resources,
            tableOfContents = toc,
            subcollections = subcollections
        )
    }

    /** Compute a Publication [Link] from an Epub metadata link */
    private fun mapEpubLink(link: EpubLink): Link? {
        val contains: MutableList<String> = mutableListOf()
        if (link.rels.contains(Vocabularies.LINK + "record")) {
            if (link.properties.contains(Vocabularies.LINK + "onix"))
                contains.add("onix")
            if (link.properties.contains(Vocabularies.LINK + "xmp"))
                contains.add("xmp")
        }
        return Link(
            href = link.href,
            type = link.mediaType,
            rels = link.rels,
            properties = Properties(mapOf("contains" to contains))
        )
    }

    /** Recursively find the ids of the fallback items in [items] */
    private fun computeIdsWithFallbacks(ids: List<String>): Set<String> {
        val fallbackIds: MutableSet<String> = mutableSetOf()
        ids.forEach { fallbackIds.addAll(computeFallbackChain(it)) }
        return fallbackIds
    }

    /** Compute the ids contained in the fallback chain of [item] */
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
            href = item.href,
            type = item.mediaType,
            duration = itemMetadata[item.id]?.duration,
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

        val coverId = pubMetadata.cover
        if (coverId != null && item.id == coverId) rels.add("cover")

        encryptionData[item.href]?.let {
            properties["encrypted"] = it.toJSON().toMap()
        }

        return Pair(rels, Properties(properties))
    }

    /** Compute alternate links for [item], checking for an infinite recursion */
    private fun computeAlternates(item: Item, fallbackChain: Set<String>): List<Link> {

        val fallback = item.fallback?.let { id ->
            if (id in fallbackChain) null else
                itemById[id]?.let {
                    val updatedChain = if (item.id != null) fallbackChain + item.id else fallbackChain
                    computeLink(it, updatedChain)
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
                Vocabularies.ITEMREF + "page-spread-left" -> "left"
                Vocabularies.RENDITION + "page-spread-right",
                Vocabularies.ITEMREF + "page-spread-right" -> "right"
                else -> null
            }?.let { linkProperties["page"] = it }
            //  Spread
            when (property) {
                Vocabularies.RENDITION + "spread-node" -> "none"
                Vocabularies.RENDITION + "spread-auto" -> "auto"
                Vocabularies.RENDITION + "spread-landscape" -> "landscape"
                Vocabularies.RENDITION + "spread-portrait",
                Vocabularies.RENDITION + "spread-both" -> "both"
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
                Vocabularies.RENDITION + "flow-scrolled-doc" -> "scrolled"
                else -> null
            }?.let { linkProperties["overflow"] = it }
        }
        return linkProperties
    }
}
