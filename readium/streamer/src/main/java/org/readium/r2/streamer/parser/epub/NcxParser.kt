/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.fromEpubHref
import org.readium.r2.shared.util.xml.ElementNode

internal object NcxParser {

    fun parse(document: ElementNode, filePath: Url): Map<String, List<Link>> {
        val toc = document.getFirst("navMap", Namespaces.NCX)
            ?.let { parseNavMapElement(it, filePath) }?.let { Pair("toc", it) }
        val pageList = document.getFirst("pageList", Namespaces.NCX)
            ?.let { parsePageListElement(it, filePath) }?.let { Pair("page-list", it) }
        return listOfNotNull(toc, pageList).toMap()
    }

    private fun parseNavMapElement(element: ElementNode, filePath: Url): List<Link> =
        element.get("navPoint", Namespaces.NCX).mapNotNull { parseNavPointElement(it, filePath) }

    @OptIn(DelicateReadiumApi::class)
    private fun parsePageListElement(element: ElementNode, filePath: Url): List<Link> =
        element.get("pageTarget", Namespaces.NCX).mapNotNull {
            val href = extractHref(it, filePath)
            val title = extractTitle(it)
            if (href == null || title.isNullOrBlank()) {
                null
            } else {
                Link(title = title, href = href)
            }
        }

    @OptIn(DelicateReadiumApi::class)
    private fun parseNavPointElement(element: ElementNode, filePath: Url): Link? {
        val title = extractTitle(element)
        val href = extractHref(element, filePath)
        val children = element.get("navPoint", Namespaces.NCX).mapNotNull {
            parseNavPointElement(
                it,
                filePath
            )
        }
        return if (children.isEmpty() && (href == null || title == null)) {
            null
        } else {
            Link(
                title = title,
                href = href ?: Url("#")!!,
                children = children
            )
        }
    }

    private fun extractTitle(element: ElementNode) =
        element.getFirst("navLabel", Namespaces.NCX)?.getFirst("text", Namespaces.NCX)
            ?.text?.replace("\\s+".toRegex(), " ")?.trim()?.ifBlank { null }

    private fun extractHref(element: ElementNode, filePath: Url) =
        element.getFirst("content", Namespaces.NCX)?.getAttr("src")
            ?.ifBlank { null }
            ?.let { Url.fromEpubHref(it) }
            ?.let { filePath.resolve(it) }
}
