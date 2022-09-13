/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services.content

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.content.Content.Element
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
    interface Element : AttributesHolder {
        /**
         * Locator targeting this element in the Publication.
         */
        val locator: Locator
    }

    /**
     * An element which can be represented as human-readable text.
     *
     * The default implementation returns the first accessibility label associated to the element.
     */
    interface TextualElement : Element {
        /** Human-readable text representation for this element. */
        val text: String? get() = accessibilityLabel
    }

    /** An element referencing an embedded external resource. */
    interface EmbeddedElement : Element {
        /** Referenced resource in the publication. */
        val embeddedLink: Link
    }

    /** An audio clip. */
    data class AudioElement(
        override val locator: Locator,
        override val embeddedLink: Link,
        override val attributes: List<Attribute<*>> = emptyList(),
    ) : EmbeddedElement, TextualElement

    /** A video clip. */
    data class VideoElement(
        override val locator: Locator,
        override val embeddedLink: Link,
        override val attributes: List<Attribute<*>> = emptyList(),
    ) : EmbeddedElement, TextualElement

    /**
     * A bitmap image.
     *
     * @param caption Short piece of text associated with the image.
     */
    data class ImageElement(
        override val locator: Locator,
        override val embeddedLink: Link,
        val caption: String?,
        override val attributes: List<Attribute<*>> = emptyList(),
    ) : EmbeddedElement, TextualElement {

        override val text: String? get() =
            // The caption might be a better text description than the accessibility label, when
            // available.
            caption?.takeIf { it.isNotBlank() }
                ?: super.text
    }

    /**
     * A text element.
     *
     * @param role Purpose of this element in the broader context of the document.
     * @param segments Ranged portions of text with associated attributes.
     */
    data class TextElement(
        override val locator: Locator,
        val role: Role,
        val segments: List<Segment>,
        override val attributes: List<Attribute<*>> = emptyList(),
    ) : TextualElement {

        override val text: String
            get() = segments.joinToString(separator = "") { it.text }

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
            override val attributes: List<Attribute<*>>,
        ) : AttributesHolder
    }

    /**
     * An attribute is an arbitrary key-value metadata pair.
     */
    data class Attribute<V>(
        val key: AttributeKey<V>,
        val value: V
    )

    /**
     * An attribute key  uniquely an attribute.
     *
     * The [V] phantom type is there to perform static type checking when requesting an attribute.
     */
    data class AttributeKey<V>(val id: String) {
        companion object {
            val ACCESSIBILITY_LABEL = AttributeKey<String>("accessibilityLabel")
            val LANGUAGE = AttributeKey<Language>("language")
        }
    }

    /**
     * An object associated with a list of attributes.
     */
    interface AttributesHolder {

        /**
         * Associated list of attributes.
         */
        val attributes: List<Attribute<*>>

        val language: Language?
            get() = attribute(AttributeKey.LANGUAGE)

        val accessibilityLabel: String?
            get() = attribute(AttributeKey.ACCESSIBILITY_LABEL)

        /**
         * Gets the first attribute with the given [key].
         */
        @Suppress("UNCHECKED_CAST")
        fun <V> attribute(key: AttributeKey<V>): V? =
            attributes.firstOrNull { it.key == key }?.value as V

        /**
         * Gets all the attributes with the given [key].
         */
        @Suppress("UNCHECKED_CAST")
        fun <V> attributes(key: AttributeKey<V>): List<V> =
            attributes
                .filter { it.key == key }
                .map { it.value as V }
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
     * Extracts the full raw text, or returns null if no text content can be found.
     *
     * @param separator Separator to use between individual elements. Defaults to newline.
     */
    suspend fun text(separator: String = "\n"): String? =
        elements()
            .mapNotNull { (it as? TextualElement)?.text }
            .joinToString(separator = separator)
            .takeIf { it.isNotBlank() }

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
