/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.extensions.iso8601ToDate
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.Collection
import org.readium.r2.shared.publication.presentation.Presentation

internal class PubMetadataAdapter(
    epubVersion: Double,
    items: List<MetadataItem>,
    fallbackTitle: String,
    uniqueIdentifierId: String?,
    readingProgression: ReadingProgression,
    displayOptions: Map<String, String>
) {
    val links: List<Link> =
        LinksAdapter(items).adapt()

    val duration: Double? =
        DurationAdapter(items).adapt()

    val languages: List<String> =
        LanguageAdapter(readingProgression, items).adapt()

    val identifier: String? =
        IdentifierAdapter(uniqueIdentifierId, items).adapt()

    val published = items
        .firstWithProperty(Vocabularies.DCTERMS + "date")?.value
        ?.iso8601ToDate()

    val modified = items
        .firstWithProperty(Vocabularies.DCTERMS + "modified")?.value
        ?.iso8601ToDate()

    val description = items
        .firstWithProperty(Vocabularies.DCTERMS + "description")?.value

    val cover: String? = items
        .firstWithProperty("cover")?.value

    private val titles: Triple<LocalizedString, LocalizedString?, LocalizedString?> =
        TitleAdapter(fallbackTitle, items).adapt()

    val localizedTitle: LocalizedString =
        titles.first

    val localizedSortAs: LocalizedString? =
        titles.second

    val localizedSubtitle: LocalizedString? =
        titles.third

    private val allCollections: Pair<List<Collection>, List<Collection>> =
        CollectionAdapter(items).adapt()

    val belongsToCollections: List<Collection> =
        allCollections.first

    val belongsToSeries: List<Collection> =
        allCollections.second

    val subjects: List<Subject> =
        SubjectAdapter(items).adapt()

    private val allContributors: Map<String?, List<Contributor>> =
        ContributorAdapter(items).adapt()

    fun contributors(role: String?): List<Contributor> =
        allContributors[role].orEmpty()

    val accessibility: Accessibility? =
        AccessibilityAdapter(items).adapt()

    private val presentation: Presentation =
        PresentationAdapter(epubVersion, displayOptions, items).adapt()

    val otherMetadata: Map<String, Any> =
        OtherMetadataAdapter(items).adapt() + Pair("presentation", presentation.toJSON().toMap())
}

internal class DurationAdapter(private val items: List<MetadataItem>) {

    fun adapt(): Pair<Double? =
        items.firstWithProperty(Vocabularies.MEDIA + "duration")
            ?.let { ClockValueParser.parse(it.value) }
}

private class LinksAdapter(private val items: List<MetadataItem>) {

    fun adapt(): List<Link> =
        items.filterIsInstance(MetadataItem.Link::class.java)
            .map(::mapLink)

    /** Compute a Publication [Link] from an Epub metadata link */
    private fun mapLink(link: MetadataItem.Link): Link {
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
}

private class IdentifierAdapter(private val uniqueIdentifierId: String?, private val items: List<MetadataItem>) {

    fun adapt(): String? {
        val identifiers = items.metasWithProperty(Vocabularies.DCTERMS + "identifier")

        return uniqueIdentifierId
            ?.let { uniqueId -> identifiers.firstOrNull { it.id == uniqueId }?.value }
            ?: identifiers.firstOrNull()?.value
    }
}


private class LanguageAdapter(private val readingProgression: ReadingProgression, private val items: List<MetadataItem>) {

    fun adapt(): List<String> = items
        .metasWithProperty(Vocabularies.DCTERMS + "language")
        .map(MetadataItem.Meta::value)
        .let { forceRtlPrimaryLangIfNeeded(it) }

    private fun forceRtlPrimaryLangIfNeeded(langs: List<String>): List<String> {
        // https://github.com/readium/readium-css/blob/master/docs/CSS16-internationalization.md#multiple-language-items

        if (langs.size > 1 && readingProgression == ReadingProgression.RTL) {
            val rtlLanguages = listOf("ar", "fa", "he", "zh", "zh-hant", "zh-tw", "zh-hk", "ko", "ja")
            val primaryLangIndex = langs.indexOfFirst { it in rtlLanguages }
            if (primaryLangIndex > 0) {
                return langs
                    .toMutableList()
                    .apply {
                        val primaryLang = removeAt(primaryLangIndex)
                        add(0, primaryLang)
                    }
                    .toList()
            }
        }

        return langs
    }
}

private class TitleAdapter(private val fallbackTitle: String, private val items: List<MetadataItem>) {

