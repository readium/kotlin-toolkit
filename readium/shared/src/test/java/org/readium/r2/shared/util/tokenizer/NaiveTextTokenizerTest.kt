/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.tokenizer

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.ExperimentalReadiumApi
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalReadiumApi::class)
@RunWith(RobolectricTestRunner::class)
class NaiveTextTokenizerTest {

    @Test
    fun tokenizeEmptyContent() = runBlocking {
        val tokenizer = NaiveTextTokenizer(unit = TextUnit.Sentence)
        assertEquals(emptyList(), tokenizer.tokenize(""))
    }

    @Test
    fun tokenizeByWords() = runBlocking {
        val tokenizer = NaiveTextTokenizer(unit = TextUnit.Word)
        val source = "He said: \n\"What?\""
        assertContentEquals(
            listOf("He", "said", "What"),
            tokenizer.tokenize(source)
                .map { source.substring(it) }
        )
    }

    @Test
    fun tokenizeBySentences() = runBlocking {
        val tokenizer = NaiveTextTokenizer(unit = TextUnit.Sentence)
        val source = """
            Alice said, looking above: "and what is the use of a book?". So she was considering (as well as she could), whether making a daisy-chain would be worth the trouble
            In the end, she went ahead.
        """.trimIndent()
        assertContentEquals(
            listOf(
                "Alice said, looking above: \"and what is the use of a book?\".",
                "So she was considering (as well as she could), whether making a daisy-chain would be worth the trouble\nIn the end, she went ahead.",
            ),
            tokenizer.tokenize(source)
                .map { source.substring(it) }
        )
    }

    @Test
    fun tokenizeByParagraphIsNotSupported(): Unit = runBlocking {
        assertFails("NaiveTextTokenizer does not handle TextContentUnit.Paragraph") {
            NaiveTextTokenizer(unit = TextUnit.Paragraph)
        }
    }
}
