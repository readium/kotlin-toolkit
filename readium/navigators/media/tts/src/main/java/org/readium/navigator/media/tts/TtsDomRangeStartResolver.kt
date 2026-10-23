/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media.tts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.html.domRange
import org.readium.r2.shared.util.data.asInputStream
import org.readium.r2.shared.util.use

/**
 * A DOM start position resolved to the text block emitted by Readium's HTML content iterator.
 *
 * @param cssSelector Selector of the containing block, used to match its content fragments.
 * @param text The block's flattened, normalized text, with leading and trailing whitespace removed.
 * @param offset UTF-16 position of the start point after normalizing its text prefix.
 */
@ExperimentalReadiumApi
internal class TtsHtmlTarget(
    val cssSelector: String,
    val text: String,
    val offset: Int,
)

/**
 * Resolves an EPUB locator's `domRange.start` against its source XHTML.
 *
 * The start selector identifies an element; `textNodeIndex` indexes its direct child nodes, and
 * an omitted `charOffset` means the beginning of the selected text node. The top-level selector,
 * range end, and highlight text do not determine the start position.
 *
 * @param locator Locator containing the resource href and DOM start point.
 * @return The containing block and normalized start offset, or `null` for an invalid or stale point.
 */
@ExperimentalReadiumApi
internal suspend fun Publication.resolveTtsHtmlTarget(
    locator: Locator,
): TtsHtmlTarget? = withContext(Dispatchers.IO) {
    val start = locator.locations.domRange?.start ?: return@withContext null

    val document = get(locator.href)
        ?.asInputStream()
        ?.use { resourceStream ->
            runCatching {
                Jsoup.parse(
                    resourceStream,
                    "UTF-8",
                    baseUrl?.toString().orEmpty()
                )
            }.getOrNull()
        } ?: return@withContext null

    val parent = runCatching { document.selectFirst(start.cssSelector) }.getOrNull()
        ?: return@withContext null

    val paragraph = generateSequence(parent) { element -> element.parent() }
        .firstOrNull(Element::isBlock) ?: return@withContext null

    val node = parent.childNodes().getOrNull(start.textNodeIndex) as? TextNode
        ?: return@withContext null

    // Without charOffset, the point identifies the beginning of the child text node.
    val charOffset = start.charOffset ?: 0
    if (charOffset !in 0..node.wholeText.length) {
        return@withContext null
    }

    val rawText = StringBuilder()
    var rawOffset: Int? = null

    fun visit(current: Node) {
        if (current is TextNode) {
            if (current === node) {
                rawOffset = rawText.length + if (current.wholeText.isNotBlank()) {
                    charOffset
                } else {
                    0
                }
            }
            // HtmlResourceContentIterator skips whitespace-only text nodes entirely.
            if (current.wholeText.isNotBlank()) {
                rawText.append(current.wholeText)
            }
        } else {
            current.childNodes().forEach(::visit)
        }
    }

    visit(paragraph)

    val offset = rawOffset ?: return@withContext null

    TtsHtmlTarget(
        cssSelector = paragraph.cssSelector(),
        text = normalizeTtsHtmlText(rawText.toString()).trim(),
        offset = normalizeTtsHtmlText(rawText.substring(0, offset)).trimStart().length,
    )
}

/**
 * Normalizes block text the same way as Readium's HTML content iterator.
 *
 * Whitespace-only text nodes are excluded by the caller before normalization.
 *
 * @param raw Flattened text from nonblank DOM text nodes, or a prefix of it.
 * @return Text with the iterator's entity decoding, whitespace collapsing, and ignored characters.
 */
private fun normalizeTtsHtmlText(raw: String): String = buildString {
    var lastWasSpace = false
    var index = 0

    // Match HtmlResourceContentIterator's handling of parsed HTML text.
    val text = Parser.unescapeEntities(raw, false)

    while (index < text.length) {
        val codePoint = text.codePointAt(index)

        when (codePoint) {
            '\u200B'.code, '\u00AD'.code -> {
                // Ignore zero-width spaces and soft hyphens
            }
            ' '.code, '\t'.code, '\n'.code, '\u000c'.code, '\r'.code -> {
                if (!lastWasSpace) {
                    append(' ')
                    lastWasSpace = true
                }
            }
            else -> {
                appendCodePoint(codePoint)
                lastWasSpace = false
            }
        }

        index += Character.charCount(codePoint)
    }
}
