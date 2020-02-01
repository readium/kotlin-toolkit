/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */


package org.readium.r2.streamer.parser.epub

import org.joda.time.DateTime
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.Metadata as SharedMetadata
import org.readium.r2.shared.publication.Link as SharedLink
import org.readium.r2.shared.publication.Contributor as SharedContributor
import org.readium.r2.shared.publication.ReadingProgression as SharedDirection
import org.readium.r2.shared.publication.Subject as SharedSubject
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.PublicationCollection
import org.readium.r2.shared.publication.encryption.Encryption

import org.readium.r2.streamer.parser.normalize
import java.lang.Exception

internal fun Epub.toPublication() : Publication {
    // Compute links
    val itemById = packageDocument.manifest.associateBy(Item::id)
    val itemrefByIdref = packageDocument.spine.itemrefs.associateBy(Itemref::idref)
    val links = packageDocument.manifest.map { computeLink(it, itemById, itemrefByIdref) }
    val (readingOrder, resources) = links.partition {
        itemrefByIdref.containsKey(it.title) && (itemrefByIdref[it.title] as Itemref).linear
    }

    // Compute toc and otherCollections
    val toc = navigationData?.let { when(navigationData) {
        is Ncx -> navigationData.toc
        is NavigationDocument -> navigationData.toc
    }} ?: emptyList()
    val pageList = navigationData?.let { when(navigationData) {
        is Ncx -> navigationData.pageList
        is NavigationDocument -> navigationData.pageList
    }}
    val navigationDocument = navigationData as? NavigationDocument
    val otherCollections = listOf(
            pageList?.let { PublicationCollection(links = it, role= "page-list") },
            navigationDocument?.landmarks?.let { PublicationCollection(links = it, role = "landmarks") },
            navigationDocument?.loa?.let { PublicationCollection(links = it, role = "loa") },
            navigationDocument?.loi?.let { PublicationCollection(links = it, role = "loi") },
            navigationDocument?.lot?.let { PublicationCollection(links = it, role = "lot") },
            navigationDocument?.lov?.let { PublicationCollection(links = it, role = "lov") }
    ).filterNotNull()

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

private fun mapLink(link: Link) : SharedLink? {
    val contains: MutableList<String> = mutableListOf()
    if (link.rel.contains(DEFAULT_VOCAB.LINK.iri + "record")) {
        if (link.properties.contains(DEFAULT_VOCAB.LINK.iri + "onix"))
            contains.add("onix")
        if (link.properties.contains(DEFAULT_VOCAB.LINK.iri + "xmp"))
            contains.add("xmp")
    }
    return SharedLink(
            href = link.href,
            type = link.mediaType,
            rels = link.rel,
            properties = Properties(mapOf("contains" to contains))
    )
}

private data class ContributorsByRole(
        val authors: MutableList<SharedContributor> = mutableListOf(),
        val translators: MutableList<SharedContributor> = mutableListOf(),
        val editors: MutableList<SharedContributor> = mutableListOf(),
        val artists: MutableList<SharedContributor> = mutableListOf(),
        val illustrators: MutableList<SharedContributor> = mutableListOf(),
        val letterers: MutableList<SharedContributor> = mutableListOf(),
        val pencilers: MutableList<SharedContributor> = mutableListOf(),
        val colorists: MutableList<SharedContributor> = mutableListOf(),
        val inkers: MutableList<SharedContributor> = mutableListOf(),
        val narrators: MutableList<SharedContributor> = mutableListOf(),
        val publishers: MutableList<SharedContributor> = mutableListOf(),
        val imprints: MutableList<SharedContributor> = mutableListOf(),
        val others: MutableList<SharedContributor> = mutableListOf()
)

private fun Epub.computeMetadata() : SharedMetadata {
    val generalMetadata = packageDocument.metadata.generalMetadata

    // Contributors
    val contributorsByRole = ContributorsByRole()
    addContributors(generalMetadata.publishers.map { mapContributor(it, "pbl") }, contributorsByRole)
    addContributors(generalMetadata.creators.map { mapContributor(it, "aut") }, contributorsByRole)
    addContributors(generalMetadata.contributors.map { mapContributor(it) }, contributorsByRole)

    // Miscellaneous
    val published =   try {
        DateTime(generalMetadata.date).toDate()
    } catch(e: Exception) {
        null
    }

    // Other Metadata
    val otherMetadata: MutableMap<String, Any> = mutableMapOf()
    otherMetadata["presentation"] =  mapRendition(packageDocument.metadata.renditionMetadata).toJSON().toMap()
    generalMetadata.rights?.let { otherMetadata["http://purl.org/dc/elements/1.1/rights"] = it }
    generalMetadata.source?.let { otherMetadata["http://purl.org/dc/elements/1.1/source"] = it }

    return org.readium.r2.shared.publication.Metadata(
            identifier = generalMetadata.uniqueIdentifier,
            modified = generalMetadata.modified,
            published = published,
            languages = generalMetadata.languages,
            localizedTitle = getMaintitle(),
            localizedSubtitle = getSubtitle(),
            duration = packageDocument.metadata.mediaMetadata.duration,
            subjects = generalMetadata.subjects.map(::mapSubject),
            description = generalMetadata.description,
            readingProgression = mapDirection(packageDocument.spine.direction),
            otherMetadata = otherMetadata,

            authors = contributorsByRole.authors,
            translators = contributorsByRole.translators,
            editors = contributorsByRole.editors,
            artists = contributorsByRole.artists,
            illustrators = contributorsByRole.illustrators,
            letterers = contributorsByRole.letterers,
            pencilers = contributorsByRole.pencilers,
            colorists = contributorsByRole.colorists,
            inkers = contributorsByRole.inkers,
            narrators = contributorsByRole.narrators,
            publishers = contributorsByRole.publishers,
            contributors = contributorsByRole.others
    )
}

private fun Epub.getMaintitle() : LocalizedString {
    val metadata = packageDocument.metadata.generalMetadata
    val titles = metadata.titles
    val main =  titles.firstOrNull { it.type == "main" } ?: titles.firstOrNull()
    val translations = main?.value?.alt?.toMutableMap() ?: mutableMapOf()

    if ("" in translations.keys && metadata.languages.isNotEmpty()) {
        val v = translations.remove("") as String
        val l = metadata.languages.first()
        translations[l] = v
    }
    return LocalizedString(translations)
}

private fun Epub.getSubtitle() : LocalizedString {
    val metadata = packageDocument.metadata.generalMetadata
    val titles = metadata.titles
    val sub =  titles.filter { it.type == "subtitle" }.sortedBy(Title::displaySeq).firstOrNull()
    val translations = sub?.value?.alt?.toMutableMap() ?: mutableMapOf()

    if ("" in translations.keys && metadata.languages.isNotEmpty()) {
        val v = translations.remove("") as String
        val l = metadata.languages.first()
        translations[l] = v
    }
    return LocalizedString(translations)
}

private fun Epub.mapContributor(contributor: Contributor, defaultRole: String? = null) : SharedContributor {
    val metadata = packageDocument.metadata.generalMetadata
    val translations = contributor.name.alt.toMutableMap()
    if ("" in translations.keys && metadata.languages.isNotEmpty()) {
        val v = translations.remove("") as String
        val l = metadata.languages.first()
        translations[l] = v
    }
    val roles = contributor.roles.toMutableSet()
    if (roles.isEmpty() && defaultRole != null) roles.add(defaultRole)

    return SharedContributor(
            localizedName = LocalizedString(translations),
            sortAs = contributor.name.fileAs,
            roles = roles
    )
}

private fun addContributors(contributors: List<SharedContributor>, byRole: ContributorsByRole) {
    for (contributor in contributors) {
        for (role in contributor.roles) {
            when (role) {
                "aut" -> byRole.authors.add(contributor)
                "trl" -> byRole.translators.add(contributor)
                "art" -> byRole.artists.add(contributor)
                "edt" -> byRole.editors.add(contributor)
                "ill" -> byRole.illustrators.add(contributor)
                "clr" -> byRole.colorists.add(contributor)
                "nrt" -> byRole.narrators.add(contributor)
                "pbl" -> byRole.publishers.add(contributor)
                else -> byRole.others.add(contributor)
            }
        }
    }
}

private fun mapSubject(subject: Subject) : SharedSubject =
        SharedSubject(
            localizedName = LocalizedString(subject.value),
            scheme = subject.authority,
            code = subject.term
        )

private fun mapRendition(renditionMetadata: RenditionMetadata) : Presentation {
    val (overflow, continuous) = when(renditionMetadata.flow) {
        RenditionMetadata.Flow.Auto ->  Pair(Presentation.Overflow.AUTO, false)
        RenditionMetadata.Flow.Paginated -> Pair(Presentation.Overflow.PAGINATED, false)
        RenditionMetadata.Flow.Continuous -> Pair(Presentation.Overflow.SCROLLED, true)
        RenditionMetadata.Flow.Document -> Pair(Presentation.Overflow.SCROLLED, false)
    }

    val layout = when(renditionMetadata.layout) {
        RenditionMetadata.Layout.Reflowable -> EpubLayout.REFLOWABLE
        RenditionMetadata.Layout.PrePaginated -> EpubLayout.FIXED
    }

    val orientation = when(renditionMetadata.orientation) {
        RenditionMetadata.Orientation.Auto -> Presentation.Orientation.AUTO
        RenditionMetadata.Orientation.Landscape -> Presentation.Orientation.LANDSCAPE
        RenditionMetadata.Orientation.Portait -> Presentation.Orientation.PORTRAIT
    }

    val spread = when(renditionMetadata.spread) {
        RenditionMetadata.Spread.Auto -> Presentation.Spread.AUTO
        RenditionMetadata.Spread.None -> Presentation.Spread.NONE
        RenditionMetadata.Spread.Landscape -> Presentation.Spread.LANDSCAPE
        RenditionMetadata.Spread.Both -> Presentation.Spread.BOTH
    }
    return Presentation(
            overflow = overflow,
            continuous = continuous,
            layout = layout,
            orientation = orientation,
            spread = spread
    )
}

private fun mapDirection(direction: Direction) : SharedDirection =
    when(direction) {
        Direction.Default -> SharedDirection.AUTO
        Direction.Ltr -> SharedDirection.LTR
        Direction.Rtl -> SharedDirection.RTL
    }

private fun Epub.computeLink(
        item: Item,
        itemById: Map<String, Item>,
        itemrefByIdref: Map<String, Itemref>,
        fallbackChain: Set<String> = emptySet()) : SharedLink {

    val (rels, properties) = computePropertiesAndRels(item, itemrefByIdref[item.id])
    val alternates = computeAlternates(item, itemById, itemrefByIdref, fallbackChain)

    return SharedLink(
            title = item.id,
            href = normalize(packageDocument.path, item.href),
            type = item.mediaType,
            duration = packageDocument.metadata.mediaMetadata.durationById[item.id],
            rels = rels,
            properties = properties,
            alternates = alternates
    )
}

private fun Epub.computePropertiesAndRels(item: Item, itemref: Itemref?) : Pair<List<String>, Properties> {
    val rels: MutableList<String> = mutableListOf()
    val properties: MutableMap<String, Any> = mutableMapOf()
    val contains: MutableList<String> = mutableListOf()
    parseItemProperties(item.properties, contains, rels)
    if (contains.isNotEmpty()) {
        properties["contains"] = contains
    }
    if (itemref != null) {
        parseItemrefProperties(itemref.properties, properties)
    }

    if (packageDocument.epubVersion < 3.0) {
        val coverId = packageDocument.metadata.oldMeta["cover"]
        if (coverId != null && item.id == coverId) rels.add("cover")
    }

    val encryption: Encryption? = encryptionData?.let { it[item.href] }
    if (encryption != null) {
        properties["encrypted"] = encryption
    }

    return Pair(rels, Properties(properties))
}

private fun Epub.computeAlternates(
        item: Item,
        itemById: Map<String, Item>,
        itemrefByIdref: Map<String, Itemref>,
        fallbackChain: Set<String>) : List<SharedLink> {

    val fallback = item.fallback?.let { id ->
        if (id in fallbackChain) null else
            itemById[id]?.let {
                computeLink(it, itemById, itemrefByIdref, fallbackChain.plus(item.id) ) }
    }
    val mediaOverlays = item.mediaOverlay?.let { id ->
        itemById[id]?.let {
            computeLink(it, itemById, itemrefByIdref) }
    }
    return listOf(fallback, mediaOverlays).filterNotNull()
}

private fun parseItemProperties(properties: List<String>,
                                linkContains: MutableList<String>,
                                linkRels: MutableList<String>) {

    for (property in properties) {
        when (property) {
            DEFAULT_VOCAB.ITEM.iri + "scripted" -> "js"
            DEFAULT_VOCAB.ITEM.iri + "mathml" -> "onix-record"
            DEFAULT_VOCAB.ITEM.iri + "svg" -> "svg"
            DEFAULT_VOCAB.ITEM.iri + "xmp-record" -> "xmp"
            DEFAULT_VOCAB.ITEM.iri + "remote-resources" -> "remote-resources"
            else -> null
        }?.let { linkContains.add(it) }
        when (property) {
            "nav" -> linkRels.add("contents")
            "cover-image" -> linkRels.add("cover")
        }
    }
}

private fun parseItemrefProperties(properties: List<String>, linkProperties: MutableMap<String, Any>) {
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
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread-portrait" -> "both"
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
            PACKAGE_RESERVED_PREFIXES["rendition"] + "flow-scrolled-continuous" -> "scrolled"
            PACKAGE_RESERVED_PREFIXES["rendition"] + "flow-scrolled-doc" -> "scrolled"
            else -> null
        }?.let { linkProperties["overflow"] = it }
    }
}
