/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication.services.content.iterators

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.html.cssSelector
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.Content.Attribute
import org.readium.r2.shared.publication.services.content.Content.AttributeKey
import org.readium.r2.shared.publication.services.content.Content.AudioElement
import org.readium.r2.shared.publication.services.content.Content.ImageElement
import org.readium.r2.shared.publication.services.content.Content.TextElement
import org.readium.r2.shared.publication.services.content.Content.VideoElement
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.decodeString
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.toDebugDescription
import org.readium.r2.shared.util.use
import timber.log.Timber

/**
 * Iterates an HTML [resource], starting from the given [locator].
 *
 * If you want to start mid-resource, the [locator] must contain a `cssSelector` key in its
 * [Locator.Locations] object.
 *
 * If you want to start from the end of the resource, the [locator] must have a `progression` of 1.0.
 *
 * Locators will contain a `before` context of up to `beforeMaxLength` characters.
 */
@ExperimentalReadiumApi
public class HtmlResourceContentIterator internal constructor(
    private val resource: Resource,
    private val totalProgressionRange: ClosedRange<Double>?,
    private val locator: Locator,
    private val beforeMaxLength: Int = 50,
) : Content.Iterator {

    public class Factory : ResourceContentIteratorFactory {
        override suspend fun create(
            manifest: Manifest,
            servicesHolder: PublicationServicesHolder,
            readingOrderIndex: Int,
            resource: Resource,
            mediaType: MediaType,
            locator: Locator,
        ): Content.Iterator? {
            if (!mediaType.matchesAny(MediaType.HTML, MediaType.XHTML)) {
                return null
            }

            val positions = servicesHolder.positionsByReadingOrder()
            return HtmlResourceContentIterator(
                resource,
                totalProgressionRange = positions.getOrNull(readingOrderIndex)
                    ?.firstOrNull()?.locations?.totalProgression
                    ?.let { start ->
                        val end = positions.getOrNull(readingOrderIndex + 1)
                            ?.firstOrNull()?.locations?.totalProgression
                            ?: 1.0

                        start..end
                    },
                locator = locator
            )
        }
    }

    /**
     * [Content.Element] loaded with [hasPrevious] or [hasNext], associated with the move delta.
     */
    private data class ElementWithDelta(
        val element: Content.Element,
        val delta: Int,
    )

    private var currentElement: ElementWithDelta? = null

    override suspend fun hasPrevious(): Boolean {
        if (currentElement?.delta == -1) return true

        val elements = elements()
        val index = (currentIndex ?: elements.startIndex) - 1

        val content = elements.elements.getOrNull(index)
            ?: return false

        currentIndex = index
        currentElement = ElementWithDelta(content, -1)
        return true
    }

    override fun previous(): Content.Element =
        currentElement
            ?.takeIf { it.delta == -1 }?.element
            ?.also { currentElement = null }
            ?: throw IllegalStateException(
                "Called previous() without a successful call to hasPrevious() first"
            )

    override suspend fun hasNext(): Boolean {
        if (currentElement?.delta == +1) return true

        val elements = elements()
        val index = (currentIndex ?: (elements.startIndex - 1)) + 1

        val content = elements.elements.getOrNull(index)
            ?: return false

        currentIndex = index
        currentElement = ElementWithDelta(content, +1)
        return true
    }

    override fun next(): Content.Element =
        currentElement
            ?.takeIf { it.delta == +1 }?.element
            ?.also { currentElement = null }
            ?: throw IllegalStateException(
                "Called next() without a successful call to hasNext() first"
            )

    private var currentIndex: Int? = null

    private suspend fun elements(): ParsedElements =
        parsedElements
            ?: parseElements().also { parsedElements = it }

    private var parsedElements: ParsedElements? = null

    private suspend fun parseElements(): ParsedElements =
        withContext(Dispatchers.Default) {
            val document = resource.use { res ->
                val html = res
                    .read()
                    .flatMap { it.decodeString() }
                    .getOrElse {
                        val error = DebugError("Failed to read HTML resource", it.cause)
                        Timber.w(error.toDebugDescription())
                        return@withContext ParsedElements()
                    }

                Jsoup.parse(html)
            }

            val contentParser = ContentParser(
                baseLocator = locator,
                startElement = locator.locations.cssSelector?.let {
                    tryOrNull { document.selectFirst(it) }
                },
                beforeMaxLength = beforeMaxLength
            )
            NodeTraversor.traverse(contentParser, document.body())
            val elements = contentParser.result()
            val elementCount = elements.elements.size
            if (elementCount == 0) {
                return@withContext elements
            }

            elements.copy(
                elements = elements.elements.mapIndexed { index, element ->
                    val progression = index.toDouble() / elementCount
                    element.copy(
                        progression = progression,
                        totalProgression = totalProgressionRange?.let {
                            totalProgressionRange.start + progression * (totalProgressionRange.endInclusive - totalProgressionRange.start)
                        }
                    )
                }
            )
        }

    private fun Content.Element.copy(progression: Double?, totalProgression: Double?): Content.Element {
        fun Locator.update(): Locator =
            copyWithLocations(
                progression = progression,
                totalProgression = totalProgression
            )

        return when (this) {
            is TextElement -> copy(
                locator = locator.update(),
                segments = segments.map {
                    it.copy(locator = it.locator.update())
                }
            )
            is AudioElement -> copy(locator = locator.update())
            is VideoElement -> copy(locator = locator.update())
            is ImageElement -> copy(locator = locator.update())
            else -> this
        }
    }

    /**
     * Holds the result of parsing the HTML resource into a list of [Content.Element].
     *
     * The [startIndex] will be calculated from the element matched by the base [locator], if
     * possible. Defaults to 0.
     */
    public data class ParsedElements(
        val elements: List<Content.Element> = emptyList(),
        val startIndex: Int = 0,
    )

    private class ContentParser(
        private val baseLocator: Locator,
        private val startElement: Element?,
        private val beforeMaxLength: Int,
    ) : NodeVisitor {

        fun result() = ParsedElements(
            elements = elements,
            startIndex = if (baseLocator.locations.progression == 1.0) {
                elements.size
            } else {
                startIndex
            }
        )

        private val elements = mutableListOf<Content.Element>()
        private var startIndex = 0

        /** Segments accumulated for the current element. */
        private val segmentsAcc = mutableListOf<TextElement.Segment>()

        /** Text since the beginning of the current segment, after coalescing whitespaces. */
        private var textAcc = StringBuilder()

        /** Text content since the beginning of the resource, including whitespaces. */
        private var wholeRawTextAcc: String? = null

        /** Text content since the beginning of the current element, including whitespaces. */
        private var elementRawTextAcc: String = ""

        /** Text content since the beginning of the current segment, including whitespaces. */
        private var rawTextAcc: String = ""

        /** Language of the current segment. */
        private var currentLanguage: String? = null

        /** LIFO stack of the current element's block ancestors. */
        private val breadcrumbs = mutableListOf<ParentElement>()

        private data class ParentElement(
            val element: Element,
            val cssSelector: String?,
        ) {
            constructor(element: Element) : this(
                element = element,
                cssSelector = tryOrLog { element.cssSelector() }
            )
        }

        @OptIn(DelicateReadiumApi::class)
        override fun head(node: Node, depth: Int) {
            if (node is Element) {
                val parent = ParentElement(node)
                if (node.isBlock) {
                    flushText()
                    breadcrumbs.add(parent)
                }

                val tag = node.normalName()

                val elementLocator: Locator by lazy {
                    baseLocator.copy(
                        locations = Locator.Locations(
                            otherLocations = buildMap {
                                parent.cssSelector?.let {
                                    put("cssSelector", it as Any)
                                }
                            }
                        )
                    )
                }

                when {
                    tag == "br" -> {
                        flushText()
                    }

                    tag == "img" -> {
                        flushText()

                        node.srcRelativeToHref(baseLocator.href)?.let { url ->
                            elements.add(
                                ImageElement(
                                    locator = elementLocator,
                                    embeddedLink = Link(href = url),
                                    caption = null, // FIXME: Get the caption from figcaption
                                    attributes = buildList {
                                        val alt = node.attr("alt").takeIf { it.isNotBlank() }
                                        if (alt != null) {
                                            add(Attribute(AttributeKey.ACCESSIBILITY_LABEL, alt))
                                        }
                                    }
                                )
                            )
                        }
                    }

                    tag == "audio" || tag == "video" -> {
                        flushText()

                        val url = node.srcRelativeToHref(baseLocator.href)
                        val link: Link? =
                            if (url != null) {
                                Link(href = url)
                            } else {
                                val sources = node.select("source")
                                    .mapNotNull { source ->
                                        source.srcRelativeToHref(baseLocator.href)?.let { url ->
                                            Link(
                                                href = url,
                                                mediaType = MediaType(source.attr("type"))
                                            )
                                        }
                                    }

                                sources.firstOrNull()?.copy(alternates = sources.drop(1))
                            }

                        if (link != null) {
                            when (tag) {
                                "audio" -> elements.add(
                                    AudioElement(
                                        locator = elementLocator,
                                        embeddedLink = link,
                                        attributes = emptyList()
                                    )
                                )
                                "video" -> elements.add(
                                    VideoElement(
                                        locator = elementLocator,
                                        embeddedLink = link,
                                        attributes = emptyList()
                                    )
                                )
                                else -> {}
                            }
                        }
                    }

                    node.isBlock -> {
                        flushText()
                    }
                }
            }
        }

        override fun tail(node: Node, depth: Int) {
            if (node is TextNode && node.wholeText.isNotBlank()) {
                val language = node.language
                if (currentLanguage != language) {
                    flushSegment()
                    currentLanguage = language
                }

                val text = Parser.unescapeEntities(node.wholeText, false)
                rawTextAcc += text
                appendNormalisedText(text)
            } else if (node is Element) {
                if (node.isBlock) {
                    assert(breadcrumbs.last().element == node)
                    flushText()
                    breadcrumbs.removeAt(breadcrumbs.lastIndex)
                }
            }
        }

        private fun appendNormalisedText(text: String) {
            textAcc.appendNormalisedWhitespace(text, lastCharIsWhitespace())
        }

        private fun lastCharIsWhitespace(): Boolean =
            textAcc.lastOrNull() == ' '

        private fun flushText() {
            flushSegment()

            val parent = breadcrumbs.lastOrNull()

            if (startIndex == 0 && startElement != null && parent?.element == startElement) {
                startIndex = elements.size
            }

            if (segmentsAcc.isEmpty()) return

            // Trim the end of the last segment's text to get a cleaner output for the TextElement.
            // Only whitespaces between the segments are meaningful.
            segmentsAcc[segmentsAcc.size - 1] = segmentsAcc.last().run { copy(text = text.trimEnd()) }

            elements.add(
                TextElement(
                    locator = baseLocator.copy(
                        locations = Locator.Locations(
                            otherLocations = buildMap {
                                parent?.cssSelector?.let {
                                    put("cssSelector", it as Any)
                                }
                            }
                        ),
                        text = Locator.Text.trimmingText(
                            elementRawTextAcc,
                            before = segmentsAcc.firstOrNull()?.locator?.text?.before
                        )
                    ),
                    role = TextElement.Role.Body,
                    segments = segmentsAcc.toList()
                )
            )
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

                val parent = breadcrumbs.lastOrNull()

                segmentsAcc.add(
                    TextElement.Segment(
                        locator = baseLocator.copy(
                            locations = Locator.Locations(
                                otherLocations = buildMap {
                                    parent?.cssSelector?.let {
                                        put("cssSelector", it as Any)
                                    }
                                }
                            ),
                            text = Locator.Text.trimmingText(
                                rawTextAcc,
                                before = wholeRawTextAcc?.takeLast(beforeMaxLength)
                            )
                        ),
                        text = text,
                        attributes = buildList {
                            currentLanguage?.let {
                                add(Attribute(AttributeKey.LANGUAGE, Language(it)))
                            }
                        }
                    )
                )
            }

            if (rawTextAcc != "") {
                wholeRawTextAcc = (wholeRawTextAcc ?: "") + rawTextAcc
                elementRawTextAcc += rawTextAcc
            }
            rawTextAcc = ""
            textAcc.clear()
        }
    }
}

