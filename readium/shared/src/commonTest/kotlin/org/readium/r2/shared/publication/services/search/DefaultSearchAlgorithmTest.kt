/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.services.search.SearchService.Options
import org.readium.r2.shared.util.Language

/**
 * Asserts the search behavior shared by every platform algorithm (including the naive fallback
 * used on the plain host JVM, where `Build.VERSION.SDK_INT` is 0). Case and diacritic
 * insensitivity are asserted in the platform test source sets (`androidHostTest` under
 * Robolectric for ICU, `iosTest` for `NSString`).
 */
@OptIn(ExperimentalReadiumApi::class)
class DefaultSearchAlgorithmTest {

    private val algorithm = DefaultSearchAlgorithm()
    private val english = Language("en")

    private suspend fun findRanges(query: String, text: String, options: Options = Options()): List<IntRange> =
        algorithm.findRanges(query = query, options = options, text = text, language = english)

    @Test
    fun exactMatch() = runTest {
        val text = "The quick brown fox jumps over the lazy dog"
        assertEquals(listOf(4..8), findRanges("quick", text))
    }

    @Test
    fun multipleMatches() = runTest {
        val text = "cat catalog cat"
        assertEquals(listOf(0..2, 4..6, 12..14), findRanges("cat", text))
    }

    @Test
    fun noMatch() = runTest {
        assertEquals(emptyList(), findRanges("horse", "The quick brown fox"))
    }
}
