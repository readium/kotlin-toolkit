/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content

import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.*
import org.jsoup.parser.Parser
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.mapCatching
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.use

sealed class Content {
    data class Audio(
        val locator: Locator,
        val link: Link,
        val extras: Map<String, Any> = emptyMap()
    ) : Content()

    data class Image(
        val locator: Locator,
        val link: Link,
        val description: String?,
        val extras: Map<String, Any> = emptyMap()
    ) : Content()

    data class Text(
        val spans: List<Span>,
        val style: Style = Style.Body,
        val extras: Map<String, Any> = emptyMap()
    ) : Content() {
        data class Span(
            val locator: Locator,
            val language: String?,
            val text: String,
        )

        sealed class Style {
            class Heading(val level: Int) : Style()
            object Body : Style()
            object Callout : Style()
            object Caption : Style()
            object Footnote: Style()
            object Quote: Style()
            object ListItem: Style()
        }
    }
}

/**
 * Iterates through a publication's content.
 */
interface ContentIterator : SuspendingCloseable {

    suspend fun hasNext(): Boolean

    /**
     * Retrieves the next piece of content.
     *
     * @return Null when reaching the end of the publication, or an error in case of failure.
     */
    suspend fun next(): Try<Content?, Exception>

    /**
     * Closes any resources allocated for the search query, such as a cursor.
     * To be called when the user dismisses the search.
     */
    override suspend fun close() {}
}

class PublicationContentIterator(
    private val publication: Publication,
    start: Locator?,
    private val resourceContentIteratorFactories: List<ResourceContentIteratorFactory>
) : ContentIterator {

    private var nextIndex =
        start?.let { publication.readingOrder.indexOfFirstWithHref(it.href) }
            ?: 0

    private var currentIterator: ContentIterator? = null

    private suspend fun iterator(): ContentIterator? {
        currentIterator?.takeIf { it.hasNext() }
            ?.let { return it }

        if (nextIndex >= publication.readingOrder.count()) {
            return null
        }

        val link = publication.readingOrder[nextIndex]
        val locator = publication.locatorFromLink(link) ?: return null
        val resource = publication.get(link)
        currentIterator = resourceContentIteratorFactories
            .firstNotNullOfOrNull { factory -> factory(resource, locator) }
        nextIndex += 1
        return currentIterator
    }

    override suspend fun hasNext(): Boolean =
        iterator()?.hasNext() ?: false

    override suspend fun next(): Try<Content?, Exception> =
        iterator()?.next()
            ?: Try.success(null)

    override suspend fun close() {
        currentIterator?.close()
    }
}

/**
 * Creates a [ContentIterator] instance for the given [resource].
 *
 * Return null if the resource format is not supported.
*/
typealias ResourceContentIteratorFactory =
    suspend (resource: Resource, locator: Locator) -> ContentIterator?

class HtmlResourceContentIterator(val resource: Resource, val locator: Locator) : ContentIterator {
    companion object {
        // FIXME: Custom skipped elements
        fun createFactory(): ResourceContentIteratorFactory = { res, locator ->
            if (res.link().mediaType.matchesAny(MediaType.HTML, MediaType.XHTML))
                HtmlResourceContentIterator(res, locator)
            else null
        }
    }
    private val scope = MainScope()

    override suspend fun close() {
        scope.cancel()
    }

    private suspend fun openDocument(): Try<Document, Exception> =
        resource.use { res ->
            res.readAsString()
                .mapCatching { Jsoup.parse(it) }
        }

    private fun parseElement(element: Element): List<Content> {
        val contentParser = ContentParser(locator)
        NodeTraversor.traverse(contentParser, element)
        return contentParser.content.toList()
    }

    private var items: Try<MutableList<Content>, Exception>? = null

    private suspend fun items(): Try<MutableList<Content>, Exception> {
        if (items == null) {
            items = openDocument()
                .map { parseElement(it.body()).toMutableList() }
        }
        return requireNotNull(items)
    }

