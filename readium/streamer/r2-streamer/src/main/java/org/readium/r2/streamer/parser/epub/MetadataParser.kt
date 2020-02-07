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
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.streamer.parser.normalize

internal class MetadataParser (private val epubVersion: Double, private val prefixMap: Map<String, String>) {
    fun parse(document: ElementNode, filePath: String) : EpubMetadata? {
        val metadataElement = document.getFirst("metadata", Namespaces.Opf) ?: return null
        val (metas, links) = parseElements(metadataElement, filePath)
        val (globalMetas, refineMetas) = resolveMetaHierarchy(metas).partition { it.refines == null }
        val uniqueIdentifierId = document.getAttr("unique-identifier")
        val (metadata, remainder) = computeMetadata(globalMetas, uniqueIdentifierId)
        @Suppress("Unchecked_cast")
        val refineMap = refineMetas.groupBy(MetaItem::refines) as Map<String, List<MetaItem>>
        return EpubMetadata(metadata, links, remainder, refineMap)
    }

    private fun parseElements(metadataElement: ElementNode, filePath: String) : Pair<List<MetaItem>, List<EpubLink>> {
        val metas: MutableList<MetaItem> = mutableListOf()
        val links: MutableList<EpubLink> = mutableListOf()
        for (e in metadataElement.getAll()) {
            when {
                e.namespace == Namespaces.Dc -> parseDcElement(e)?.let { metas.add(it) }
                e.namespace == Namespaces.Opf && e.name == "meta" -> parseMetaElement(e)?.let { metas.add(it)  }
                e.namespace == Namespaces.Opf && e.name == "link" -> parseLinkElement(e, filePath)?.let { links.add(it) }
            }
        }
        return Pair(metas, links)
    }

