/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.tokenizer.DefaultTextContentTokenizer
import org.readium.r2.shared.util.tokenizer.TextTokenizer
import org.readium.r2.shared.util.tokenizer.TextUnit
import org.readium.r2.shared.util.tokenizer.Tokenizer

/** A tokenizer splitting a [Content.Element] into smaller pieces. */
@ExperimentalReadiumApi
fun interface ContentTokenizer : Tokenizer<Content.Element, Content.Element>

/**
 * A [ContentTokenizer] using a [TextTokenizer] to split the text of the [Content.Element] into smaller
 * portions.
 *
 * @param contextSnippetLength Length of `before` and `after` snippets in the produced [Locator]s.
 */
@ExperimentalReadiumApi
class TextContentTokenizer(
    private val defaultLanguage: Language?,
    private val contextSnippetLength: Int = 50,
    private val textTokenizerFactory: (Language?) -> TextTokenizer
) : ContentTokenizer {

    /**
     * A [ContentTokenizer] using the default [TextTokenizer] to split the text of the [Content.Element].
     */
    constructor(defaultLanguage: Language?, unit: TextUnit) : this(
        defaultLanguage = defaultLanguage,
        textTokenizerFactory = { language -> DefaultTextContentTokenizer(unit, language) }
    )

    override fun tokenize(data: Content.Element): List<Content.Element> = listOf(
        if (data is Content.TextElement) {
            data.copy(
                segments = data.segments.flatMap { tokenize(it) }
            )
        } else {
            data
        }
    )

    private fun tokenize(segment: Content.TextElement.Segment): List<Content.TextElement.Segment> =
        textTokenizerFactory(segment.language ?: defaultLanguage).tokenize(segment.text)
            .map { range ->
                segment.copy(
                    locator = segment.locator.copy(text = extractTextContextIn(segment.text, range)),
                    text = segment.text.substring(range)
                )
            }

    private fun extractTextContextIn(string: String, range: IntRange): Locator.Text {
        val after = string.substring(range.last, (range.last + contextSnippetLength).coerceAtMost(string.length))
        val before = string.substring((range.first - contextSnippetLength).coerceAtLeast(0), range.first)
        return Locator.Text(
            after = after.takeIf { it.isNotEmpty() },
            before = before.takeIf { it.isNotEmpty() },
            highlight = string.substring(range)
        )
    }
}