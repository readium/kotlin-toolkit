/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.extensions.optStringsFromArrayOrSingle
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.extensions.removeLastComponent
import org.readium.r2.shared.publication.epub.listOfAudioClips
import org.readium.r2.shared.publication.epub.listOfVideoClips
import org.readium.r2.shared.util.logging.JsonWarning
import org.readium.r2.shared.util.logging.log
import java.io.Serializable
import java.net.URL

/**
 * Shared model for a Readium Publication.
 *
 * @param type The kind of publication it is ( Epub, Cbz, ... )
 * @param version The version of the publication, if the type needs any.
 */
data class Publication(
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

    // FIXME: To be refactored, with the TYPE and EXTENSION enums as well
    var type: TYPE = TYPE.EPUB
    var version: Double = 0.0

    enum class TYPE {
        EPUB, CBZ, FXL, WEBPUB, AUDIO, DiViNa
    }

    enum class EXTENSION(var value: String) {
        EPUB(".epub"),
        CBZ(".cbz"),
        JSON(".json"),
        DIVINA(".divina"),
        AUDIO(".audiobook"),
        LCPL(".lcpl"),
        UNKNOWN("");

        companion object {
            fun fromString(type: String): EXTENSION? =
                EXTENSION.values().firstOrNull { it.value == type }
        }
    }

    // FIXME: To refactor after specifying the User and Rendition Settings API
    var userSettingsUIPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf()
    var cssStyle: String? = null

    // FIXME: This is not specified and need to be refactored
    var internalData: MutableMap<String, String> = mutableMapOf()

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
            add(Link(href = href, type = "application/webpub+json", rels = listOf("self")))
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
        link { it.hasHref(href) }

    /**
     * Finds the first resource [Link] (asset or [readingOrder] item) at the given relative path.
     */
    fun resourceWithHref(href: String): Link? {
        return readingOrder.find { it.hasHref(href) }
            ?: resources.find { it.hasHref(href) }
    }

    // FIXME: Why do we need to check if there's a / at the beginning? Hrefs should be normalized everywhere
    private fun Link.hasHref(href: String) =
        this.href == href || this.href == "/$href"

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
            warnings: WarningLogger<JsonWarning>? = null
        ): Publication? {
            json ?: return null

            val context = json.optStringsFromArrayOrSingle("@context", remove = true)

            val metadata = Metadata.fromJSON(json.remove("metadata") as? JSONObject, normalizeHref, warnings)
            if (metadata == null) {
                warnings?.log(Publication::class.java, "[metadata] is required", json)
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

    @Deprecated("Renamed to [listOfAudioClips]", ReplaceWith("listOfAudioClips"))
    val listOfAudioFiles: List<Link> = listOfAudioClips

    @Deprecated("Renamed to [listOfVideoClips]", ReplaceWith("listOfVideoClips"))
    val listOfVideos: List<Link> = listOfVideoClips

    @Deprecated("Renamed to [resourceWithHref]", ReplaceWith("resourceWithHref(href)"))
    fun resource(href: String): Link? = resourceWithHref(href)

    @Deprecated("Refactored as a property", ReplaceWith("baseUrl"))
    fun baseUrl(): URL? = baseUrl

    @Deprecated("Refactored as a property", ReplaceWith("manifest"))
    fun manifest(): String = manifest

    @Deprecated("Use [setSelfLink] instead", ReplaceWith("setSelfLink"))
    fun addSelfLink(endPoint: String, baseURL: URL) {
        setSelfLink(Uri.parse(baseURL.toString())
            .buildUpon()
            .appendEncodedPath("$endPoint/manifest.json")
            .build()
            .toString()
        )
    }

}
