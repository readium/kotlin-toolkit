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
import org.json.JSONObject
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.extensions.HashAlgorithm
import org.readium.r2.shared.extensions.hash
import org.readium.r2.shared.extensions.removeLastComponent
import org.readium.r2.shared.extensions.toUrlOrNull
import org.readium.r2.shared.fetcher.EmptyFetcher
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.epub.listOfAudioClips
import org.readium.r2.shared.publication.epub.listOfVideoClips
import org.readium.r2.shared.publication.services.CoverService
import org.readium.r2.shared.publication.services.DefaultCoverService
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.publication.services.positions
import java.net.URL
import java.net.URLEncoder

internal typealias ServiceFactory = (Publication.Service.Context) -> Publication.Service?


/**
 * Shared model for a Readium Publication.
 * @param type The kind of publication it is ( Epub, Cbz, ... )
 * @param version The version of the publication, if the type needs any.
 * @param positionsFactory Factory used to build lazily the [positions].
 */
data class Publication(
    private val manifest: Manifest,
    private val fetcher: Fetcher = EmptyFetcher(),
    private val servicesBuilder: ServicesBuilder = ServicesBuilder(),
    @Deprecated("Provide a [ServiceFactory] for a [PositionsService] instead.", level = DeprecationLevel.ERROR)
    @Suppress("DEPRECATION")
    val positionsFactory: PositionListFactory? = null,

    // FIXME: To refactor after specifying the User and Rendition Settings API
    var userSettingsUIPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(),
    var cssStyle: String? = null,

    // FIXME: This is not specified and need to be refactored
    var internalData: MutableMap<String, String> = mutableMapOf()
) {
    private val services: List<Service> = servicesBuilder.build(Service.Context(manifest, fetcher))

    val context: List<String> get() = manifest.context
    val metadata: Metadata get() = manifest.metadata
    val links: List<Link> get() = manifest.links
    val readingOrder: List<Link> get() = manifest.readingOrder
    val resources: List<Link> get() = manifest.resources
    val tableOfContents: List<Link> get() = manifest.tableOfContents
    val otherCollections: List<PublicationCollection> get() = manifest.otherCollections

    // FIXME: To be refactored, with the TYPE and EXTENSION enums as well
    var type: Publication.TYPE = Publication.TYPE.EPUB
    var version: Double = 0.0

    /**
     * Base interface to be implemented by all publication services.
     */
    interface Service {
        /**
         * Container for the context from which a service is created.
         */
        data class Context(
            val manifest: Manifest,
            val fetcher: Fetcher
        )

        /**
         * Links which will be added to [Publication.links].
         * It can be used to expose a web API for the service, through [Publication.get].
         *
         * To disambiguate the href with a publication's local resources, you should use the prefix
         * `/~readium/`. A custom media type or rel should be used to identify the service.
         *
         * You can use a templated URI to accept query parameters, e.g.:
         *
         * ```
         * Link(
         *     href = "/~readium/search{?text}",
         *     type = "application/vnd.readium.search+json",
         *     templated = true
         * )
         * ```
         */
        val links: List<Link> get () = emptyList()

        /**
         * A service can return a Resource to:
         *  - respond to a request to its web API declared in links,
         *  - serve additional resources on behalf of the publication,
         *  - replace a publication resource by its own version.
         *
         * Called by [Publication.get] for each request.
         *
         * @return The Resource containing the response, or null if the service doesn't recognize
         *         this request.
         */
        fun get(link: Link, parameters: Map<String, String> = emptyMap()): Resource? = null

        /**
         * Closes any opened file handles, removes temporary files, etc.
         */
        fun close() {}

    }

    data class ServicesBuilder(internal var serviceFactories: MutableMap<String, ServiceFactory>) {

        @Suppress("UNCHECKED_CAST")
        constructor(
            positions: ServiceFactory? = null,
            cover: ServiceFactory? = (DefaultCoverService)::create
        ) : this(mapOf(
                PositionsService::class.java.simpleName to positions,
                CoverService::class.java.simpleName to cover
            ).filterValues { it != null }.toMutableMap() as MutableMap<String, ServiceFactory>)

        /** Builds the actual list of publication services to use in a Publication. */
        fun build(context: Service.Context) : List<Service> = serviceFactories.values.mapNotNull { it(context) }

        /** Sets the publication service factory for the given service type. */
        operator fun <T> set(serviceType: Class<T>, factory: ServiceFactory) {
            requireNotNull(serviceType.simpleName)
            serviceFactories[serviceType.simpleName] = factory
        }

        /** Removes any service factory producing the given kind of service. */
        fun <T> remove(serviceType: Class<T>) {
            requireNotNull(serviceType.simpleName)
            serviceFactories.remove(serviceType.simpleName)
        }

        /* Replaces the service factory associated with the given service type with the result of `transform`. */
        fun <T> wrap(serviceType: Class<T>, transform: ((ServiceFactory)?) -> ServiceFactory) {
            requireNotNull(serviceType.simpleName)
            serviceFactories[serviceType.simpleName] = transform(serviceFactories[serviceType.simpleName])
        }

    }

    /**
     * Returns the resource targeted by the given [link].
     *
     * The [link].href property is searched for in the [links], [readingOrder] and [resources] properties
     * to find the matching manifest Link. This is to make sure that
     * the Link given to the Fetcher contains all properties declared in the manifest.
     *
     * The properties are searched recursively following [Link::alternate], then [Link::children].
     * But only after comparing all the links at the current level.
     *
     * @param parameters Parameters used when link is templated. They must not be percent-encoded.
     */
    fun get(link: Link, parameters: Map<String, String> = emptyMap()): Resource {
        @Suppress("NAME_SHADOWING")
        val link = linkWithHref(link.href) ?: link
        services.forEach { service -> service.get(link, parameters)?.let { return it } }
        return fetcher.get(link, parameters)
    }

    /**
     * Returns the resource targeted by the given [href].
     */
    fun get(href: String): Resource =
        get(Link(href = href))

    /**
     * Closes any opened resource associated with the [Publication], including [services].
     */
    fun close() {
        fetcher.close()
        services.forEach { it.close() }
    }

    /**
     * Returns the first publication service that is an instance of [klass].
     */
    fun <T: Service> findService(klass: Class<T>): T? = services.filterIsInstance(klass).firstOrNull()

    /**
     * Creates a [Publication]'s [positions].
     *
     * The parsers provide an implementation of this interface for each format, but a host app
     * might want to use a custom factory to implement, for example, a caching mechanism or use a
     * different calculation method.
     */
    @Deprecated("Use a [ServiceFactory] for a [PositionsService] instead.")
    interface PositionListFactory {
        fun create(): List<Locator>
    }

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

    /**
     * Returns the RWPM JSON representation for this [Publication]'s manifest, as a string.
     */
    val jsonManifest: String = manifest.toJSON().toString().replace("\\/", "/")

    /**
     * Returns the URL where this [Publication] is served.
     * This is computed from the self link.
     */
    val baseUrl: URL?
        get() = links.firstWithRel("self")
            ?.let { it.href.toUrlOrNull()?.removeLastComponent() }

    /**
     * Sets the URL where this [Publication]'s RWPM manifest is served.
     */
    fun setSelfLink(href: String) {
        manifest.links = manifest.links.toMutableList().apply {
            removeAll { it.rels.contains("self") }
            add(Link(href = href, type = MediaType.WEBPUB_MANIFEST.toString(), rels = setOf("self")))
        }
    }

    /**
     * Returns the [ContentLayout] for the default language.
     */
    val contentLayout: ContentLayout get() = metadata.contentLayout

    /**
     * Returns the [ContentLayout] for the given [language].
     */
    fun contentLayoutForLanguage(language: String?) = metadata.contentLayoutForLanguage(language)

    /**
     * Finds the first [Link] having the given [rel] in the publications's links.
     */
    fun linkWithRel(rel: String): Link? =
        link { it.rels.contains(rel) }

    /**
     * Finds the first [Link] having the given [rel] matching the given [predicate], in the
     * publications' links.
     */
    internal fun linkWithRelMatching(predicate: (String) -> Boolean): Link? {
        for (link in links) {
            for (rel in link.rels) {
                if (predicate(rel)) {
                    return link
                }
            }
        }
        return null
    }

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
        // FIXME: This should do a breadth-first traversal
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
     * Finds the first [Link] to the publication's cover (rel = cover).
     */
    val coverLink: Link? get() = linkWithRel("cover")

    /**
     * Returns the [links] of the first child [PublicationCollection] with the given role, or an
     * empty list.
     */
    internal fun linksWithRole(role: String): List<Link> =
        otherCollections.firstWithRole(role)?.links ?: emptyList()

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
     * Copy the [Publication] with a different [PositionListFactory].
     * The provided closure will be used to build the [PositionListFactory], with this being the
     * [Publication].
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use [Publication.copy(serviceFactories)] instead", ReplaceWith("Publication.copy(serviceFactories = listOf(positionsServiceFactory)"), level = DeprecationLevel.ERROR)
    fun copyWithPositionsFactory(createFactory: Publication.() -> PositionListFactory): Publication {
        return run { copy(positionsFactory = createFactory()) }
    }

    @Deprecated("Renamed to [listOfAudioClips]", ReplaceWith("listOfAudioClips"))
    val listOfAudioFiles: List<Link> = listOfAudioClips

    @Deprecated("Renamed to [listOfVideoClips]", ReplaceWith("listOfVideoClips"))
    val listOfVideos: List<Link> = listOfVideoClips

    @Deprecated("Renamed to [resourceWithHref]", ReplaceWith("resourceWithHref(href)"))
    fun resource(href: String): Link? = resourceWithHref(href)

    @Deprecated("Refactored as a property", ReplaceWith("baseUrl"))
    fun baseUrl(): URL? = baseUrl

    @Deprecated("Use [setSelfLink] instead", ReplaceWith("setSelfLink"))
    fun addSelfLink(endPoint: String, baseURL: URL) {
        setSelfLink(Uri.parse(baseURL.toString())
            .buildUpon()
            .appendEncodedPath("$endPoint/manifest.json")
            .build()
            .toString()
        )
    }

    companion object {

        /**
         * Creates the base URL for a [Publication] locally served through HTTP, from the
         * publication's [filename] and the HTTP server [port].
         *
         * Note: This is used for backward-compatibility, but ideally this should be handled by the
         * Server, and set in the self [Link]. Unfortunately, the self [Link] is not available
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

        @Deprecated("Parse a RWPM with [Manifest::fromJSON] and then instantiate a Publication",
            ReplaceWith("Manifest.fromJSON(manifestDict)?.let { Publication(it, fetcher = aFetcher) }",
                "org.readium.r2.shared.publication.Publication", "org.readium.r2.shared.publication.Manifest"))
        fun fromJSON(json: JSONObject?, normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity): Publication? =
            Manifest.fromJSON(json, normalizeHref, null)?.let { Publication(it) }

    }

}
