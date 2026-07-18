/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.tokenizer

import android.icu.text.BreakIterator
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.locale

/**
 * A default cluster [TextTokenizer] taking advantage of the best capabilities of each Android
 * version.
 */
@ExperimentalReadiumApi
public actual class DefaultTextContentTokenizer public actual constructor(
    unit: TextUnit,
    language: Language?,
) : Tokenizer<String, IntRange> {

    private val tokenizer: TextTokenizer =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            IcuTextTokenizer(language = language, unit = unit)
        } else {
            NaiveTextTokenizer(unit = unit)
        }

    actual override fun tokenize(data: String): List<IntRange> =
        tokenizer.tokenize(data)
}

/**
 * Implementation of a [TextTokenizer] using ICU components to perform the actual
 * tokenization while taking into account languages specificities.
 */
@ExperimentalReadiumApi
@RequiresApi(Build.VERSION_CODES.N)
public class IcuTextTokenizer(language: Language?, unit: TextUnit) : TextTokenizer {

    private val iterator: BreakIterator

    init {
        val loc = language?.locale ?: Locale.ROOT
        iterator = when (unit) {
            TextUnit.Word -> BreakIterator.getWordInstance(loc)
            TextUnit.Sentence -> BreakIterator.getSentenceInstance(loc)
            TextUnit.Paragraph -> throw IllegalArgumentException(
                "IcuTextTokenizer does not handle TextContentUnit.Paragraph"
            )
        }
    }

    override fun tokenize(data: String): List<IntRange> {
        iterator.setText(data)
        var start: Int = iterator.first()
        var end: Int = iterator.next()
        return buildList {
            while (end != BreakIterator.DONE) {
                data.sanitizeRange(start, end)
                    ?.let { add(it) }

                start = end
                end = iterator.next()
            }
        }
    }
}

/**
 * A naive [Tokenizer] relying on java.text.BreakIterator to split the content.
 *
 * Use [IcuTextTokenizer] for better results.
 */
@ExperimentalReadiumApi
public class NaiveTextTokenizer(unit: TextUnit) : TextTokenizer {
    private val iterator: java.text.BreakIterator = when (unit) {
        TextUnit.Word -> java.text.BreakIterator.getWordInstance()
        TextUnit.Sentence -> java.text.BreakIterator.getSentenceInstance()
        TextUnit.Paragraph -> throw IllegalArgumentException(
            "NaiveTextTokenizer does not handle TextContentUnit.Paragraph"
        )
    }

    override fun tokenize(data: String): List<IntRange> {
        iterator.setText(data)
        var start: Int = iterator.first()
        var end: Int = iterator.next()
        return buildList {
            while (end != java.text.BreakIterator.DONE) {
                data.sanitizeRange(start, end)
                    ?.let { add(it) }

                start = end
                end = iterator.next()
            }
        }
    }
}
