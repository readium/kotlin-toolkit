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
import org.readium.r2.shared.publication.epub.listOfAudioClips
import org.readium.r2.shared.publication.epub.listOfVideoClips
import org.readium.r2.shared.publication.services.CacheService
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.publication.services.CoverService
import org.readium.r2.shared.publication.services.DefaultLocatorService
import org.readium.r2.shared.publication.services.LocatorService
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.publication.services.ResourceCoverService
import org.readium.r2.shared.publication.services.content.ContentService
import org.readium.r2.shared.publication.services.search.SearchService
import org.readium.r2.shared.util.Closeable
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.EmptyContainer
import org.readium.r2.shared.util.resource.Resource

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
    public val manifest: Manifest,
    private val container: Container<Resource> = EmptyContainer(),
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

    private val services = ListPublicationServicesHolder()

    init {
        services.services = servicesBuilder.build(
            context = Service.Context(manifest, container, services)
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
    public fun get(link: Link): Resource? =
        get(link.url())

    /**
     * Returns the resource targeted by the given [href].
     */
    public fun get(href: Url): Resource? =
        // Try first the original href and falls back to href without query and fragment.
        container[href] ?: container[href.removeQuery().removeFragment()]

    /**
     * Closes any opened resource associated with the [Publication], including services.
     */
    override fun close() {
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
            public val container: Container<Resource>,
            public val services: PublicationServicesHolder
        )

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

                    if (!containsKey(CoverService::class.java.simpleName)) {
                        val factory = ResourceCoverService.createFactory()
                        put(CoverService::class.java.simpleName, factory)
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
     * Builds a Publication from its components.
     *
     * A Publication's construction is distributed over the Streamer and its parsers,
     * so a builder is useful to pass the parts around.
     */
    public class Builder(
        public var manifest: Manifest,
        public var container: Container<Resource>,
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
    public fun resource(href: String): Link = throw NotImplementedError()

    @Deprecated("Refactored as a property", ReplaceWith("baseUrl"), level = DeprecationLevel.ERROR)
    public fun baseUrl(): URL = throw NotImplementedError()

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
    public fun resourceWithHref(href: String): Link = throw NotImplementedError()

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
        "You should resolve [ReadingProgression.AUTO] by yourself.",
        level = DeprecationLevel.ERROR
    )
    public val contentLayout: ReadingProgression get() = throw NotImplementedError()

    @Deprecated(
        "You should resolve [ReadingProgression.AUTO] by yourself.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("UNUSED_PARAMETER")
    public fun contentLayoutForLanguage(language: String?): ReadingProgression = throw NotImplementedError()

    @Deprecated(
        "Renamed to `OpenError`",
        replaceWith = ReplaceWith("Publication.OpenError"),
        level = DeprecationLevel.ERROR
    )
    public sealed class OpeningException
}
