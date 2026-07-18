/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import android.icu.text.BreakIterator
import android.icu.text.Collator
import android.icu.text.RuleBasedCollator
import android.icu.text.StringSearch
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.StringCharacterIterator
import java.util.Locale
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.services.search.SearchService.Options
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.locale

/**
 * The default cluster search [StringSearchService.Algorithm], taking advantage of the best
 * capabilities of each Android version.
 */
@ExperimentalReadiumApi
public actual class DefaultSearchAlgorithm public actual constructor() : StringSearchService.Algorithm {

    private val algorithm: StringSearchService.Algorithm =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            IcuAlgorithm()
        } else {
            StringSearchService.NaiveAlgorithm()
        }

    actual override val options: Options get() = algorithm.options

    actual override suspend fun findRanges(
        query: String,
        options: Options,
        text: String,
        language: Language?,
    ): List<IntRange> =
        algorithm.findRanges(query, options, text, language)
}

/**
 * Implementation of a search [StringSearchService.Algorithm] using ICU components to perform the
 * actual search while taking into account languages specificities.
 */
@ExperimentalReadiumApi
@RequiresApi(Build.VERSION_CODES.N)
public class IcuAlgorithm : StringSearchService.Algorithm {

    override val options: Options = Options(
        caseSensitive = false,
        diacriticSensitive = false,
        wholeWord = false
    )

    override suspend fun findRanges(
        query: String,
        options: Options,
        text: String,
        language: Language?,
    ): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        val iter = createStringSearch(query, options, text, language)
        var start = iter.first()
        while (start != android.icu.text.SearchIterator.DONE) {
            ranges.add(start until (start + iter.matchLength))
            start = iter.next()
        }
        return ranges
    }

    private fun createStringSearch(
        query: String,
        options: Options,
        text: String,
        language: Language?,
    ): StringSearch {
        val caseSensitive = options.caseSensitive ?: false
        var diacriticSensitive = options.diacriticSensitive ?: false
        val wholeWord = options.wholeWord ?: false

        // Because of an issue (see FIXME below), we can't have case sensitivity without also
        // enabling diacritic sensitivity.
        diacriticSensitive = diacriticSensitive || caseSensitive

        // http://userguide.icu-project.org/collation/customization
        // ignore diacritics and case = primary strength
        // ignore diacritics = primary strength + caseLevel on
        // ignore case = secondary strength
        val locale = language?.locale ?: Locale.getDefault()
        val collator = Collator.getInstance(locale) as RuleBasedCollator
        if (!diacriticSensitive) {
            collator.strength = Collator.PRIMARY
            // if (caseSensitive) {
            // FIXME: This doesn't seem to work despite the documentation indicating:
            // > To ignore accents but take cases into account, set strength to primary and case level to on.
            // > http://userguide.icu-project.org/collation/customization
            // collator.isCaseLevel = true
            // }
        } else if (!caseSensitive) {
            collator.strength = Collator.SECONDARY
        }

        val breakIterator: BreakIterator? =
            if (wholeWord) {
                BreakIterator.getWordInstance()
            } else {
                null
            }

        return StringSearch(query, StringCharacterIterator(text), collator, breakIterator)
    }
}
