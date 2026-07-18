/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.tokenizer

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/** A tokenizer splitting a String into range tokens (e.g. words, sentences, etc.). */
@ExperimentalReadiumApi
public typealias TextTokenizer = Tokenizer<String, IntRange>

/** A text token unit which can be used with a [TextTokenizer]. */
@ExperimentalReadiumApi
public enum class TextUnit {
    Word,
    Sentence,
    Paragraph,
}

/**
 * A default cluster [TextTokenizer] taking advantage of the best capabilities of each platform.
 *
 * On Android it uses ICU's `BreakIterator` (falling back on `java.text.BreakIterator` on older
 * API levels); on iOS it uses `CFStringTokenizer`.
 */
@ExperimentalReadiumApi
public expect class DefaultTextContentTokenizer(
    unit: TextUnit,
    language: Language?,
) : Tokenizer<String, IntRange> {
    override fun tokenize(data: String): List<IntRange>
}

/**
 * Returns a substring range from the given [start] and [end] indices, after checking that
 * the token is not blank and trimming trailing whitespaces.
 */
internal fun String.sanitizeRange(start: Int, end: Int): IntRange? {
    val token = substring(start, end)
    val trimmedToken = token.trimEnd()
    if (!trimmedToken.any { it.isLetterOrDigit() }) {
        return null
    }
    return start until (start + trimmedToken.length)
}
