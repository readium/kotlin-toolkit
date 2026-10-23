/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.navigator.media.tts

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.html.cssSelector
import org.readium.r2.shared.publication.html.domRange
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.Content.TextElement
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

    // Backward prefetch must not consume the pending sentence-level seek.
    private var requestedLocator: Locator? = initialLocator?.takeIf { it.locations.domRange != null }

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
        requestedLocator = locator.takeIf { it.locations.domRange != null }
        publicationIterator = createIterator(locator)
    }

    /**
     * Moves the iterator to the beginning of the publication.
     */
    fun seekToBeginning() {
        clearRequestedLocator()
        publicationIterator = createIterator(locator = null)
    }

    /**
     * Moves the iterator to the resource with the given [index] in the publication reading order.
     */
    fun seekToResource(index: Int) {
        val link = publication.readingOrder.getOrNull(index) ?: return
        val locator = publication.locatorFromLink(link)
        clearRequestedLocator()
        publicationIterator = createIterator(locator)
    }

    /**
     * Creates a fresh content iterator for the publication starting from [Locator].
     */

    private fun createIterator(locator: Locator?): Content.Iterator =
        contentService.content(locator).iterator()

    private fun clearRequestedLocator() {
        requestedLocator = null
    }

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
     * Loads the next element's utterances, selecting a sentence only for a pending forward DOM seek.
     *
     * @param direction Direction of traversal through the publication content.
     * @return `true` when a nonempty utterance list is ready for the cursor.
     */
    private suspend fun loadNextUtterances(direction: Direction): Boolean {
        val target = requestedLocator

        if (direction == Direction.Backward || target == null) {
            return loadOrdinaryUtterances(direction)
        }

        val htmlTarget = publication.resolveTtsHtmlTarget(target)

        if (htmlTarget == null) {
            clearRequestedLocator()
            return loadOrdinaryUtterances(direction)
        }

        val fallback = if (htmlTarget.text.isBlank()) {
            target.copy(locations = Locator.Locations(progression = 0.0))
        } else {
            target.copy(locations = Locator.Locations(otherLocations = mapOf("cssSelector" to htmlTarget.cssSelector)))
        }

        var fragmentEnd = 0
        var matchingBlockFound = false

        while (currentCoroutineContext().isActive) {
            val content = publicationIterator.nextIn(Direction.Forward) ?: break

            if (content.locator.href != target.href) {
                break
            }

            if (content !is TextElement || content.locator.locations.cssSelector != htmlTarget.cssSelector) {
                if (matchingBlockFound) {
                    break
                }
                continue
            }

            matchingBlockFound = true

            val fragment = content.text

            val start = htmlTarget.text.indexOf(fragment, fragmentEnd)
            if (start < 0) {
                continue
            }

            fragmentEnd = start + fragment.length

            if (htmlTarget.offset !in start until fragmentEnd) {
                continue
            }

            val spokenStart = fragment.indexOfFirstFrom(htmlTarget.offset - start) { it != ' ' }
            val nextUtterances = content.locatedUtterances()

            val selected = nextUtterances.indexOfFirst { spokenStart in it.range }
            if (selected < 0) {
                continue
            }

            clearRequestedLocator()

            utterances = CursorList(nextUtterances.map { it.utterance }, selected - 1)

            return true
        }

        currentCoroutineContext().ensureActive()

        // The selected block is exhausted. Restart once from its first fragment;
        // an empty block falls back to the resource's beginning.
        clearRequestedLocator()

        publicationIterator = createIterator(fallback)

        return loadOrdinaryUtterances(Direction.Forward)
    }

    /**
     * Loads ordinary utterances, preserving navigation after the DOM seek.
     *
     * @param direction Direction of traversal through the publication content.
     * @return `true` when a nonempty utterance list is ready for the cursor.
     */
    private suspend fun loadOrdinaryUtterances(direction: Direction): Boolean {
        val content = publicationIterator.nextIn(direction) ?: return false

        val nextUtterances = content.tokenize().flatMap { it.utterances() }

        if (nextUtterances.isEmpty()) {
            return loadOrdinaryUtterances(direction)
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
     * Splits the selected text element into utterances and their source ranges.
     *
     * Ranges refer to offsets in the emitted element text, not to token indices; punctuation-only
     * tokens are excluded just as they are from ordinary speech.
     *
     * @return Spoken utterances and their UTF-16 ranges in this text element.
     */
    private fun TextElement.locatedUtterances(): List<LocatedUtterance> {
        val contentTokenizer = TextContentTokenizer(
            language = this@TtsUtteranceIterator.language,
            textTokenizerFactory = tokenizerFactory,
            overrideContentLanguage = overrideContentLanguage
        )

        var elementOffset = 0

        return buildList {
            for (segment in segments) {
                val tokenized = contentTokenizer.tokenize(copy(segments = listOf(segment)))
                    .single() as TextElement

                var searchFrom = 0

                for (token in tokenized.segments) {
                    // Readium's tokenizer preserves the exact source substring for each token.
                    val start = segment.text.indexOf(token.text, startIndex = searchFrom)

                    check(start >= 0) { "Token does not belong to its source segment." }

                    searchFrom = start + token.text.length

                    utterance(token.text, token.locator, token.language)?.let { utterance ->
                        add(LocatedUtterance(utterance, (elementOffset + start) until (elementOffset + searchFrom)))
                    }
                }

                elementOffset += segment.text.length
            }
        }
    }

    private data class LocatedUtterance(val utterance: Utterance, val range: IntRange)

    private fun String.indexOfFirstFrom(start: Int, predicate: (Char) -> Boolean): Int {
        for (index in start.coerceAtLeast(0) until length) {
            if (predicate(this[index])) {
                return index
            }
        }

        return -1
    }

    private fun utterance(text: String, locator: Locator, language: Language? = null): Utterance? {
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

    /**
     * Splits a publication [Content.Element] item into the utterances to be spoken.
     */
    private fun Content.Element.utterances(): List<Utterance> {
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
