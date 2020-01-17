/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.parser.xml.ElementNode

object PackageDocumentParser {
    fun parse(document: ElementNode, filePath: String) : PackageDocument? {
        val prefixAttribute = document.getAttr("prefix")
        val packagePrefixes = if (prefixAttribute == null) mapOf() else parsePackagePrefixes(prefixAttribute)
        val prefixMap = RESERVED_PREFIXES + packagePrefixes // prefix element overrides reserved prefixes

        val epubVersion = document.getAttr("version")?.toDoubleOrNull() ?: return null
        val metadata = MetadataParser(epubVersion, prefixMap).parse(document) ?: return null

        val manifestElement = document.getFirst("manifest", Namespaces.Opf) ?: return null
        val manifest = manifestElement.get("item", Namespaces.Opf).mapNotNull { parseItem(it) }
        val spineElement = document.getFirst("spine", Namespaces.Opf) ?: return null
        val itemrefs = spineElement.get("itemref", Namespaces.Opf).mapNotNull { parseItemref(it) }
        val pageProgressionDirection = spineElement.getAttr("page-progression-direction")?.let {
            Direction.getOrNull(it)} ?: Direction.default
        val ncx = if (epubVersion >= 3.0) spineElement.getAttr("toc") else null
        val spine = Spine(itemrefs, pageProgressionDirection, ncx)

        return PackageDocument(filePath, epubVersion, metadata, manifest, spine)
    }

    private fun parsePrefixEntry(entry: String): Pair<String, String>? {
        val splitted = entry.split(":", limit = 2)
        return if (splitted.size == 1)
            null
        else {
            Pair(splitted[0].trim(), splitted[1].trim())
        }
    }

    private fun parsePackagePrefixes(prefixes: String): Map<String, String> =
            prefixes.split(" ").mapNotNull { parsePrefixEntry(it) }.toMap()

    private fun parseItem(element: ElementNode) : Item? {
        val id = element.id ?: return null
        val href = element.getAttr("href") ?: return null
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