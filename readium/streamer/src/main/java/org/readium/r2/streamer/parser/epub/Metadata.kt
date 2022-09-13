/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.extensions.iso8601ToDate
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.Collection
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.util.Href

internal data class Title(
    val value: LocalizedString,
    val fileAs: LocalizedString? = null,
    val type: String? = null,
    val displaySeq: Int? = null
)

internal data class EpubMetadata(
    val globalMetadata: List<MetadataItem>,
    val otherMetadata: Map<String, List<MetadataItem>>
)

internal class MetadataParser(private val epubVersion: Double, private val prefixMap: Map<String, String>) {

    fun parse(document: ElementNode, filePath: String): EpubMetadata? {
        val metadata = document.getFirst("metadata", Namespaces.OPF)
            ?: return null
        val items = parseElements(metadata, filePath)
        val (globalItems, refineItems) = resolveItemsHierarchy(items).partition { it.refines == null }
        @Suppress("unchecked_cast")
        val refinesByRefinee = refineItems.groupBy(MetadataItem::refines) as Map<String, List<MetadataItem>>
        return EpubMetadata(globalItems, refinesByRefinee)
    }

    private fun parseElements(metadataElement: ElementNode, filePath: String): List<MetadataItem> =
        metadataElement.getAll().mapNotNull { e ->
            when {
                e.namespace == Namespaces.DC ->
                    parseDcElement(e)
                e.namespace == Namespaces.OPF && e.name == "meta" ->
                    parseMetaElement(e)
                e.namespace == Namespaces.OPF && e.name == "link" ->
                    parseLinkElement(e, filePath)
                else -> null
            }
        }

    private fun parseLinkElement(element: ElementNode, filePath: String): MetadataItem.Link? {
        val href = element.getAttr("href") ?: return null
        val relAttr = element.getAttr("rel").orEmpty()
        val rel = parseProperties(relAttr).map { resolveProperty(it, prefixMap, DEFAULT_VOCAB.LINK) }
        val propAttr = element.getAttr("properties").orEmpty()
        val properties = parseProperties(propAttr).map { resolveProperty(it, prefixMap, DEFAULT_VOCAB.LINK) }
        val mediaType = element.getAttr("media-type")
        val refines = element.getAttr("refines")?.removePrefix("#")
        return MetadataItem.Link(
            id = element.id,
            refines = refines,
            href = Href(href, baseHref = filePath).string,
            rels = rel.toSet(),
            mediaType = mediaType,
            properties = properties
        )
    }

    private fun parseMetaElement(element: ElementNode): MetadataItem.Meta? {
        return if (element.getAttr("property") == null) {
            val name = element.getAttr("name")?.trim()?.ifEmpty { null }
                ?: return null
            val content = element.getAttr("content")?.trim()?.ifEmpty { null }
                ?: return null
            val resolvedName = resolveProperty(name, prefixMap)
            MetadataItem.Meta(
                id = element.id,
                refines = null,
                property = resolvedName,
                value = content,
                lang = element.lang
            )
        } else {
            val propName = element.getAttr("property")?.trim()?.ifEmpty { null }
                ?: return null
            val propValue = element.text?.trim()?.ifEmpty { null }
                ?: return null
            val resolvedProp = resolveProperty(propName, prefixMap, DEFAULT_VOCAB.META)
            val resolvedScheme =
                element.getAttr("scheme")?.trim()?.ifEmpty { null }?.let { resolveProperty(it, prefixMap) }
            val refines = element.getAttr("refines")?.removePrefix("#")
            MetadataItem.Meta(
                id = element.id,
                refines = refines,
                property = resolvedProp,
                value = propValue,
                lang = element.lang,
                scheme = resolvedScheme
            )
        }
    }

    private fun parseDcElement(element: ElementNode): MetadataItem.Meta? {
        val propValue = element.text?.trim()?.ifEmpty { null } ?: return null
        val propName = Vocabularies.DCTERMS + element.name
        return when (element.name) {
            "creator", "contributor", "publisher" -> contributorWithLegacyAttr(element, propName, propValue)
            "date" -> dateWithLegacyAttr(element, propName, propValue)
            else -> MetadataItem.Meta(
                id = element.id,
                property = propName,
                value = propValue,
                lang = element.lang
            )
        }
    }

