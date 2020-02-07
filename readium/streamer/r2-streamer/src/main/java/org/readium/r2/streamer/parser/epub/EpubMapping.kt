/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.PublicationCollection
import org.readium.r2.streamer.parser.normalize

internal fun Epub.toPublication() : Publication {
    // Compute links
    @Suppress("Unchecked_cast")
    val itemById = packageDocument.manifest.filter { it.id != null }.associateBy(Item::id) as Map<String, Item>
    val itemrefByIdref = packageDocument.spine.itemrefs.associateBy(Itemref::idref)
    val links = packageDocument.manifest.map { computeLink(it, itemById, itemrefByIdref) }
    val readingOrderIds = computeReadingOrderIds(links, itemrefByIdref)
    val (readingOrder, resources) = links.partition { it.title in readingOrderIds }

    // Compute toc and otherCollections
    val toc = navigationData["toc"].orEmpty()
    val otherCollections = navigationData.minus("toc").map {PublicationCollection(links = it.value, role= it.key) }

    // Build Publication object
    return Publication(
            metadata = computeMetadata(),
            links = packageDocument.metadata.links.mapNotNull(::mapLink),
            readingOrder = readingOrder,
            resources = resources,
            tableOfContents = toc,
            otherCollections = otherCollections
        ).apply {
            type = Publication.TYPE.EPUB
            version = packageDocument.epubVersion
        }
}

private fun mapLink(link: EpubLink) : Link? {
    val contains: MutableList<String> = mutableListOf()
    if (link.rel.contains(DEFAULT_VOCAB.LINK.iri + "record")) {
        if (link.properties.contains(DEFAULT_VOCAB.LINK.iri + "onix"))
            contains.add("onix")
        if (link.properties.contains(DEFAULT_VOCAB.LINK.iri + "xmp"))
            contains.add("xmp")
    }
    return Link(
            href = link.href,
            type = link.mediaType,
            rels = link.rel,
            properties = Properties(mapOf("contains" to contains))
    )
}

private fun computeReadingOrderIds(links: List<Link>, itemrefByIdref: Map<String, Itemref>) : Set<String> {
    val ids: MutableSet<String> = mutableSetOf()
    for (l in links) {
        if (itemrefByIdref.containsKey(l.title) && (itemrefByIdref[l.title] as Itemref).linear) {
            ids.addAll(computeIdChain(l))
        }
    }
    return ids
}

private fun computeIdChain(link: Link) : Set<String> {
    // The termination has already been checked while computing links
    val ids: MutableSet<String> = mutableSetOf( link.title as String )
    for (a in link.alternates) {
        ids.addAll(computeIdChain(a))
    }
    return ids
}

private fun Epub.computeLink(
        item: Item,
        itemById: Map<String, Item>,
        itemrefByIdref: Map<String, Itemref>,
        fallbackChain: Set<String> = emptySet()) : Link {

    val (rels, properties) = computePropertiesAndRels(item, itemrefByIdref[item.id])
    val alternates = computeAlternates(item, itemById, itemrefByIdref, fallbackChain)
    val duration = packageDocument.metadata.metas[item.id]
            ?.firstOrNull { it.property == PACKAGE_RESERVED_PREFIXES["media"] + "duration" }
            ?.value?.let { ClockValueParser.parse(it) }

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

private fun Epub.computePropertiesAndRels(item: Item, itemref: Itemref?) : Pair<List<String>, Properties> {
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

    if (packageDocument.epubVersion < 3.0) {
        val coverId = packageDocument.metadata.metas[null]?.firstOrNull { it.property == "cover" }?.value
        if (coverId != null && item.id == coverId) rels.add("cover")
    }

    encryptionData[item.href]?.let {
        properties["encrypted"] = it
    }

    return Pair(rels, Properties(properties))
}

private fun Epub.computeAlternates(
        item: Item,
        itemById: Map<String, Item>,
        itemrefByIdref: Map<String, Itemref>,
        fallbackChain: Set<String>) : List<Link> {

    val fallback = item.fallback?.let { id ->
        if (id in fallbackChain) null else
            itemById[id]?.let {
                val updatedChain = if (item.id != null) fallbackChain + item.id else fallbackChain
                computeLink(it, itemById, itemrefByIdref, updatedChain) }
    }
    val mediaOverlays = item.mediaOverlay?.let { id ->
        itemById[id]?.let {
            computeLink(it, itemById, itemrefByIdref) }
    }
    return listOfNotNull(fallback, mediaOverlays)
}

private fun parseItemProperties(properties: List<String>) : Triple<List<String>, List<String>, List<String>> {
    val rels: MutableList<String> = mutableListOf()
    val contains: MutableList<String> = mutableListOf()
    val others: MutableList<String> = mutableListOf()
    for (property in properties) {
        when (property) {
            DEFAULT_VOCAB.ITEM.iri + "scripted" -> contains.add("js")
            DEFAULT_VOCAB.ITEM.iri + "mathml" -> contains.add("mathml")
            DEFAULT_VOCAB.ITEM.iri + "svg" -> contains.add("svg")
            DEFAULT_VOCAB.ITEM.iri + "xmp-record" -> contains.add("xmp")
            DEFAULT_VOCAB.ITEM.iri + "remote-resources" -> contains.add("remote-resources")
            DEFAULT_VOCAB.ITEM.iri + "nav" -> rels.add("contents")
            DEFAULT_VOCAB.ITEM.iri + "cover-image" -> rels.add("cover")
            else -> others.add(property)
        }
    }
    return Triple(rels, contains, others)
}

private fun parseItemrefProperties(properties: List<String>) : Map<String, String> {
    val linkProperties: MutableMap<String, String> = mutableMapOf()
    for (property in properties) {
        //  Page
        when (property) {
            PACKAGE_RESERVED_PREFIXES["rendition"] + "page-spread-center" -> "center"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "page-spread-left",
            DEFAULT_VOCAB.ITEMREF.iri + "page-spread-left" -> "left"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "page-spread-right",
            DEFAULT_VOCAB.ITEMREF.iri + "page-spread-left" -> "right"
            else -> null
        }?.let { linkProperties["page"] = it }
        //  Spread
        when (property) {
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread-node" -> "none"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread-auto" -> "auto"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread-landscape" -> "landscape"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread-portrait",
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread-both" -> "both"
            else -> null
        }?.let { linkProperties["spread"] = it }
        //  Layout
        when (property) {
            PACKAGE_RESERVED_PREFIXES["rendition"] + "layout-reflowable" -> "reflowable"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "layout-pre-paginated" -> "fixed"
            else -> null
        }?.let { linkProperties["layout"] = it }
        //  Orientation
        when (property) {
            PACKAGE_RESERVED_PREFIXES["rendition"] + "orientation-auto" -> "auto"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "orientation-landscape" -> "landscape"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "orientation-portrait" -> "portrait"
            else -> null
        }?.let { linkProperties["orientation"] = it }
        //  Overflow
        when (property) {
            PACKAGE_RESERVED_PREFIXES["rendition"] + "flow-auto" -> "auto"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "flow-paginated" -> "paginated"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "flow-scrolled-continuous",
            PACKAGE_RESERVED_PREFIXES["rendition"] + "flow-scrolled-doc" -> "scrolled"
            else -> null
        }?.let { linkProperties["overflow"] = it }
    }
    return linkProperties
}

