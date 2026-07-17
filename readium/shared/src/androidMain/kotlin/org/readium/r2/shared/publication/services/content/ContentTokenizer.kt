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
public fun interface ContentTokenizer : Tokenizer<Content.Element, Content.Element>

/**
 * A [ContentTokenizer] using a [TextTokenizer] to split the text of the [Content.Element] into smaller
 * portions.
 *
 * @param contextSnippetLength Length of `before` and `after` snippets in the produced [Locator]s.
 * @param overrideContentLanguage If true, let [language] override language information that could be available in
 *   content. If false, [language] will be used only as a default when there is no data-specific information.
 */
@ExperimentalReadiumApi
public class TextContentTokenizer(
    private val language: Language?,
    private val overrideContentLanguage: Boolean = false,
    private val contextSnippetLength: Int = 50,
    private val textTokenizerFactory: (Language?) -> TextTokenizer,
) : ContentTokenizer {

    /**
     * A [ContentTokenizer] using the default [TextTokenizer] to split the text of the [Content.Element].
     */
    public constructor(
        language: Language?,
        unit: TextUnit,
        overrideContentLanguage: Boolean = false,
    ) : this(
        language = language,
        textTokenizerFactory = { contentLanguage ->
            DefaultTextContentTokenizer(
                unit,
                contentLanguage
            )
        },
        overrideContentLanguage = overrideContentLanguage
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
        textTokenizerFactory(resolveSegmentLanguage(segment)).tokenize(segment.text)
            .map { range ->
                segment.copy(
                    locator = segment.locator.copy(text = extractTextContextIn(segment.text, range)),
                    text = segment.text.substring(range)
                )
            }

    private fun resolveSegmentLanguage(segment: Content.TextElement.Segment): Language? =
        segment.language.takeUnless { overrideContentLanguage } ?: language

    private fun extractTextContextIn(string: String, range: IntRange): Locator.Text {
        val after = string.substring(
            range.last,
            (range.last + contextSnippetLength).coerceAtMost(string.length)
        )
        val before = string.substring(
            (range.first - contextSnippetLength).coerceAtLeast(0),
            range.first
        )
        return Locator.Text(
            after = after.takeIf { it.isNotEmpty() },
            before = before.takeIf { it.isNotEmpty() },
            highlight = string.substring(range)
        )
    }
}
