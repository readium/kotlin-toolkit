/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */


package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.Publication
import org.readium.r2.shared.MultilanguageString
import org.readium.r2.shared.Rendition
import org.readium.r2.shared.RenditionOverflow
import org.readium.r2.shared.RenditionLayout
import org.readium.r2.shared.RenditionOrientation
import org.readium.r2.shared.RenditionSpread
import org.readium.r2.shared.Metadata as SharedMetadata
import org.readium.r2.shared.Link as SharedLink
import org.readium.r2.shared.Contributor as SharedContributor
import org.readium.r2.shared.PageProgressionDirection as SharedDirection
import org.readium.r2.shared.Subject as SharedSubject

import org.readium.r2.streamer.parser.normalize

fun PackageDocument.toPublication() : Publication {
    val publication = Publication()
    publication.type = Publication.TYPE.EPUB
    publication.version = epubVersion
    publication.links.addAll(metadata.links.mapNotNull(::mapLink))
    publication.metadata = computeMetadata()
    val (readingOrder, resources) = computeResources()
    val (mediaOverlays, otherResources) = resources.partition { it.typeLink == "application/smil+xml" }
    publication.otherLinks.addAll(mediaOverlays)
    publication.resources.addAll(otherResources)
    publication.readingOrder.addAll(readingOrder)
    return publication
}

private fun mapLink(link: Link) : SharedLink? {
    val sharedLink =  SharedLink()
    sharedLink.href = link.href
    sharedLink.rel.addAll(link.rel)
    sharedLink.typeLink = link.mediaType
    if (link.rel.contains(DEFAULT_VOCAB.LINK.iri + "record")) {
        if (link.properties.contains(DEFAULT_VOCAB.LINK.iri + "onix"))
            sharedLink.properties.contains.add("onix")
        if (link.properties.contains(DEFAULT_VOCAB.LINK.iri + "xmp"))
            sharedLink.properties.contains.add("xmp")
    }
    return sharedLink
}

private fun PackageDocument.computeMetadata() : SharedMetadata {
    val generalMetadata = metadata.generalMetadata
    val pubMetadata = org.readium.r2.shared.Metadata()

    // Contributors
    addContributors(generalMetadata.publishers.map(::mapContributor), pubMetadata, "pbl")
    addContributors(generalMetadata.creators.map(::mapContributor), pubMetadata, "aut")
    addContributors(generalMetadata.contributors.map(::mapContributor), pubMetadata)

    // Titles
    pubMetadata.multilanguageTitle = getMaintitle()
    pubMetadata.multilanguageSubtitle = getSubtitle()

    // Miscellaneous
    pubMetadata.publicationDate = generalMetadata.date
    pubMetadata.identifier = generalMetadata.uniqueIdentifier
    pubMetadata.modified = generalMetadata.modified
    pubMetadata.languages.addAll(generalMetadata.languages)
    pubMetadata.duration = metadata.mediaMetadata.duration
    pubMetadata.rendition = mapRendition(metadata.renditionMetadata)
    pubMetadata.direction = mapDirection(spine.direction)
    pubMetadata.subjects.addAll(generalMetadata.subjects.map(::mapSubject))
    pubMetadata.description = generalMetadata.description
    pubMetadata.rights = generalMetadata.rights
    pubMetadata.source = generalMetadata.source

    return pubMetadata
}

private fun PackageDocument.getMaintitle() : MultilanguageString {
    val multilangString = MultilanguageString()
    val titles = metadata.generalMetadata.titles
    val main =  titles.firstOrNull { it.type == "main" } ?: titles.firstOrNull()
    multilangString.singleString = main?.value?.main
    main?.value?.alt?.let { multilangString.multiString.putAll(it) }
    if ("" in multilangString.multiString.keys && metadata.generalMetadata.languages.isNotEmpty()) {
        val v = multilangString.multiString.remove("") as String
        val l = metadata.generalMetadata.languages.first()
        multilangString.multiString[l] = v
    }
    return multilangString
}

private fun PackageDocument.getSubtitle() : MultilanguageString {
    val multilangString = MultilanguageString()
    val titles = metadata.generalMetadata.titles
    val sub =  titles.filter { it.type == "subtitle" }.sortedBy(Title::displaySeq).firstOrNull()
    multilangString.singleString = sub?.value?.main
    sub?.value?.alt?.let { multilangString.multiString.putAll(it) }
    return multilangString
}

