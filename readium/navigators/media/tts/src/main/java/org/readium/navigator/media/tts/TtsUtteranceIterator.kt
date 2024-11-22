/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.media.tts

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.ContentService
import org.readium.r2.shared.publication.services.content.TextContentTokenizer
import org.readium.r2.shared.util.CursorList
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.tokenizer.TextTokenizer

/**
 * A Content Iterator able to provide short utterances.
 *
 * Not thread-safe.
 */
@ExperimentalReadiumApi
internal class TtsUtteranceIterator(
    private val publication: Publication,
    private val tokenizerFactory: (language: Language?) -> TextTokenizer,
    initialLocator: Locator?,
) {
    data class Utterance(
        val utterance: String,
        val resourceIndex: Int,
        val locations: Locator.Locations,
        val text: Locator.Text,
        val language: Language?,
    )

    private val contentService: ContentService =
        publication.findService(ContentService::class)
            ?: throw IllegalStateException("No ContentService.")

    /**
     * Current subset of utterances with a cursor.
     */
    private var utterances: CursorList<Utterance> =
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
     * Whether language information in content should be superseded by [language] while tokenizing.
     *
     * Modifying this property is not immediate, the new value will be applied as soon as possible.
     */
    var overrideContentLanguage: Boolean =
        false

    val resourceCount: Int =
        publication.readingOrder.size

    /**
     * Moves the iterator to the position provided in [locator].
     */
    fun seek(locator: Locator) {
        publicationIterator = createIterator(locator)
    }

    /**
     * Moves the iterator to the beginning of the publication.
     */
    fun seekToBeginning() {
        publicationIterator = createIterator(locator = null)
    }

    /**
     * Moves the iterator to the resource with the given [index] in the publication reading order.
     */
    fun seekToResource(index: Int) {
        val link = publication.readingOrder.getOrNull(index) ?: return
        val locator = publication.locatorFromLink(link)
        publicationIterator = createIterator(locator)
    }

    /**
     * Creates a fresh content iterator for the publication starting from [Locator].
     */

    private fun createIterator(locator: Locator?): Content.Iterator =
        contentService.content(locator).iterator()

    suspend fun hasPrevious(): Boolean =
        hasNextIn(Direction.Backward)

    suspend fun hasNext(): Boolean =
        hasNextIn(Direction.Forward)

    private suspend fun hasNextIn(direction: Direction): Boolean {
        if (utterances.isEmpty()) {
            loadNextUtterances(direction)
        }
        return utterances.hasNextIn(direction)
    }

    /**
     * Advances to the previous item and returns it, or null if we reached the beginning.
     */
    suspend fun previous(): Utterance? =
        next(Direction.Backward)

    /**
     * Advances to the next item and returns it, or null if we reached the end.
     */
    suspend fun next(): Utterance? =
        next(Direction.Forward)

    private enum class Direction {
        Forward,
        Backward,
    }

    /**
     * Gets the next utterance in the given [direction], or null when reaching the beginning or the
     * end.
     */
    private suspend fun next(direction: Direction): Utterance? {
        val utterance = utterances.nextIn(direction)
        if (utterance == null && loadNextUtterances(direction)) {
            return next(direction)
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
            index = when (direction) {
                Direction.Forward -> -1
                Direction.Backward -> nextUtterances.size
            }
        )

        return true
    }

    /**
     * Splits a publication [Content.Element] item into smaller chunks using the provided tokenizer.
     *
     * This is used to split a paragraph into sentences, for example.
     */
    private fun Content.Element.tokenize(): List<Content.Element> {
        val contentTokenizer = TextContentTokenizer(
            language = this@TtsUtteranceIterator.language,
            textTokenizerFactory = tokenizerFactory,
            overrideContentLanguage = overrideContentLanguage
        )
        return contentTokenizer.tokenize(this)
    }

    /**
     * Splits a publication [Content.Element] item into the utterances to be spoken.
     */
    private fun Content.Element.utterances(): List<Utterance> {
        fun utterance(text: String, locator: Locator, language: Language? = null): Utterance? {
            if (!text.any { it.isLetterOrDigit() }) {
                return null
            }

            val resourceIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
                ?: throw IllegalStateException("Content Element cannot be found in readingOrder.")

            return Utterance(
                utterance = text,
                resourceIndex = resourceIndex,
                locations = locator.locations,
                text = locator.text,
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

    private fun <E> CursorList<E>.hasNextIn(direction: Direction): Boolean =
        when (direction) {
            Direction.Forward -> hasNext()
            Direction.Backward -> hasPrevious()
        }

    private fun <E> CursorList<E>.nextIn(direction: Direction): E? =
        when (direction) {
            Direction.Forward -> if (hasNext()) next() else null
            Direction.Backward -> if (hasPrevious()) previous() else null
        }

    private suspend fun Content.Iterator.nextIn(direction: Direction): Content.Element? =
        when (direction) {
            Direction.Forward -> nextOrNull()
            Direction.Backward -> previousOrNull()
        }
}
