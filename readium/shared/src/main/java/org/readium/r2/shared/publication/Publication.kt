/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)
@file:Suppress("DEPRECATION")

package org.readium.r2.shared.publication

import android.net.Uri
import android.os.Parcelable
import java.net.URL
import kotlin.reflect.KClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.readium.r2.shared.*
import org.readium.r2.shared.BuildConfig.DEBUG
import org.readium.r2.shared.error.Error
import org.readium.r2.shared.error.MessageError
import org.readium.r2.shared.error.ThrowableError
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.extensions.removeLastComponent
import org.readium.r2.shared.fetcher.EmptyFetcher
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.epub.listOfAudioClips
import org.readium.r2.shared.publication.epub.listOfVideoClips
import org.readium.r2.shared.publication.services.CacheService
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.publication.services.CoverService
import org.readium.r2.shared.publication.services.DefaultLocatorService
import org.readium.r2.shared.publication.services.LocatorService
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.publication.services.WebPositionsService
import org.readium.r2.shared.publication.services.content.ContentService
import org.readium.r2.shared.publication.services.search.SearchService
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.mediatype.MediaType

internal typealias ServiceFactory = (Publication.Service.Context) -> Publication.Service?

/**
 * A reference uniquely identifying a publication in the reading app.
 *
 * For example, a database primary key for a local publication, or a source URL for a remote one.
 *
 * We can't use publication.metadata.identifier directly because it might be null or not really
 * unique in the reading app. That's why sometimes we require an ID provided by the app.
 */
typealias PublicationId = String

/**
 * The Publication shared model is the entry-point for all the metadata and services
 * related to a Readium publication.
 *
 * @param manifest The manifest holding the publication metadata extracted from the publication file.
 * @param fetcher The underlying fetcher used to read publication resources.
 * The default implementation returns Resource.Exception.NotFound for all HREFs.
 * @param servicesBuilder Holds the list of service factories used to create the instances of
 * Publication.Service attached to this Publication.
 */
