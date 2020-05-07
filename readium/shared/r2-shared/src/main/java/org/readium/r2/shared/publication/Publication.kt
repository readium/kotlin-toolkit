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
import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.WriteWith
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.extensions.HashAlgorithm
import org.readium.r2.shared.extensions.hash
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.publication.epub.listOfAudioClips
import org.readium.r2.shared.publication.epub.listOfVideoClips
import org.readium.r2.shared.toJSON
import org.readium.r2.shared.util.logging.JsonWarning
import org.readium.r2.shared.util.logging.log
import timber.log.Timber
import java.io.Serializable
import java.net.URL
import java.net.URLEncoder

/**
 * Shared model for a Readium Publication.
 *
 * @param type The kind of publication it is ( Epub, Cbz, ... )
 * @param version The version of the publication, if the type needs any.
 * @param positionsFactory Factory used to build lazily the [positions].
 */
@Parcelize
data class Publication(
    val context: List<String> = emptyList(),
    val metadata: Metadata,
    // FIXME: Currently Readium requires to set the [Link] with [rel] "self" when adding it to the
    //     server. So we need to keep [links] as a mutable property.
    var links: List<Link> = emptyList(),
    val readingOrder: List<Link> = emptyList(),
    val resources: List<Link> = emptyList(),
    val tableOfContents: List<Link> = emptyList(),
    val otherCollections: List<PublicationCollection> = emptyList(),
    val positionsFactory: @WriteWith<PositionListFactory.Parceler> PositionListFactory? = null,

    // FIXME: To be refactored, with the TYPE and EXTENSION enums as well
    var type: TYPE = TYPE.EPUB,
    var version: Double = 0.0,

    // FIXME: To refactor after specifying the User and Rendition Settings API
    var userSettingsUIPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(),
    var cssStyle: String? = null,

    // FIXME: This is not specified and need to be refactored
    var internalData: MutableMap<String, String> = mutableMapOf()

) : JSONable, Parcelable {

    /**
     * Creates a [Publication]'s [positions].
     *
     * The parsers provide an implementation of this interface for each format, but a host app
     * might want to use a custom factory to implement, for example, a caching mechanism or use a
     * different calculation method.
     */
    interface PositionListFactory {
        fun create(): List<Locator>

        /**
         * Implementation of a [Parceler] to be used with [@Parcelize] to serialize a
         * [PositionListFactory].
         *
         * Since we can't serialize a factory, we're loading eagerly the [positions] to be
         * serialized. Upon deserialization, the positions will be wrapped in a static factory.
         *
         * This won't be needed anymore once we use [Fragment] instead of [Activity] in the
         * navigator.
         */
        object Parceler : kotlinx.android.parcel.Parceler<PositionListFactory?> {

            private class StaticPositionListFactory(private val positions: List<Locator>): PositionListFactory {
                override fun create(): List<Locator> = positions
            }

            override fun create(parcel: Parcel): PositionListFactory? =
                try {
                    mutableListOf<Locator>()
                        .apply {
                            @Suppress("UNCHECKED_CAST")
                            parcel.readList(this as MutableList<Any?>, Locator::class.java.classLoader)
                        }
                        .let { StaticPositionListFactory(it) }

                } catch (e: Exception) {
                    Timber.e(e, "Failed to read a PositionListFactory from a Parcel")
                    null
                }

            override fun PositionListFactory?.write(parcel: Parcel, flags: Int) {
                try {
                    parcel.writeList(this?.create())
                } catch (e: Exception) {
                    Timber.e(e, "Failed to write a PositionListFactory into a Parcel")
                }
            }

        }
    }

    @Parcelize
    enum class TYPE : Parcelable {
        EPUB, CBZ, FXL, WEBPUB, AUDIO, DiViNa
    }

    @Parcelize
    enum class EXTENSION(var value: String) : Parcelable {
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

    /**
     * List of all the positions in the publication.
     */
    @IgnoredOnParcel
    val positions: List<Locator> by lazy {
        positionsFactory?.create() ?: emptyList()
    }

    /**
     * List of all the positions in each resource, indexed by their [href].
     */
    @IgnoredOnParcel
    val positionsByResource: Map<String, List<Locator>> by lazy {
        positions.groupBy { it.href }
    }

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
            add(Link(href = href, type = "application/webpub+json", rels = setOf("self")))
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
        return deepFind(resources, predicate)
            ?: deepFind(readingOrder, predicate)
            ?: deepFind(links, predicate)
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
        return deepFind(readingOrder) { it.hasHref(href) }
            ?: deepFind(resources) { it.hasHref(href) }
    }

    /**
     * Finds the first [Link] in [collection] that satisfies the given [predicate]
     */
    private fun deepFind(collection: List<Link>, predicate: (Link) -> Boolean) : Link? {
        for (l in collection) {
            if (predicate(l))
                return l
            else
                deepFind(l.alternates, predicate)?.let { return it }
        }
        return null
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
        put("metadata", metadata.toJSON())
        put("links", links.toJSON())
        put("readingOrder", readingOrder.toJSON())
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

    /**
     * Copy the [Publication] with a different [PositionListFactory].
     * The provided closure will be used to build the [PositionListFactory], with [this] being the
     * [Publication].
     */
    fun copyWithPositionsFactory(createFactory: Publication.() -> PositionListFactory): Publication {
        return run { copy(positionsFactory = createFactory()) }
    }

    companion object {

        fun fromJSON(json: JSONObject?, normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity): Publication? =
            fromJSON(json, normalizeHref, null)

        /**
         * Parses a [Publication] from its RWPM JSON representation.
         *
         * If the publication can't be parsed, a warning will be logged with [warnings].
         * https://readium.org/webpub-manifest/
         * https://readium.org/webpub-manifest/schema/publication.schema.json
         */
        internal fun fromJSON(
            json: JSONObject?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger<JsonWarning>?
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

        /**
         * Creates the base URL for a [Publication] locally served through HTTP, from the
         * publication's [filename] and the HTTP server [port].
         *
         * Note: This is used for backward-compatibility, but ideally this should be handled by the
         * [Server], and set in the self [Link]. Unfortunately, the self [Link] is not available
         * in the navigator at the moment without changing the code in reading apps.
         */
        fun localBaseUrlOf(filename: String, port: Int): String {
            val sanitizedFilename = filename
                .removePrefix("/")
                .hash(HashAlgorithm.MD5)
                .let { URLEncoder.encode(it, "UTF-8") }

            return "http://127.0.0.1:$port/$sanitizedFilename"
        }

        /**
         * Gets the absolute URL of a resource locally served through HTTP.
         */
        fun localUrlOf(filename: String, port: Int, href: String): String =
            localBaseUrlOf(filename, port) + href

    }

    @IgnoredOnParcel
    @Deprecated("Renamed to [listOfAudioClips]", ReplaceWith("listOfAudioClips"))
    val listOfAudioFiles: List<Link> = listOfAudioClips

    @IgnoredOnParcel
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
