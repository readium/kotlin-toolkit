/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.robolectric.RobolectricTestRunner
import java.util.*
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class IcuTokenizerTest {

    @Test
    fun tokenizeEmptyContent() = runBlocking {
        val tokenizer = IcuTokenizer(locale = Locale.ENGLISH, unit = TextUnit.Sentence, contextLength = 5)
        assertEquals(emptyList(), tokenizer.tokenize("").getOrThrow())
    }

    @Test
    fun tokenizeByCharacters() = runBlocking {
        val tokenizer = IcuTokenizer(locale = Locale.ENGLISH, unit = TextUnit.Character, contextLength = 2)
        val result = tokenizer.tokenize("He said: \n\"What?\"").getOrThrow()
        assertContentEquals(
            listOf(
                Locator.Text(before = "", highlight = "H", after = "e "),
                Locator.Text(before = "H", highlight = "e", after = " s"),
                Locator.Text(before = "He", highlight = " ", after = "sa"),
                Locator.Text(before = "e ", highlight = "s", after = "ai"),
                Locator.Text(before = " s", highlight = "a", after = "id"),
                Locator.Text(before = "sa", highlight = "i", after = "d:"),
                Locator.Text(before = "ai", highlight = "d", after = ": "),
                Locator.Text(before = "id", highlight = ":", after = " \n"),
                Locator.Text(before = "d:", highlight = " ", after = "\n\""),
                Locator.Text(before = ": ", highlight = "\n", after = "\"W"),
                Locator.Text(before = " \n", highlight = "\"", after = "Wh"),
                Locator.Text(before = "\n\"", highlight = "W", after = "ha"),
                Locator.Text(before = "\"W", highlight = "h", after = "at"),
                Locator.Text(before = "Wh", highlight = "a", after = "t?"),
                Locator.Text(before = "ha", highlight = "t", after = "?\""),
                Locator.Text(before = "at", highlight = "?", after = "\""),
                Locator.Text(before = "t?", highlight = "\"", after = ""),
            ),
            result
        )
    }

    @Test
    fun tokenizeByWords() = runBlocking {
        val tokenizer = IcuTokenizer(locale = Locale.ENGLISH, unit = TextUnit.Word, contextLength = 2)
        val result = tokenizer.tokenize("He said: \n\"What?\"").getOrThrow()
        assertContentEquals(
            listOf(
                Locator.Text(before = "", highlight = "He", after = " s"),
                Locator.Text(before = "e ", highlight = "said", after = ": "),
                Locator.Text(before = "\n\"", highlight = "What", after = "?\""),
            ),
            result
        )
    }

    @Test
    fun tokenizeBySentences() = runBlocking {
        val tokenizer = IcuTokenizer(locale = Locale.ENGLISH, unit = TextUnit.Sentence, contextLength = 5)
        val result = tokenizer.tokenize("""
            Alice said, looking above: "and what is the use of a book?". So she was considering (as well as she could), whether making a daisy-chain would be worth the trouble
            In the end, she went ahead.
        """.trimIndent()).getOrThrow()
        assertContentEquals(
            listOf(
                Locator.Text(
                    before = "",
                    highlight = "Alice said, looking above: \"and what is the use of a book?\".",
                    after = " So sh"
                ),
                Locator.Text(
                    before = "k?\". ",
                    highlight = "So she was considering (as well as she could), whether making a daisy-chain would be worth the trouble",
                    after = "\nIn th"
                ),
                Locator.Text(
                    before = "uble\n",
                    highlight = "In the end, she went ahead.",
                    after = ""
                )
            ),
            result
        )
    }

    @Test
    fun tokenizeByParagraphIsNotSupported() = runBlocking {
        val tokenizer = IcuTokenizer(locale = Locale.ENGLISH, unit = TextUnit.Paragraph, contextLength = 5)
        val result = tokenizer.tokenize("""
            Alice said, looking above: "and what is the use of a book?". So she was considering (as well as she could), whether making a daisy-chain would be worth the trouble
            In the end, she went ahead.
        """.trimIndent())
        assertEquals("IcuUnitTextContentTokenizer does not handle TextContentUnit.Paragraph", result.exceptionOrNull()?.message)
    }
}
