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
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.*
import org.readium.r2.streamer.parser.normalize
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation

internal data class EpubLink(
    val href: String,
    val rel: List<String>,
    val mediaType: String?, val refines: String?,
    val properties: List<String> = emptyList()
)

internal data class EpubMetadata(
    val uniqueIdentifierId: String?,
    val globalItems: MetaCollection,
    val refineItems: Map<String, MetaCollection>,
    val links: List<EpubLink>
)

internal class MetadataParser(private val epubVersion: Double, private val prefixMap: Map<String, String>) {
    fun parse(document: ElementNode, filePath: String): EpubMetadata? {
        val metadata = document.getFirst("metadata", Namespaces.Opf) ?: return null
        val uniqueIdentifierId = document.getAttr("unique-identifier")
        val (metas, links) = parseElements(metadata, filePath)
        val (globalMetas, refineMetas) = resolveMetaHierarchy(metas).partition { it.refines == null }
        val globalCollection = MetaCollection(globalMetas.groupBy(MetaItem::property))
        @Suppress("Unchecked_cast")
        val refineCollections = (refineMetas.groupBy(MetaItem::refines) as Map<String, List<MetaItem>>)
            .mapValues { MetaCollection(it.value.groupBy(MetaItem::property)) }
        return EpubMetadata(uniqueIdentifierId, globalCollection, refineCollections, links)
    }

    private fun parseElements(metadataElement: ElementNode, filePath: String): Pair<List<MetaItem>, List<EpubLink>> {
        val metas: MutableList<MetaItem> = mutableListOf()
        val links: MutableList<EpubLink> = mutableListOf()
        for (e in metadataElement.getAll()) {
            when {
                e.namespace == Namespaces.Dc -> parseDcElement(e)?.let { metas.add(it) }
                e.namespace == Namespaces.Opf && e.name == "meta" -> parseMetaElement(e)?.let { metas.add(it) }
                e.namespace == Namespaces.Opf && e.name == "link" -> parseLinkElement(
                    e,
                    filePath
                )?.let { links.add(it) }
            }
        }
        return Pair(metas, links)
    }

    private fun parseLinkElement(element: ElementNode, filePath: String): EpubLink? {
        val href = element.getAttr("href") ?: return null
        val relAttr = element.getAttr("rel").orEmpty()
        val rel = parseProperties(relAttr).mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.LINK) }
        val propAttr = element.getAttr("properties").orEmpty()
        val properties = parseProperties(propAttr).mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.LINK) }
        val mediaType = element.getAttr("media-type")
        val refines = element.getAttr("refines")?.removePrefix("#")
        return EpubLink(normalize(filePath, href), rel, mediaType, refines, properties)
    }

    private fun parseMetaElement(element: ElementNode): MetaItem? {
        return if (epubVersion < 3.0) {
            val name = element.getAttr("name") ?: return null
            val content = element.getAttr("content") ?: return null
            MetaItem(name, content, element.lang, null, null, element.id)
        } else {
            val propName = element.getAttr("property")?.trim()?.ifEmpty { null } ?: return null
            val propValue = element.text?.trim()?.ifEmpty { null } ?: return null
            val resolvedProp = resolveProperty(propName, prefixMap, DEFAULT_VOCAB.META) ?: return null
            val resolvedScheme =
                element.getAttr("scheme")?.trim()?.ifEmpty { null }?.let { resolveProperty(it, prefixMap) }
            val refines = element.getAttr("refines")?.removePrefix("#")
            MetaItem(resolvedProp, propValue, element.lang, resolvedScheme, refines, element.id)
        }
    }

    private fun parseDcElement(element: ElementNode): MetaItem? {
        val propValue = element.text?.trim()?.ifEmpty { null } ?: return null
        val propName = Vocabularies.Dcterms + element.name
        return when (element.name) {
            "creator", "contributor", "publisher" -> contributorWithLegacyAttr(element, propName, propValue)
            "date" -> dateWithLegacyAttr(element, propName, propValue)
            else -> MetaItem(propName, propValue, lang = element.lang, id = element.id)
        }
    }

    private fun contributorWithLegacyAttr(element: ElementNode, name: String, value: String): MetaItem {
        val fileAs = element.getAttrNs("file-as", Namespaces.Opf)?.let {
            MetaItem(Vocabularies.Meta + "file-as", value = it, lang = element.lang, id = element.id)
        }
        val role = element.getAttrNs("role", Namespaces.Opf)?.let {
            MetaItem(Vocabularies.Meta + "role", it, lang = element.lang, id = element.id)
        }
        val children = listOfNotNull(fileAs, role).groupBy(MetaItem::property)
        return MetaItem(name, value, lang = element.lang, id = element.id, children = children)
    }

    private fun dateWithLegacyAttr(element: ElementNode, name: String, value: String): MetaItem? {
        val eventAttr = element.getAttrNs("event", Namespaces.Opf)
        val propName = if (eventAttr == "modification") Vocabularies.Dcterms + "modified" else name
        return MetaItem(propName, value, lang = element.lang, id = element.id)
    }

    private fun resolveMetaHierarchy(items: List<MetaItem>): List<MetaItem> {
        val metadataIds = items.mapNotNull { it.id }
        val rootExpr = items.filter { it.refines == null || it.refines !in metadataIds }
        @Suppress("Unchecked_cast")
        val exprByRefines = items.groupBy(MetaItem::refines) as Map<String, List<MetaItem>>
        return rootExpr.map { computeMetaItem(it, exprByRefines, emptySet()) }
    }

    private fun computeMetaItem(expr: MetaItem, metas: Map<String, List<MetaItem>>, chain: Set<String>): MetaItem {
        val updatedChain = if (expr.id == null) chain else chain + expr.id
        val refinedBy = expr.id?.let { metas[it] }?.filter { it.id !in chain }.orEmpty()
        val newChildren = refinedBy.map { computeMetaItem(it, metas, updatedChain) }
        return expr.copy(children = (expr.children.values.flatten() + newChildren).groupBy(MetaItem::property))
    }
}

