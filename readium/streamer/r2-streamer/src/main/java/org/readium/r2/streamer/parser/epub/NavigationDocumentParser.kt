/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.Link
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.streamer.parser.normalize

internal data class NavigationDocument(
        val toc: List<Link>,
        val pageList: List<Link>,
        val landmarks: List<Link>,
        val loi: List<Link>,
        val lot: List<Link>,
        val loa: List<Link>,
        val lov: List<Link>
)

internal object NavigationDocumentParser {
    fun parse(document: ElementNode, filePath: String) : NavigationDocument? {
        val prefixAttribute = document.getAttrNs("prefix", Namespaces.Ops)
        val packagePrefixes = if (prefixAttribute == null) mapOf() else parsePrefixes(prefixAttribute)
        val prefixMap = CONTENT_RESERVED_PREFIXES + packagePrefixes // prefix element overrides reserved prefixes
        val body = document.getFirst("body", Namespaces.Xhtml) ?: return null

        var toc: List<Link> = listOf()
        var pageList: List<Link> = listOf()
        var landmarks: List<Link> = listOf()
        var loi: List<Link> = listOf()
        var lot: List<Link> = listOf()
        var loa: List<Link> = listOf()
        var lov: List<Link> = listOf()

        for (nav in body.get("nav", Namespaces.Xhtml)) {
            val typeAttr = nav.getAttrNs("type", Namespaces.Ops) ?: continue
            val type = resolveProperty(typeAttr, prefixMap, DEFAULT_VOCAB.TYPE)
            val links = parseNavElement(nav, filePath) ?: continue
            when (type) {
                DEFAULT_VOCAB.TYPE.iri + "toc" ->  toc = links
                DEFAULT_VOCAB.TYPE.iri + "page-list" -> pageList = links
                DEFAULT_VOCAB.TYPE.iri + "landmarks" -> landmarks = links
                DEFAULT_VOCAB.TYPE.iri + "loi" -> loi = links
                DEFAULT_VOCAB.TYPE.iri + "lot" -> lot = links
                DEFAULT_VOCAB.TYPE.iri + "loa" -> loa = links
                DEFAULT_VOCAB.TYPE.iri + "lov" -> lov = links
            }
        }

        return NavigationDocument(toc, pageList, landmarks, loi, lot, loa, lov)
    }

    private fun parseNavElement(nav: ElementNode, filePath: String) : List<Link>? =
        nav.getFirst("ol", Namespaces.Xhtml)?.let { parseOlElement(it, filePath) }

    private fun parseOlElement(element: ElementNode, filePath: String): List<Link> =
        element.get("li", Namespaces.Xhtml).mapNotNull {  parseLiElement(it, filePath) }

    private fun parseLiElement(element: ElementNode, filePath: String): Link? {
        val first = element.getAll().firstOrNull() ?: return null // should be <a>,  <span>, or <ol>
        val title = if (first.name == "ol") null else first.text
        val rawHref = first.getAttr("href")
        val href = if (first.name == "a" && rawHref != null) normalize(filePath, rawHref) else null
        val children = element.getFirst("ol", Namespaces.Xhtml)?.let { parseOlElement(it, filePath) } ?: emptyList()
        return Link().apply {
            this.title = title
            this.href = href
            this.children = children.toMutableList()
       }
    }
}