    fun adapt(): Triple<LocalizedString, LocalizedString?, LocalizedString?> {
        val titles = items.metasWithProperty(Vocabularies.DCTERMS + "title")
            .map { it.title }
        val mainTitle = titles.firstOrNull { it.type == "main" }
            ?: titles.firstOrNull()

        val localizedTitle = mainTitle?.value
            ?: LocalizedString(fallbackTitle)
        val localizedSortAs = mainTitle?.fileAs
            ?: items.firstWithProperty("calibre:title_sort")
                ?.let { LocalizedString(it.value) }
        val localizedSubtitle = titles.filter { it.type == "subtitle" }
            .sortedBy(Title::displaySeq).firstOrNull()?.value

        return Triple(localizedTitle, localizedSortAs, localizedSubtitle)
    }
}

private class SubjectAdapter(private val items: List<MetadataItem>) {

    fun adapt(): List<Subject> {
        val subjectItems = items.metasWithProperty(Vocabularies.DCTERMS + "subject")
        val parsedSubjects = subjectItems.map { it.toSubject() }
        val hasToSplit = parsedSubjects.size == 1 && parsedSubjects.first().run {
            localizedName.translations.size == 1 && code == null && scheme == null && sortAs == null
        }

        return if (hasToSplit) splitSubject(parsedSubjects.first()) else parsedSubjects
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

    private fun MetadataItem.Meta.toSubject(): Subject {
        require(property == Vocabularies.DCTERMS + "subject")
        val localizedSortAs = fileAs?.let { LocalizedString(it.second, it.first) }
        return Subject(localizedString, localizedSortAs, authority, term)
    }
}

private class ContributorAdapter(private val items: List<MetadataItem>) {

    fun adapt(): Map<String?, List<Contributor>> {
        val contributors = items.metasWithProperty(Vocabularies.DCTERMS + "creator") +
            items.metasWithProperty(Vocabularies.DCTERMS + "contributor") +
            items.metasWithProperty(Vocabularies.DCTERMS + "publisher") +
            items.metasWithProperty(Vocabularies.MEDIA + "narrator")

        return contributors
            .map(MetadataItem.Meta::toContributor)
            .groupBy(Pair<String?, Contributor>::first)
            .mapValues { it.value.map(Pair<String?, Contributor>::second) }
    }
}

private fun MetadataItem.Meta.toContributor(): Pair<String?, Contributor> {
    require(property in listOf("creator", "contributor", "publisher").map { Vocabularies.DCTERMS + it } +
        (Vocabularies.MEDIA + "narrator") + (Vocabularies.META + "belongs-to-collection"))
    val knownRoles = setOf("aut", "trl", "edt", "pbl", "art", "ill", "clr", "nrt")
    val localizedSortAs = fileAs?.let { LocalizedString(it.second, it.first) }
    val roles = role.takeUnless { it in knownRoles  }?.let { setOf(it) }.orEmpty()
    val type = when(property) {
        Vocabularies.META + "belongs-to-collection" -> collectionType
        Vocabularies.DCTERMS + "creator" -> "aut"
        Vocabularies.DCTERMS + "publisher" -> "pbl"
        Vocabularies.MEDIA + "narrator" -> "nrt"
        else -> role.takeIf { it in knownRoles } // Vocabularies.DCTERMS + "contributor"
    }

    val contributor =  Contributor(localizedString, localizedSortAs = localizedSortAs,
        roles = roles, identifier = identifier, position = groupPosition)

    return Pair(type, contributor)
}

private class CollectionAdapter(private val items: List<MetadataItem>) {

