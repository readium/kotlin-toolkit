/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.streamer.parser.normalize

internal class MetadataParser (private val epubVersion: Double, private val prefixMap: Map<String, String>) {
    fun parse(document: ElementNode, filePath: String) : EpubMetadata? {
        val metadata = document.getFirst("metadata", Namespaces.Opf) ?: return null
        val uniqueIdentifierId = document.getAttr("unique-identifier")
        val (metas, links) = parseElements(metadata, filePath)
        val (globalMetas, refineMetas) = resolveMetaHierarchy(metas).partition { it.refines == null }
        @Suppress("Unchecked_cast")
        val refineMap = refineMetas.groupBy(MetaItem::refines) as Map<String, List<MetaItem>>
        return EpubMetadata(uniqueIdentifierId, globalMetas, refineMap, links)
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
}