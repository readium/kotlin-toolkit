/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlin.reflect.KClass
import kotlinx.parcelize.Parcelize
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
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
    @InternalReadiumApi
    public val container: Container<Resource> = EmptyContainer(),
    private val servicesBuilder: ServicesBuilder = ServicesBuilder(),
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

    /**
     * Returns the [links] of the first child [PublicationCollection] with the given role, or an
     * empty list.
     */
    internal fun linksWithRole(role: String): List<Link> =
        subcollections[role]?.firstOrNull()?.links ?: emptyList()

    public companion object

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
            public val services: PublicationServicesHolder,
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
        private val serviceFactories: MutableMap<String, ServiceFactory>,
    ) {

        @OptIn(ExperimentalReadiumApi::class)
        @Suppress("UNCHECKED_CAST")
        public constructor(
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
            transform: ((ServiceFactory)?) -> ServiceFactory,
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
        public var servicesBuilder: ServicesBuilder = ServicesBuilder(),
    ) {

        public fun build(): Publication = Publication(
            manifest = manifest,
            container = container,
            servicesBuilder = servicesBuilder
        )
    }
}
