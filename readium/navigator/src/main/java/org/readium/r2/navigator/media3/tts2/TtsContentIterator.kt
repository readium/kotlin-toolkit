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

    /**
     * Current subset of utterances with a cursor.
     */
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

    /**
     * The tokenizer language.
     *
     * Modifying this property is not immediate, the new value will be applied as soon as possible.
     */
    var language: Language? =
        null

    /**
     * Moves the iterator to the position provided in [locator].
     */
    fun seek(locator: TtsLocator) {
        publicationIterator = createIterator(locator)
    }

    /**
     * Moves the iterator to the beginning of the publication.
     */
    fun seekToBeginning() {
        publicationIterator = createIterator(null)
    }

    /**
     * Creates a fresh content iterator for the publication starting from [locator].
     */
    private fun createIterator(locator: TtsLocator?): Content.Iterator =
        publication.content(locator?.toLocator(publication))
            ?.iterator()
            ?: throw IllegalStateException("No ContentService.")

    /**
     * Advances to the previous item and returns it, or null if we reached the beginning.
     */
    suspend fun previousUtterance(): TtsUtterance? =
        nextUtterance(Direction.Backward)

    /**
     * Advances to the next item and returns it, or null if we reached the end.
     */
    suspend fun nextUtterance(): TtsUtterance? =
        nextUtterance(Direction.Forward)

    private enum class Direction {
        Forward, Backward;
    }

    /**
     * Gets the next utterance in the given [direction], or null when reaching the beginning or the
     * end.
     */
    private suspend fun nextUtterance(direction: Direction): TtsUtterance? {
        val utterance = utterances.nextIn(direction)
        if (utterance == null && loadNextUtterances(direction)) {
            return nextUtterance(direction)
        }
        return utterance
    }

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
