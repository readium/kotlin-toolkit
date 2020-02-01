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

object PackageDocumentParser {
    fun parse(document: ElementNode, filePath: String) : PackageDocument? {
        val prefixAttribute = document.getAttr("prefix")
        val packagePrefixes = if (prefixAttribute == null) mapOf() else parsePrefixes(prefixAttribute)
        val prefixMap = PACKAGE_RESERVED_PREFIXES + packagePrefixes // prefix element overrides reserved prefixes

        val epubVersion = document.getAttr("version")?.toDoubleOrNull() ?: 1.2
        val metadata = MetadataParser(epubVersion, prefixMap).parse(document, filePath) ?: return null

        val manifestElement = document.getFirst("manifest", Namespaces.Opf) ?: return null
        val manifest = manifestElement.get("item", Namespaces.Opf).mapNotNull { parseItem(it, filePath) }
        val spineElement = document.getFirst("spine", Namespaces.Opf) ?: return null
        val itemrefs = spineElement.get("itemref", Namespaces.Opf).mapNotNull { parseItemref(it) }
        val pageProgressionDirection = spineElement.getAttr("page-progression-direction")?.let {
            Direction.getOrNull(it)} ?: Direction.default
        val ncx = if (epubVersion >= 3.0) spineElement.getAttr("toc") else null
        val spine = Spine(itemrefs, pageProgressionDirection, ncx)

        return PackageDocument(filePath, epubVersion, metadata, manifest, spine)
    }

    private fun parseItem(element: ElementNode, filePath: String) : Item? {
        val id = element.id ?: return null
        val href = element.getAttr("href")?.let { normalize(filePath, it) } ?: return null
        val fallback = element.getAttr("fallback")
        val mediaOverlay = element.getAttr("media-overlay")
        val mediaType = element.getAttr("media-type")
        val properties = parseProperties(element)
        return Item(id, href, fallback, mediaOverlay, mediaType, properties)
    }

    private fun parseItemref(element: ElementNode) : Itemref? {
        val idref = element.getAttr("idref") ?: return null
        val notLinear = element.getAttr("linear") == "no"
        val properties = parseProperties(element)
        return Itemref(idref, !notLinear, properties)
    }

    private fun parseProperties(element: ElementNode) : List<String> =
            element.getAttr("properties")?.split("""\\s+""".toRegex()) ?: listOf()

}