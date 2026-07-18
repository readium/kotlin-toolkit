/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.tokenizer

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

/**
 * Asserts the behavior shared by the platform tokenizers (ICU on Android, CFStringTokenizer on
 * iOS). Platform-specific edge cases are asserted in the platform test source sets.
 */
@OptIn(ExperimentalReadiumApi::class)
class DefaultTextContentTokenizerTest {

    private val english = Language("en")

    @Test
    fun tokenizeEmptyContent() {
        val tokenizer = DefaultTextContentTokenizer(TextUnit.Sentence, english)
        assertEquals(emptyList(), tokenizer.tokenize(""))
    }

    @Test
    fun tokenizeByWords() {
        val tokenizer = DefaultTextContentTokenizer(TextUnit.Word, english)
        val source = "He said: \n\"What?\""
        assertContentEquals(
            listOf("He", "said", "What"),
            tokenizer.tokenize(source)
                .map { source.substring(it) }
        )
    }

    @Test
    fun tokenizeBySentences() {
        val tokenizer = DefaultTextContentTokenizer(TextUnit.Sentence, english)
        val source = "Alice was beginning to get very tired. So she went away! Was it worth it?"
        assertContentEquals(
            listOf(
                "Alice was beginning to get very tired.",
                "So she went away!",
                "Was it worth it?"
            ),
            tokenizer.tokenize(source)
                .map { source.substring(it) }
        )
    }

    @Test
    fun tokenizeByParagraphIsNotSupported() {
        assertFails {
            DefaultTextContentTokenizer(TextUnit.Paragraph, english)
        }
    }
}