    override suspend fun hasNext(): Boolean =
        items().getOrNull()?.isNotEmpty()
            ?: false

    override suspend fun next(): Try<Content?, Exception> =
        items().map { it.removeFirstOrNull() }

    private class ContentParser(
        private var baseLocator: Locator
    ) : NodeVisitor {
        val content = mutableListOf<Content>()

        private val spansAcc = mutableListOf<Content.Text.Span>()
        private var textAcc = StringBuilder()
        private var wholeRawTextAcc: String = ""
        private var rawTextAcc: String = ""
        private var currentLanguage: String? = null
        private var currentCssSelector: String? = null
        private var ignoredNode: Node? = null

        override fun head(node: Node, depth: Int) {
            if (ignoredNode != null) return

            if (node.isHidden) {
                ignoredNode = node
                return
            }

            if (node is Element) {
                val tag = node.normalName()
                when {
                    tag == "br" -> {
                        flushText()
                    }
                    tag == "img" -> {
                        flushText()
                        val href = node.attr("src")
                            .takeIf { it.isNotBlank() }
                            ?.let { Href(it, baseLocator.href).string }

                        if (href != null) {
                            content.add(
                                Content.Image(
                                    locator = baseLocator.copy(
                                        locations = Locator.Locations(
                                            otherLocations = buildMap {
                                                put("cssSelector", node.cssSelector() as Any)
                                            }
                                        )
                                    ),
                                    link = Link(href = href),
                                    description = node.attr("alt").takeIf { it.isNotBlank() },
                                )
                            )
                        }
                    }
                    node.isBlock -> {
                        spansAcc.clear()
                        textAcc.clear()
                        rawTextAcc = ""
                        currentCssSelector = node.cssSelector()
                    }
                }
            }
        }

        override fun tail(node: Node, depth: Int) {
            if (ignoredNode == node) {
                ignoredNode = null
            }

            if (node is TextNode) {
                val language = node.language
                if (currentLanguage != language) {
                    flushSpan()
                    currentLanguage = language
                }

                rawTextAcc += Parser.unescapeEntities(node.wholeText, false)
                appendNormalisedText(node)

            } else if (node is Element) {
                if (node.isBlock) {
                    flushText()
                }
            }
        }

        private fun appendNormalisedText(textNode: TextNode) {
            val text = Parser.unescapeEntities(textNode.wholeText, false)
            StringUtil.appendNormalisedWhitespace(textAcc, text, lastCharIsWhitespace())
        }

        private fun lastCharIsWhitespace(): Boolean =
            textAcc.lastOrNull() == ' '

        private fun flushText() {
            flushSpan()
            if (spansAcc.isEmpty()) return

            content.add(Content.Text(
                spans = spansAcc.toList(),
            ))
            spansAcc.clear()
        }

        private fun flushSpan() {
            var text = textAcc.toString()

            if (text.isNotBlank()) {
                if (spansAcc.isEmpty()) {
                    text = text.trimStart()
                }

                spansAcc.add(Content.Text.Span(
                    locator = baseLocator.copy(
                        locations = Locator.Locations(
                            otherLocations = buildMap {
                                currentCssSelector?.let {
                                    put("cssSelector", it as Any)
                                }
                            }
                        ),
                        text = Locator.Text(
                            highlight = rawTextAcc,
                            before = wholeRawTextAcc.takeLast(50) // FIXME: custom length
                        )
                    ),
                    language = currentLanguage,
                    text = text
                ))
            }

            wholeRawTextAcc += rawTextAcc
            rawTextAcc = ""
            textAcc.clear()
        }
    }
}

// FIXME: Setup ignore conditions
private val Node.isHidden: Boolean get() = false

private val Node.language: String? get() =
    attr("xml:lang").takeUnless { it.isBlank() }
        ?: attr("lang").takeUnless { it.isBlank() }
        ?: parent()?.language

private fun Node.parentElement(): Element? =
    parent()?.let {
        if (it is Element) {
            it
        } else {
            it.parentElement()
        }
    }
