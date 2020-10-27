/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.normalize
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.util.Href

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
            val metadata = MetadataParser(epubVersion, prefixMap).parse(document, filePath)
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
    val href: String,
    val id: String?,
    val fallback: String?,
    val mediaOverlay: String?,
    val mediaType: String?,
    val properties: List<String>
) {
    companion object {
        fun parse(element: ElementNode, filePath: String, prefixMap: Map<String, String>): Item? {
            val href = element.getAttr("href")?.let { Href(it, baseHref = filePath).string }
                ?: return null
            val propAttr = element.getAttr("properties").orEmpty()
            val properties = parseProperties(propAttr).mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.ITEM) }
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
    val direction: ReadingProgression,
    val toc: String? = null
) {
    companion object {
        fun parse(element: ElementNode, prefixMap: Map<String, String>, epubVersion: Double): Spine {
            val itemrefs = element.get("itemref", Namespaces.OPF).mapNotNull { Itemref.parse(it, prefixMap) }
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
