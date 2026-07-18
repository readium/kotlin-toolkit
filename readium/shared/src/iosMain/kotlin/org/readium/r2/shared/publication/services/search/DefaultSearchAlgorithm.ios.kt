/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import kotlin.math.max
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.services.search.SearchService.Options
import org.readium.r2.shared.util.Language
import platform.Foundation.NSCaseInsensitiveSearch
import platform.Foundation.NSDiacriticInsensitiveSearch
import platform.Foundation.NSLocale
import platform.Foundation.NSMakeRange
import platform.Foundation.NSNotFound
import platform.Foundation.NSString
import platform.Foundation.NSStringCompareOptions
import platform.Foundation.create
import platform.Foundation.rangeOfString

/**
 * The default cluster search [StringSearchService.Algorithm] on iOS, implemented with
 * `NSString.rangeOfString(options:range:locale:)`.
 *
 * Contrary to the Android ICU-backed algorithm, whole-word search is not supported.
 */
@ExperimentalReadiumApi
public actual class DefaultSearchAlgorithm public actual constructor() : StringSearchService.Algorithm {

    actual override val options: Options = Options(
        caseSensitive = false,
        diacriticSensitive = false
    )

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual override suspend fun findRanges(
        query: String,
        options: Options,
        text: String,
        language: Language?,
    ): List<IntRange> {
        if (query.isEmpty() || text.isEmpty()) {
            return emptyList()
        }

        var compareOptions: NSStringCompareOptions = 0u
        if (options.caseSensitive != true) {
            compareOptions = compareOptions or NSCaseInsensitiveSearch
        }
        if (options.diacriticSensitive != true) {
            compareOptions = compareOptions or NSDiacriticInsensitiveSearch
        }

        val nsText = NSString.create(string = text)
        val locale = language?.let { NSLocale(localeIdentifier = it.code.replace('-', '_')) }

        val ranges = mutableListOf<IntRange>()
        var index = 0
        while (index < text.length) {
            val match: Pair<Int, Int> = nsText.rangeOfString(
                query,
                options = compareOptions,
                range = NSMakeRange(index.toULong(), (text.length - index).toULong()),
                locale = locale
            ).useContents {
                if (location.toLong() == NSNotFound) {
                    null
                } else {
                    location.toInt() to length.toInt()
                }
            } ?: break

            val (location, length) = match
            if (length > 0) {
                ranges.add(location until (location + length))
            }
            // Always advance by at least one unit, so that a zero-length match (which can
            // happen with some composed character sequences) does not end the search early.
            index = max(location + length, index + 1)
        }
        return ranges
    }
}
