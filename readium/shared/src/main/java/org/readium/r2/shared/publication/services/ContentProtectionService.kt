/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import java.util.Locale
import org.json.JSONObject
import org.readium.r2.shared.UserException
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.resource.FailureResource
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.StringResource
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Provides information about a publication's content protection and manages user rights.
 */
public interface ContentProtectionService : Publication.Service {

    /**
     * Whether the [Publication] has a restricted access to its resources, and can't be rendered in
     * a Navigator.
     */
    public val isRestricted: Boolean

    /**
     * The error raised when trying to unlock the [Publication], if any.
     */
    public val error: UserException?

    /**
     * Credentials used to unlock this [Publication].
     */
    public val credentials: String?

    /**
     * Manages consumption of user rights and permissions.
     */
    public val rights: UserRights

    /**
     * Known technology for this type of Content Protection.
     */
    public val scheme: ContentProtection.Scheme? get() = null

    /**
     * User-facing name for this Content Protection, e.g. "Readium LCP".
     * It could be used in a sentence such as "Protected by {name}"
     */
    public val name: LocalizedString? get() = null

    override val links: List<Link>
        get() = RouteHandler.links

    override fun get(href: Url): Resource? {
        val route = RouteHandler.route(href) ?: return null
        return route.handleRequest(href, this)
    }

    /**
     * Manages consumption of user rights and permissions.
     */
    public interface UserRights {

        /**
         * Returns whether the user is currently allowed to copy content to the pasteboard.
         *
         * Navigators and reading apps can use this to know if the "Copy" action should be greyed
         * out or not. This should be called every time the "Copy" action will be displayed,
         * because the value might change during runtime.
         */
        public val canCopy: Boolean

        /**
         * Returns whether the user is allowed to copy the given text to the pasteboard.
         *
         * This is more specific than the [canCopy] property, and can return false if the given text
         * exceeds the allowed amount of characters to copy.
         *
         * To be used before presenting, for example, a pop-up to share a selected portion of
         * content.
         */
        public fun canCopy(text: String): Boolean

        /**
         * Consumes the given text with the copy right.
         *
         * Returns whether the user is allowed to copy the given text.
         */
        public fun copy(text: String): Boolean

        /**
         * Returns whether the user is currently allowed to print the content.
         *
         * Navigators and reading apps can use this to know if the "Print" action should be greyed
         * out or not.
         */
        public val canPrint: Boolean

        /**
         * Returns whether the user is allowed to print the given amount of pages.
         *
         * This is more specific than the [canPrint] property, and can return false if the given
         * [pageCount] exceeds the allowed amount of pages to print.
         *
         * To be used before attempting to launch a print job, for example.
         */
        public fun canPrint(pageCount: Int): Boolean

        /**
         * Consumes the given amount of pages with the print right.
         *
         * Returns whether the user is allowed to print the given amount of pages.
         */
        public fun print(pageCount: Int): Boolean

        /**
         * A [UserRights] without any restriction.
         */
        public object Unrestricted : UserRights {
            override val canCopy: Boolean = true

            override fun canCopy(text: String): Boolean = true

            override fun copy(text: String): Boolean = true

            override val canPrint: Boolean = true

            override fun canPrint(pageCount: Int): Boolean = true

            override fun print(pageCount: Int): Boolean = true
        }

        /**
         * A [UserRights] which forbids any right.
         */
        public object AllRestricted : UserRights {
            override val canCopy: Boolean = false

            override fun canCopy(text: String): Boolean = false

            override fun copy(text: String): Boolean = false

            override val canPrint: Boolean = false

            override fun canPrint(pageCount: Int): Boolean = false

            override fun print(pageCount: Int): Boolean = false
        }
    }
}

private val PublicationServicesHolder.protectionService: ContentProtectionService?
    get() {
        findService(ContentProtectionService::class)?.let { return it }
        /* TODO: return links.firstWithMediaType(RouteHandler.ContentProtectionHandler.link.mediaType!!)?.let {
            WebContentProtection(it)
        } */
        return null
    }

/** Factory to build a [ContentProtectionService]. */
public var Publication.ServicesBuilder.contentProtectionServiceFactory: ServiceFactory?
    get() = get(ContentProtectionService::class)
    set(value) = set(ContentProtectionService::class, value)

/**
 * Returns whether this Publication is protected by a Content Protection technology.
 */
public val Publication.isProtected: Boolean
    get() = protectionService != null

/**
 * Whether the [Publication] has a restricted access to its resources, and can't be rendered in
 * a Navigator.
 */
public val Publication.isRestricted: Boolean
    get() = protectionService?.isRestricted
        ?: false

/**
 * The error raised when trying to unlock the [Publication], if any.
 */
public val Publication.protectionError: UserException?
    get() = protectionService?.error

/**
 * Credentials used to unlock this [Publication].
 */
public val Publication.credentials: String?
    get() = protectionService?.credentials

/**
 * Manages consumption of user rights and permissions.
 */
