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

internal object PackageDocumentParser {
    fun parse(document: ElementNode, filePath: String) : PackageDocument? {
        val packagePrefixes = document.getAttr("prefix")?.let { parsePrefixes(it) }.orEmpty()
        val prefixMap = PACKAGE_RESERVED_PREFIXES + packagePrefixes // prefix element overrides reserved prefixes

        val epubVersion = document.getAttr("version")?.toDoubleOrNull() ?: 1.2
        val metadata = MetadataParser(epubVersion, prefixMap).parse(document, filePath) ?: return null

        val manifestElement = document.getFirst("manifest", Namespaces.Opf) ?: return null
        val manifest = manifestElement.get("item", Namespaces.Opf).mapNotNull { parseItem(it, filePath, prefixMap) }
        val spineElement = document.getFirst("spine", Namespaces.Opf) ?: return null
        val itemrefs = spineElement.get("itemref", Namespaces.Opf).mapNotNull { parseItemref(it, prefixMap) }
        val pageProgressionDirection = when(spineElement.getAttr("page-progression-direction")) {
            "rtl" -> ReadingProgression.RTL
            "ltr" -> ReadingProgression.LTR
            else -> ReadingProgression.AUTO // null or "default"
        }
        val ncx = if (epubVersion >= 3.0) spineElement.getAttr("toc") else null
        val spine = Spine(itemrefs, pageProgressionDirection, ncx)

        return PackageDocument(filePath, epubVersion, metadata, manifest, spine)
    }

    private fun parseItem(element: ElementNode, filePath: String, prefixMap: Map<String, String>) : Item? {
        val href = element.getAttr("href")?.let { normalize(filePath, it) } ?: return null
        val propAttr = element.getAttr("properties").orEmpty()
        val properties = parseProperties(propAttr).mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.ITEM ) }
        return Item(
                href,
                element.id,
                element.getAttr("fallback"),
                element.getAttr("media-overlay"),
                element.getAttr("media-type"),
                properties
        )
    }

    private fun parseItemref(element: ElementNode, prefixMap: Map<String, String>) : Itemref? {
        val idref = element.getAttr("idref") ?: return null
        val notLinear = element.getAttr("linear") == "no"
        val propAttr = element.getAttr("properties").orEmpty()
        val properties = parseProperties(propAttr).mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.ITEMREF ) }
        return Itemref(idref, !notLinear, properties)
    }
}