    private fun contributorWithLegacyAttr(element: ElementNode, name: String, value: String): MetadataItem.Meta {
        val fileAs = element.getAttrNs("file-as", Namespaces.OPF)?.let {
            MetadataItem.Meta(
                property = Vocabularies.META + "file-as",
                value = it,
                lang = element.lang,
                id = element.id
            )
        }
        val role = element.getAttrNs("role", Namespaces.OPF)?.let {
            MetadataItem.Meta(
                property = Vocabularies.META + "role",
                value = it,
                lang = element.lang,
                id = element.id
            )
        }
        return MetadataItem.Meta(
            id = element.id,
            property = name,
            value = value,
            lang = element.lang,
            children = listOfNotNull(fileAs, role)
        )
    }

    private fun dateWithLegacyAttr(element: ElementNode, name: String, value: String): MetadataItem.Meta {
        val eventAttr = element.getAttrNs("event", Namespaces.OPF)
        val propName = if (eventAttr == "modification") Vocabularies.DCTERMS + "modified" else name
        return MetadataItem.Meta(
            id = element.id,
            property = propName,
            value = value,
            lang = element.lang
        )
    }

    private fun resolveItemsHierarchy(items: List<MetadataItem>): List<MetadataItem> {
        val metadataIds = items.mapNotNull { it.id }
        val rootExpr = items.filter { it.refines == null || it.refines !in metadataIds }
        @Suppress("Unchecked_cast")
        val exprByRefines = items.groupBy(MetadataItem::refines) as Map<String, List<MetadataItem.Meta>>
        return rootExpr.map { computeMetadataItem(it, exprByRefines, emptySet()) }
    }

    private fun computeMetadataItem(expr: MetadataItem, items: Map<String, List<MetadataItem>>, chain: Set<String>): MetadataItem {
        val updatedChain = expr.id?.let { chain + it } ?: chain
        val refinedBy = expr.id?.let { items[it] }?.filter { it.id !in chain }.orEmpty()
        val newChildren = refinedBy.map { computeMetadataItem(it, items, updatedChain) }
        return when (expr) {
            is MetadataItem.Meta ->
                expr.copy(children = expr.children + newChildren)
            is MetadataItem.Link ->
                expr.copy(children = expr.children + newChildren)
        }
    }
}

internal open class MetadataAdapter(val epubVersion: Double, protected val items: List<MetadataItem>) {
    val duration = firstWithProperty(Vocabularies.MEDIA + "duration")
        ?.let { ClockValueParser.parse(it.value) }

    protected fun itemsWithProperty(property: String) = items
        .filterIsInstance(MetadataItem.Meta::class.java)
        .filter { it.property == property }

    protected fun firstWithProperty(property: String) = items
        .filterIsInstance(MetadataItem.Meta::class.java)
        .firstOrNull { it.property == property }

    protected fun firstWithRel(rel: String) = items
        .filterIsInstance(MetadataItem.Link::class.java)
        .firstOrNull { it.rels.contains(rel) }

    protected fun firstValue(property: String) =
        firstWithProperty(property)?.value
}

internal class LinkMetadataAdapter(
    epubVersion: Double,
    items: List<MetadataItem>
) : MetadataAdapter(epubVersion, items)

