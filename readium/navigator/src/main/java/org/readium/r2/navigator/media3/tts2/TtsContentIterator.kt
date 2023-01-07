/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.tts2

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.ContentTokenizer
import org.readium.r2.shared.publication.services.content.content
import org.readium.r2.shared.util.CursorList
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
internal class TtsContentIterator(
    private val publication: Publication,
    private val tokenizerFactory: (language: Language?) -> ContentTokenizer,
    initialLocator: TtsLocator?
) {
    enum class Direction {
        Forward, Backward;
    }

    private var utterances: CursorList<TtsUtterance> =
        CursorList()

    /**
     * [Content.Iterator] used to iterate through the [publication].
     */
    private var publicationIterator: Content.Iterator = createIterator(initialLocator)
        set(value) {
            field = value
            utterances = CursorList()
        }

    private var language: Language? =
        null

    /**
     * Sets the tokenizer language.
     *
     * The change is not immediate, it will be applied as soon as possible.
     */
    fun setLanguage(language: Language?) {
        this.language = language
    }

    /**
     * Gets the next utterance in the given [direction], or null when reaching the beginning or the
     * end.
     */
    suspend fun nextUtterance(direction: Direction): TtsUtterance? {
        val utterance = utterances.nextIn(direction)
        if (utterance == null && loadNextUtterances(direction)) {
            return nextUtterance(direction)
        }
        return utterance
    }

    /**
     * Moves the iterator to the position provided in [locator].
     */
    fun seek(locator: TtsLocator) {
        publicationIterator = createIterator(locator)
    }

    fun restart() {
        publicationIterator = createIterator(null)
    }

    private fun createIterator(locator: TtsLocator?) =
        publication.content(locator?.toLocator(publication))
            ?.iterator()
            ?: throw IllegalStateException("No ContentService.")

    /**
     * Loads the utterances for the next publication [Content.Element] item in the given [direction].
     */
    private suspend fun loadNextUtterances(direction: Direction): Boolean {
        val content = publicationIterator.nextIn(direction)
            ?: return false

        val nextUtterances = content
            .tokenize()
            .flatMap { it.utterances() }

        if (nextUtterances.isEmpty()) {
            return loadNextUtterances(direction)
        }

        utterances = CursorList(
            list = nextUtterances,
            startIndex = when (direction) {
                Direction.Forward -> 0
                Direction.Backward -> nextUtterances.size - 1
            }
        )

        return true
    }

    /**
     * Splits a publication [Content.Element] item into smaller chunks using the provided tokenizer.
     *
     * This is used to split a paragraph into sentences, for example.
     */
    private fun Content.Element.tokenize(): List<Content.Element> =
        tokenizerFactory(language).tokenize(this)

    /**
     * Splits a publication [Content.Element] item into the utterances to be spoken.
     */
    private fun Content.Element.utterances(): List<TtsUtterance> {
        fun utterance(text: String, locator: Locator, language: Language? = null): TtsUtterance? {
            if (!text.any { it.isLetterOrDigit() })
                return null

            return TtsUtterance(
                text = text,
                locator = checkNotNull(locator.toTtsLocator(publication)) { "Missing data in locator." },
                language = language
            )
        }

        return when (this) {
            is Content.TextElement -> {
                segments.mapNotNull { segment ->
                    utterance(
                        text = segment.text,
                        locator = segment.locator,
                        language = segment.language
                    )
                }
            }

            is Content.TextualElement -> {
                listOfNotNull(
                    text
                        ?.takeIf { it.isNotBlank() }
                        ?.let { utterance(text = it, locator = locator) }
                )
            }

            else -> emptyList()
        }
    }

    private fun <E> CursorList<E>.nextIn(direction: Direction): E? =
        when (direction) {
            Direction.Forward -> next()
            Direction.Backward -> previous()
        }

    private suspend fun Content.Iterator.nextIn(direction: Direction): Content.Element? =
        when (direction) {
            Direction.Forward -> nextOrNull()
            Direction.Backward -> previousOrNull()
        }
}