    private fun parseLinkElement(element: ElementNode, filePath: String) : EpubLink? {
        val href = element.getAttr("href") ?: return null
        val relAttr = element.getAttr("rel").orEmpty()
        val rel = parseProperties(relAttr).mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.LINK) }
        val propAttr = element.getAttr("properties").orEmpty()
        val properties = parseProperties(propAttr).mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.LINK) }
        val mediaType = element.getAttr("media-type")
        val refines =  element.getAttr("refines")?.removePrefix("#")
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
            val resolvedScheme = element.getAttr("scheme")?.trim()?.ifEmpty { null }?.let { resolveProperty(it, prefixMap) }
            val refines = element.getAttr("refines")?.removePrefix("#")
            MetaItem(resolvedProp, propValue, element.lang, resolvedScheme, refines, element.id)
        }
    }

    private fun parseDcElement(element: ElementNode): MetaItem? {
        val propValue = element.text?.trim()?.ifEmpty{ null } ?: return null
        val propName = Namespaces.Dcterms + element.name
        return when(element.name) {
            "creator", "contributor", "publisher" -> contributorFromLegacy(element, propName, propValue)
            "date" -> dateFromLegacy(element, propName, propValue)
            else -> MetaItem(propName, propValue, lang=element.lang, id=element.id)
        }
    }

    private fun contributorFromLegacy(element: ElementNode, name: String, value: String) : MetaItem {
        val fileAs = element.getAttrNs("file-as", Namespaces.Opf)?.let {
            MetaItem(DEFAULT_VOCAB.META.iri + "file-as", value=it,  lang=element.lang, id=element.id)
        }
        val role = element.getAttrNs("role", Namespaces.Opf)?.let {
            MetaItem(DEFAULT_VOCAB.META.iri + "role", it, lang=element.lang, id=element.id)
        }
        val children = listOfNotNull(fileAs, role)
        return MetaItem(name, value, lang=element.lang, id=element.id, children=children)
    }

    private fun dateFromLegacy(element: ElementNode, name: String, value: String) : MetaItem? {
        val eventAttr = element.getAttrNs("event", Namespaces.Opf)
        val propName = if (eventAttr == "modification") Namespaces.Dcterms + "modified" else name
        return MetaItem(propName, value, lang=element.lang, id=element.id)
    }

    private fun resolveMetaHierarchy(items: List<MetaItem>): List<MetaItem> {
        val metadataIds = items.mapNotNull { it.id }
        val rootExpr = items.filter { it.refines == null || it.refines !in metadataIds }
        @Suppress("Unchecked_cast")
        val exprByRefines = items.groupBy(MetaItem::refines) as Map<String, List<MetaItem>>
        return rootExpr.map { computeMetaItem(it, exprByRefines, emptySet()) }
    }

    private fun computeMetaItem(expr: MetaItem, metas: Map<String, List<MetaItem>>, chain: Set<String>) : MetaItem {
        val refinedBy = expr.id?.let { metas[it] }?.filter { it.id !in chain }.orEmpty()
        val updatedChain = if (expr.id == null) chain else chain + expr.id
        val newChildren = refinedBy.map { computeMetaItem(it, metas, updatedChain) }
        return expr.copy(children=expr.children + newChildren)
    }

    private fun computeMetadata(metas: List<MetaItem>, uniqueIdentifierId: String?): Pair<GeneralMetadata, List<MetaItem>> {
        val remainder: MutableList<MetaItem> = LinkedList()
        val titles: MutableList<Title> = LinkedList()
        val languages: MutableList<String> = LinkedList()
        val subjects: MutableList<Subject> = LinkedList()
        val identifiers: MutableMap<String?, String> = mutableMapOf()

        val creators: MutableList<Contributor> = LinkedList()
        val contributors: MutableList<Contributor> = LinkedList()
        val publishers: MutableList<Contributor> = LinkedList()
        val narrators: MutableList<Contributor> = LinkedList()

        var description: String? = null
        var published: Date? = null
        var modified: Date? = null

        for (meta in metas) {
            when (meta.property) {
                Namespaces.Dcterms + "identifier" -> identifiers[meta.id] = meta.value
                Namespaces.Dcterms + "title" -> titles.add(parseTitle(meta, metas))
                Namespaces.Dcterms + "language" -> languages.add(meta.value)
                Namespaces.Dcterms + "creator"  -> creators.add(parseContributor(meta))
                Namespaces.Dcterms + "contributor" -> contributors.add(parseContributor(meta))
                Namespaces.Dcterms + "publisher" -> publishers.add(parseContributor(meta))
                PACKAGE_RESERVED_PREFIXES["media"] + "narrator" -> narrators.add(parseContributor((meta)))
                Namespaces.Dcterms + "date" -> meta.value.toDateOrNull()?.let { if (published == null) published = it }
                Namespaces.Dcterms + "modified" -> modified = meta.value.toDateOrNull()
                Namespaces.Dcterms + "description" -> description = meta.value
                Namespaces.Dcterms + "subject" -> subjects.add(parseSubject(meta))
                else -> remainder.add(meta)
            }
        }

        val uniqueIdentifier = identifiers[uniqueIdentifierId] ?: identifiers.values.firstOrNull()
        val metadata = GeneralMetadata(
                uniqueIdentifier, titles, languages,
                published, modified, description, subjects,
                creators, contributors, publishers, narrators
        )
        return Pair(metadata, remainder)
    }

    private fun parseTitle(item: MetaItem, others: List<MetaItem>) : Title {
        val values: MutableMap<String?, String> = mutableMapOf(item.lang to item.value)
        var type: String? = null
        var fileAs: String? = null
        var displaySeq: Int? = null

        if (epubVersion < 3.0)
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

    private fun parseContributor(item: MetaItem): Contributor {
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
        return Contributor(localizedName=LocalizedString.fromStrings(names), sortAs=fileAs, roles=roles)
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
        return Subject(LocalizedString.fromStrings(values), fileAs, authority, term)
    }
}

private fun String.toDateOrNull() =
    try {
        DateTime(this).toDate()
    } catch(e: Exception) {
        null
    }