/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.Collection
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.util.Instant

internal class MetadataAdapter(
    private val epubVersion: Double,
    private val uniqueIdentifierId: String?,
    private val readingProgression: ReadingProgression?,
    private val displayOptions: Map<String, String>,
) {
    data class Result(
        val links: List<Link>,
        val metadata: Metadata,
        val durationById: Map<String, Double?>,
        val coverId: String?,
    )

    fun adapt(items: List<MetadataItem>): Result {
        val (globalItems: List<MetadataItem>, refiningItems: List<MetadataItem>) =
            items.partition { it.refines == null }

        @Suppress("Unchecked_cast")
        val durationById = refiningItems
            .groupBy(MetadataItem::refines)
            .mapValues { DurationAdapter().adapt(it.value).first }
            as Map<String, Double?>

        val globalItemsHolder = MetadataItemsHolder(globalItems)

        val coverId: String? = globalItemsHolder
            .adapt { it.takeFirstWithProperty("cover") }
            ?.value

        val duration: Double? = globalItemsHolder
            .adapt(DurationAdapter()::adapt)

        val languages: List<String> = globalItemsHolder
            .adapt(LanguageAdapter()::adapt)

        val identifier: String? = globalItemsHolder
            .adapt(IdentifierAdapter(uniqueIdentifierId)::adapt)

        val published = globalItemsHolder
            .adapt { it.takeFirstWithProperty(Vocabularies.DCTERMS + "date") }
            ?.value
            ?.let { Instant.parse(it) }

        val modified = globalItemsHolder
            .adapt { it.takeFirstWithProperty(Vocabularies.DCTERMS + "modified") }
            ?.value
            ?.let { Instant.parse(it) }

        val description = globalItemsHolder
            .adapt { it.takeFirstWithProperty(Vocabularies.DCTERMS + "description") }
            ?.value

        val (localizedTitle, localizedSortAs, localizedSubtitle) = globalItemsHolder
            .adapt(TitleAdapter()::adapt)

        val (belongsToCollections, belongsToSeries) = globalItemsHolder
            .adapt(CollectionAdapter()::adapt)

        val subjects: List<Subject> = globalItemsHolder
            .adapt(SubjectAdapter()::adapt)

        val allContributors: Map<String?, List<Contributor>> = globalItemsHolder
            .adapt(ContributorAdapter()::adapt)

        fun contributors(role: String?): List<Contributor> =
            allContributors[role].orEmpty()

        val accessibility: Accessibility? = globalItemsHolder
            .adapt(AccessibilityAdapter()::adapt)

        val presentation: Presentation = globalItemsHolder
            .adapt(PresentationAdapter(epubVersion, displayOptions)::adapt)

        val links: List<Link> = globalItemsHolder
            .adapt(LinksAdapter()::adapt)

        val remainingMetadata: Map<String, Any> = OtherMetadataAdapter()
            .adapt(globalItemsHolder.remainingItems)

        val otherMetadata: Map<String, Any> =
            remainingMetadata + Pair("presentation", presentation.toJSON().toMap())

        val metadata = Metadata(
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

        return Result(
            links = links,
            metadata = metadata,
            durationById = durationById,
            coverId = coverId
        )
    }
}

private class DurationAdapter {

    fun adapt(items: List<MetadataItem>): Pair<Double?, List<MetadataItem>> =
        items.takeFirstWithProperty(Vocabularies.MEDIA + "duration")
            .mapFirstNotNull { ClockValueParser.parse(it.value) }
}

private class LinksAdapter {

    @Suppress("Unchecked_cast")
    fun adapt(items: List<MetadataItem>): Pair<List<Link>, List<MetadataItem>> =
        items.partition { it is MetadataItem.Link }
            .mapFirst { (it as List<MetadataItem.Link>).map(::mapLink) }

    private fun mapLink(link: MetadataItem.Link): Link {
        val contains: MutableList<String> = mutableListOf()
        if (link.rels.contains(Vocabularies.LINK + "record")) {
            if (link.properties.contains(Vocabularies.LINK + "onix")) {
                contains.add("onix")
            }
            if (link.properties.contains(Vocabularies.LINK + "xmp")) {
                contains.add("xmp")
            }
        }
        return Link(
            href = link.href,
            mediaType = link.mediaType,
            rels = link.rels,
            properties = Properties(mapOf("contains" to contains))
        )
    }
}

private class IdentifierAdapter(private val uniqueIdentifierId: String?) {

    fun adapt(items: List<MetadataItem>): Pair<String?, List<MetadataItem>> {
        val uniqueIdentifier = items
            .takeFirstWithProperty(Vocabularies.DCTERMS + "identifier", id = uniqueIdentifierId)

        if (uniqueIdentifier.first != null) {
            return uniqueIdentifier
                .mapFirstNotNull { it.value }
        }

        return items
            .takeFirstWithProperty(Vocabularies.DCTERMS + "identifier")
            .mapFirstNotNull { it.value }
    }
}

private class LanguageAdapter {

    fun adapt(items: List<MetadataItem>): Pair<List<String>, List<MetadataItem>> = items
        .takeAllWithProperty(Vocabularies.DCTERMS + "language")
        .mapFirst { it.map(MetadataItem.Meta::value) }
}

private class TitleAdapter() {

    data class Result(
        val localizedTitle: LocalizedString?,
        val localizedSortAs: LocalizedString?,
        val localizedSubtitle: LocalizedString?,
    )

    fun adapt(items: List<MetadataItem>): Pair<Result, List<MetadataItem>> {
        val titles = items.metasWithProperty(Vocabularies.DCTERMS + "title")
            .map { it.title to it }

        val mainTitleWithItem = titles.firstOrNull { it.first.type == "main" }
            ?: titles.firstOrNull()
        val mainTitle = mainTitleWithItem?.first
        val mainTitleItem = mainTitleWithItem?.second

        val localizedTitle = mainTitle?.value
        val localizedSortAs = mainTitle?.fileAs
            ?: items.firstWithProperty("calibre:title_sort")
                ?.let { LocalizedString(it.value) }

        val subtitleWithItem = titles
            .filter { it.first.type == "subtitle" }
            .sortedBy { it.first.displaySeq }
            .firstOrNull()
        val localizedSubtitle = subtitleWithItem?.first?.value
        val subtitleItem = subtitleWithItem?.second

        val remainingItems = items
            .removeFirstOrNull { it == mainTitleItem }.second
            .removeFirstOrNull { it == subtitleItem }.second

        return Result(
            localizedTitle = localizedTitle,
            localizedSortAs = localizedSortAs,
            localizedSubtitle = localizedSubtitle
        ) to remainingItems
    }
}

private class SubjectAdapter {

    fun adapt(items: List<MetadataItem>): Pair<List<Subject>, List<MetadataItem>> {
        val (subjectItems, remainingItems) = items
            .takeAllWithProperty(Vocabularies.DCTERMS + "subject")
        val parsedSubjects = subjectItems
            .map { it.toSubject() }

        val hasToSplit = parsedSubjects.size == 1 && parsedSubjects.first().run {
            localizedName.translations.size == 1 && code == null && scheme == null && sortAs == null
        }
        val subjects = if (hasToSplit) splitSubject(parsedSubjects.first()) else parsedSubjects

        return subjects to remainingItems
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

private class ContributorAdapter {

    fun adapt(items: List<MetadataItem>): Pair<Map<String?, List<Contributor>>, List<MetadataItem>> {
        val (contributorItems, remainingItems) = items.takeAllWithProperty(
            Vocabularies.DCTERMS + "creator",
            Vocabularies.DCTERMS + "contributor",
            Vocabularies.DCTERMS + "publisher",
            Vocabularies.MEDIA + "narrator"
        )

        val contributors = contributorItems
            .map(MetadataItem.Meta::toContributor)
            .groupBy(Pair<String?, Contributor>::first)
            .mapValues { it.value.map(Pair<String?, Contributor>::second) }

        return contributors to remainingItems
    }
}

private fun MetadataItem.Meta.toContributor(): Pair<String?, Contributor> {
    require(
        property in listOf("creator", "contributor", "publisher").map { Vocabularies.DCTERMS + it } +
            (Vocabularies.MEDIA + "narrator") + (Vocabularies.META + "belongs-to-collection")
    )
    val knownRoles = setOf("aut", "trl", "edt", "pbl", "art", "ill", "clr", "nrt")
    val localizedSortAs = fileAs?.let { LocalizedString(it.second, it.first) }
    val roles = role.takeUnless { it in knownRoles }?.let { setOf(it) }.orEmpty()
    val type = when (property) {
        Vocabularies.META + "belongs-to-collection" -> collectionType
        Vocabularies.DCTERMS + "creator" -> "aut"
        Vocabularies.DCTERMS + "publisher" -> "pbl"
        Vocabularies.MEDIA + "narrator" -> "nrt"
        else -> role.takeIf { it in knownRoles } // Vocabularies.DCTERMS + "contributor"
    }

    val contributor = Contributor(
        localizedString,
        localizedSortAs = localizedSortAs,
        roles = roles,
        identifier = identifier,
        position = groupPosition
    )

    return Pair(type, contributor)
}

private class CollectionAdapter {

    data class Result(
        val belongsToCollections: List<Collection>,
        val belongsToSeries: List<Collection>,
    )

    fun adapt(items: List<MetadataItem>): Pair<Result, List<MetadataItem>> {
        var remainingItems: List<MetadataItem> = items

        val collectionItems = remainingItems
            .takeAllWithProperty(Vocabularies.META + "belongs-to-collection")
            .let {
                remainingItems = it.second
                it.first
            }

        val allCollections = collectionItems
            .map { it.toCollection() }
        val (series, collections) = allCollections
            .partition { it.first == "series" }

        val belongsToCollections = collections.map(Pair<String?, Collection>::second)
        val belongsToSeries = series.map(Pair<String?, Collection>::second)
            .ifEmpty {
                legacySeries(items).let {
                    remainingItems = it.second
                    it.first
                }
            }

        return Result(belongsToCollections, belongsToSeries) to remainingItems
    }

    private fun legacySeries(items: List<MetadataItem>): Pair<List<Collection>, List<MetadataItem>> {
        val (seriesItem, remainingItems) = items.takeFirstWithProperty("calibre:series")

        val series = seriesItem?.let {
            val name = LocalizedString.fromStrings(mapOf(it.lang to it.value))
            val position = items.firstWithProperty("calibre:series_index")?.value?.toDoubleOrNull()
            listOf(Collection(localizedName = name, position = position))
        }.orEmpty()

        return series to remainingItems
    }

    private fun MetadataItem.Meta.toCollection(): Pair<String?, Contributor> =
        toContributor()
}

private class OtherMetadataAdapter {

    fun adapt(items: List<MetadataItem>): Map<String, Any> =
        items.filterIsInstance(MetadataItem.Meta::class.java)
            .groupBy(MetadataItem.Meta::property)
            .mapValues { entry ->
                val values = entry.value.map { it.toMap() }
                when (values.size) {
                    1 -> values[0]
                    else -> values
                }
            }

    private fun MetadataItem.Meta.toMap(): Any =
        if (children.isEmpty()) {
            value
        } else {
            val mappedMetaChildren = children
                .filterIsInstance(MetadataItem.Meta::class.java)
                .associate { Pair(it.property, it.toMap()) }
            val mappedLinkChildren = children
                .filterIsInstance(MetadataItem.Link::class.java)
                .flatMap { link -> link.rels.map { rel -> Pair(rel, link.url()) } }
                .toMap()
            mappedMetaChildren + mappedLinkChildren + Pair("@value", value)
        }
}

private data class Title(
    val value: LocalizedString,
    val fileAs: LocalizedString? = null,
    val type: String? = null,
    val displaySeq: Int? = null,
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
        ?.let { Pair(it.lang.takeUnless(String::isEmpty), it.value) }

private val MetadataItem.Meta.authority
    get() = children.firstWithProperty(Vocabularies.META + "authority")?.value

private val MetadataItem.Meta.term
    get() = children.firstWithProperty(Vocabularies.META + "term")?.value

private val MetadataItem.Meta.titleType
    get() = children.firstWithProperty(Vocabularies.META + "title-type")?.value

private val MetadataItem.displaySeq
    get() = children.firstWithProperty(Vocabularies.META + "display-seq")
        ?.value
        ?.toIntOrNull()

private val MetadataItem.collectionType
    get() = children.firstWithProperty(Vocabularies.META + "collection-type")
        ?.value

private val MetadataItem.groupPosition
    get() = children.firstWithProperty(Vocabularies.META + "group-position")
        ?.value
        ?.toDoubleOrNull()

private val MetadataItem.identifier
    get() = children.firstWithProperty(Vocabularies.DCTERMS + "identifier")
        ?.value

private val MetadataItem.role
    get() = children.firstWithProperty(Vocabularies.META + "role")
        ?.value