internal class PubMetadataAdapter(
    epubVersion: Double,
    metadataItems: List<MetadataItem>,
    fallbackTitle: String,
    uniqueIdentifierId: String?,
    readingProgression: ReadingProgression,
    displayOptions: Map<String, String>
) : MetadataAdapter(epubVersion, metadataItems) {

    fun metadata() = Metadata(
        identifier = identifier,
        conformsTo = setOf(Publication.Profile.EPUB),
        modified = modified,
        published = published,
        accessibility = accessibility,
        languages = languages,
        localizedTitle = localizedTitle,
        localizedSortAs = localizedSortAs,
        localizedSubtitle = localizedSubtitle,
        duration = duration,
        subjects = subjects,
        description = description,
        readingProgression = readingProgression,
        belongsToCollections = belongsToCollections,
        belongsToSeries = belongsToSeries,
        otherMetadata = otherMetadata,

        authors = contributors("aut"),
        translators = contributors("trl"),
        editors = contributors("edt"),
        publishers = contributors("pbl"),
        artists = contributors("art"),
        illustrators = contributors("ill"),
        colorists = contributors("clr"),
        narrators = contributors("nrt"),
        contributors = contributors(null)
    )

    fun links(): List<Link> = items
        .filterIsInstance(MetadataItem.Link::class.java)
        .mapNotNull(::mapEpubLink)

    /** Compute a Publication [Link] from an Epub metadata link */
    private fun mapEpubLink(link: MetadataItem.Link): Link? {
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

    val languages = run {
        var langs = itemsWithProperty(Vocabularies.DCTERMS + "language")
            .map(MetadataItem.Meta::value)

        // https://github.com/readium/readium-css/blob/master/docs/CSS16-internationalization.md#multiple-language-items
        if (langs.size > 1 && readingProgression == ReadingProgression.RTL) {
            val rtlLanguages = listOf("ar", "fa", "he", "zh", "zh-hant", "zh-tw", "zh-hk", "ko", "ja")
            val primaryLangIndex = langs.indexOfFirst { it in rtlLanguages }
            if (primaryLangIndex > 0) {
                langs = langs
                    .toMutableList()
                    .apply {
                        val primaryLang = removeAt(primaryLangIndex)
                        add(0, primaryLang)
                    }
                    .toList()
            }
        }

        langs
    }

    val identifier: String?

    init {
        val identifiers = itemsWithProperty(Vocabularies.DCTERMS + "identifier")

        identifier = uniqueIdentifierId
            ?.let { uniqueId -> identifiers.firstOrNull { it.id == uniqueId }?.value }
            ?: identifiers.firstOrNull()?.value
    }

    val published = firstValue(Vocabularies.DCTERMS + "date")?.iso8601ToDate()

    val modified = firstValue(Vocabularies.DCTERMS + "modified")?.iso8601ToDate()

    val description = firstValue(Vocabularies.DCTERMS + "description")

    val cover = firstValue("cover")

    val localizedTitle: LocalizedString

    val localizedSubtitle: LocalizedString?

    val localizedSortAs: LocalizedString?

    init {
        val titles = itemsWithProperty(Vocabularies.DCTERMS + "title")
            .map { it.toTitle() }
        val mainTitle = titles.firstOrNull { it.type == "main" }
            ?: titles.firstOrNull()

        localizedTitle = mainTitle?.value
            ?: LocalizedString(fallbackTitle)
        localizedSubtitle = titles.filter { it.type == "subtitle" }
            .sortedBy(Title::displaySeq).firstOrNull()?.value
        localizedSortAs = mainTitle?.fileAs
            ?: firstWithProperty("calibre:title_sort")?.let { LocalizedString(it.value) }
    }

    val belongsToSeries: List<Collection>

    val belongsToCollections: List<Collection>

    init {
        val allCollections = itemsWithProperty(Vocabularies.META + "belongs-to-collection")
            .map { it.toCollection() }
        val (seriesMeta, collectionsMeta) = allCollections.partition { it.first == "series" }

        belongsToCollections = collectionsMeta.map(Pair<String?, Collection>::second)

        belongsToSeries =
            if (seriesMeta.isNotEmpty())
                seriesMeta.map(Pair<String?, Collection>::second)
            else
                firstWithProperty("calibre:series")?.let {
                    val name = LocalizedString.fromStrings(mapOf(it.lang to it.value))
                    val position = firstValue("calibre:series_index")?.toDoubleOrNull()
                    listOf(Collection(localizedName = name, position = position))
                }.orEmpty()
    }

    val subjects: List<Subject>

    init {
        val subjectItems = itemsWithProperty(Vocabularies.DCTERMS + "subject")
        val parsedSubjects = subjectItems.map { it.toSubject() }
        val hasToSplit = parsedSubjects.size == 1 && parsedSubjects.first().run {
            localizedName.translations.size == 1 && code == null && scheme == null && sortAs == null
        }

        subjects = if (hasToSplit) splitSubject(parsedSubjects.first()) else parsedSubjects
    }

    private fun splitSubject(subject: Subject): List<Subject> {
        val lang = subject.localizedName.translations.keys.first()
        val names = subject.localizedName.translations.values.first().string.split(",", ";")
            .map(kotlin.String::trim).filter(kotlin.String::isNotEmpty)
        return names.map {
            val newName = LocalizedString.fromStrings(mapOf(lang to it))
            Subject(localizedName = newName)
        }
    }

    private val allContributors: Map<String?, List<Contributor>>

    init {
        val contributors = itemsWithProperty(Vocabularies.DCTERMS + "creator") +
                itemsWithProperty(Vocabularies.DCTERMS + "contributor") +
                itemsWithProperty(Vocabularies.DCTERMS + "publisher") +
                itemsWithProperty(Vocabularies.MEDIA + "narrator")

        allContributors = contributors
            .map(MetadataItem.Meta::toContributor)
            .groupBy(Pair<String?, Contributor>::first)
            .mapValues { it.value.map(Pair<String?, Contributor>::second) }
    }

    fun contributors(role: String?) = allContributors[role].orEmpty()

    val readingProgression = readingProgression

    val presentation: Presentation

    init {
        val flowProp = firstValue(Vocabularies.RENDITION + "flow")
        val spreadProp = firstValue(Vocabularies.RENDITION + "spread")
        val orientationProp = firstValue(Vocabularies.RENDITION + "orientation")
        val layoutProp =
            if (epubVersion < 3.0)
                if (displayOptions["fixed-layout"] == "true") "pre-paginated" else "reflowable"
            else firstValue(Vocabularies.RENDITION + "layout")

        val (overflow, continuous) = when (flowProp) {
            "paginated" -> Pair(Presentation.Overflow.PAGINATED, false)
            "scrolled-continuous" -> Pair(Presentation.Overflow.SCROLLED, true)
            "scrolled-doc" -> Pair(Presentation.Overflow.SCROLLED, false)
            else -> Pair(Presentation.Overflow.AUTO, false)
        }

        val layout = when (layoutProp) {
            "pre-paginated" -> EpubLayout.FIXED
            else -> EpubLayout.REFLOWABLE
        }

        val orientation = when (orientationProp) {
            "landscape" -> Presentation.Orientation.LANDSCAPE
            "portrait" -> Presentation.Orientation.PORTRAIT
            else -> Presentation.Orientation.AUTO
        }

        val spread = when (spreadProp) {
            "none" -> Presentation.Spread.NONE
            "landscape" -> Presentation.Spread.LANDSCAPE
            "both", "portrait" -> Presentation.Spread.BOTH
            else -> Presentation.Spread.AUTO
        }

        presentation = Presentation(
            overflow = overflow, continuous = continuous,
            layout = layout, orientation = orientation, spread = spread
        )
    }

    val accessibility: Accessibility?

    private val nonAccessibilityProfiles: List<MetadataItem.Meta>

    init {
        val (accessibilityProfiles, otherProfiles) =
            itemsWithProperty(Vocabularies.DCTERMS + "conformsTo")
                .map { it to accessibilityProfileFromString(it.value) }
                .partition { it.second != null }

        nonAccessibilityProfiles = otherProfiles.map { it.first }

        val conformsTo = accessibilityProfiles.mapNotNull { it.second }
            .toSet()

        val summary = firstWithProperty(Vocabularies.SCHEMA + "accessibilitySummary")
            ?.value

        val accessModes = itemsWithProperty(Vocabularies.SCHEMA + "accessMode")
            .map { Accessibility.AccessMode(it.value) }
            .toSet()

        val accessModesSufficient = itemsWithProperty(Vocabularies.SCHEMA + "accessModeSufficient")
            .map { it.value.split(",").map(String::trim).distinct() }
            .distinct()
            .map { modeGroups -> modeGroups.map { Accessibility.AccessMode(it) }.toSet() }
            .toSet()

        val features = itemsWithProperty(Vocabularies.SCHEMA + "accessibilityFeature")
            .map { Accessibility.Feature(it.value) }
            .toSet()

        val hazards = itemsWithProperty(Vocabularies.SCHEMA + "accessibilityHazard")
            .map { Accessibility.Hazard(it.value) }
            .toSet()

        var certification = firstWithProperty(Vocabularies.A11Y + "certifiedBy")
            ?.toCertification()
            ?: Accessibility.Certification(certifiedBy = null, credential = null, report = null)

        if (certification.credential == null) {
            val credential = firstWithProperty(Vocabularies.A11Y + "certifierCredential")
                ?.value
            credential?.let { certification = certification.copy(credential = it) }
        }

        if (certification.report == null) {
            val report = firstWithRel(Vocabularies.A11Y + "certifierReport")?.href
                ?: firstWithProperty(Vocabularies.A11Y + "certifierReport")?.value
            certification = certification.copy(report = report)
        }

        val finalCertification = certification
            .takeUnless { certification.certifiedBy == null && certification.credential == null &&
                certification.report == null }

        accessibility = if (conformsTo.isNotEmpty() || finalCertification != null || summary != null ||
            accessModes.isNotEmpty() || accessModesSufficient.isNotEmpty() ||
            features.isNotEmpty() || hazards.isNotEmpty()) {
            Accessibility(
                conformsTo = conformsTo,
                certification = certification,
                summary = summary,
                accessModes = accessModes,
                accessModesSufficient = accessModesSufficient,
                features = features,
                hazards = hazards
            )
        } else null
    }


    val otherMetadata: Map<String, Any>

    init {
        val dcterms = listOf(
            "identifier", "language", "title", "date", "modified", "description",
            "duration", "creator", "publisher", "contributor", "conformsTo"
        ).map { Vocabularies.DCTERMS + it }
        val a11y = listOf("certifiedBy", "certifierReport", "certifierCredential")
            .map { Vocabularies.A11Y + it }
        val schema = listOf(
            "accessMode", "accessModeSufficient", "accessibilityFeature",
            "accessibilityHazard", "accessibilitySummary"
        ).map { Vocabularies.SCHEMA + it }
        val media = listOf("narrator", "duration").map { Vocabularies.MEDIA + it }
        val rendition = listOf("flow", "spread", "orientation", "layout").map { Vocabularies.RENDITION + it }
        val usedProperties: List<String> = dcterms + media + rendition + a11y + schema

        val otherItemsMap = metadataItems
            .filterIsInstance(MetadataItem.Meta::class.java)
            .filter { it.property !in usedProperties }
            .plus(nonAccessibilityProfiles)
            .groupBy(MetadataItem.Meta::property)
            .mapValues {
                val values = it.value.map(MetadataItem.Meta::toMap)
                when(values.size) {
                    1 -> values[0]
                    else -> values
                }
            }
        otherMetadata = otherItemsMap + Pair("presentation", presentation.toJSON().toMap())
    }
}

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
    ) : MetadataItem() {

        private val metaChildren: List<Meta> =
            children.filterIsInstance(Meta::class.java)

        private val metaChildrenByProperty: Map<String, List<Meta>> =
            metaChildren.groupBy(Meta::property)

        private val linkChildren: List<Link> =
            children.filterIsInstance(Link::class.java)

        private val linkChildrenByRel: Map<String, List<Link>> =
            linkChildren
                .flatMap { link -> link.rels.map { rel -> Pair(rel, link) } }
                .groupBy({ it.first} ) { it.second }

        fun toSubject():
            Subject {
            require(property == Vocabularies.DCTERMS + "subject")
            val values = localizedString()
            val localizedSortAs = fileAs?.let { LocalizedString(it.second, it.first) }
            return Subject(values, localizedSortAs, authority, term)
        }

        fun toTitle(): Title {
            require(property == Vocabularies.DCTERMS + "title")
            val values = localizedString()
            val localizedSortAs = fileAs?.let { LocalizedString(it.second, it.first) }
            return Title(values, localizedSortAs, titleType, displaySeq)
        }

        fun toContributor(): Pair<String?, Contributor> {
            require(property in listOf("creator", "contributor", "publisher").map { Vocabularies.DCTERMS + it } +
                (Vocabularies.MEDIA + "narrator") + (Vocabularies.META + "belongs-to-collection"))
            val knownRoles = setOf("aut", "trl", "edt", "pbl", "art", "ill", "clr", "nrt")
            val names = localizedString()
            val localizedSortAs = fileAs?.let { LocalizedString(it.second, it.first) }
            val roles = role.takeUnless { it in knownRoles  }?.let { setOf(it) }.orEmpty()
            val type = when(property) {
                Vocabularies.META + "belongs-to-collection" -> collectionType
                Vocabularies.DCTERMS + "creator" -> "aut"
                Vocabularies.DCTERMS + "publisher" -> "pbl"
                Vocabularies.MEDIA + "narrator" -> "nrt"
                else -> role.takeIf { it in knownRoles } // Vocabularies.DCTERMS + "contributor"
            }

            val contributor =  Contributor(names, localizedSortAs = localizedSortAs,
                roles = roles, identifier = identifier, position = groupPosition)

            return Pair(type, contributor)
        }

        fun toCollection() = toContributor()

        fun toCertification(): Accessibility.Certification {
            require(property == Vocabularies.A11Y + "certifiedBy")

            val credential = metaChildrenByProperty[Vocabularies.A11Y + "certifierCredential"]
                ?.firstOrNull()
                ?.value

            val report = linkChildrenByRel[Vocabularies.A11Y + "certifierReport"]?.firstOrNull()?.href
                ?: metaChildrenByProperty[Vocabularies.A11Y + "certifierReport"]?.firstOrNull()?.value

            return Accessibility.Certification(
                certifiedBy = value,
                credential = credential,
                report = report
            )
        }

        fun toMap(): Any =
            if (children.isEmpty())
                value
            else {
                val mappedMetaChildren = children
                    .filterIsInstance(Meta::class.java)
                    .associate { Pair(it.property, it.toMap()) }
                val mappedLinkChildren = children
                    .filterIsInstance(Link::class.java)
                    .flatMap { link -> link.rels.map { rel -> Pair(rel, link.href) } }
                    .toMap()
                mappedMetaChildren + mappedLinkChildren + Pair("@value", value)
            }

        private val fileAs
            get() = metaChildrenByProperty[Vocabularies.META + "file-as"]
                ?.firstOrNull()
                ?.let { Pair(it.lang.takeUnless(String::isEmpty) , it.value) }

        private val titleType
            get() = firstValue(Vocabularies.META + "title-type")

        private val displaySeq
            get() = firstValue(Vocabularies.META + "display-seq")?.toIntOrNull()

        private val authority
            get() = firstValue(Vocabularies.META + "authority")

        private val term
            get() = firstValue(Vocabularies.META + "term")

        private val alternateScript
            get() = metaChildrenByProperty[Vocabularies.META + "alternate-script"]
                ?.associate { Pair(it.lang, it.value)
                }.orEmpty()

        private val collectionType
            get() = firstValue(Vocabularies.META + "collection-type")

        private val groupPosition
            get() = firstValue(Vocabularies.META + "group-position")
                ?.toDoubleOrNull()

        private val identifier
            get() = firstValue(Vocabularies.DCTERMS + "identifier")

        private val role
            get() = firstValue(Vocabularies.META + "role")

        private fun localizedString(): LocalizedString {
            val values = mapOf(lang.takeUnless(String::isEmpty) to value).plus(alternateScript)
            return LocalizedString.fromStrings(values)
        }

        private fun firstValue(property: String) = metaChildrenByProperty[property]
            ?.firstOrNull()?.value
    }
}
