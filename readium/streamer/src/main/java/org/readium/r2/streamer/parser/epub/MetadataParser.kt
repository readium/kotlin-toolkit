/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.fromEpubHref
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.xml.ElementNode

internal class MetadataParser(
    private val prefixMap: Map<String, String>,
) {

    fun parse(document: ElementNode, filePath: Url): List<MetadataItem>? {
        val metadata = document.getFirst("metadata", Namespaces.OPF)
            ?: return null
        val items = parseElements(metadata, filePath)
        return resolveItemsHierarchy(items)
    }

    private fun parseElements(metadataElement: ElementNode, filePath: Url): List<MetadataItem> {
        val oldMetas: MutableList<ElementNode> = mutableListOf()
        val newMetas: MutableList<ElementNode> = mutableListOf()
        val links: MutableList<ElementNode> = mutableListOf()
        val dcItems: MutableList<ElementNode> = mutableListOf()

        metadataElement
            .getAll()
            .forEach { e ->
                when {
                    e.namespace == Namespaces.DC -> {
                        dcItems.add(e)
                    }
                    e.namespace == Namespaces.OPF && e.name == "meta" -> {
                        if (e.getAttr("property") == null) {
                            oldMetas.add(e)
                        } else {
                            newMetas.add(e)
                        }
                    }
                    e.namespace == Namespaces.OPF && e.name == "link" -> {
                        links.add(e)
                    }
                    else -> {}
                }
            }

        val parsedNewMetas = newMetas
            .mapNotNull { parseNewMetaElement(it) }

        val propertiesFromGlobalNewMetas = parsedNewMetas
            .filter { it.refines == null }
            .map { it.property }
            .toSet()

        val parsedOldMetas = oldMetas
            .mapNotNull { parseOldMetaElement(it) }
            // Ignore EPUB2 fallbacks in EPUB3
            .filter { it.property !in propertiesFromGlobalNewMetas }

        return parsedNewMetas + parsedOldMetas +
            dcItems.mapNotNull { parseDcElement(it) } +
            links.mapNotNull { parseLinkElement(it, filePath) }
    }

    private fun parseLinkElement(element: ElementNode, filePath: Url): MetadataItem.Link? {
        val href = element.getAttr("href")?.let { Url.fromEpubHref(it) } ?: return null
        val relAttr = element.getAttr("rel").orEmpty()
        val rel = parseProperties(relAttr).map { resolveProperty(it, prefixMap, DEFAULT_VOCAB.LINK) }
        val propAttr = element.getAttr("properties").orEmpty()
        val properties = parseProperties(propAttr).map {
            resolveProperty(
                it,
                prefixMap,
                DEFAULT_VOCAB.LINK
            )
        }
        val mediaType = element.getAttr("media-type")
        val refines = element.getAttr("refines")?.removePrefix("#")
        return MetadataItem.Link(
            id = element.id,
            refines = refines,
            href = Href(filePath.resolve(href)),
            rels = rel.toSet(),
            mediaType = mediaType?.let { MediaType(it) },
            properties = properties
        )
    }

    private fun parseNewMetaElement(element: ElementNode): MetadataItem.Meta? {
        val propName = element.getAttr("property")?.trim()?.ifEmpty { null }
            ?: return null
        val propValue = element.text?.trim()?.ifEmpty { null }
            ?: return null
        val resolvedProp = resolveProperty(propName, prefixMap, DEFAULT_VOCAB.META)
        val resolvedScheme =
            element.getAttr("scheme")?.trim()?.ifEmpty { null }?.let {
                resolveProperty(
                    it,
                    prefixMap
                )
            }
        val refines = element.getAttr("refines")?.removePrefix("#")
        return MetadataItem.Meta(
            id = element.id,
            refines = refines,
            property = resolvedProp,
            value = propValue,
            lang = element.lang,
            scheme = resolvedScheme
        )
    }

    private fun parseOldMetaElement(element: ElementNode): MetadataItem.Meta? {
        val name = element.getAttr("name")?.trim()?.ifEmpty { null }
            ?: return null
        val content = element.getAttr("content")?.trim()?.ifEmpty { null }
            ?: return null
        val resolvedName = resolveProperty(name, prefixMap)
        return MetadataItem.Meta(
            id = element.id,
            refines = null,
            property = resolvedName,
            value = content,
            lang = element.lang
        )
    }

    private fun parseDcElement(element: ElementNode): MetadataItem.Meta? {
        val propValue = element.text?.trim()?.ifEmpty { null } ?: return null
        val propName = Vocabularies.DCTERMS + element.name
        return when (element.name) {
            "creator", "contributor", "publisher" -> contributorWithLegacyAttr(
                element,
                propName,
                propValue
            )
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

    private fun computeMetadataItem(
        expr: MetadataItem,
        items: Map<String, List<MetadataItem>>,
        chain: Set<String>,
    ): MetadataItem {
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

internal sealed class MetadataItem {

    abstract val id: String?
    abstract val children: List<MetadataItem>
    abstract val refines: String?

    data class Link(
        override val id: String?,
        override val refines: String?,
        override val children: List<MetadataItem> = emptyList(),
        val href: Href,
        val rels: Set<String>,
        val mediaType: MediaType?,
        val properties: List<String> = emptyList(),
    ) : MetadataItem() {
        fun url(): Url = href.resolve()
    }

    data class Meta(
        override val id: String?,
        override val refines: String? = null,
        override val children: List<MetadataItem> = emptyList(),
        val property: String,
        val value: String,
        val lang: String,
        val scheme: String? = null,
    ) : MetadataItem()
}
