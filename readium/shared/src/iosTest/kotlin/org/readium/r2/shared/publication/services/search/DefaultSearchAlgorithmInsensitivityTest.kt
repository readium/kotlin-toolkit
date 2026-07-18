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

@OptIn(ExperimentalReadiumApi::class)
class DefaultSearchAlgorithmInsensitivityTest {

    private val algorithm = DefaultSearchAlgorithm()
    private val english = Language("en")

    private suspend fun findRanges(query: String, text: String, options: Options = Options()): List<IntRange> =
        algorithm.findRanges(query = query, options = options, text = text, language = english)

    @Test
    fun matchIsCaseInsensitiveByDefault() = runTest {
        val text = "Apple APPLE apple"
        assertEquals(listOf(0..4, 6..10, 12..16), findRanges("apple", text))
    }

    @Test
    fun matchIsDiacriticInsensitiveByDefault() = runTest {
        val text = "caf\u00e9 cafe"
        assertEquals(listOf(0..3, 5..8), findRanges("cafe", text))
    }

    @Test
    fun supportsCaseAndDiacriticInsensitiveOptions() = runTest {
        val options = algorithm.options
        assertEquals(false, options.caseSensitive)
        assertEquals(false, options.diacriticSensitive)
    }
}
