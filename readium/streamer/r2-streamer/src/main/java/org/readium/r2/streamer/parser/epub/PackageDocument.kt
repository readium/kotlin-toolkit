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
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.streamer.parser.normalize

internal data class PackageDocument(
    val path: String,
    val epubVersion: Double,
    val uniqueIdentifierId: String?,
    val metadata: EpubMetadata,
    val manifest: List<Item>,
    val spine: Spine
) {

    companion object {
        fun parse(document: ElementNode, filePath: String): PackageDocument? {
            val packagePrefixes = document.getAttr("prefix")?.let { parsePrefixes(it) }.orEmpty()
            val prefixMap = PACKAGE_RESERVED_PREFIXES + packagePrefixes // prefix element overrides reserved prefixes

            val epubVersion = document.getAttr("version")?.toDoubleOrNull() ?: 1.2
            val uniqueIdentifierId = document.getAttr("unique-identifier")
            val metadata = MetadataParser(epubVersion, prefixMap).parse(document, filePath)
                ?: return null

            val manifestElement = document.getFirst("manifest", Namespaces.Opf) ?: return null
            val manifest =
                manifestElement.get("item", Namespaces.Opf).mapNotNull { Item.parse(it, filePath, prefixMap) }
            val spineElement = document.getFirst("spine", Namespaces.Opf) ?: return null
            val spine = Spine.parse(spineElement, prefixMap, epubVersion)
            return PackageDocument(filePath, epubVersion, uniqueIdentifierId, metadata, manifest, spine)
        }
    }
}

internal data class Item(
    val href: String,
    val id: String?,
    val fallback: String?,
    val mediaOverlay: String?,
    val mediaType: String?,
    val properties: List<String>
) {
    companion object {
        fun parse(element: ElementNode, filePath: String, prefixMap: Map<String, String>): Item? {
            val href = element.getAttr("href")?.let { normalize(filePath, it) } ?: return null
            val propAttr = element.getAttr("properties").orEmpty()
            val properties = parseProperties(propAttr).mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.ITEM) }
            return Item(
                href,
                element.id,
                element.getAttr("fallback"),
                element.getAttr("media-overlay"),
                element.getAttr("media-type"),
                properties
            )
        }
    }
}

internal data class Spine(
    val itemrefs: List<Itemref>,
    val direction: ReadingProgression,
    val toc: String? = null
) {
    companion object {
        fun parse(element: ElementNode, prefixMap: Map<String, String>, epubVersion: Double): Spine {
            val itemrefs = element.get("itemref", Namespaces.Opf).mapNotNull { Itemref.parse(it, prefixMap) }
            val pageProgressionDirection = when (element.getAttr("page-progression-direction")) {
                "rtl" -> ReadingProgression.RTL
                "ltr" -> ReadingProgression.LTR
                else -> ReadingProgression.AUTO // null or "default"
            }
            val ncx = if (epubVersion >= 3.0) element.getAttr("toc") else null
            return Spine(itemrefs, pageProgressionDirection, ncx)
        }
    }
}

internal data class Itemref(
    val idref: String,
    val linear: Boolean,
    val properties: List<String>
) {
    companion object {
        fun parse(element: ElementNode, prefixMap: Map<String, String>): Itemref? {
            val idref = element.getAttr("idref") ?: return null
            val notLinear = element.getAttr("linear") == "no"
            val propAttr = element.getAttr("properties").orEmpty()
            val properties = parseProperties(propAttr)
                .mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.ITEMREF) }
            return Itemref(idref, !notLinear, properties)
        }
    }
}