public val Publication.rights: ContentProtectionService.UserRights
    get() = protectionService?.rights
        ?: ContentProtectionService.UserRights.Unrestricted

/**
 * Known technology for this type of Content Protection.
 */
public val Publication.protectionScheme: ContentProtection.Scheme?
    get() = protectionService?.scheme

/**
 * User-facing localized name for this Content Protection, e.g. "Readium LCP".
 * It could be used in a sentence such as "Protected by {name}".
 */
public val Publication.protectionLocalizedName: LocalizedString?
    get() = protectionService?.name

/**
 * User-facing name for this Content Protection, e.g. "Readium LCP".
 * It could be used in a sentence such as "Protected by {name}".
 */
public val Publication.protectionName: String?
    get() = protectionLocalizedName?.string

private sealed class RouteHandler {

    companion object {

        private val handlers = listOf(
            ContentProtectionHandler,
            RightsCopyHandler,
            RightsPrintHandler
        )

        val links = handlers.map { it.link }

        fun route(url: Url): RouteHandler? = handlers.firstOrNull { it.acceptRequest(url) }
    }

    abstract val link: Link

    abstract fun acceptRequest(url: Url): Boolean

    abstract fun handleRequest(url: Url, service: ContentProtectionService): Resource

    object ContentProtectionHandler : RouteHandler() {

        private val path = "/~readium/content-protection"
        private val mediaType = MediaType("application/vnd.readium.content-protection+json")!!

        override val link = Link(
            href = Url(path)!!,
            mediaType = mediaType
        )

        override fun acceptRequest(url: Url): Boolean =
            url.path == path

        override fun handleRequest(url: Url, service: ContentProtectionService): Resource =
            StringResource(mediaType = mediaType) {
                Try.success(
                    JSONObject().apply {
                        put("isRestricted", service.isRestricted)
                        putOpt("error", service.error?.localizedMessage)
                        putIfNotEmpty("name", service.name)
                        put("rights", service.rights.toJSON())
                    }.toString()
                )
            }
    }

    object RightsCopyHandler : RouteHandler() {

        private val mediaType = MediaType("application/vnd.readium.rights.copy+json")!!
        private val path = "/~readium/rights/copy"

        override val link: Link = Link(
            href = Href("$path{?text,peek}", templated = true)!!,
            mediaType = mediaType
        )

        override fun acceptRequest(url: Url): Boolean =
            url.path == path

        override fun handleRequest(url: Url, service: ContentProtectionService): Resource {
            val query = url.query
            val text = query.firstNamedOrNull("text")
                ?: return FailureResource(
                    Resource.Exception.BadRequest(
                        IllegalArgumentException("'text' parameter is required")
                    )
                )
            val peek = (query.firstNamedOrNull("peek") ?: "false").toBooleanOrNull()
                ?: return FailureResource(
                    Resource.Exception.BadRequest(
                        IllegalArgumentException("if present, 'peek' must be true or false")
                    )
                )

            val copyAllowed = with(service.rights) { if (peek) canCopy(text) else copy(text) }

            return if (!copyAllowed) {
                FailureResource(Resource.Exception.Forbidden())
            } else {
                StringResource("true", MediaType.JSON)
            }
        }
    }

    object RightsPrintHandler : RouteHandler() {

        private val mediaType = MediaType("application/vnd.readium.rights.print+json")!!
        private val path = "/~readium/rights/print"

        override val link = Link(
            href = Href("$path{?pageCount,peek}", templated = true)!!,
            mediaType = mediaType
        )

        override fun acceptRequest(url: Url): Boolean =
            url.path == path

        override fun handleRequest(url: Url, service: ContentProtectionService): Resource {
            val query = url.query
            val pageCountString = query.firstNamedOrNull("pageCount")
                ?: return FailureResource(
                    Resource.Exception.BadRequest(
                        IllegalArgumentException("'pageCount' parameter is required")
                    )
                )

            val pageCount = pageCountString.toIntOrNull()?.takeIf { it >= 0 }
                ?: return FailureResource(
                    Resource.Exception.BadRequest(
                        IllegalArgumentException("'pageCount' must be a positive integer")
                    )
                )
            val peek = (query.firstNamedOrNull("peek") ?: "false").toBooleanOrNull()
                ?: return FailureResource(
                    Resource.Exception.BadRequest(
                        IllegalArgumentException("if present, 'peek' must be true or false")
                    )
                )

            val printAllowed = with(service.rights) {
                if (peek) {
                    canPrint(pageCount)
                } else {
                    print(
                        pageCount
                    )
                }
            }

            return if (!printAllowed) {
                FailureResource(Resource.Exception.Forbidden())
            } else {
                StringResource("true", mediaType = MediaType.JSON)
            }
        }
    }

    fun String.toBooleanOrNull(): Boolean? = when (this.lowercase(Locale.getDefault())) {
        "true" -> true
        "false" -> false
        else -> null
    }

    fun ContentProtectionService.UserRights.toJSON() = JSONObject().apply {
        put("canCopy", canCopy)
        put("canPrint", canPrint)
    }
}
