/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.tokenizer

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.Content.Data
import org.readium.r2.shared.util.Language
import java.util.*

/** A tokenizer splitting a [Content] into smaller pieces. */
@ExperimentalReadiumApi
fun interface ContentTokenizer : Tokenizer<Content, Content>

/**
 * A [ContentTokenizer] using a [TextTokenizer] to split the text of the [Content].
 */
@ExperimentalReadiumApi
class TextContentTokenizer(
    private val defaultLanguage: Language?,
    private val textTokenizerFactory: (Language?) -> TextTokenizer
) : ContentTokenizer {

    /**
     * A [ContentTokenizer] using the default [TextTokenizer] to split the text of the [Content].
     */
    constructor(defaultLanguage: Language?, unit: TextUnit) : this(
        defaultLanguage = defaultLanguage,
        textTokenizerFactory = { language -> DefaultTextContentTokenizer(unit, language) }
    )

    override fun tokenize(data: Content): List<Content> = listOf(
        if (data.data is Data.Text) {
            data.copy(
                data = data.data.copy(
                    spans = data.data.spans.flatMap { tokenize(it) }
                )
            )
        } else {
            data
        }
    )

    private fun tokenize(span: Data.Text.Span): List<Data.Text.Span> =
        textTokenizerFactory(span.language ?: defaultLanguage).tokenize(span.text)
            .map { range ->
                span.copy(
                    locator = span.locator.copy(text = extractTextContextIn(span.text, range)),
                    text = span.text.substring(range)
                )
            }

    private fun extractTextContextIn(string: String, range: IntRange): Locator.Text {
        val after = string.substring(range.last, (range.last + 50).coerceAtMost(string.length))
        val before = string.substring((range.first - 50).coerceAtLeast(0), range.first)
        return Locator.Text(
            after = after.takeIf { it.isNotEmpty() },
            before = before.takeIf { it.isNotEmpty() },
            highlight = string.substring(range)
        )
    }
}