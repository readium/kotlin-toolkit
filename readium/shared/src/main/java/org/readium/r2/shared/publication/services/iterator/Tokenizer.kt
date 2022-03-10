/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.iterator

import android.icu.text.BreakIterator
import android.os.Build
import androidx.annotation.RequiresApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Try
import java.util.*

internal interface Tokenizer {
    suspend fun tokenize(content: String): TextIteratorTry<List<Locator.Text>>
}

internal fun unitTextContentTokenizer(unit: TextUnit, locale: Locale?, contextLength: Int = 200): Tokenizer =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        IcuTokenizer(locale = locale, unit = unit, contextLength = contextLength)
    else
        NaiveTokenizer(unit = unit, contextLength = contextLength)

/**
 * Implementation of a [Tokenizer] using ICU components to perform the actual
 * tokenization while taking into account languages specificities.
 */
@RequiresApi(Build.VERSION_CODES.N)
internal class IcuTokenizer(
    locale: Locale?,
    private val unit: TextUnit,
    private val contextLength: Int
) : Tokenizer {
    private val breakIterator: TextIteratorTry<BreakIterator> by lazy {
        val loc = locale ?: Locale.ROOT
        when (unit) {
            TextUnit.Character ->
                Try.success(BreakIterator.getCharacterInstance(loc))
            TextUnit.Word ->
                Try.success(BreakIterator.getWordInstance(loc))
            TextUnit.Sentence ->
                Try.success(BreakIterator.getSentenceInstance(loc))
            TextUnit.Paragraph ->
                Try.failure(TextIteratorException.UnsupportedOption("IcuUnitTextContentTokenizer does not handle TextContentUnit.Paragraph"))
        }
    }

    override suspend fun tokenize(content: String): TextIteratorTry<List<Locator.Text>> =
        breakIterator.map { iter ->
            iter.setText(content)
            var start: Int = iter.first()
            var end: Int = iter.next()
            val tokens = mutableListOf<Locator.Text>()
            while (end != BreakIterator.DONE) {
                content.extractText(start = start, end = end, contextLength = contextLength, unit = unit)
                    ?.let { tokens.add(it) }

                start = end
                end = iter.next()
            }

            tokens
        }
}

/**
 * A naive [Tokenizer] relying on java.text.BreakIterator to split the content.
 * Use [IcuTokenizer] for better results.
 */
internal class NaiveTokenizer(
    private val unit: TextUnit,
    private val contextLength: Int
) : Tokenizer {
    private val breakIterator: TextIteratorTry<java.text.BreakIterator> by lazy {
        when (unit) {
            TextUnit.Character ->
                Try.success(java.text.BreakIterator.getCharacterInstance())
            TextUnit.Word ->
                Try.success(java.text.BreakIterator.getWordInstance())
            TextUnit.Sentence ->
                Try.success(java.text.BreakIterator.getSentenceInstance())
            TextUnit.Paragraph ->
                Try.failure(TextIteratorException.UnsupportedOption("NaiveUnitTextContentTokenizer does not handle TextContentUnit.Paragraph"))
        }
    }

    override suspend fun tokenize(content: String): TextIteratorTry<List<Locator.Text>> =
        breakIterator.map { iter ->
            iter.setText(content)
            var start: Int = iter.first()
            var end: Int = iter.next()
            val tokens = mutableListOf<Locator.Text>()
            while (end != java.text.BreakIterator.DONE) {
                content.extractText(start = start, end = end, contextLength = contextLength, unit = unit)
                    ?.let { tokens.add(it) }

                start = end
                end = iter.next()
            }

            tokens
        }
}

private fun String.extractText(start: Int, end: Int, contextLength: Int, unit: TextUnit): Locator.Text? {
    val before = substring((start - contextLength).coerceAtLeast(0), start)
    var highlight = substring(start, end)
    var after = substring(end, (end + contextLength).coerceAtMost(length))

    if (
        unit != TextUnit.Character &&
        (
            highlight.isBlank() ||
            highlight.find { it.isLetterOrDigit() } == null
        )
    ) {
        return null
    }

    if (unit == TextUnit.Sentence) {
        val origHighlight = highlight
        highlight = origHighlight.trimEnd()
        val whitespaceSuffix = origHighlight.removePrefix(highlight)
        after = whitespaceSuffix + after
    }

    return Locator.Text(
        highlight = highlight,
        before = before,
        after = after
    )
}
