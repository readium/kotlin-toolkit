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
 * Represents a single semantic content element part of a publication.
 *
 * @param locator Locator targeting this element in the Publication.
 * @param data Data associated with this element.
 * @param extras Additional metadata for extensions.
 */
@ExperimentalReadiumApi
data class Content(
    val locator: Locator,
    val data: Data,
    val extras: Map<String, Any> = emptyMap()
) {
    /** A marker interface for a [Content] associated data. */
    interface Data

    /** A piece of data which can be represented as human-readable text. */
    interface TextualData : Data {
        /** Human-readable text representation for this data. */
        val text: String?
    }

    /** A piece of data referencing an embedded external resource. */
    interface EmbeddedData : Data {
        val link: Link
    }

    /** An audio clip. */
    data class Audio(
        override val link: Link
    ) : EmbeddedData

    /**
     * A bitmap image.
     *
     * @param caption Short piece of text associated with the image.
     * @param description Accessibility label.
     */
    data class Image(
        override val link: Link,
        val caption: String?,
        val description: String?
    ) : EmbeddedData, TextualData {
        override val text: String?
            get() = caption?.takeIf { it.isNotBlank() }
                ?: description
    }

    /**
     * A text element.
     *
     * @param role Purpose of this element in the broader context of the document.
     * @param segments Ranged portions of text with associated attributes.
     */
    data class Text(
        val role: Role,
        val segments: List<Segment>
    ) : TextualData {

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
}