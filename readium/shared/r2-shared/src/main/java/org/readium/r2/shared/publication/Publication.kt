/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.readium.r2.shared.publication.webpub.WebPublication
import org.readium.r2.shared.publication.webpub.WebPublicationInterface
import org.readium.r2.shared.publication.webpub.link.Link
import java.util.*

/**
 * Shared model for a Readium Publication.
 *
 * It extends the Web Publication model, which holds the metadata and resources.
 * On top of that, the Publication holds:
 *   - the publication state used by the Streamer and Navigator
 *   - shortcuts to various publication resources to be used by the Streamer and Navigator
 *   - additional metadata not part of the RWPM
 *
 * @param webpub Readium Web Publication Manifest model, for metadata retrieval.
 * @param format Format of the publication, if specified.
 * @param formatVersion Version of the publication's format, eg. 3 for EPUB 3.
 */
class Publication(
    private val webpub: WebPublication,
    val format: Format = Format.UNKNOWN,
    val formatVersion: String? = null
) : WebPublicationInterface by webpub {

    /**
     * Returns the [ContentLayout] for the default language.
     */
    val contentLayout: ContentLayout get() = contentLayoutForLanguage(null)

    /**
     * Returns the [ContentLayout] for the given [language].
     */
    fun contentLayoutForLanguage(language: String?): ContentLayout {
        @Suppress("NAME_SHADOWING")
        val language = language?.ifEmpty { null }

        return ContentLayout.from(
            language = language ?: metadata.languages.firstOrNull() ?: "",
            readingProgression = metadata.readingProgression
        )
    }

    /**
     * Finds the first [Link] matching the given [predicate] in the publications's [Link]
     * properties: [resources], [readingOrder] and [links].
     */
    fun link(predicate: (Link) -> Boolean): Link? {
        return resources.find(predicate)
            ?: readingOrder.find(predicate)
            ?: links.find(predicate)
    }

    /**
     * Finds the first [Link] having the given [rel] in the publications's links.
     */
    fun linkWithRel(rel: String): Link? =
        link { it.rels.contains(rel) }

    /**
     * Finds the first [Link] having the given [href] in the publications's links.
     */
    fun linkWithHref(href: String): Link? =
        link { it.href == href }

    /**
     * Finds the first resource [Link] (asset or [readingOrder] item) at the given relative path.
     */
    fun resourceWithHref(href: String): Link? {
        return readingOrder.find { it.href == href }
            ?: resources.find { it.href == href }
    }

    /**
     * Finds the first [Link] to the publication's cover ([rel] = cover).
     */
    val coverLink: Link? get() = linkWithRel("cover")

    /**
     * Sets the URL where this [Publication]'s RWPM manifest is served.
     */
    fun setSelfLink(href: String) {
        links = links.toMutableList().apply {
            removeAll { it.rels.contains("self") }
            add(Link(href = href, rels = listOf("self")))
        }
    }

    enum class Format {
        // Formats natively supported by Readium.
        CBZ, EPUB, PDF, WEBPUB, AUDIOBOOK,
        // Default value when the format is not specified.
        UNKNOWN;

        companion object {

            /**
             * Finds the format for the given [mimetype] or fallback on a [fileExtension].
             */
            fun from(mimetype: String?, fileExtension: String? = null): Format =
                from(listOfNotNull(mimetype), fileExtension)

            /**
             * Finds the format from a list of possible [mimetypes] or fallback on a [fileExtension].
             */
            fun from(mimetypes: List<String>, fileExtension: String? = null): Format {
                for (mimetype in mimetypes) {
                    when (mimetype) {
                        "application/epub+zip", "application/oebps-package+xml" ->
                            return EPUB

                        "application/x-cbr" ->
                            return CBZ

                        "application/pdf", "application/pdf+lcp" ->
                            return PDF

                        "application/webpub+json" ->
                            return WEBPUB

                        "application/audiobook+zip", "application/audiobook+json" ->
                            return AUDIOBOOK
                    }
                }

                return when (fileExtension?.toLowerCase(Locale.ROOT)) {
                    "epub" -> EPUB
                    "cbz" -> CBZ
                    "pdf", "lcpdf" -> PDF
                    "json" -> WEBPUB
                    "audiobook" -> AUDIOBOOK
                    else -> UNKNOWN
                }
            }

        }
    }

}
