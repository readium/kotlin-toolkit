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
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.LocalizedString
import java.lang.Exception
import java.util.*
import org.readium.r2.streamer.parser.normalize


internal class MetadataParser (private val epubVersion: Double, private val prefixMap: Map<String, String>) {
    fun parse(document: ElementNode, filePath: String) : Metadata? {
        val metadataElement = document.getFirst("metadata", Namespaces.Opf) ?: return null
        val links = parseLinks(metadataElement.get("link", Namespaces.Opf), prefixMap, filePath)
        val (globalProperties, otherProperties) = MetadataExpressionParser(prefixMap).parse(metadataElement)

        val uniqueIdentifierId = document.getAttr("unique-identifier")
        val dcElements = metadataElement.getAll().filter { it.namespace == Namespaces.Dc }
        val modified : java.util.Date? = globalProperties.firstOrNull { it.property == PACKAGE_RESERVED_PREFIXES["dcterms"] + "modified" }
                    ?.value?.let { parseModified(it) }

        val dcMetadata = parseDcElements(dcElements, otherProperties, uniqueIdentifierId, modified)
        val rendition = parseRenditionProperties(globalProperties)
        val media = parseMediaProperties(globalProperties, otherProperties)

        val oldMeta = if (epubVersion >= 3.0) mapOf() else parseXhtmlMeta(metadataElement)

        return Metadata(dcMetadata, media, rendition, links, oldMeta)
    }

    private fun parseModified(date: String) =
        try {
            DateTime(date).toDate()
        } catch(e: Exception) {
            null
        }