private fun PackageDocument.mapContributor(contributor: Contributor) : SharedContributor {
    val pubContrib = org.readium.r2.shared.Contributor()
    val multilangString = MultilanguageString()
    multilangString.singleString = contributor.name.main
    multilangString.multiString = contributor.name.alt.toMutableMap()
    if ("" in multilangString.multiString.keys && metadata.generalMetadata.languages.isNotEmpty()) {
        val v = multilangString.multiString.remove("") as String
        val l = metadata.generalMetadata.languages.first()
        multilangString.multiString[l] = v
    }
    pubContrib.multilanguageName = multilangString
    pubContrib.sortAs = contributor.name.fileAs
    pubContrib.roles = contributor.roles.toMutableList()
    return pubContrib
}

private fun addContributors(contributors: List<SharedContributor>,
                            pubMetadata: SharedMetadata,
                            defaultRole: String? = null) {
    for (contributor in contributors) {
        if (contributor.roles.isEmpty()) {
            if (defaultRole == null)
                pubMetadata.contributors.add(contributor)
            else
                contributor.roles.add(defaultRole)
        }
        for (role in contributor.roles) {
            when (role) {
                "aut" -> pubMetadata.authors.add(contributor)
                "trl" -> pubMetadata.translators.add(contributor)
                "art" -> pubMetadata.artists.add(contributor)
                "edt" -> pubMetadata.editors.add(contributor)
                "ill" -> pubMetadata.illustrators.add(contributor)
                "clr" -> pubMetadata.colorists.add(contributor)
                "nrt" -> pubMetadata.narrators.add(contributor)
                "pbl" -> pubMetadata.publishers.add(contributor)
                else -> pubMetadata.contributors.add(contributor)
            }
        }
    }
}

private fun mapSubject(subject: Subject) : SharedSubject =
        SharedSubject().apply {
            name = subject.value
            scheme = subject.authority
            code = subject.term
        }

private fun mapRendition(renditionMetadata: RenditionMetadata) : Rendition {
    val rendition = Rendition()
    val (overflow, continuous) = when(renditionMetadata.flow) {
        RenditionMetadata.Flow.Auto ->  Pair(RenditionOverflow.Auto, false)
        RenditionMetadata.Flow.Paginated -> Pair(RenditionOverflow.Paginated, false)
        RenditionMetadata.Flow.Continuous -> Pair(RenditionOverflow.Scrolled, true)
        RenditionMetadata.Flow.Document -> Pair(RenditionOverflow.Scrolled, false)
    }
    rendition.flow = overflow
    rendition.continuous = continuous

    rendition.layout = when(renditionMetadata.layout) {
        RenditionMetadata.Layout.Reflowable -> RenditionLayout.Reflowable
        RenditionMetadata.Layout.PrePaginated -> RenditionLayout.Fixed
    }

    rendition.orientation = when(renditionMetadata.orientation) {
        RenditionMetadata.Orientation.Auto -> RenditionOrientation.Auto
        RenditionMetadata.Orientation.Landscape -> RenditionOrientation.Landscape
        RenditionMetadata.Orientation.Portait -> RenditionOrientation.Portrait
    }

    rendition.spread = when(renditionMetadata.spread) {
        RenditionMetadata.Spread.Auto -> RenditionSpread.Auto
        RenditionMetadata.Spread.None -> RenditionSpread.None
        RenditionMetadata.Spread.Landscape -> RenditionSpread.Landscape
        RenditionMetadata.Spread.Both -> RenditionSpread.Both
    }
    return rendition
}

private fun mapDirection(direction: Direction) : SharedDirection =
    when(direction) {
        Direction.Default -> SharedDirection.default
        Direction.Ltr -> SharedDirection.ltr
        Direction.Rtl -> SharedDirection.rtl
    }