class Publication(
    manifest: Manifest,
    private val fetcher: Fetcher = EmptyFetcher(),
    private val servicesBuilder: ServicesBuilder = ServicesBuilder(),
    @Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
    var userSettingsUIPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(),
    @Deprecated("Migrate to the new Settings API (see migration guide)", level = DeprecationLevel.ERROR)
    var cssStyle: String? = null,
) : PublicationServicesHolder {

    private val _manifest: Manifest

    private val services = ListPublicationServicesHolder()

    init {
        services.services = servicesBuilder.build(Service.Context(manifest, fetcher, services))
        _manifest = manifest.copy(links = manifest.links + services.services.map(Service::links).flatten())
    }

    // Shortcuts to manifest properties

    val context: List<String> get() = _manifest.context
    val metadata: Metadata get() = _manifest.metadata
    val links: List<Link> get() = _manifest.links

    /** Identifies a list of resources in reading order for the publication. */
    val readingOrder: List<Link> get() = _manifest.readingOrder

    /** Identifies resources that are necessary for rendering the publication. */
    val resources: List<Link> get() = _manifest.resources

    /** Identifies the collection that contains a table of contents. */
    val tableOfContents: List<Link> get() = _manifest.tableOfContents

    val subcollections: Map<String, List<PublicationCollection>> get() = _manifest.subcollections

    @Deprecated("Use conformsTo() to check the kind of a publication.", level = DeprecationLevel.ERROR)
    var type: TYPE = TYPE.EPUB

    @Deprecated("Version is not available any more.", level = DeprecationLevel.ERROR)
    var version: Double = 0.0

    /**
     * Returns the RWPM JSON representation for this [Publication]'s manifest, as a string.
     */
    val jsonManifest: String
        get() = _manifest.toJSON().toString().replace("\\/", "/")

    /**
     * The URL where this publication is served, computed from the [Link] with `self` relation.
     */

    val baseUrl: URL?
        get() = links.firstWithRel("self")
            ?.let { it.href.toUrlOrNull()?.removeLastComponent() }

    /**
     * Returns whether this publication conforms to the given Readium Web Publication Profile.
     */
    fun conformsTo(profile: Profile): Boolean =
        _manifest.conformsTo(profile)

    /**
     * Finds the first [Link] with the given HREF in the publication's links.
     *
     * Searches through (in order) [readingOrder], [resources] and [links] recursively following
     * [alternate] and [children] links.
     *
     * If there's no match, tries again after removing any query parameter and anchor from the
     * given [href].
     */
    fun linkWithHref(href: String): Link? = _manifest.linkWithHref(href)

    /**
     * Finds the first [Link] having the given [rel] in the publications's links.
     */
    fun linkWithRel(rel: String): Link? = _manifest.linkWithRel(rel)

    /**
     * Finds all [Link]s having the given [rel] in the publications's links.
     */
    fun linksWithRel(rel: String): List<Link> = _manifest.linksWithRel(rel)

    /**
     * Creates a new [Locator] object from a [Link] to a resource of this publication.
     *
     * Returns null if the resource is not found in this publication.
     */
    fun locatorFromLink(link: Link): Locator? = _manifest.locatorFromLink(link)

    /**
     * Returns the resource targeted by the given non-templated [link].
     */
    fun get(link: Link): Fetcher.Resource {
        if (DEBUG) { require(!link.templated) { "You must expand templated links before calling [Publication.get]" } }

        services.services.forEach { service -> service.get(link)?.let { return it } }
        return fetcher.get(link)
    }

    /**
     * Closes any opened resource associated with the [Publication], including services.
     */
    override suspend fun close() {
        fetcher.close()
        services.close()
    }

    // PublicationServicesHolder

    override fun <T : Service> findService(serviceType: KClass<T>): T? =
        services.findService(serviceType)

    override fun <T : Service> findServices(serviceType: KClass<T>): List<T> =
        services.findServices(serviceType)

    @Deprecated(
        "Use Publication.Profile ",
        replaceWith = ReplaceWith("Publication.Profile"),
        level = DeprecationLevel.WARNING
    )
    enum class TYPE {
        EPUB
    }

    @Deprecated(
        "Use Publication.Profile ",
        replaceWith = ReplaceWith("Publication.Profile"),
        level = DeprecationLevel.ERROR
    )
    enum class EXTENSION(var value: String) {
        EPUB(".epub"),
        CBZ(".cbz"),
        JSON(".json"),
        DIVINA(".divina"),
        AUDIO(".audiobook"),
        LCPL(".lcpl"),
        UNKNOWN("");
    }

    /**
     * Sets the URL where this [Publication]'s RWPM manifest is served.
     */
    fun setSelfLink(href: String) {
        _manifest.links = _manifest.links.toMutableList().apply {
            removeAll { it.rels.contains("self") }
            add(Link(href = href, type = MediaType.READIUM_WEBPUB_MANIFEST.toString(), rels = setOf("self")))
        }
    }

    /**
     * Returns the [links] of the first child [PublicationCollection] with the given role, or an
     * empty list.
     */
    internal fun linksWithRole(role: String): List<Link> =
        subcollections[role]?.firstOrNull()?.links ?: emptyList()

    companion object {

        /**
         * Creates the base URL for a [Publication] locally served through HTTP, from the
         * publication's [filename] and the HTTP server [port].
         *
         * Note: This is used for backward-compatibility, but ideally this should be handled by the
         * Server, and set in the self [Link]. Unfortunately, the self [Link] is not available
         * in the navigator at the moment without changing the code in reading apps.
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated("The HTTP server is not needed anymore (see migration guide)", level = DeprecationLevel.ERROR)
        fun localBaseUrlOf(filename: String, port: Int): String {
            throw NotImplementedError()
        }

        /**
         * Gets the absolute URL of a resource locally served through HTTP.
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated("The HTTP server is not needed anymore (see migration guide)", level = DeprecationLevel.ERROR)
        fun localUrlOf(filename: String, port: Int, href: String): String {
            throw NotImplementedError()
        }

        @Suppress("UNUSED_PARAMETER")
        @Deprecated(
            "Parse a RWPM with [Manifest::fromJSON] and then instantiate a Publication",
            ReplaceWith(
                "Manifest.fromJSON(json)",
                "org.readium.r2.shared.publication.Publication", "org.readium.r2.shared.publication.Manifest"
            ),
            level = DeprecationLevel.ERROR
        )
        fun fromJSON(
            json: JSONObject?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity
        ): Publication? {
            throw NotImplementedError()
        }
    }

    /**
     * Represents a Readium Web Publication Profile a [Publication] can conform to.
     *
     * For a list of supported profiles, see the registry:
     * https://readium.org/webpub-manifest/profiles/
     */
    @Parcelize
    data class Profile(val uri: String) : Parcelable {
        companion object {
            /** Profile for EPUB publications. */
            val EPUB = Profile("https://readium.org/webpub-manifest/profiles/epub")
            /** Profile for audiobooks. */
            val AUDIOBOOK = Profile("https://readium.org/webpub-manifest/profiles/audiobook")
            /** Profile for visual narratives (comics, manga and bandes dessinées). */
            val DIVINA = Profile("https://readium.org/webpub-manifest/profiles/divina")
            /** Profile for PDF documents. */
            val PDF = Profile("https://readium.org/webpub-manifest/profiles/pdf")
        }
    }

    /**
     * Base interface to be implemented by all publication services.
     */
    interface Service : Closeable {

        /**
         * Container for the context from which a service is created.
         */
        class Context(
            val manifest: Manifest,
            val fetcher: Fetcher,
            val services: PublicationServicesHolder
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
        val links: List<Link> get() = emptyList()

        /**
         * A service can return a Resource to:
         *  - respond to a request to its web API declared in links,
         *  - serve additional resources on behalf of the publication,
         *  - replace a publication resource by its own version.
         *
         * Called by [Publication.get] for each request.
         *
         * Warning: If you need to request one of the publication resources to answer the request,
         * use the [Fetcher] provided by the [Publication.Service.Context] instead of
         * [Publication.get], otherwise it will trigger an infinite loop.
         *
         * @return The [Resource] containing the response, or null if the service doesn't recognize
         *         this request.
         */
        fun get(link: Link): Fetcher.Resource? = null

        /**
         * Closes any opened file handles, removes temporary files, etc.
         */
        override fun close() {}
    }

    /**
     * Builds a list of [Publication.Service] from a collection of service factories.
     *
     * Provides helpers to manipulate the list of services of a [Publication].
     */
    class ServicesBuilder private constructor(
        private val serviceFactories: MutableMap<String, ServiceFactory>
    ) {

        @OptIn(Search::class, ExperimentalReadiumApi::class)
        @Suppress("UNCHECKED_CAST")
        constructor(
            cache: ServiceFactory? = null,
            content: ServiceFactory? = null,
            contentProtection: ServiceFactory? = null,
            cover: ServiceFactory? = null,
            locator: ServiceFactory? = null,
            positions: ServiceFactory? = null,
            search: ServiceFactory? = null,
        ) : this(
            mapOf(
                CacheService::class.java.simpleName to cache,
                ContentService::class.java.simpleName to content,
                ContentProtectionService::class.java.simpleName to contentProtection,
                CoverService::class.java.simpleName to cover,
                LocatorService::class.java.simpleName to locator,
                PositionsService::class.java.simpleName to positions,
                SearchService::class.java.simpleName to search,
            ).filterValues { it != null }.toMutableMap() as MutableMap<String, ServiceFactory>
        )

        /** Builds the actual list of publication services to use in a Publication. */
        fun build(context: Service.Context): List<Service> {
            val serviceFactories =
                buildMap<String, ServiceFactory> {
                    putAll(this@ServicesBuilder.serviceFactories)

                    if (!containsKey(LocatorService::class.java.simpleName)) {
                        val factory: ServiceFactory = {
                            DefaultLocatorService(it.manifest.readingOrder, it.services)
                        }
                        put(LocatorService::class.java.simpleName, factory)
                    }

                    if (!containsKey(PositionsService::class.java.simpleName)) {
                        val factory = WebPositionsService.createFactory()
                        put(PositionsService::class.java.simpleName, factory)
                    }
                }

            return serviceFactories.values
                .mapNotNull { it(context) }
        }

        /** Gets the publication service factory for the given service type. */
        operator fun <T : Service> get(serviceType: KClass<T>): ServiceFactory? {
            val key = requireNotNull(serviceType.simpleName)
            return serviceFactories[key]
        }

        /** Sets the publication service factory for the given service type. */
        operator fun <T : Service> set(serviceType: KClass<T>, factory: ServiceFactory?) {
            val key = requireNotNull(serviceType.simpleName)
            if (factory != null) {
                serviceFactories[key] = factory
            } else {
                serviceFactories.remove(key)
            }
        }

        /** Removes the service factory producing the given kind of service, if any. */
        fun <T : Service> remove(serviceType: KClass<T>) {
            val key = requireNotNull(serviceType.simpleName)
            serviceFactories.remove(key)
        }

        /**
         * Replaces the service factory associated with the given service type with the result of
         * [transform].
         */
        fun <T : Service> decorate(
            serviceType: KClass<T>,
            transform: ((ServiceFactory)?) -> ServiceFactory
        ) {
            val key = requireNotNull(serviceType.simpleName)
            serviceFactories[key] = transform(serviceFactories[key])
        }
    }

    /**
     * Errors occurring while opening a Publication.
     */
    sealed class OpeningException : Error {

        /**
         * The file format could not be recognized by any parser.
         */
        class UnsupportedAsset(override val cause: Error? = null) : OpeningException() {

            constructor(message: String) : this(MessageError(message))

            override val message: String =
                "Asset is not supported."
        }

        /**
         * The publication file was not found on the file system.
         */
        class NotFound(override val cause: Error? = null) : OpeningException() {

            override val message: String =
                "Asset couldn't be found."
        }

        /**
         * The publication parser failed with the given underlying exception.
         */
        class ParsingFailed(override val cause: Error? = null) : OpeningException() {

            override val message: String =
                "The asset is corrupted so the publication cannot be opened."
        }

        /**
         * We're not allowed to open the publication at all, for example because it expired.
         */
        class Forbidden(override val cause: Error? = null) : OpeningException() {

            override val message: String =
                "You are not allowed to open this publication."
        }

        /**
         * The publication can't be opened at the moment, for example because of a networking error.
         * This error is generally temporary, so the operation may be retried or postponed.
         */
        class Unavailable(override val cause: Error? = null) : OpeningException() {

            override val message: String =
                "Not available, please try again later."
        }

        /**
         * The provided credentials are incorrect and we can't open the publication in a
         * `restricted` state (e.g. for a password-protected ZIP).
         */
        class IncorrectCredentials(override val cause: Error? = null) : OpeningException() {

            override val message: String =
                "Provided credentials were incorrect."
        }

        class OutOfMemory(override val cause: Error? = null) : OpeningException() {

            override val message: String =
                "There is not enough memory available to open device to read the publication."
        }

        class Unexpected(override val cause: Error? = null) : OpeningException() {

            constructor(exception: Exception) : this(ThrowableError(exception))

            override val message: String =
                "An expected error occurred."
        }
    }

    /**
     * Builds a Publication from its components.
     *
     * A Publication's construction is distributed over the Streamer and its parsers,
     * so a builder is useful to pass the parts around.
     */
    class Builder(
        var manifest: Manifest,
        var fetcher: Fetcher,
        var servicesBuilder: ServicesBuilder = ServicesBuilder()
    ) {

        fun build(): Publication = Publication(
            manifest = manifest,
            fetcher = fetcher,
            servicesBuilder = servicesBuilder
        )
    }

    /**
     * Finds the first [Link] to the publication's cover (rel = cover).
     */
    @Deprecated("Use [Publication.cover] to get the cover as a [Bitmap]", ReplaceWith("cover"), level = DeprecationLevel.ERROR)
    val coverLink: Link? get() = linkWithRel("cover")

    /**
     * Copy the [Publication] with a different [PositionListFactory].
     * The provided closure will be used to build the [PositionListFactory], with this being the
     * [Publication].
     */
    @Suppress("DEPRECATION", "UNUSED_PARAMETER")
    @Deprecated("Use [Publication.copy(serviceFactories)] instead", ReplaceWith("Publication.copy(serviceFactories = listOf(positionsServiceFactory)"), level = DeprecationLevel.ERROR)
    fun copyWithPositionsFactory(): Publication {
        throw NotImplementedError()
    }

    @Deprecated("Renamed to [listOfAudioClips]", ReplaceWith("listOfAudioClips"), level = DeprecationLevel.ERROR)
    val listOfAudioFiles: List<Link> = listOfAudioClips

    @Deprecated("Renamed to [listOfVideoClips]", ReplaceWith("listOfVideoClips"), level = DeprecationLevel.ERROR)
    val listOfVideos: List<Link> = listOfVideoClips

    @Deprecated("Renamed to [linkWithHref]", ReplaceWith("linkWithHref(href)"), level = DeprecationLevel.ERROR)
    fun resource(href: String): Link? = linkWithHref(href)

    @Deprecated("Refactored as a property", ReplaceWith("baseUrl"), level = DeprecationLevel.ERROR)
    fun baseUrl(): URL? = baseUrl

    @Deprecated("Renamed [subcollections]", ReplaceWith("subcollections"), level = DeprecationLevel.ERROR)
    val otherCollections: Map<String, List<PublicationCollection>> get() = subcollections

    @Deprecated("Use [setSelfLink] instead", ReplaceWith("setSelfLink"), level = DeprecationLevel.ERROR)
    fun addSelfLink(endPoint: String, baseURL: URL) {
        setSelfLink(
            Uri.parse(baseURL.toString())
                .buildUpon()
                .appendEncodedPath("$endPoint/manifest.json")
                .build()
                .toString()
        )
    }

    /**
     * Finds the first resource [Link] (asset or [readingOrder] item) at the given relative path.
     */
    @Deprecated("Use [linkWithHref] instead.", ReplaceWith("linkWithHref(href)"), level = DeprecationLevel.ERROR)
    fun resourceWithHref(href: String): Link? = linkWithHref(href)

    /**
     * Creates a [Publication]'s [positions].
     *
     * The parsers provide an implementation of this interface for each format, but a host app
     * might want to use a custom factory to implement, for example, a caching mechanism or use a
     * different calculation method.
     */
    @Deprecated("Use a [ServiceFactory] for a [PositionsService] instead.", level = DeprecationLevel.ERROR)
    interface PositionListFactory {
        fun create(): List<Locator>
    }

    /**
     * Finds the first [Link] matching the given [predicate] in the publications's [Link]
     * properties: [resources], [readingOrder] and [links].
     *
     * Searches through (in order) [readingOrder], [resources] and [links]
     * recursively following [alternate] and [children] links.
     * The search order is unspecified.
     */
    @Deprecated("Use [linkWithHref()] to find a link with the given HREF", replaceWith = ReplaceWith("linkWithHref"), level = DeprecationLevel.ERROR)
    @Suppress("UNUSED_PARAMETER")
    fun link(predicate: (Link) -> Boolean): Link? = null

    @Deprecated("Use [jsonManifest] instead", ReplaceWith("jsonManifest"))
    fun toJSON() = JSONObject(jsonManifest)

    @Deprecated("Use `metadata.effectiveReadingProgression` instead", ReplaceWith("metadata.effectiveReadingProgression"), level = DeprecationLevel.ERROR)
    val contentLayout: ReadingProgression get() = metadata.effectiveReadingProgression

    @Deprecated("Use `metadata.effectiveReadingProgression` instead", ReplaceWith("metadata.effectiveReadingProgression"), level = DeprecationLevel.ERROR)
    @Suppress("UNUSED_PARAMETER")
    fun contentLayoutForLanguage(language: String?) = metadata.effectiveReadingProgression
}

/**
 * Holds [Publication.Service] instances for a [Publication].
 */
interface PublicationServicesHolder : SuspendingCloseable {
    /**
     * Returns the first publication service that is an instance of [serviceType].
     */
    fun <T : Publication.Service> findService(serviceType: KClass<T>): T?

    /**
     * Returns all the publication services that are instances of [serviceType].
     */
    fun <T : Publication.Service> findServices(serviceType: KClass<T>): List<T>
}

internal class ListPublicationServicesHolder(
    var services: List<Publication.Service> = emptyList()
) : PublicationServicesHolder {
    override fun <T : Publication.Service> findService(serviceType: KClass<T>): T? =
        findServices(serviceType).firstOrNull()

    override fun <T : Publication.Service> findServices(serviceType: KClass<T>): List<T> =
        services.filterIsInstance(serviceType.java)

    override suspend fun close() {
        for (service in services) {
            tryOrLog { service.close() }
        }
    }
}
