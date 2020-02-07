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
import org.readium.r2.streamer.parser.normalize
import java.util.Date
import java.util.LinkedList


internal class MetadataParser (private val epubVersion: Double, private val prefixMap: Map<String, String>) {
    fun parse(document: ElementNode, filePath: String) : EpubMetadata? {
        val metadataElement = document.getFirst("metadata", Namespaces.Opf) ?: return null
        val links = metadataElement.get("link", Namespaces.Opf).mapNotNull{ parseLink(it, prefixMap, filePath) }
        val metas = if (epubVersion >= 3.0) parseMetaElements(metadataElement) else parseXhtmlMetas(metadataElement)
        val metasByRefines = metas.groupBy(MetaItem::refines)

        val uniqueIdentifierId = document.getAttr("unique-identifier")
        val dcElements = metadataElement.getAll().filter { it.namespace == Namespaces.Dc }
        val dcMetadata = parseDcElements(dcElements, metasByRefines, uniqueIdentifierId)
        return EpubMetadata(dcMetadata, links, metasByRefines)
    }

    private fun parseLink(element: ElementNode, prefixMap: Map<String, String>, filePath: String) : EpubLink? {
        val href = element.getAttr("href") ?: return null
        val relAttr = element.getAttr("rel").orEmpty()
        val rel = parseProperties(relAttr).mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.LINK) }
        val propAttr = element.getAttr("properties").orEmpty()
        val properties = parseProperties(propAttr).mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.LINK) }
        return EpubLink(normalize(filePath, href), rel, element.getAttr("media-type"), element.getAttr("refines"), properties)
    }

    private fun parseDcElements(dcElements: List<ElementNode>,
                                metasByRefines: Map<String?, List<MetaItem>>,
                                uniqueIdentifierId: String?): GeneralMetadata {

        val titles: MutableList<Title> = LinkedList()
        val languages: MutableList<String> = LinkedList()

        val creators: MutableList<Contributor> = LinkedList()
        val contributors: MutableList<Contributor> = LinkedList()
        val publishers: MutableList<Contributor> = LinkedList()

        val dates: MutableList<DateEvent> = mutableListOf()
        val identifiers: MutableMap<String?, String> = mutableMapOf()
        val subjects: MutableList<Subject> = mutableListOf()

        var description: String? = null
        var rights: String? = null
        var source: String? = null

        for (el in dcElements) {
            val props = metasByRefines[el.id]
            when (el.name) {
                "identifier" -> el.text?.let { identifiers[el.id] = it }
                "title" -> parseTitle(el, metasByRefines)?.let { titles.add(it) }
                "language" -> el.text?.trim()?.let { languages.add(it) }
                "creator" -> parseContributor(el, props)?.let { creators.add(it) }
                "contributor" -> parseContributor(el, props)?.let { contributors.add(it) }
                "publisher" -> parseContributor(el, props)?.let { publishers.add(it) }
                "date" -> parseDate(el)?.let { dates.add(it) }
                "description" ->  el.text?.let { description = it }
                "subject" -> parseSubject(el, props)?.let { subjects.add(it) }
                "rights" -> el.text?.let { rights = it }
                "source" -> el.text?.let { source = it }
            }
        }

        val uniqueIdentifier = identifiers[uniqueIdentifierId] ?: identifiers.values.firstOrNull()

        val (published, modified) = when {
            epubVersion >= 3.0 -> {
                val modified = metasByRefines[null]
                        ?.firstOrNull { it.property == PACKAGE_RESERVED_PREFIXES["dcterms"] + "modified" }
                        ?.value?.toDateOrNull()
                val published = dates.firstOrNull()?.date
                Pair(published, modified)
            }
            else -> {
                val modified = dates.firstOrNull { it.event == "modification" }?.date
                val published = dates.firstOrNull { it.event == "publication" }?.date
                        ?: dates.firstOrNull { it.event == null }?.date
                Pair(published, modified)
            }
        }

        return GeneralMetadata(
                uniqueIdentifier,
                titles,
                languages,
                published,
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

    private fun parseTitle(node: ElementNode, metas: Map<String?, List<MetaItem>>): Title? {
        val values: MutableMap<String?, String> = mutableMapOf()
        values[node.lang] = node.text?.trim()?.ifEmpty { null } ?: return null
        var type: String? = null
        var fileAs: String? = null
        var displaySeq: Int? = null

        if (epubVersion >= 3.0) {
            metas[node.id]?.forEach {
                when (it.property) {
                    DEFAULT_VOCAB.META.iri + "alternate-script" -> if (it.lang != null) values[it.lang] = it.value
                    DEFAULT_VOCAB.META.iri + "file-as" -> fileAs = it.value
                    DEFAULT_VOCAB.META.iri + "title-type" -> type = it.value
                    DEFAULT_VOCAB.META.iri + "display-seq" -> it.value.toIntOrNull()?.let { v -> displaySeq = v }
                }
            }
        } else {
            fileAs = metas[null]?.firstOrNull { it.property == "calibre:title_sort" }?.value
        }
        return Title(LocalizedString.fromStrings(values), fileAs, type, displaySeq)
    }

    private fun parseContributor(node: ElementNode, props: List<MetaItem>?): Contributor? {
        val names: MutableMap<String?, String> = mutableMapOf()
        names[node.lang] = node.text?.trim()?.ifEmpty{ null } ?: return null
        val roles: MutableSet<String> = mutableSetOf()
        var fileAs: String? = null

        if (epubVersion >= 3.0) {
            props?.forEach {
                when (it.property) {
                    DEFAULT_VOCAB.META.iri + "alternate-script" -> if (it.lang != null) names[it.lang] = it.value
                    DEFAULT_VOCAB.META.iri + "file-as" -> fileAs = it.value
                    DEFAULT_VOCAB.META.iri + "role" -> roles.add(it.value)
                }
            }
        } else {
            node.getAttrNs("role", Namespaces.Opf)?.let { roles.add(it) }
            node.getAttrNs("file-as", Namespaces.Opf)?.let { fileAs = it }
        }
        return Contributor(
                localizedName = LocalizedString.fromStrings(names),
                sortAs = fileAs,
                roles = roles
        )
    }

    private fun parseDate(node: ElementNode) : DateEvent? {
        val date = node.text?.trim()?.toDateOrNull() ?: return null
        return DateEvent(date, node.getAttrNs("event", Namespaces.Opf))
    }

    private fun parseSubject(node: ElementNode, props: List<MetaItem>?) : Subject? {
        val values: MutableMap<String?, String> = mutableMapOf()
        node.text?.trim()?.let { values[node.lang] = it }
        var authority: String? = null
        var term: String? = null
        var fileAs: String? = null

        props?.forEach {
            when (it.property) {
                DEFAULT_VOCAB.META.iri + "alternate-script" -> if (it.lang != null) values[it.lang] = it.value
                DEFAULT_VOCAB.META.iri + "authority" -> authority = it.value
                DEFAULT_VOCAB.META.iri + "term" -> term = it.value
                DEFAULT_VOCAB.META.iri + "file-as" -> fileAs = it.value
            }
        }

        return if (values.isNotEmpty() || (authority != null && term != null))
            Subject(
                    LocalizedString.fromStrings(values),
                    fileAs,
                    authority,
                    term
                )
        else null
    }

    private fun parseXhtmlMetas(metadataElement: ElementNode) : List<MetaItem> =
            metadataElement.get("meta", Namespaces.Opf)
                    .mapNotNull {
                        val name = it.getAttr("name")
                        val content =  it.getAttr("content")
                        if (name != null && content != null) MetaItem(name, content) else null }

    private fun parseMetaElements(metadataElement: ElementNode): List<MetaItem> {
        val metaElements = metadataElement.get("meta", Namespaces.Opf)
        val expressions = metaElements.mapNotNull { parseMetaExpression(it) }
        val metaIds = expressions.mapNotNull { it.id }
        val (primaryExpr, secondExpr) = expressions.partition { it.refines == null || it.refines !in metaIds }
        @Suppress("Unchecked_cast")
        val secondExprByRefines = secondExpr.groupBy(MetaExpr::refines) as Map<String, List<MetaExpr>>
        return primaryExpr.map { computeMetaItem(it, secondExprByRefines, emptyList()) }
    }

    private fun parseMetaExpression(element: ElementNode): MetaExpr? {
        val propName = element.getAttr("property")?.trim()?.ifEmpty { null } ?: return null
        val propValue = element.text?.trim()?.ifEmpty{ null } ?: return null
        val resolvedProp = resolveProperty(propName, prefixMap, DEFAULT_VOCAB.META) ?: return null
        val resolvedScheme = element.getAttr("scheme")?.trim()?.ifEmpty { null }?.let { resolveProperty(it, prefixMap) }
        val refines = element.getAttr("refines")?.removePrefix("#")
        return MetaExpr(resolvedProp, propValue, resolvedScheme, refines, element.id, element.lang)
    }

    private fun computeMetaItem(expr: MetaExpr,
                                metasByRefines: Map<String, List<MetaExpr>>,
                                refineChain: List<String>) : MetaItem {

        val refinedBy = metasByRefines[expr.id]?.filter { it.id !in refineChain }.orEmpty()
        val updatedChain = if (expr.id == null) refineChain else refineChain + expr.id
        val children = refinedBy.map { computeMetaItem(it, metasByRefines, updatedChain) }
        return MetaItem(expr.property, expr.value, expr.scheme, expr.refines, expr.lang, children)
    }
}

private data class MetaExpr(
        val property: String,
        val value: String,
        val scheme: String?,
        val refines: String?,
        val id: String?,
        val lang: String
)

private data class DateEvent(val date: Date, val event: String?)

private fun String.toDateOrNull() =
    try {
        DateTime(this).toDate()
    } catch(e: Exception) {
        null
    }