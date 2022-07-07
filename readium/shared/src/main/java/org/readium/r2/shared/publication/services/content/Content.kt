/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Language
import java.net.URL

/**
 * Provides an iterable list of content [Element]s.
 */
@ExperimentalReadiumApi
interface Content {

    /**
     * Represents a single semantic content element part of a publication.
     */
    @ExperimentalReadiumApi
    interface Element {
        /**
         * Locator targeting this element in the Publication.
         */
        val locator: Locator
    }

    /** An element which can be represented as human-readable text. */
    interface TextualElement : Element {
        /** Human-readable text representation for this element. */
        val text: String?
    }

    /** An element referencing an embedded external resource. */
    interface EmbeddedElement : Element {
        /** Referenced resource in the publication. */
        val embeddedLink: Link
    }

    /**
     * An audio clip.
     *
     * @param extras Additional metadata for extensions.
     */
    data class AudioElement(
        override val locator: Locator,
        override val embeddedLink: Link,
        val extras: Map<String, Any> = emptyMap(),
    ) : EmbeddedElement

    /**
     * A video clip.
     *
     * @param extras Additional metadata for extensions.
     */
    data class VideoElement(
        override val locator: Locator,
        override val embeddedLink: Link,
        val extras: Map<String, Any> = emptyMap(),
    ) : EmbeddedElement

    /**
     * A bitmap image.
     *
     * @param caption Short piece of text associated with the image.
     * @param description Accessibility label.
     * @param extras Additional metadata for extensions.
     */
    data class ImageElement(
        override val locator: Locator,
        override val embeddedLink: Link,
        val caption: String?,
        val description: String?,
        val extras: Map<String, Any> = emptyMap(),
    ) : EmbeddedElement, TextualElement {
        override val text: String?
            get() = caption?.takeIf { it.isNotBlank() }
                ?: description
    }

    /**
     * A text element.
     *
     * @param role Purpose of this element in the broader context of the document.
     * @param segments Ranged portions of text with associated attributes.
     * @param extras Additional metadata for extensions.
     */
    data class TextElement(
        override val locator: Locator,
        val role: Role,
        val segments: List<Segment>,
        val extras: Map<String, Any> = emptyMap(),
    ) : TextualElement {

        override val text: String
            get() = segments.joinToString { it.text }

        /**
         * Represents a purpose of an element in the broader context of the document.
         */
        interface Role {
            /**
             * Title of a section.
             *
             * @param level Heading importance, 1 being the highest.
             */
            data class Heading(val level: Int) : Role

            /**
             * Normal body of content.
             */
            object Body : Role

            /**
             * A footnote at the bottom of a document.
             */
            object Footnote : Role

            /**
             * A quotation.
             *
             * @param referenceUrl URL to the source for this quote.
             * @param referenceTitle Name of the source for this quote.
             */
            data class Quote(
                val referenceUrl: URL?,
                val referenceTitle: String?
            ) : Role
        }

        /**
         * Ranged portion of text with associated attributes.
         *
         * @param locator Locator to the segment of text.
         * @param text Text in the segment.
         * @param attributes Attributes associated with this segment, e.g. language.
         */
        data class Segment(
            val locator: Locator,
            val text: String,
            val attributes: List<Attribute<*>>,
        ) {
            /**
             * Language of the text, if any.
             */
            val language: Language?
                get() = attribute(AttributeKey.LANGUAGE)

            /**
             * An attribute is an arbitrary key-value pair.
             */
            data class Attribute<V>(
                val key: AttributeKey<V>,
                val value: V
            )

            /**
             * An attribute key identifies uniquely an attribute.
             *
             * The [V] phantom type is there to perform static type checking when requesting an
             * attribute.
             */
            data class AttributeKey<V>(val id: String) {
                companion object {
                    val LANGUAGE = AttributeKey<Language>("language")
                    val LINK = AttributeKey<URL>("link")
                }
            }

            /**
             * Gets the first attribute with the given [key].
             */
            @Suppress("UNCHECKED_CAST")
            fun <V> attribute(key: AttributeKey<V>): V? =
                attributes.firstOrNull { it.key == key }?.value as V

            @Suppress("UNCHECKED_CAST")
            fun <V> attributes(key: AttributeKey<V>): List<V> =
                attributes
                    .filter { it.key == key }
                    .map { it.value as V }
        }
    }

    /**
     * Iterates through a list of [Element] items asynchronously.
     */
    @ExperimentalReadiumApi
    interface Iterator {

        /**
         * Returns true if the iterator has a next element, suspending the caller while processing
         * it.
         */
        suspend operator fun hasNext(): Boolean

        /**
         * Retrieves the element computed by a preceding call to [hasNext], or throws an
         * [IllegalStateException] if [hasNext] was not invoked. This method should only be used in
         * pair with [hasNext].
         */
        operator fun next(): Element

        /**
         * Advances to the next item and returns it, or null if we reached the end.
         */
        suspend fun nextOrNull(): Element? =
            if (hasNext()) next() else null

        /**
         * Returns true if the iterator has a previous element, suspending the caller while processing
         * it.
         */
        suspend fun hasPrevious(): Boolean

        /**
         * Retrieves the element computed by a preceding call to [hasPrevious], or throws an
         * [IllegalStateException] if [hasPrevious] was not invoked. This method should only be used in
         * pair with [hasPrevious].
         */
        fun previous(): Element

        /**
         * Advances to the previous item and returns it, or null if we reached the beginning.
         */
        suspend fun previousOrNull(): Element? =
            if (hasPrevious()) previous() else null
    }

    /**
     * Creates a new iterator for this content.
     */
    operator fun iterator(): Iterator

    /**
     * Returns all the elements as a list.
     */
    suspend fun elements(): List<Element> =
        buildList {
            for (element in this@Content) {
                add(element)
            }
        }
}

/**
 * An empty [Content].
 */
@ExperimentalReadiumApi
class EmptyContent : Content {
    override fun iterator(): Content.Iterator = EmptyContentIterator()
}

/**
 * An empty [Content.Iterator].
 */
@ExperimentalReadiumApi
class EmptyContentIterator : Content.Iterator {
    override suspend fun hasNext(): Boolean = false

    override fun next(): Content.Element {
        throw IllegalStateException("Called next() without a successful call to hasPrevious() first")
    }

    override suspend fun hasPrevious(): Boolean = false

    override fun previous(): Content.Element {
        throw IllegalStateException("Called previous() without a successful call to hasPrevious() first")
    }
}
