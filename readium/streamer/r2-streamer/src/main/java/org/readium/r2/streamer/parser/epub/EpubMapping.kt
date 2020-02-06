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
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.Metadata as SharedMetadata
import org.readium.r2.shared.publication.Link as SharedLink
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.PublicationCollection
import org.readium.r2.shared.publication.encryption.Encryption

import org.readium.r2.streamer.parser.normalize
import java.lang.Exception

internal fun Epub.toPublication() : Publication {
    // Compute links
    @Suppress("UNCHECKED_CAST")
    val itemById = packageDocument.manifest.filter { it.id != null }.associateBy(Item::id) as Map<String, Item>
    val itemrefByIdref = packageDocument.spine.itemrefs.associateBy(Itemref::idref)
    val links = packageDocument.manifest.map { computeLink(it, itemById, itemrefByIdref) }
    val readingOrderIds = computeReadingOrderIds(links, itemrefByIdref)
    val (readingOrder, resources) = links.partition { it.title in readingOrderIds }

    // Compute toc and otherCollections
    val toc = navigationData?.get("toc").orEmpty()
    val otherCollections = navigationData?.minus("toc")
            ?.map {PublicationCollection(links = it.value, role= it.key) }
            .orEmpty()

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

private fun computeReadingOrderIds(links: List<SharedLink>, itemrefByIdref: Map<String, Itemref>) : Set<String> {
    val ids: MutableSet<String> = mutableSetOf()
    for (l in links) {
        if (itemrefByIdref.containsKey(l.title) && (itemrefByIdref[l.title] as Itemref).linear) {
            ids.addAll(computeIdChain(l))
        }
    }
    return ids
}

private fun computeIdChain(link: SharedLink) : Set<String> {
    // The termination has already been checked while computing links
    val ids: MutableSet<String> = mutableSetOf( link.title as String )
    for (a in link.alternates) {
        ids.addAll(computeIdChain(a))
    }
    return ids
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
        val authors: MutableList<Contributor> = mutableListOf(),
        val translators: MutableList<Contributor> = mutableListOf(),
        val editors: MutableList<Contributor> = mutableListOf(),
        val artists: MutableList<Contributor> = mutableListOf(),
        val illustrators: MutableList<Contributor> = mutableListOf(),
        val letterers: MutableList<Contributor> = mutableListOf(),
        val pencilers: MutableList<Contributor> = mutableListOf(),
        val colorists: MutableList<Contributor> = mutableListOf(),
        val inkers: MutableList<Contributor> = mutableListOf(),
        val narrators: MutableList<Contributor> = mutableListOf(),
        val publishers: MutableList<Contributor> = mutableListOf(),
        val imprints: MutableList<Contributor> = mutableListOf(),
        val others: MutableList<Contributor> = mutableListOf()
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
    val (title, sortAs) = getMaintitle()
    val mediaNarrators = packageDocument.metadata.mediaMetadata.narrators.map { mapContributor(it, "nrt") }

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
            localizedTitle = title ?: LocalizedString(),
            sortAs = sortAs,
            localizedSubtitle = getSubtitle(),
            duration = packageDocument.metadata.mediaMetadata.duration,
            subjects = mapSubjects(generalMetadata.subjects),
            description = generalMetadata.description,
            readingProgression = packageDocument.spine.direction,
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
            narrators = contributorsByRole.narrators + mediaNarrators,
            publishers = contributorsByRole.publishers,
            contributors = contributorsByRole.others
    )
}

private fun mapLocalizedString(lstring: LocalizedString, languages: List<String>) : LocalizedString {
    return lstring.mapLanguages {
        if (it.key.isNullOrEmpty()) {
            if (languages.isEmpty())
                null
            else languages.first()
        } else it.key
    }
}

private fun Epub.getMaintitle() : Pair<LocalizedString?, String?> {
    val metadata = packageDocument.metadata.generalMetadata
    val titles = metadata.titles
    val main =  titles.firstOrNull { it.type == "main" } ?: titles.firstOrNull()
    val lstring = main?.value?.let { mapLocalizedString( it , metadata.languages) }
    val sortAs = if (packageDocument.epubVersion >= 3.0) main?.fileAs else packageDocument.metadata.oldMeta["calibre:title_sort"]
    return Pair(lstring, sortAs)
}

private fun Epub.getSubtitle() : LocalizedString? {
    val metadata = packageDocument.metadata.generalMetadata
    val titles = metadata.titles
    val sub =  titles.filter { it.type == "subtitle" }.sortedBy(Title::displaySeq).firstOrNull()
    val lstring = sub?.value?.let { mapLocalizedString( it , metadata.languages) }
    return lstring
}

private fun Epub.mapSubjects(subjects: List<Subject>) : List<Subject> {
    val languages = packageDocument.metadata.generalMetadata.languages
    return if (subjects.size == 1 && subjects.first().run {
                localizedName.translations.size == 1 && code == null && scheme == null && sortAs == null}) {
        with (subjects.first()) {
            val lang = localizedName.translations.keys.first()
            localizedName.translations.values.first().string.split(",", ";")
                    .map(String::trim).filter(String::isNotEmpty)
                    .map {
                        val newName = LocalizedString.fromStrings( mapOf (lang to it))
                        Subject(localizedName = mapLocalizedString(newName, languages))
                    }
        }
    } else {
        subjects.map {
            val localizedName = mapLocalizedString(it.localizedName, languages)
            it.copy(localizedName = localizedName)
        }
    }
}

private fun Epub.mapContributor(contributor: Contributor, defaultRole: String? = null) : Contributor {
    val metadata = packageDocument.metadata.generalMetadata
    val lstring = mapLocalizedString(contributor.localizedName, metadata.languages)
    val roles = contributor.roles.toMutableSet()
    if (roles.isEmpty() && defaultRole != null) roles.add(defaultRole)

    return Contributor(
            localizedName = lstring,
            sortAs = contributor.sortAs,
            roles = roles
    )
}

private fun addContributors(contributors: List<Contributor>, byRole: ContributorsByRole) {
    for (contributor in contributors) {
        if (contributor.roles.isEmpty())
            byRole.others.add(contributor)
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
                val updatedChain = if (item.id != null) fallbackChain + item.id else fallbackChain
                computeLink(it, itemById, itemrefByIdref, updatedChain) }
    }
    val mediaOverlays = item.mediaOverlay?.let { id ->
        itemById[id]?.let {
            computeLink(it, itemById, itemrefByIdref) }
    }
    return listOfNotNull(fallback, mediaOverlays)
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