private fun PackageDocument.computeResources() : Pair<List<SharedLink>, List<SharedLink>> {
    val itemById = manifest.associateBy(Item::id)
    val itemrefByIdref = spine.itemrefs.associateBy(Itemref::idref)
    val links = manifest.map { computeLink(it, itemById, itemrefByIdref) }
    val linkById = links.associateBy { it.title as String }
    if (epubVersion < 3.0) {
        metadata.oldMeta["cover"]?.let { linkById[it] }?.rel?.add("cover")
    }
    // items in resources collection might be used only as fallback or not
    // so there might me duplicated elements in readingOrder alternate links and resources
    linkById.values.forEach { // ensure id unicity for links with fallback
        val fallbackId = itemById[it.title as String]?.fallback
        val fallbackLink = linkById[fallbackId]
        if (fallbackLink != null)
        it.alternate.add(fallbackLink) }
    links.forEach{ checkFallbackChain(it) } // ensure there is no cycle in fallback chains using id unicity
    return links.partition {
        itemrefByIdref.containsKey(it.title) && (itemrefByIdref[it.title] as Itemref).linear  }
}

private fun PackageDocument.computeLink(item: Item, itemById: Map<String, Item>, itemrefByIdref: Map<String, Itemref>) : SharedLink {
    val link = SharedLink().apply {
        title = item.id
        href = normalize(path, item.href)
        typeLink = item.mediaType
        duration = metadata.mediaMetadata.durationById[item.id]
    }

    parseItemProperties(item.properties, link)

    val itemref = itemrefByIdref[item.id]
    if (itemref != null) {
        parseItemrefProperties(itemref.properties, link)
    }
    if (item.mediaOverlay != null) {
        link.properties.mediaOverlay = normalize(path, itemById[item.mediaOverlay]?.href )
    }
    return link
}

private fun checkFallbackChain(link: SharedLink, alreadySeen: MutableSet<String> = mutableSetOf()) {
    alreadySeen.add(link.title as String)
    link.alternate = link.alternate.filterNot { alreadySeen.contains(it.title) }.toMutableList()
    link.alternate.forEach { checkFallbackChain(it) }
}

private fun parseItemProperties(properties: List<String>, link: SharedLink) {
    for (property in properties) {
        when (property) {
            DEFAULT_VOCAB.ITEM.iri + "scripted" -> "js"
            DEFAULT_VOCAB.ITEM.iri + "mathml" -> "onix-record"
            DEFAULT_VOCAB.ITEM.iri + "svg" -> "svg"
            DEFAULT_VOCAB.ITEM.iri + "xmp-record" -> "xmp"
            DEFAULT_VOCAB.ITEM.iri + "remote-resources" -> "remote-resources"
            else -> null
        }?.let { link.properties.contains.add(it) }
        when (property) {
            "nav" -> link.rel.add("contents")
            "cover-image" -> link.rel.add("cover")
        }
    }
}

private fun parseItemrefProperties(properties: List<String>, link: SharedLink) {
    for (property in properties) {
        //  Page
        when (property) {
            PACKAGE_RESERVED_PREFIXES["rendition"] + "page-spread-center" -> "center"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "page-spread-left",
            DEFAULT_VOCAB.ITEMREF.iri + "page-spread-left" -> "left"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "page-spread-right",
            DEFAULT_VOCAB.ITEMREF.iri + "page-spread-left" -> "right"
            else -> null
        }?.let { link.properties.page = it }
        //  Spread
        when (property) {
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread-node" -> "none"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread-auto" -> "auto"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread-landscape" -> "landscape"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread-portrait" -> "both"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread-both" -> "both"
            else -> null
        }?.let { link.properties.spread = it }
        //  Layout
        when (property) {
            PACKAGE_RESERVED_PREFIXES["rendition"] + "layout-reflowable" -> "reflowable"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "layout-pre-paginated" -> "fixed"
            else -> null
        }?.let { link.properties.layout = it }
        //  Orientation
        when (property) {
            PACKAGE_RESERVED_PREFIXES["rendition"] + "orientation-auto" -> "auto"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "orientation-landscape" -> "landscape"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "orientation-portrait" -> "portrait"
            else -> null
        }?.let { link.properties.orientation = it }
        //  Overflow
        when (property) {
            PACKAGE_RESERVED_PREFIXES["rendition"] + "flow-auto" -> "auto"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "flow-paginated" -> "paginated"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "flow-scrolled-continuous" -> "scrolled"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "flow-scrolled-doc" -> "scrolled"
            else -> null
        }?.let { link.properties.overflow = it }
    }
}