internal data class MetaCollection(val metas: Map<String, List<MetaItem>>) {
    private val defaultLang = first(Vocabularies.Dcterms + "language")

    private val titles = metas[Vocabularies.Dcterms + "title"]?.map { it.toTitle(defaultLang) }.orEmpty()

    private val maintitle = titles.firstOrNull { it.type == "main" } ?: titles.firstOrNull()

    private val allContributors: Map<String?, List<Contributor>>

    init {
        val creators = metas[Vocabularies.Dcterms + "creator"].orEmpty()
            .map { it.toContributor(defaultLang, "aut") }
        val publishers = metas[Vocabularies.Dcterms + "publisher"].orEmpty()
            .map { it.toContributor(defaultLang, "pbl") }
        val others = metas[Vocabularies.Dcterms + "contributor"].orEmpty()
            .map { it.toContributor(defaultLang) }
        val narrators = metas[Vocabularies.Media + "narrator"].orEmpty()
            .map { it.toContributor(defaultLang, "nrt") }
        val contributors = creators + publishers + narrators + others
        val knownRoles = setOf("aut", "trl", "edt", "pbl", "art", "ill", "clr", "nrt")
        allContributors = contributors.distributeBy(knownRoles, Contributor::roles)
    }

    fun toMetadata(uniqueIdentifierId: String?, readingProgression: ReadingProgression) = Metadata(
        identifier = identifier(uniqueIdentifierId),
        modified = modified(),
        published = published(),
        languages = languages(),
        localizedTitle = title(),
        sortAs = sortAs(),
        localizedSubtitle = subtitle(),
        duration = duration(),
        subjects = subjects(),
        description = description(),
        readingProgression = readingProgression,
        otherMetadata = otherMetadata(),

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

    fun languages() = metas[Vocabularies.Dcterms + "language"]?.map(MetaItem::value).orEmpty()

    fun title() = maintitle?.value ?: LocalizedString()

    fun sortAs() = maintitle?.fileAs ?: first("calibre:title_sort")

    fun subtitle() = titles.filter { it.type == "subtitle" }.sortedBy(Title::displaySeq).firstOrNull()?.value

    fun contributors(role: String?) = allContributors[role].orEmpty()

    fun published() = first(Vocabularies.Dcterms + "date")?.toDateOrNull()

    fun modified() = first(Vocabularies.Dcterms + "modified")?.toDateOrNull()

    fun description() = first(Vocabularies.Dcterms + "description")

    fun duration() = first(Vocabularies.Media + "duration")?.let { ClockValueParser.parse(it) }

    fun cover() = first("cover")

    fun identifier(id: String?): String? {
        val identifiers = metas[Vocabularies.Dcterms + "identifier"]
            ?.associate { Pair(it.property, it.value) }.orEmpty()
        return id?.let { identifiers[it] } ?: identifiers.values.firstOrNull()
    }

    fun subjects(): List<Subject> {
        val subjects = metas[Vocabularies.Dcterms + "subject"].orEmpty()
        val parsedSubjects = subjects.map { it.toSubject(defaultLang) }
        val hasToSplit = parsedSubjects.size == 1 && parsedSubjects.first().run {
            localizedName.translations.size == 1 && code == null && scheme == null && sortAs == null
        }
        return if (hasToSplit) splitSubject(parsedSubjects.first()) else parsedSubjects
    }

    private fun presentation(): Presentation {
        val flowProp = first(Vocabularies.Rendition + "flow")
        val spreadProp = first(Vocabularies.Rendition + "spread")
        val orientationProp = first(Vocabularies.Rendition + "orientation")
        val layoutProp = first(Vocabularies.Rendition + "layout")

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
        return Presentation(
            overflow = overflow, continuous = continuous,
            layout = layout, orientation = orientation, spread = spread
        )
    }

    fun otherMetadata(): Map<String, Any> {
        val dcterms = listOf(
            "identifier", "language", "title", "date", "modified", "description",
            "duration", "creator", "publisher", "contributor"
        ).map { Vocabularies.Dcterms + it }
        val media = listOf("narrator", "duration").map { Vocabularies.Media + it }
        val rendition = listOf("flow", "spread", "orientation", "layout").map { Vocabularies.Rendition + it }
        val usedProperties: List<String> = dcterms + media + rendition

        val otherMetadata: MutableMap<String, Any> = mutableMapOf()
        val others = metas.filterKeys { it !in usedProperties }.values.flatten()
        others.forEach { otherMetadata[it.property] = it.toMap() }
        return otherMetadata + Pair("presentation", presentation().toJSON().toMap())
    }

    private fun first(property: String) = metas[property]?.firstOrNull()?.value

    private fun splitSubject(subject: Subject): List<Subject> {
        val lang = subject.localizedName.translations.keys.first()
        val names = subject.localizedName.translations.values.first().string.split(",", ";")
            .map(kotlin.String::trim).filter(kotlin.String::isNotEmpty)
        return names.map {
            val newName = LocalizedString.fromStrings(mapOf(lang to it))
            Subject(localizedName = newName)
        }
    }
}

internal data class MetaItem(
    val property: String,
    val value: String,
    val lang: String,
    val scheme: String? = null,
    val refines: String? = null,
    val id: String?,
    val children: Map<String, List<MetaItem>> = emptyMap()
) {

    fun toSubject(defaultLang: String?): Subject {
        require(property == Vocabularies.Dcterms + "subject")
        val values = localizedString(defaultLang)
        return Subject(values, fileAs, authority, term)
    }

    fun toTitle(defaultLang: String?): Title {
        require(property == Vocabularies.Dcterms + "title")
        val values = localizedString(defaultLang)
        return Title(values, fileAs, titleType, displaySeq)
    }

    fun toContributor(defaultLang: String?, defaultRole: String? = null): Contributor {
        require(property in listOf("creator", "contributor", "publisher").map { Vocabularies.Dcterms + it } +
                (Vocabularies.Media + "narrator"))
        val names = localizedString(defaultLang)
        return Contributor(names, sortAs = fileAs, roles = roles(defaultRole))
    }

    fun toMap(): Any {
        return if (children.isEmpty())
            value
        else {
            val mappedChildren = children.values.flatten().associate { Pair(it.property, it.toMap()) }
            mappedChildren + Pair("@value", value)
        }
    }

    private val fileAs
        get() = first(Vocabularies.Meta + "file-as")

    private val titleType
        get() = first(Vocabularies.Meta + "title-type")

    private val displaySeq
        get() = first(Vocabularies.Meta + "display-seq")?.toIntOrNull()

    private val authority
        get() = first(Vocabularies.Meta + "authority")

    private val term
        get() = first(Vocabularies.Meta + "term")

    private val alternateScript
        get() = children[Vocabularies.Meta + "alternate-script"]?.associate { Pair(it.lang, it.value) }.orEmpty()

    private fun localizedString(defaultLang: String?): LocalizedString {
        val values =
            mapOf(lang to value).plus(alternateScript).mapKeys { if (it.key.isEmpty()) defaultLang else it.key }
        return LocalizedString.fromStrings(values)
    }

    private fun roles(default: String?): Set<String> {
        val roles = all(Vocabularies.Meta + "role")
        return if (roles.isEmpty() && default != null) setOf(default) else roles.toSet()
    }

    private fun first(property: String) = children[property]?.firstOrNull()?.value

    private fun all(property: String) = children[property]?.map(MetaItem::value).orEmpty()
}

internal data class Title(
    val value: LocalizedString,
    val fileAs: String? = null,
    val type: String? = null,
    val displaySeq: Int? = null
)

private fun <K, V> List<V>.distributeBy(classes: Set<K>, transform: (V) -> Collection<K>): Map<K?, List<V>> {
    /* Map all elements with [transform] and compute a [Map] with keys [null] and elements from [classes] and,
     as values, lists of elements whose transformed values contain the key.
     If a transformed element is in no class, it is assumed to be in [null] class. */

    val map: MutableMap<K?, MutableList<V>> = mutableMapOf()
    for (element in this) {
        val transformed = transform(element).filter { it in classes }
        if (transformed.isEmpty())
            map.getOrPut(null) { mutableListOf() }.add(element)
        for (v in transformed)
            map.getOrPut(v) { mutableListOf() }.add(element)
    }
    return map
}

private fun String.toDateOrNull() =
    try {
        DateTime(this).toDate()
    } catch (e: Exception) {
        null
    }