/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)
@file:Suppress("DEPRECATION")

package org.readium.r2.shared.publication

import android.os.Parcelable
import java.net.URL
import kotlin.reflect.KClass
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.readium.r2.shared.*
import org.readium.r2.shared.extensions.*
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
import org.readium.r2.shared.util.BaseError
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Container
import org.readium.r2.shared.util.resource.EmptyContainer
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceTry
import org.readium.r2.shared.util.resource.fallback

internal typealias ServiceFactory = (Publication.Service.Context) -> Publication.Service?

/**
 * A reference uniquely identifying a publication in the reading app.
 *
 * For example, a database primary key for a local publication, or a source URL for a remote one.
 *
 * We can't use publication.metadata.identifier directly because it might be null or not really
 * unique in the reading app. That's why sometimes we require an ID provided by the app.
 */
public typealias PublicationId = String

/**
 * The Publication shared model is the entry-point for all the metadata and services
 * related to a Readium publication.
 *
 * @param manifest The manifest holding the publication metadata extracted from the publication file.
 * @param container The underlying container used to read publication resources.
 * The default implementation returns Resource.Exception.NotFound for all HREFs.
 * @param servicesBuilder Holds the list of service factories used to create the instances of
 * Publication.Service attached to this Publication.
 */
public class Publication(
    manifest: Manifest,
    private val container: Container = EmptyContainer(),
    private val servicesBuilder: ServicesBuilder = ServicesBuilder(),
    @Deprecated(
        "Migrate to the new Settings API (see migration guide)",
        level = DeprecationLevel.ERROR
    )
    public var userSettingsUIPreset: MutableMap<ReadiumCSSName, Boolean> = mutableMapOf(),
    @Deprecated(
        "Migrate to the new Settings API (see migration guide)",
        level = DeprecationLevel.ERROR
    )
    public var cssStyle: String? = null
) : PublicationServicesHolder {

    public val manifest: Manifest

    private val services = ListPublicationServicesHolder()

    init {
        services.services = servicesBuilder.build(Service.Context(manifest, container, services))
        this.manifest = manifest.copy(
            links = manifest.links + services.services.map(Service::links).flatten()
        )
    }

    // Shortcuts to manifest properties

    public val context: List<String> get() = manifest.context
    public val metadata: Metadata get() = manifest.metadata
    public val links: List<Link> get() = manifest.links

    /** Identifies a list of resources in reading order for the publication. */
    public val readingOrder: List<Link> get() = manifest.readingOrder

    /** Identifies resources that are necessary for rendering the publication. */
    public val resources: List<Link> get() = manifest.resources

    /** Identifies the collection that contains a table of contents. */
    public val tableOfContents: List<Link> get() = manifest.tableOfContents

    public val subcollections: Map<String, List<PublicationCollection>> get() = manifest.subcollections

    @Deprecated(
        "Use conformsTo() to check the kind of a publication.",
        level = DeprecationLevel.ERROR
    )
    public var type: TYPE = TYPE.EPUB

    @Deprecated("Version is not available any more.", level = DeprecationLevel.ERROR)
    public var version: Double = 0.0

    /**
     * Returns the RWPM JSON representation for this [Publication]'s manifest, as a string.
     */
    @Deprecated(
        "Jsonify the manifest by yourself.",
        replaceWith = ReplaceWith("""manifest.toJSON().toString().replace("\\/", "/")"""),
        DeprecationLevel.ERROR
    )
    public val jsonManifest: String
        get() = manifest.toJSON().toString().replace("\\/", "/")

    /**
     * The URL from which the publication resources are relative to, computed from the [Link] with
     * `self` relation.
     */
    public val baseUrl: Url?
        get() = links.firstWithRel("self")?.href
            ?.takeUnless { it.isTemplated }
            ?.resolve()

    /**
     * Returns the URL to the resource represented by the given [locator], relative to the
     * publication's link with `self` relation.
     */
    public fun url(locator: Locator): Url =
        baseUrl?.let { locator.href.resolve(it) } ?: locator.href

    /**
     * Returns the URL to the resource represented by the given [link], relative to the
     * publication's link with `self` relation.
     *
     * If the link HREF is a template, the [parameters] are used to expand it according to RFC 6570.
     */
    public fun url(link: Link, parameters: Map<String, String> = emptyMap()): Url =
        url(link.href, parameters)

    /**
     * Returns the URL to the resource represented by the given [href], relative to the
     * publication's link with `self` relation.
     *
     * If the HREF is a template, the [parameters] are used to expand it according to RFC 6570.
     */
    public fun url(href: Href, parameters: Map<String, String> = emptyMap()): Url =
        href.resolve(baseUrl, parameters = parameters)

    /**
     * Returns whether this publication conforms to the given Readium Web Publication Profile.
     */
    public fun conformsTo(profile: Profile): Boolean =
        manifest.conformsTo(profile)

    /**
     * Finds the first [Link] with the given HREF in the publication's links.
     *
     * Searches through (in order) [readingOrder], [resources] and [links] recursively following
     * `alternate` and `children` links.
     *
     * If there's no match, tries again after removing any query parameter and anchor from the
     * given [href].
     */
    public fun linkWithHref(href: Url): Link? = manifest.linkWithHref(href)

    /**
     * Finds the first [Link] having the given [rel] in the publications's links.
     */
    public fun linkWithRel(rel: String): Link? = manifest.linkWithRel(rel)

    /**
     * Finds all [Link]s having the given [rel] in the publications's links.
     */
    public fun linksWithRel(rel: String): List<Link> = manifest.linksWithRel(rel)

    /**
     * Creates a new [Locator] object from a [Link] to a resource of this publication.
     *
     * Returns null if the resource is not found in this publication.
     */
    public fun locatorFromLink(link: Link): Locator? = manifest.locatorFromLink(link)

    /**
     * Returns the resource targeted by the given non-templated [link].
     */
    public fun get(link: Link): Resource =
        get(link.url(), link.mediaType)

    /**
     * Returns the resource targeted by the given [href].
     */
    public fun get(href: Url): Resource =
        get(href, linkWithHref(href)?.mediaType)

    private fun get(href: Url, mediaType: MediaType?): Resource {
        services.services.forEach { service -> service.get(href)?.let { return it } }

        return container.get(href)
            .fallback { error ->
                if (error is Resource.Exception.NotFound) {
                    // Try again after removing query and fragment.
                    container.get(href.removeQuery().removeFragment())
                } else {
                    null
                }
            }
            .withMediaType(mediaType)
    }

    /**
     * Closes any opened resource associated with the [Publication], including services.
     */
    override suspend fun close() {
        container.close()
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
    public enum class TYPE {
        EPUB
    }

    @Deprecated(
        "Use Publication.Profile ",
        replaceWith = ReplaceWith("Publication.Profile"),
        level = DeprecationLevel.ERROR
    )
    public enum class EXTENSION(public var value: String) {
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
    @Deprecated(message = "Not used anymore.", level = DeprecationLevel.ERROR)
    @Suppress("UNUSED_PARAMETER")
    public fun setSelfLink(href: String) {
        throw NotImplementedError()
    }

    /**
     * Returns the [links] of the first child [PublicationCollection] with the given role, or an
     * empty list.
     */
    internal fun linksWithRole(role: String): List<Link> =
        subcollections[role]?.firstOrNull()?.links ?: emptyList()

    public companion object {

        /**
         * Creates the base URL for a [Publication] locally served through HTTP, from the
         * publication's [filename] and the HTTP server [port].
         *
         * Note: This is used for backward-compatibility, but ideally this should be handled by the
         * Server, and set in the self [Link]. Unfortunately, the self [Link] is not available
         * in the navigator at the moment without changing the code in reading apps.
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated(
            "The HTTP server is not needed anymore (see migration guide)",
            level = DeprecationLevel.ERROR
        )
        public fun localBaseUrlOf(filename: String, port: Int): String {
            throw NotImplementedError()
        }

        /**
         * Gets the absolute URL of a resource locally served through HTTP.
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated(
            "The HTTP server is not needed anymore (see migration guide)",
            level = DeprecationLevel.ERROR
        )
        public fun localUrlOf(filename: String, port: Int, href: String): String {
            throw NotImplementedError()
        }

        @Suppress("UNUSED_PARAMETER")
        @Deprecated(
            "Parse a RWPM with [Manifest::fromJSON] and then instantiate a Publication",
            ReplaceWith(
                "Manifest.fromJSON(json)",
                "org.readium.r2.shared.publication.Publication",
                "org.readium.r2.shared.publication.Manifest"
            ),
            level = DeprecationLevel.ERROR
        )
        public fun fromJSON(json: JSONObject?): Publication? {
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
    public data class Profile(val uri: String) : Parcelable {
        public companion object {
            /** Profile for EPUB publications. */
            public val EPUB: Profile = Profile("https://readium.org/webpub-manifest/profiles/epub")

            /** Profile for audiobooks. */
            public val AUDIOBOOK: Profile = Profile(
                "https://readium.org/webpub-manifest/profiles/audiobook"
            )

            /** Profile for visual narratives (comics, manga and bandes dessinées). */
            public val DIVINA: Profile = Profile(
                "https://readium.org/webpub-manifest/profiles/divina"
            )

            /** Profile for PDF documents. */
            public val PDF: Profile = Profile("https://readium.org/webpub-manifest/profiles/pdf")
        }
    }

    /**
     * Base interface to be implemented by all publication services.
     */
    public interface Service : Closeable {

        /**
         * Container for the context from which a service is created.
         */
        public class Context(
            public val manifest: Manifest,
            public val container: Container,
            public val services: PublicationServicesHolder
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
        public val links: List<Link> get() = emptyList()

        /**
         * A service can return a Resource to:
         *  - respond to a request to its web API declared in links,
         *  - serve additional resources on behalf of the publication,
         *  - replace a publication resource by its own version.
         *
         * Called by [Publication.get] for each request.
         *
         * Warning: If you need to request one of the publication resources to answer the request,
         * use the [Container] provided by the [Publication.Service.Context] instead of
         * [Publication.get], otherwise it will trigger an infinite loop.
         *
         * @return The [Resource] containing the response, or null if the service doesn't
         * recognize this request.
         */
        public fun get(href: Url): Resource? = null

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
    public class ServicesBuilder private constructor(
        private val serviceFactories: MutableMap<String, ServiceFactory>
    ) {

        @OptIn(Search::class, ExperimentalReadiumApi::class)
        @Suppress("UNCHECKED_CAST")
        public constructor(
            cache: ServiceFactory? = null,
            content: ServiceFactory? = null,
            contentProtection: ServiceFactory? = null,
            cover: ServiceFactory? = null,
            locator: ServiceFactory? = null,
            positions: ServiceFactory? = null,
            search: ServiceFactory? = null
        ) : this(
            mapOf(
                CacheService::class.java.simpleName to cache,
                ContentService::class.java.simpleName to content,
                ContentProtectionService::class.java.simpleName to contentProtection,
                CoverService::class.java.simpleName to cover,
                LocatorService::class.java.simpleName to locator,
                PositionsService::class.java.simpleName to positions,
                SearchService::class.java.simpleName to search
            ).filterValues { it != null }.toMutableMap() as MutableMap<String, ServiceFactory>
        )

        /** Builds the actual list of publication services to use in a Publication. */
        public fun build(context: Service.Context): List<Service> {
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
        public operator fun <T : Service> get(serviceType: KClass<T>): ServiceFactory? {
            val key = requireNotNull(serviceType.simpleName)
            return serviceFactories[key]
        }

        /** Sets the publication service factory for the given service type. */
        public operator fun <T : Service> set(serviceType: KClass<T>, factory: ServiceFactory?) {
            val key = requireNotNull(serviceType.simpleName)
            if (factory != null) {
                serviceFactories[key] = factory
            } else {
                serviceFactories.remove(key)
            }
        }

        /** Removes the service factory producing the given kind of service, if any. */
        public fun <T : Service> remove(serviceType: KClass<T>) {
            val key = requireNotNull(serviceType.simpleName)
            serviceFactories.remove(key)
        }

        /**
         * Replaces the service factory associated with the given service type with the result of
         * [transform].
         */
        public fun <T : Service> decorate(
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
    public sealed class OpenError(message: String, cause: Error? = null) :
        BaseError(message, cause) {

        /**
         * The file format could not be recognized by any parser.
         */
        public class UnsupportedAsset(
            message: String,
            cause: Error?
        ) : OpenError(message, cause) {
            public constructor(message: String) : this(message, null)
            public constructor(cause: Error? = null) : this("Asset is not supported.", cause)
        }

        /**
         * The publication parsing failed with the given underlying error.
         */
        public class InvalidAsset private constructor(
            message: String,
            cause: Error?
        ) : OpenError(message, cause) {
            public constructor(message: String) : this(message, null)
            public constructor(cause: Error? = null) : this(
                "The asset seems corrupted so the publication cannot be opened.",
                cause
            )
        }

        /**
         * The publication file was not found on the file system.
         */
        public class NotFound(cause: Error? = null) :
            OpenError("Asset could not be found.", cause)

        /**
         * We're not allowed to open the publication at all, for example because it expired.
         */
        public class Forbidden(cause: Error? = null) :
            OpenError("You are not allowed to open this publication.", cause)

        /**
         * The publication can't be opened at the moment, for example because of a networking error.
         * This error is generally temporary, so the operation may be retried or postponed.
         */
        public class Unavailable(cause: Error? = null) :
            OpenError("The publication is not available at the moment.", cause)

        /**
         * The provided credentials are incorrect and we can't open the publication in a
         * `restricted` state (e.g. for a password-protected ZIP).
         */
        public class IncorrectCredentials(cause: Error? = null) :
            OpenError("Provided credentials were incorrect.", cause)

        /**
         * Opening the publication exceeded the available device memory.
         */
        public class OutOfMemory(cause: Error? = null) :
            OpenError("There is not enough memory available to open the publication.", cause)

        /**
         * An unexpected error occurred.
         */
        public class Unknown(cause: Error? = null) :
            OpenError("An unexpected error occurred.", cause) {

            public constructor(exception: Exception) : this(ThrowableError(exception))
        }
    }

    /**
     * Builds a Publication from its components.
     *
     * A Publication's construction is distributed over the Streamer and its parsers,
     * so a builder is useful to pass the parts around.
     */
    public class Builder(
        public var manifest: Manifest,
        public var container: Container,
        public var servicesBuilder: ServicesBuilder = ServicesBuilder()
    ) {

        public fun build(): Publication = Publication(
            manifest = manifest,
            container = container,
            servicesBuilder = servicesBuilder
        )
    }

    /**
     * Finds the first [Link] to the publication's cover (rel = cover).
     */
    @Deprecated(
        "Use [Publication.cover] to get the cover as a [Bitmap]",
        ReplaceWith("cover"),
        level = DeprecationLevel.ERROR
    )
    public val coverLink: Link? get() = linkWithRel("cover")

    /**
     * Copy the [Publication] with a different [PositionListFactory].
     * The provided closure will be used to build the [PositionListFactory], with this being the
     * [Publication].
     */
    @Deprecated(
        "Use [Publication.copy(serviceFactories)] instead",
        ReplaceWith("Publication.copy(serviceFactories = listOf(positionsServiceFactory)"),
        level = DeprecationLevel.ERROR
    )
    public fun copyWithPositionsFactory(): Publication {
        throw NotImplementedError()
    }

    @Deprecated(
        "Renamed to [listOfAudioClips]",
        ReplaceWith("listOfAudioClips"),
        level = DeprecationLevel.ERROR
    )
    public val listOfAudioFiles: List<Link> = listOfAudioClips

    @Deprecated(
        "Renamed to [listOfVideoClips]",
        ReplaceWith("listOfVideoClips"),
        level = DeprecationLevel.ERROR
    )
    public val listOfVideos: List<Link> = listOfVideoClips

    @Deprecated(
        "Renamed to [linkWithHref]",
        ReplaceWith("linkWithHref(href)"),
        level = DeprecationLevel.ERROR
    )
    @Suppress("UNUSED_PARAMETER")
    public fun resource(href: String): Link? = throw NotImplementedError()

    @Deprecated("Refactored as a property", ReplaceWith("baseUrl"), level = DeprecationLevel.ERROR)
    public fun baseUrl(): URL? = throw NotImplementedError()

    @Deprecated(
        "Renamed [subcollections]",
        ReplaceWith("subcollections"),
        level = DeprecationLevel.ERROR
    )
    public val otherCollections: Map<String, List<PublicationCollection>> get() = subcollections

    @Deprecated(
        "Use [setSelfLink] instead",
        ReplaceWith("setSelfLink"),
        level = DeprecationLevel.ERROR
    )
    @Suppress("UNUSED_PARAMETER")
    public fun addSelfLink(endPoint: String, baseURL: URL) {
        throw NotImplementedError()
    }

    @Deprecated(
        "Use [linkWithHref] instead.",
        ReplaceWith("linkWithHref(href)"),
        level = DeprecationLevel.ERROR
    )
    @Suppress("UNUSED_PARAMETER")
    public fun resourceWithHref(href: String): Link? = throw NotImplementedError()

    @Deprecated(
        "Use a [ServiceFactory] for a [PositionsService] instead.",
        level = DeprecationLevel.ERROR
    )
    public interface PositionListFactory {
        public fun create(): List<Locator>
    }

    @Deprecated(
        "Use [linkWithHref()] to find a link with the given HREF",
        replaceWith = ReplaceWith("linkWithHref"),
        level = DeprecationLevel.ERROR
    )
    @Suppress("UNUSED_PARAMETER")
    public fun link(predicate: (Link) -> Boolean): Link? = null

    @Deprecated(
        "Jsonify the manifest by yourself",
        ReplaceWith("manifest.toJSON()"),
        level = DeprecationLevel.ERROR
    )
    public fun toJSON(): JSONObject = throw NotImplementedError()

    @Deprecated(
        "Use `metadata.effectiveReadingProgression` instead",
        ReplaceWith("metadata.effectiveReadingProgression"),
        level = DeprecationLevel.ERROR
    )
    public val contentLayout: ReadingProgression get() = metadata.effectiveReadingProgression

    @Deprecated(
        "Use `metadata.effectiveReadingProgression` instead",
        ReplaceWith("metadata.effectiveReadingProgression"),
        level = DeprecationLevel.ERROR
    )
    @Suppress("UNUSED_PARAMETER")
    public fun contentLayoutForLanguage(language: String?): ReadingProgression = metadata.effectiveReadingProgression

    @Deprecated(
        "Renamed to `OpenError`",
        replaceWith = ReplaceWith("Publication.OpenError"),
        level = DeprecationLevel.ERROR
    )
    public sealed class OpeningException
}

private fun Resource.withMediaType(mediaType: MediaType?): Resource {
    if (mediaType == null) {
        return this
    }

    return object : Resource by this {
        override suspend fun mediaType(): ResourceTry<MediaType> =
            ResourceTry.success(mediaType)
    }
}