    private fun parseLinks(elements: List<ElementNode>, prefixMap: Map<String, String>, filePath: String) : List<Link> =
        elements.mapNotNull {
            val href = it.getAttr("href")
            val rel = it.getAttr("rel")?.split("""\\s+""".toRegex())
                    ?.mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.LINK) }
            if (href != null && rel != null) {
                val properties = it.getAttr("properties")?.split("""\\s+""".toRegex())
                        ?.mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.LINK) }
                        ?: listOf()
                Link(rel, normalize(filePath, href), it.getAttr("media-type"), it.getAttr("refines"), properties)
            } else null
        }

    private fun parseDcElements(dcElements: List<ElementNode>,
                                metaProperties: Map<String, List<Property>>,
                                uniqueIdentifierId: String?,
                                dctermsModified: java.util.Date?): GeneralMetadata {
        var uniqueIdentifier: String? = null
        val titles: MutableList<Title> = LinkedList()
        val languages: MutableList<String> = LinkedList()

        val creators: MutableList<Contributor> = LinkedList()
        val contributors: MutableList<Contributor> = LinkedList()
        val publishers: MutableList<Contributor> = LinkedList()

        val dates: MutableList<Date> = mutableListOf()
        val subjects: MutableList<Subject> = mutableListOf()
        var description: String? = null
        var rights: String? = null
        var source: String? = null

        dcElements.forEach {
            val props = metaProperties[it.id]
            when (it.name) {
                "identifier" -> {
                    val id = it.text
                    if (id != null && (it.id == uniqueIdentifierId || uniqueIdentifier == null)) {
                        uniqueIdentifier = id
                    }
                }
                "title" -> parseTitle(it, props)?.let { titles.add(it) }
                "language" -> it.text?.let { languages.add(it) }
                "creator" -> parseContributor(it, props)?.let { creators.add(it) }
                "contributor" -> parseContributor(it, props)?.let { contributors.add(it) }
                "publisher" -> parseContributor(it, props)?.let { publishers.add(it) }
                "date" -> parseDate(it)?.let { dates.add(it) }
                "description" ->  it.text?.let { description = it }
                "subject" -> parseSubject(it, props)?.let { subjects.add(it) }
                "rights" -> it.text?.let { rights = it }
                "source" -> it.text?.let { source = it }
            }
        }

        val modified = if (epubVersion >= 3.0)
            dctermsModified
        else
            dates.firstOrNull { it.event == "modification"}?.value?.let { parseModified(it) }

        val date = if (epubVersion >= 3.0)
            dates.firstOrNull()?.value
        else
            dates.firstOrNull { it.event == "publication"}?.value ?: dates.firstOrNull { it.event == null}?.value


        return GeneralMetadata(
                uniqueIdentifier,
                titles,
                languages,
                date,
                modified,
                description,
                rights,
                source,
                subjects,
                creators,
                contributors,
                publishers
        )
    }

    private fun parseTitle(node: ElementNode, props: List<Property>?): Title? {
        val values: MutableMap<String?, String> = mutableMapOf()
        values[node.lang] = node.text ?: return null
        var type: String? = null
        var fileAs: String? = null
        var displaySeq: Int? = null

        if (epubVersion >= 3.0) {
            props?.forEach {
                when (it.property) {
                    DEFAULT_VOCAB.META.iri + "alternate-script" -> if (it.lang != null) values[it.lang] = it.value
                    DEFAULT_VOCAB.META.iri + "file-as" -> fileAs = it.value
                    DEFAULT_VOCAB.META.iri + "title-type" -> type = it.value
                    DEFAULT_VOCAB.META.iri + "display-seq" -> it.value.toIntOrNull()?.let { v -> displaySeq = v }
                }
            }

        } else {
            /* TODO: According to https://github.com/readium/architecture/blob/master/streamer/parser/metadata.md
             "The string for fileAs should be the value of content in a meta
             whose name is calibre:title_sort and content is the value to use."
              */
        }
        return Title(LocalizedString.fromStrings(values), fileAs, type, displaySeq)
    }

    private fun parseContributor(node: ElementNode, props: List<Property>?): Contributor? {
        val names: MutableMap<String?, String> = mutableMapOf()
        names[node.lang] = node.text ?: return null
        val roles: MutableSet<String> = mutableSetOf()
        var fileAs: String? = null

        if (epubVersion >= 3.0) {
            props?.forEach {
                when (it.property) {
                    DEFAULT_VOCAB.META.iri + "alternate-script" -> if (it.lang != null) names[it.lang] = it.value
                    DEFAULT_VOCAB.META.iri + "file-as" -> fileAs = it.value
                    DEFAULT_VOCAB.META.iri + "role" ->
                        if (it.scheme == PACKAGE_RESERVED_PREFIXES["marc"] + "relators") roles.add(it.value)
                }
            }
        } else {
            node.getAttrNs("role", Namespaces.Opf)?.let { roles.add(it) }
            node.getAttrNs("file-as", Namespaces.Opf)?.let { fileAs = it }
        }
        return Contributor(
                localizedName = LocalizedString.fromStrings(names),
                sortAs = fileAs,
                roles = roles)
    }

    private fun parseDate(node: ElementNode) : Date? =
            node.text?.let {
                Date(it, node.getAttr("event"))
            }

    private fun parseSubject(node: ElementNode, props: List<Property>?) : Subject? {
        val values: MutableMap<String?, String> = mutableMapOf()
        node.text?.let { values[node.lang] = it }
        var authority: String? = null
        var term: String? = null

        props?.forEach {
            when (it.property) {
                DEFAULT_VOCAB.META.iri + "alternate-script" -> if (it.lang != null) values[it.lang] = it.value
                DEFAULT_VOCAB.META.iri + "authority" -> authority = it.value
                DEFAULT_VOCAB.META.iri + "term" -> term = it.value
            }
        }

        return if (values.isNotEmpty() || (authority != null && term != null))
            Subject(
                LocalizedString.fromStrings(values),
                authority,
                term

        ) else null
    }

    private fun parseRenditionProperties(properties: Collection<Property>): RenditionMetadata {
        var flow: RenditionMetadata.Flow = RenditionMetadata.Flow.default
        var layout: RenditionMetadata.Layout = RenditionMetadata.Layout.default
        var orientation: RenditionMetadata.Orientation = RenditionMetadata.Orientation.default
        var spread: RenditionMetadata.Spread = RenditionMetadata.Spread.default

        properties.forEach {
            when (it.property) {
                PACKAGE_RESERVED_PREFIXES["rendition"] + "flow" ->
                    if (it.value in RenditionMetadata.Flow.names)
                        flow = RenditionMetadata.Flow.get(it.value)
                PACKAGE_RESERVED_PREFIXES["rendition"] + "layout" ->
                    if (it.value in RenditionMetadata.Layout.names)
                        layout = RenditionMetadata.Layout.get(it.value)
                PACKAGE_RESERVED_PREFIXES["rendition"] + "orientation" ->
                    if (it.value in RenditionMetadata.Orientation.names)
                        orientation = RenditionMetadata.Orientation.get(it.value)
                PACKAGE_RESERVED_PREFIXES["rendition"] + "spread" -> {
                    val value = if (it.value == "portrait") "both" else it.value
                    if (value in RenditionMetadata.Spread.names)
                        spread = RenditionMetadata.Spread.get(value)
                }
            }
        }
        return RenditionMetadata(flow, layout, orientation, spread)
    }

    private fun parseMediaProperties(globalProperties: Collection<Property>,
                                     propertyById: Map<String, List<Property>>): MediaMetadata {
        var activeClass: String? = null
        var playbackActiveClass: String? = null
        val narrators: MutableList<Contributor> = LinkedList()
        var duration: Double? = null

        globalProperties.forEach {
            when (it.property) {
                PACKAGE_RESERVED_PREFIXES["media"] + "active-class" ->
                    activeClass = it.value
                PACKAGE_RESERVED_PREFIXES["media"] + "playback-active-class" ->
                    playbackActiveClass = it.value
                PACKAGE_RESERVED_PREFIXES["media"] + "duration" ->
                    duration = ClockValueParser.parse(it.value)
                PACKAGE_RESERVED_PREFIXES["media"] + "narrator" -> {
                    val names: Map<String?, String> = it.children.filter { c -> c.property == "alternate-script" && c.lang != null }
                            .associate { Pair(it.lang as String, it.value) }
                    val fileAs = it.children.firstOrNull { c -> c.property == "file-as" }?.value
                    narrators.add(Contributor( localizedName = LocalizedString.fromStrings(names), sortAs = fileAs, roles = setOf("nrt")))
                }
            }
        }

        @Suppress("Unchecked_cast")
        val durationById = propertyById
                .mapValues { v->
                    v.value.firstOrNull { it.property == PACKAGE_RESERVED_PREFIXES["media"] + "duration" }
                            ?.value?.let { ClockValueParser.parse(it) }
                }
                .filterValues { it != null } as Map<String, Double>

        return MediaMetadata(
                duration,
                durationById,
                activeClass,
                playbackActiveClass,
                narrators)
    }

    private class MetadataExpressionParser(val prefixMap: Map<String, String>) {
        private data class MetaExpression(val property: String, val value: String, val scheme: String? = null,
                                          val refines: String? = null, val id: String? = null, val lang: String? = null)

        fun parse(metadataElement: ElementNode): Pair<List<Property>, Map<String, List<Property>>> {
            val metaElements = metadataElement.get("meta", Namespaces.Opf)
            val metadataExpr = metaElements.mapNotNull { parseExpression(it) }
            val metadataProp = metadataExpr.map { Property(it.property, it.value, it.scheme, it.id, it.lang) }

            val itemById = metadataProp.associateBy(Property::id)
            val globalItems: MutableList<Property> = mutableListOf()
            val mainItems: MutableMap<String, MutableList<Property>> = mutableMapOf()
            metadataExpr.zip(metadataProp).forEach {
                val refinedId = it.first.refines?.removePrefix("#")
                val refinedProp = refinedId?.let { itemById[it] }
                when {
                    refinedProp != null -> refinedProp.children.add(it.second) //  subexpression refining subexpression
                    refinedId != null -> { // subexpression refining primary expression
                        if (!mainItems.containsKey(refinedId)) mainItems[refinedId] = mutableListOf()
                        mainItems[refinedId]?.add(it.second)
                    }
                    else -> globalItems.add(it.second) // primary expression
                }
            }
            return Pair(globalItems, mainItems)
        }

        private fun parseExpression(element: ElementNode): MetaExpression? {
            val propName = element.getAttr("property") ?: return null
            val propValue = element.text ?: return null
            val resolvedProp = resolveProperty(propName, prefixMap, DEFAULT_VOCAB.META)
                    ?: return null
            val resolvedScheme = element.getAttr("scheme")?.let { resolveProperty(it, prefixMap) }
            val refines = element.getAttr("refines")
            val lang = element.lang
            return MetaExpression(resolvedProp, propValue, resolvedScheme, refines, element.id, lang)
        }
    }

    private fun parseXhtmlMeta(metadataElement: ElementNode) : Map<String, String> =
            metadataElement.get("meta", Namespaces.Opf)
            .mapNotNull {
                val name = it.getAttr("name")
                val content =  it.getAttr("content")
                if (name != null && content != null) Pair(name, content)
                else null }
             .associate { it }
}

private data class Property(
        val property: String,
        val value: String,
        val scheme: String? = null,
        val id: String? = null,
        val lang: String? = null,
        val children: MutableList<Property> = mutableListOf()
)