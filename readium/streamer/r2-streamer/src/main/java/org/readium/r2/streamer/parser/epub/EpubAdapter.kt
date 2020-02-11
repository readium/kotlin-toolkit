/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.PublicationCollection
import org.readium.r2.shared.publication.encryption.Encryption
import org.readium.r2.shared.normalize

/**
 * Creates a [Publication] model from an EPUB package's document.
 *
 * @param displayOptions iBooks Display Options XML file to use as a fallback for the metadata.
 *        See https://github.com/readium/architecture/blob/master/streamer/parser/metadata.md#epub-2x-9
 */
internal class EpubAdapter(
    private val packageDocument: PackageDocument,
    private val navigationData: Map<String, List<Link>> = emptyMap(),
    private val encryptionData: Map<String, Encryption> = emptyMap(),
    displayOptions: Map<String, String> = emptyMap()
) {
    private val epubVersion = packageDocument.epubVersion
    private val links = packageDocument.metadata.links
    private val spine = packageDocument.spine
    private val manifest = packageDocument.manifest

    private val pubMetadata = PubMetadataAdapter(
        epubVersion,
        packageDocument.metadata.global,
        packageDocument.uniqueIdentifierId,
        spine.direction,
        displayOptions
    )

    private val itemMetadata = packageDocument.metadata.refine
        .mapValues { MetadataAdapter(epubVersion, it.value) }

    @Suppress("Unchecked_cast")
    private val itemById = manifest
        .filter { it.id != null }
        .associateBy(Item::id) as Map<String, Item>

    private val itemrefByIdref = spine.itemrefs.associateBy(Itemref::idref)

    fun toPublication(): Publication {
        // Compute metadata
        val metadata = pubMetadata.metadata()
        val metadataLinks = links.mapNotNull(::mapEpubLink)

        // Compute links
        val links = manifest.map { computeLink(it) }
        val readingOrderIds = computeReadingOrderIds(links)
        val (readingOrder, resources) = links.partition { it.title in readingOrderIds }

        // Compute toc and otherCollections
        val toc = navigationData["toc"].orEmpty()
        val otherCollections =
            navigationData.minus("toc").map { PublicationCollection(links = it.value, role = it.key) }

        // Build Publication object
        return Publication(
            metadata = metadata,
            links = metadataLinks,
            readingOrder = readingOrder,
            resources = resources,
            tableOfContents = toc,
            otherCollections = otherCollections
        ).apply {
            type = Publication.TYPE.EPUB
            version = epubVersion
        }
    }

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

    private fun computeReadingOrderIds(links: List<Link>): Set<String> {
        val ids: MutableSet<String> = mutableSetOf()
        for (l in links) {
            if (itemrefByIdref.containsKey(l.title) && (itemrefByIdref[l.title] as Itemref).linear) {
                ids.addAll(computeIdChain(l))
            }
        }
        return ids
    }

    private fun computeIdChain(link: Link): Set<String> {
        // The termination has already been checked while computing links
        val ids: MutableSet<String> = mutableSetOf(link.title as String)
        for (a in link.alternates) {
            ids.addAll(computeIdChain(a))
        }
        return ids
    }

    private fun computeLink(item: Item, fallbackChain: Set<String> = emptySet()): Link {
        val (rels, properties) = computePropertiesAndRels(item, itemrefByIdref[item.id])
        val alternates = computeAlternates(item, fallbackChain)
        val duration = itemMetadata[item.id]?.duration()

        return Link(
            title = item.id,
            href = normalize(packageDocument.path, item.href),
            type = item.mediaType,
            duration = duration,
            rels = rels,
            properties = properties,
            alternates = alternates
        )
    }

    private fun computePropertiesAndRels(item: Item, itemref: Itemref?): Pair<List<String>, Properties> {
        val properties: MutableMap<String, Any> = mutableMapOf()
        val rels: MutableList<String> = mutableListOf()
        val (manifestRels, contains, others) = parseItemProperties(item.properties)
        rels.addAll(manifestRels)
        if (contains.isNotEmpty()) {
            properties["contains"] = contains
        }
        if (others.isNotEmpty()) {
            properties["others"] = others
        }
        if (itemref != null) {
            properties.putAll(parseItemrefProperties(itemref.properties))
        }

        if (epubVersion < 3.0) {
            val coverId = pubMetadata.cover()
            if (coverId != null && item.id == coverId) rels.add("cover")
        }

        encryptionData[item.href]?.let {
            properties["encrypted"] = it
        }

        return Pair(rels, Properties(properties))
    }

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
