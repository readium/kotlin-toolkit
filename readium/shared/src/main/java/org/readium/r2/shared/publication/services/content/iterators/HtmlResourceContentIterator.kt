/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content.iterators

import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.html.cssSelector
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.Content.Text
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.use

// FIXME: Support custom skipped elements

/**
 * Iterates an HTML [resource], starting from the given [locator].
 *
 * If you want to start mid-resource, the [locator] must contain a `cssSelector` key in its
 * [Locator.Locations] object.
 *
 * If you want to start from the end of the resource, the [locator] must have a `progression` of 1.0.
 */
@ExperimentalReadiumApi
class HtmlResourceContentIterator(
    private val resource: Resource,
    private val locator: Locator
) : ContentIterator {

    companion object {
        /**
         * Creates a new factory for [HtmlResourceContentIterator].
         */
        fun createFactory(): ResourceContentIteratorFactory = { res, locator ->
            if (res.link().mediaType.matchesAny(MediaType.HTML, MediaType.XHTML))
                HtmlResourceContentIterator(res, locator)
            else null
        }
    }

    override suspend fun close() {}

    override suspend fun previous(): Content? = nextBy(-1)
    override suspend fun next(): Content? = nextBy(1)

    private suspend fun nextBy(delta: Int): Content? {
        val elements = elements()
        val index = currentIndex?.let { it + delta }
            ?: elements.startIndex

        return elements.elements.getOrNull(index)
            ?.also { currentIndex = index }
    }

    private var currentIndex: Int? = null

    private suspend fun elements(): ParsedElements =
        parsedElements
            ?: parseElements().also { parsedElements = it }

    private var parsedElements: ParsedElements? = null

    private suspend fun parseElements(): ParsedElements {
        val body = resource.use { res ->
            val html = res.readAsString().getOrThrow()
            Jsoup.parse(html)
        }.body()

        val contentParser = ContentParser(
            baseLocator = locator,
            startElement = locator.locations.cssSelector?.let {
                // The JS third-party library used to generate the CSS Selector sometimes adds
                // :root >, which doesn't work with JSoup.
                tryOrNull { body.selectFirst(it.removePrefix(":root > ")) }
            }
        )
        NodeTraversor.traverse(contentParser, body)
        return contentParser.result()
    }

    /**
     * Holds the result of parsing the HTML resource into a list of [Content].
     *
     * The [startIndex] will be calculated from the element matched by the base [locator], if
     * possible. Defaults to 0.
     */
    data class ParsedElements(
        val elements: List<Content>,
        val startIndex: Int,
    )

    private class ContentParser(
        private val baseLocator: Locator,
        private val startElement: Element?,
    ) : NodeVisitor {

        fun result() = ParsedElements(
            elements = elements,
            startIndex = if (baseLocator.locations.progression == 1.0) elements.size - 1
                else startIndex
        )

        private val elements = mutableListOf<Content>()
        private var startIndex = 0
        private var currentElement: Element? = null

        private val segmentsAcc = mutableListOf<Text.Segment>()
        private var textAcc = StringBuilder()
        private var wholeRawTextAcc: String = ""
        private var elementRawTextAcc: String = ""
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
                currentElement = node

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
                            elements.add(
                                Content(
                                    locator = baseLocator.copy(
                                        locations = Locator.Locations(
                                            otherLocations = buildMap {
                                                put("cssSelector", node.cssSelector() as Any)
                                            }
                                        )
                                    ),
                                    data = Content.Image(
                                        link = Link(href = href),
                                        caption = null, // FIXME: Get the caption from figcaption
                                        description = node.attr("alt").takeIf { it.isNotBlank() },
                                    )
                                )
                            )
                        }
                    }
                    node.isBlock -> {
                        segmentsAcc.clear()
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
                    flushSegment()
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
            flushSegment()
            if (segmentsAcc.isEmpty()) return

            if (startElement != null && currentElement == startElement) {
                startIndex = elements.size
            }
            elements.add(Content(
                locator = baseLocator.copy(
                    locations = Locator.Locations(
                        otherLocations = buildMap {
                            currentCssSelector?.let {
                                put("cssSelector", it as Any)
                            }
                        }
                    ),
                    text = Locator.Text(highlight = elementRawTextAcc)
                ),
                data = Text(
                    role = Text.Role.Body,
                    segments = segmentsAcc.toList()
                )
            ))
            elementRawTextAcc = ""
            segmentsAcc.clear()
        }

        private fun flushSegment() {
            var text = textAcc.toString()
            val trimmedText = text.trim()

            if (text.isNotBlank()) {
                if (segmentsAcc.isEmpty()) {
                    text = text.trimStart()

                    val whitespaceSuffix = text.lastOrNull()
                        ?.takeIf { it.isWhitespace() }
                        ?: ""

                    text = trimmedText + whitespaceSuffix
                }

                segmentsAcc.add(
                    Text.Segment(
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
                        text = text,
                        attributes = buildList {
                            currentLanguage?.let {
                                add(Text.Segment.Attribute(Text.Segment.AttributeKey.LANGUAGE, Language(it)))
                            }
                        },
                    )
                )
            }

            wholeRawTextAcc += rawTextAcc
            elementRawTextAcc += rawTextAcc
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