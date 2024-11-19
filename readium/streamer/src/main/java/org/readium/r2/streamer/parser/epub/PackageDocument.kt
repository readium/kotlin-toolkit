/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.fromEpubHref
import org.readium.r2.shared.util.xml.ElementNode

internal data class PackageDocument(
    val path: Url,
    val epubVersion: Double,
    val uniqueIdentifierId: String?,
    val metadata: List<MetadataItem>,
    val manifest: List<Item>,
    val spine: Spine,
) {

    companion object {
        fun parse(document: ElementNode, filePath: Url): PackageDocument? {
            val packagePrefixes = document.getAttr("prefix")?.let { parsePrefixes(it) }.orEmpty()
            val prefixMap = PACKAGE_RESERVED_PREFIXES + packagePrefixes // prefix element overrides reserved prefixes
            val epubVersion = document.getAttr("version")?.toDoubleOrNull() ?: 1.2
            val metadata = MetadataParser(prefixMap).parse(document, filePath)
                ?: return null
            val manifestElement = document.getFirst("manifest", Namespaces.OPF)
                ?: return null
            val spineElement = document.getFirst("spine", Namespaces.OPF)
                ?: return null

            return PackageDocument(
                path = filePath,
                epubVersion = epubVersion,
                uniqueIdentifierId = document.getAttr("unique-identifier"),
                metadata = metadata,
                manifest = manifestElement.get("item", Namespaces.OPF)
                    .mapNotNull { Item.parse(it, filePath, prefixMap) },
                spine = Spine.parse(spineElement, prefixMap, epubVersion)
            )
        }
    }
}

internal data class Item(
    val href: Url,
    val id: String?,
    val fallback: String?,
    val mediaOverlay: String?,
    val mediaType: String?,
    val properties: List<String>,
) {
    companion object {
        fun parse(element: ElementNode, filePath: Url, prefixMap: Map<String, String>): Item? {
            val href = element.getAttr("href")
                ?.let { Url.fromEpubHref(it) }
                ?.let { filePath.resolve(it) }
                ?: return null
            val propAttr = element.getAttr("properties").orEmpty()
            val properties = parseProperties(propAttr).map {
                resolveProperty(
                    it,
                    prefixMap,
                    DEFAULT_VOCAB.ITEM
                )
            }
            return Item(
                href = href,
                id = element.id,
                fallback = element.getAttr("fallback"),
                mediaOverlay = element.getAttr("media-overlay"),
                mediaType = element.getAttr("media-type"),
                properties = properties
            )
        }
    }
}

internal data class Spine(
    val itemrefs: List<Itemref>,
    val direction: ReadingProgression?,
    val toc: String? = null,
) {
    companion object {
        fun parse(element: ElementNode, prefixMap: Map<String, String>, epubVersion: Double): Spine {
            val itemrefs = element.get("itemref", Namespaces.OPF).mapNotNull {
                Itemref.parse(
                    it,
                    prefixMap
                )
            }
            val pageProgressionDirection = when (element.getAttr("page-progression-direction")) {
                "rtl" -> ReadingProgression.RTL
                "ltr" -> ReadingProgression.LTR
                else -> null // null or "default"
            }
            val ncx = if (epubVersion < 3.0) element.getAttr("toc") else null
            return Spine(itemrefs, pageProgressionDirection, ncx)
        }
    }
}

internal data class Itemref(
    val idref: String,
    val linear: Boolean,
    val properties: List<String>,
) {
    companion object {
        fun parse(element: ElementNode, prefixMap: Map<String, String>): Itemref? {
            val idref = element.getAttr("idref") ?: return null
            val notLinear = element.getAttr("linear") == "no"
            val propAttr = element.getAttr("properties").orEmpty()
            val properties = parseProperties(propAttr)
                .map { resolveProperty(it, prefixMap, DEFAULT_VOCAB.ITEMREF) }
            return Itemref(idref, !notLinear, properties)
        }
    }
}
