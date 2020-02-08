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
import org.readium.r2.streamer.parser.normalize


internal object NcxParser {
    fun parse(document: ElementNode, filePath: String): Map<String, List<Link>> {
        val toc = document.getFirst("navMap", Namespaces.Ncx)
                ?.let { parseNavMapElement(it, filePath) }?.let { Pair("toc", it) }
        val pageList = document.getFirst("pageList", Namespaces.Ncx)
                ?.let { parsePageListElement(it, filePath) }?.let { Pair("page-list", it) }
        return listOfNotNull(toc, pageList).toMap()
    }

    private fun parseNavMapElement(element: ElementNode, filePath: String): List<Link> =
            element.get("navPoint", Namespaces.Ncx).mapNotNull { parseNavPointElement(it, filePath) }

    private fun parsePageListElement(element: ElementNode, filePath: String): List<Link> =
            element.get("pageTarget", Namespaces.Ncx).mapNotNull {
                val href = extractHref(it, filePath)
                val title = extractTitle(it)
                if (href.isNullOrBlank() || title.isNullOrBlank())
                    null
                else Link(title = title, href = href)
            }

    private fun parseNavPointElement(element: ElementNode, filePath: String): Link? {
        val title = extractTitle(element)
        val href = extractHref(element, filePath)
        val children = element.get("navPoint", Namespaces.Ncx).mapNotNull { parseNavPointElement(it, filePath) }
        return if (children.isEmpty() && (href == null || title == null))
            null
        else
            Link(title = title, href = href ?: "#", children = children)
    }

    private fun extractTitle(element: ElementNode) =
            element.getFirst("navLabel", Namespaces.Ncx)?.getFirst("text", Namespaces.Ncx)
                    ?.text?.replace("\\s+".toRegex(), " ")?.trim()?.ifBlank { null }

    private fun extractHref(element: ElementNode, filePath: String) =
            element.getFirst("content", Namespaces.Ncx)?.getAttr("src")
                    ?.ifBlank { null }?.let { normalize(filePath, it) }
}
