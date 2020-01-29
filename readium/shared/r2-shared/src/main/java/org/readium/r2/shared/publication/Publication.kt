/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.Warning
import org.readium.r2.shared.WarningLogger
import org.readium.r2.shared.extensions.optStringsFromArrayOrSingle
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.extensions.removeLastComponent
import org.readium.r2.shared.publication.link.Link
import org.readium.r2.shared.publication.link.LinkHrefNormalizer
import org.readium.r2.shared.publication.link.LinkHrefNormalizerIdentity
import org.readium.r2.shared.publication.metadata.Metadata
import java.io.Serializable
import java.net.URL
import java.util.*

/**
 * Shared model for a Readium Publication.
 *
 * @param format Format of the publication, if specified.
 * @param formatVersion Version of the publication's format, eg. 3 for EPUB 3.
 */
data class Publication(
    val format: Format = Format.UNKNOWN,
    val formatVersion: String? = null,
    val context: List<String> = emptyList(),
    val metadata: Metadata,
    // FIXME: Currently Readium requires to set the [Link] with [rel] "self" when adding it to the
    //     server. So we need to keep [links] as a mutable property.
    var links: List<Link> = emptyList(),
    val readingOrder: List<Link> = emptyList(),
    val resources: List<Link> = emptyList(),
    val tableOfContents: List<Link> = emptyList(),
    val otherCollections: List<PublicationCollection> = emptyList()
) : JSONable, Serializable {

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

    // FIXME: To refactor after specifying the User and Rendition Settings API
    var userSettingsUIPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf()
    var cssStyle: String? = null

    /**
     * Returns the RWPM JSON representation for this manifest, as a string.
     */
    val manifest: String
        get() = toJSON().toString()
            .replace("\\/", "/")

    /**
     * Returns the URL where this [Publication] is served.
     * This is computed from the [self] link.
     */
    val baseUrl: URL? get() =
        linkWithRel("self")
            ?.let { URL(it.href).removeLastComponent() }

    /**
     * Sets the URL where this [Publication]'s RWPM manifest is served.
     */
    fun setSelfLink(href: String) {
        links = links.toMutableList().apply {
            removeAll { it.rels.contains("self") }
            add(Link(href = href, rels = listOf("self")))
        }
    }

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
     * Serializes a [Publication] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        putIfNotEmpty("@context", context)
        putIfNotEmpty("metadata", metadata)
        putIfNotEmpty("links", links)
        putIfNotEmpty("readingOrder", readingOrder)
        putIfNotEmpty("resources", resources)
        putIfNotEmpty("toc", tableOfContents)
        otherCollections.appendToJSONObject(this)
    }

    /**
     * Returns the [links] of the first child [PublicationCollection] with the given role, or an
     * empty list.
     */
    internal fun linksWithRole(role: String): List<Link> =
        otherCollections.firstWithRole(role)?.links ?: emptyList()

    companion object {

        /**
         * Parses a [Publication] from its RWPM JSON representation.
         *
         * If the publication can't be parsed, a warning will be logged with [warnings].
         * https://readium.org/webpub-manifest/
         * https://readium.org/webpub-manifest/schema/publication.schema.json
         */
        fun fromJSON(
            json: JSONObject?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger? = null
        ): Publication? {
            json ?: return null

            val context = json.optStringsFromArrayOrSingle("@context", remove = true)

            val metadata = Metadata.fromJSON(json.remove("metadata") as? JSONObject, normalizeHref, warnings)
            if (metadata == null) {
                warnings?.log(Warning.JsonParsing(Publication::class.java, "[metadata] is required", json))
                return null
            }

            val links = Link.fromJSONArray(json.remove("links") as? JSONArray, normalizeHref, warnings)
                .filter { it.rels.isNotEmpty() }

            // [readingOrder] used to be [spine], so we parse [spine] as a fallback.
            val readingOrderJSON = (json.remove("readingOrder") ?: json.remove("spine")) as? JSONArray
            val readingOrder = Link.fromJSONArray(readingOrderJSON, normalizeHref, warnings)
                .filter { it.type != null }

            val resources = Link.fromJSONArray(json.remove("resources") as? JSONArray, normalizeHref, warnings)
                .filter { it.type != null }

            val tableOfContents = Link.fromJSONArray(json.remove("toc") as? JSONArray, normalizeHref, warnings)

            // Parses sub-collections from the remaining JSON properties.
            val otherCollections = PublicationCollection.collectionsFromJSON(json, normalizeHref, warnings)

            return Publication(
                context = context,
                metadata = metadata,
                links = links,
                readingOrder = readingOrder,
                resources = resources,
                tableOfContents = tableOfContents,
                otherCollections = otherCollections
            )
        }

    }
}
