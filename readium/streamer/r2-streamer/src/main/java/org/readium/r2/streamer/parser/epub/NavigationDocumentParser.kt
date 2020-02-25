/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.normalize

internal object NavigationDocumentParser {

    fun parse(document: ElementNode, filePath: String): Map<String, List<Link>> {
        val docPrefixes = document.getAttrNs("prefix", Namespaces.OPS)
            ?.let { parsePrefixes(it) }.orEmpty()
        val prefixMap = CONTENT_RESERVED_PREFIXES + docPrefixes // prefix element overrides reserved prefixes

        val body = document.getFirst("body", Namespaces.XHTML) ?: return emptyMap()
        val navs = body.collect("nav", Namespaces.XHTML).mapNotNull { parseNavElement(it, filePath, prefixMap) }
        val navMap = navs.flatMap { nav ->
                nav.first.map { type -> Pair(type, nav.second) }
            }.toMap()
        return navMap.mapKeys {
            val suffix = it.key.removePrefix(Vocabularies.TYPE)
            if (suffix in listOf("toc", "page-list", "landmarks", "lot", "loi", "loa", "lov")) suffix else it.key
        }
    }

    private fun parseNavElement(
        nav: ElementNode,
        filePath: String,
        prefixMap: Map<String, String>
    ): Pair<List<String>, List<Link>>? {
        val typeAttr = nav.getAttrNs("type", Namespaces.OPS) ?: return null
        val types = parseProperties(typeAttr).mapNotNull { resolveProperty(it, prefixMap, DEFAULT_VOCAB.TYPE) }
        val links = nav.getFirst("ol", Namespaces.XHTML)?.let { parseOlElement(it, filePath) }
        return if (types.isNotEmpty() && !links.isNullOrEmpty()) Pair(types, links) else null
    }

    private fun parseOlElement(element: ElementNode, filePath: String): List<Link> =
        element.get("li", Namespaces.XHTML).mapNotNull { parseLiElement(it, filePath) }

    private fun parseLiElement(element: ElementNode, filePath: String): Link? {
        val first = element.getAll().firstOrNull() ?: return null // should be <a>,  <span>, or <ol>
        val title = if (first.name == "ol") "" else first.collectText().replace("\\s+".toRegex(), " ").trim()
        val rawHref = first.getAttr("href")
        val href = if (first.name == "a" && !rawHref.isNullOrBlank()) normalize(filePath, rawHref) else "#"
        val children = element.getFirst("ol", Namespaces.XHTML)?.let { parseOlElement(it, filePath) }.orEmpty()

        return if (children.isEmpty() && (href == "#" || title == "")) {
            null
        } else {
            Link(
                title = title,
                href = href,
                children = children
            )
        }
    }
}