    fun adapt(): Pair<List<Collection>, List<Collection>> {
        val allCollections = items.metasWithProperty(Vocabularies.META + "belongs-to-collection")
            .map { it.toCollection() }
        val (seriesMeta, collectionsMeta) = allCollections.partition { it.first == "series" }

        val belongsToCollections = collectionsMeta.map(Pair<String?, Collection>::second)

        val belongsToSeries =
            if (seriesMeta.isNotEmpty())
                seriesMeta.map(Pair<String?, Collection>::second)
            else
                items.firstWithProperty("calibre:series")?.let {
                    val name = LocalizedString.fromStrings(mapOf(it.lang to it.value))
                    val position = items.firstWithProperty("calibre:series_index")?.value?.toDoubleOrNull()
                    listOf(Collection(localizedName = name, position = position))
                }.orEmpty()

        return Pair(belongsToCollections, belongsToSeries)
    }

    private fun MetadataItem.Meta.toCollection(): Pair<String?, Contributor> =
        toContributor()
}

private class OtherMetadataAdapter(private val items: List<MetadataItem>) {

    fun adapt(): Map<String, Any> {
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
        val rendition =
            listOf("flow", "spread", "orientation", "layout").map { Vocabularies.RENDITION + it }
        val usedProperties: List<String> = dcterms + media + rendition + a11y + schema

        return items
            .filterIsInstance(MetadataItem.Meta::class.java)
            .filter { it.property !in usedProperties }
            //.plus(nonAccessibilityProfiles)
            .groupBy(MetadataItem.Meta::property)
            .mapValues {
                val values = it.value.map(MetadataItem.Meta::toMap)
                when (values.size) {
                    1 -> values[0]
                    else -> values
                }
            }
    }
}

private fun MetadataItem.Meta.toMap(): Any =
    if (children.isEmpty())
        value
    else {
        val mappedMetaChildren = children
            .filterIsInstance(MetadataItem.Meta::class.java)
            .associate { Pair(it.property, it.toMap()) }
        val mappedLinkChildren = children
            .filterIsInstance(MetadataItem.Link::class.java)
            .flatMap { link -> link.rels.map { rel -> Pair(rel, link.href) } }
            .toMap()
        mappedMetaChildren + mappedLinkChildren + Pair("@value", value)
    }

private data class Title(
    val value: LocalizedString,
    val fileAs: LocalizedString? = null,
    val type: String? = null,
    val displaySeq: Int? = null
)

private val MetadataItem.Meta.title: Title
    get() {
        require(property == Vocabularies.DCTERMS + "title")
        val localizedSortAs = fileAs?.let { LocalizedString(it.second, it.first) }
        return Title(localizedString, localizedSortAs, titleType, displaySeq)
    }

private val MetadataItem.Meta.localizedString: LocalizedString
    get() {
        val values = mapOf(lang.takeUnless(String::isEmpty) to value).plus(alternateScript)
        return LocalizedString.fromStrings(values)
    }

private val MetadataItem.Meta.alternateScript: Map<String, String>
    get() = children
        .metasWithProperty(Vocabularies.META + "alternate-script")
        .associate { Pair(it.lang, it.value) }

private val MetadataItem.Meta.fileAs
    get() = children
        .firstWithProperty(Vocabularies.META + "file-as")
        ?.let { Pair(it.lang.takeUnless(String::isEmpty) , it.value) }

private val MetadataItem.Meta.authority
    get() = children.firstValue(Vocabularies.META + "authority")

private val MetadataItem.Meta.term
    get() = children.firstValue(Vocabularies.META + "term")

private val MetadataItem.Meta.titleType
    get() = children.firstValue(Vocabularies.META + "title-type")

private val MetadataItem.displaySeq
    get() = children.firstValue(Vocabularies.META + "display-seq")
        ?.toIntOrNull()

private val MetadataItem.collectionType
    get() = children.firstValue(Vocabularies.META + "collection-type")

private val MetadataItem.groupPosition
    get() = children.firstValue(Vocabularies.META + "group-position")
        ?.toDoubleOrNull()

private val MetadataItem.identifier
    get() = children.firstValue(Vocabularies.DCTERMS + "identifier")

private val MetadataItem.role
    get() = children.firstValue(Vocabularies.META + "role")