private fun Locator.Text.Companion.trimmingText(text: String, before: String?): Locator.Text {
    val leadingWhitespace = text.takeWhile { it.isWhitespace() }
    val trailingWhitespace = text.takeLastWhile { it.isWhitespace() }
    return Locator.Text(
        before = ((before ?: "") + leadingWhitespace).takeUnless { it.isBlank() },
        highlight = text.substring(
            leadingWhitespace.length,
            text.length - trailingWhitespace.length
        ),
        after = trailingWhitespace.takeUnless { it.isBlank() }
    )
}

private val Node.language: String? get() =
    attr("xml:lang").takeUnless { it.isBlank() }
        ?: attr("lang").takeUnless { it.isBlank() }
        ?: parent()?.language

private fun Node.srcRelativeToHref(baseUrl: Url): Url? =
    attr("src")
        .takeIf { it.isNotBlank() }
        ?.let { Url(it) }
        ?.let { baseUrl.resolve(it) }

/**
 * After normalizing the whitespace within a string, appends it to a string builder.
 *
 * Largely inspired by JSoup's `StringUtil.appendNormalisedWhitespace`.
 *
 * Note that we don't use directly JSoup's method because we need to keep the non-breaking
 * spaces in the text. Otherwise, they will be lost post-text tokenization and Hypothesis won't
 * match the results.
 *
 * @param string String to normalize whitespace within.
 * @param stripLeading Set to true if you wish to remove any leading whitespace.
 */
private fun StringBuilder.appendNormalisedWhitespace(
    string: String,
    stripLeading: Boolean,
) {
    var lastWasWhite = false
    var reachedNonWhite = false
    val len = string.length
    var c: Int
    var i = 0
    while (i < len) {
        c = string.codePointAt(i)
        if (isWhitespace(c)) {
            if (stripLeading && !reachedNonWhite || lastWasWhite) {
                i += Character.charCount(c)
                continue
            }
            append(' ')
            lastWasWhite = true
        } else if (!isInvisibleChar(c)) {
            appendCodePoint(c)
            lastWasWhite = false
            reachedNonWhite = true
        }
        i += Character.charCount(c)
    }
}

/**
 * Tests if a code point is "whitespace" as defined in the HTML spec.
 */
private fun isWhitespace(c: Int): Boolean {
    return c == ' '.code || c == '\t'.code || c == '\n'.code || c == '\u000c'.code || c == '\r'.code
}

private fun isInvisibleChar(c: Int): Boolean {
    return c == 8203 || c == 173 // zero width sp, soft hyphen
    // previously also included zw non join, zw join - but removing those breaks semantic meaning of text
}
