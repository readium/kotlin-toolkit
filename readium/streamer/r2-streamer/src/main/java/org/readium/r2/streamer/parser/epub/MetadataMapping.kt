/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import java.util.Date
import java.util.LinkedList
import org.joda.time.DateTime
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.Presentation

internal fun Epub.computeMetadata() : Metadata {
    val metas = packageDocument.metadata.globalItems
    val others: MutableList<MetaItem> = LinkedList()

    val titles: MutableList<Title> = LinkedList()
    val languages: MutableList<String> = LinkedList()
    val subjects: MutableList<Subject> = LinkedList()
    val contributors: MutableList<Contributor> = LinkedList()
    val identifiers: MutableMap<String?, String> = mutableMapOf()

    var description: String? = null
    var published: Date? = null
    var modified: Date? = null

    var activeClass: String? = null
    var playbackActiveClass: String? = null
    var duration: Double? = null

    var flow: String? = null
    var spread: String? = null
    var orientation: String? = null
    var layout: String? = null


    for (meta in metas) {
        when (meta.property) {
            PACKAGE_RESERVED_PREFIXES["dcterms"] + "identifier" -> identifiers[meta.id] = meta.value
            PACKAGE_RESERVED_PREFIXES["dcterms"] + "title" -> titles.add(parseTitle(meta, metas))
            PACKAGE_RESERVED_PREFIXES["dcterms"] + "language" -> languages.add(meta.value)
            PACKAGE_RESERVED_PREFIXES["dcterms"] + "date" -> meta.value.toDateOrNull()?.let { if (published == null) published = it }
            PACKAGE_RESERVED_PREFIXES["dcterms"] + "modified" -> modified = meta.value.toDateOrNull()
            PACKAGE_RESERVED_PREFIXES["dcterms"] + "description" -> description = meta.value
            PACKAGE_RESERVED_PREFIXES["dcterms"] + "subject" -> subjects.add(parseSubject(meta))

            PACKAGE_RESERVED_PREFIXES["dcterms"] + "creator"  -> contributors.add(parseContributor(meta, "aut"))
            PACKAGE_RESERVED_PREFIXES["dcterms"] + "contributor" -> contributors.add(parseContributor(meta))
            PACKAGE_RESERVED_PREFIXES["dcterms"] + "publisher" -> contributors.add(parseContributor(meta, "pbl"))
            PACKAGE_RESERVED_PREFIXES["media"] + "narrator" -> contributors.add(parseContributor(meta, "nrt"))

            PACKAGE_RESERVED_PREFIXES["media"] + "active-class" -> activeClass = meta.value
            PACKAGE_RESERVED_PREFIXES["media"] + "playback-active-class" -> playbackActiveClass = meta.value
            PACKAGE_RESERVED_PREFIXES["media"] + "duration" -> duration = ClockValueParser.parse(meta.value)

            PACKAGE_RESERVED_PREFIXES["rendition"] + "flow" -> flow = meta.value
            PACKAGE_RESERVED_PREFIXES["rendition"] + "layout" -> layout = meta.value
            PACKAGE_RESERVED_PREFIXES["rendition"] + "orientation" -> orientation = meta.value
            PACKAGE_RESERVED_PREFIXES["rendition"] + "spread" -> spread = meta.value
            else -> others.add(meta)
        }
    }

    val authors: MutableList<Contributor> = mutableListOf()
    val translators: MutableList<Contributor> = mutableListOf()
    val editors: MutableList<Contributor> = mutableListOf()
    val publishers: MutableList<Contributor> = mutableListOf()
    val artists: MutableList<Contributor> = mutableListOf()
    val illustrators: MutableList<Contributor> = mutableListOf()
    val colorists: MutableList<Contributor> = mutableListOf()
    val narrators: MutableList<Contributor> = mutableListOf()
    val otherContributors: MutableList<Contributor> = mutableListOf()

    for (contributor in contributors.map { it.copy(localizedName=it.localizedName.withDefaultLang(languages)) }) {
        if (contributor.roles.isEmpty())
            otherContributors.add(contributor)
        for (role in contributor.roles) {
            when (role) {
                "aut" -> authors.add(contributor)
                "trl" -> translators.add(contributor)
                "edt" -> editors.add(contributor)
                "pbl" -> publishers.add(contributor)
                "art" -> artists.add(contributor)
                "ill" -> illustrators.add(contributor)
                "clr" -> colorists.add(contributor)
                "nrt" -> narrators.add(contributor)
                else -> otherContributors.add(contributor)
            }
        }
    }

    val uniqueIdentifier = identifiers[packageDocument.metadata.uniqueIdentifierId] ?: identifiers.values.firstOrNull()
    val mainTitle =  titles.firstOrNull { it.type == "main" } ?: titles.firstOrNull()
    val subtitle = titles.filter { it.type == "subtitle" }.sortedBy(Title::displaySeq).firstOrNull()
    val presentation = computePresentation(flow, spread, orientation, layout)

    val otherMetadata: MutableMap<String, Any> = mutableMapOf()
    otherMetadata["presentation"] = presentation.toJSON().toMap()
    others.forEach { otherMetadata[it.property] = it.toMap() }

    return Metadata(
            identifier = uniqueIdentifier,
            modified = modified,
            published = published,
            languages = languages,
            localizedTitle = mainTitle?.value?.withDefaultLang(languages) ?: LocalizedString(),
            sortAs = mainTitle?.fileAs,
            localizedSubtitle = subtitle?.value?.withDefaultLang(languages),
            duration = duration,
            subjects = computeSubjects(subjects, languages),
            description = description,
            readingProgression = packageDocument.spine.direction,
            otherMetadata = otherMetadata,

            authors = authors,
            translators = translators,
            editors = editors,
            artists = artists,
            illustrators = illustrators,
            colorists = colorists,
            narrators = narrators,
            publishers = publishers,
            contributors = otherContributors
    )
}

private fun computeSubjects(subjects: List<Subject>, languages: List<String>) : List<Subject> {
    val newSubjects = subjects.map { it.copy(localizedName=it.localizedName.withDefaultLang(languages)) }
    val hasToSplit = subjects.size == 1 && subjects.first().run {
                localizedName.translations.size == 1 && code == null && scheme == null && sortAs == null}
    return if (hasToSplit) splitSubject(newSubjects.first()) else newSubjects
}

private fun parseSubject(item: MetaItem) : Subject {
    val values: MutableMap<String?, String> = mutableMapOf(item.lang to item.value)
    var authority: String? = null
    var term: String? = null
    var fileAs: String? = null

    for (child in item.children) {
        when (child.property) {
            DEFAULT_VOCAB.META.iri + "alternate-script" -> if (child.lang != item.lang) values[child.lang] = child.value
            DEFAULT_VOCAB.META.iri + "authority" -> authority = child.value
            DEFAULT_VOCAB.META.iri + "term" -> term = child.value
            DEFAULT_VOCAB.META.iri + "file-as" -> fileAs = child.value
        }
    }
    values.remove("")?.let { values[null] = it }
    return Subject(LocalizedString.fromStrings(values), fileAs, authority, term)
}

private fun splitSubject(subject: Subject) : List<Subject> {
    val lang = subject.localizedName.translations.keys.first()
    val names = subject.localizedName.translations.values.first().string.split(",", ";")
            .map(kotlin.String::trim).filter(kotlin.String::isNotEmpty)
    return names.map {
        val newName = LocalizedString.fromStrings(mapOf(lang to it))
        Subject(localizedName=newName)
    }
}

private fun Epub.parseTitle(item: MetaItem, others: List<MetaItem>) : Title {
    val values: MutableMap<String?, String> = mutableMapOf(item.lang to item.value)
    var type: String? = null
    var fileAs: String? = null
    var displaySeq: Int? = null

    if (packageDocument.epubVersion < 3.0)
        fileAs = others.firstOrNull { it.property == "calibre:title_sort" }?.value
    else {
        for (child in item.children) {
            when (child.property) {
                DEFAULT_VOCAB.META.iri + "alternate-script" -> if (child.lang != item.lang) values[child.lang] = child.value
                DEFAULT_VOCAB.META.iri + "file-as" -> fileAs = child.value
                DEFAULT_VOCAB.META.iri + "title-type" -> type = child.value
                DEFAULT_VOCAB.META.iri + "display-seq" -> child.value.toIntOrNull()?.let { displaySeq = it }
            }
        }
    }
    return Title(LocalizedString.fromStrings(values), fileAs, type, displaySeq)
}

private fun parseContributor(item: MetaItem, defaultRole: String? = null): Contributor {
    val names: MutableMap<String?, String> = mutableMapOf(item.lang to item.value)
    val roles: MutableSet<String> = mutableSetOf()
    var fileAs: String? = null

    for (child in item.children) {
        when (child.property) {
            DEFAULT_VOCAB.META.iri + "alternate-script" -> if (child.lang != item.lang) names[child.lang] = child.value
            DEFAULT_VOCAB.META.iri + "file-as" -> fileAs = child.value
            DEFAULT_VOCAB.META.iri + "role" -> roles.add(child.value)
        }
    }

    names.remove("")?.let { names[null] = it }
    if (roles.isEmpty() && defaultRole != null)
        roles.add(defaultRole)

    return Contributor(localizedName=LocalizedString.fromStrings(names), sortAs=fileAs, roles=roles)
}

private fun computePresentation(flowProp: String?, spreadProp: String?, orientationProp: String?, layoutProp: String?) : Presentation {
    val (overflow, continuous) = when(flowProp) {
        "paginated" -> Pair(Presentation.Overflow.PAGINATED, false)
        "scrolled-continuous" -> Pair(Presentation.Overflow.SCROLLED, true)
        "scrolled-doc" -> Pair(Presentation.Overflow.SCROLLED, false)
        else -> Pair(Presentation.Overflow.AUTO, false)
    }

    val layout = when(layoutProp) {
        "pre-paginated" -> EpubLayout.FIXED
        else -> EpubLayout.REFLOWABLE
    }

    val orientation = when(orientationProp) {
        "landscape" -> Presentation.Orientation.LANDSCAPE
        "portrait" -> Presentation.Orientation.PORTRAIT
        else -> Presentation.Orientation.AUTO
    }

    val spread = when(spreadProp) {
        "none" -> Presentation.Spread.NONE
        "landscape" -> Presentation.Spread.LANDSCAPE
        "both", "portrait" -> Presentation.Spread.BOTH
        else -> Presentation.Spread.AUTO
    }
    return Presentation(overflow = overflow, continuous = continuous,
            layout = layout, orientation = orientation, spread = spread)
}

private fun LocalizedString.withDefaultLang(languages: List<String>) : LocalizedString {
    return mapLanguages {
        if (it.key.isNullOrEmpty()) {
            if (languages.isEmpty())
                null
            else languages.first()
        } else it.key
    }
}

private fun String.toDateOrNull() =
        try {
            DateTime(this).toDate()
        } catch(e: Exception) {
            null
        }

private fun MetaItem.toMap() : Any {
    return if (children.isEmpty())
        value
    else {
        val mappedChildren = children.associate { Pair(it.property, it.toMap()) }
        mappedChildren + Pair("@value", value)
    }
}
