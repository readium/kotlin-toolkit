/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import org.json.JSONObject
import org.readium.r2.shared.UserException
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.extensions.queryParameters
import org.readium.r2.shared.fetcher.FailureResource
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.StringResource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ServiceFactory
import java.lang.IllegalArgumentException
import java.util.Locale

/**
 * Provides information about a publication's content protection and manages user rights.
 */
interface ContentProtectionService: Publication.Service {

    /**
     * Whether the [Publication] has a restricted access to its resources, and can't be rendered in
     * a Navigator.
     */
    val isRestricted: Boolean

    /**
     * The error raised when trying to unlock the [Publication], if any.
     */
    val error: UserException?

    /**
     * Credentials used to unlock this [Publication].
     */
    val credentials: String?

    /**
     * Manages consumption of user rights and permissions.
     */
    val rights: UserRights

    /**
     * User-facing name for this Content Protection, e.g. "Readium LCP".
     * It could be used in a sentence such as "Protected by {name}"
     */
    val name: LocalizedString?

    override val links: List<Link>
        get() = RouteHandler.links

    override fun get(link: Link): Resource? {
        val route = RouteHandler.route(link) ?: return null
        return route.handleRequest(link, this)
    }

    /**
     * Manages consumption of user rights and permissions.
     */
    interface UserRights {

        /**
         * Returns whether the user is currently allowed to copy content to the pasteboard.
         *
         * Navigators and reading apps can use this to know if the "Copy" action should be greyed
         * out or not. This should be called every time the "Copy" action will be displayed,
         * because the value might change during runtime.
         */
        val canCopy: Boolean

        /**
         * Returns whether the user is allowed to copy the given text to the pasteboard.
         *
         * This is more specific than the [canCopy] property, and can return false if the given text
         * exceeds the allowed amount of characters to copy.
         *
         * To be used before presenting, for example, a pop-up to share a selected portion of
         * content.
         */
        fun canCopy(text: String): Boolean

        /**
         * Consumes the given text with the copy right.
         *
         * Returns whether the user is allowed to copy the given text.
         */
        fun copy(text: String): Boolean

        /**
         * Returns whether the user is currently allowed to print the content.
         *
         * Navigators and reading apps can use this to know if the "Print" action should be greyed
         * out or not.
         */
        val canPrint: Boolean

        /**
         * Returns whether the user is allowed to print the given amount of pages.
         *
         * This is more specific than the [canPrint] property, and can return false if the given
         * [pageCount] exceeds the allowed amount of pages to print.
         *
         * To be used before attempting to launch a print job, for example.
         */
        fun canPrint(pageCount: Int): Boolean

        /**
         * Consumes the given amount of pages with the print right.
         *
         * Returns whether the user is allowed to print the given amount of pages.
         */
        fun print(pageCount: Int): Boolean

        /**
         * A [UserRights] without any restriction.
         */
        object Unrestricted : UserRights {
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
        object AllRestricted : UserRights {
            override val canCopy: Boolean = false

            override fun canCopy(text: String): Boolean = false

            override fun copy(text: String): Boolean = false

            override val canPrint: Boolean = false

            override fun canPrint(pageCount: Int): Boolean = false

            override fun print(pageCount: Int): Boolean  = false
        }
    }
}

private val Publication.protectionService: ContentProtectionService?
    get() {
        findService(ContentProtectionService::class)?.let { return it }
        /* TODO: return links.firstWithMediaType(RouteHandler.ContentProtectionHandler.link.mediaType!!)?.let {
            WebContentProtection(it)
        } */
        return null
    }

/** Factory to build a [ContentProtectionService]. */
var Publication.ServicesBuilder.contentProtectionServiceFactory: ServiceFactory?
    get() = get(ContentProtectionService::class)
    set(value) = set(ContentProtectionService::class, value)


/**
 * Returns whether this Publication is protected by a Content Protection technology.
 */
val Publication.isProtected: Boolean
    get() = protectionService != null

/**
 * Whether the [Publication] has a restricted access to its resources, and can't be rendered in
 * a Navigator.
 */
val Publication.isRestricted: Boolean
    get() = protectionService?.isRestricted
        ?: false


/**
 * The error raised when trying to unlock the [Publication], if any.
 */
val Publication.protectionError: UserException?
    get() = protectionService?.error

/**
 * Credentials used to unlock this [Publication].
 */
val Publication.credentials: String?
    get() = protectionService?.credentials

/**
 * Manages consumption of user rights and permissions.
 */
val Publication.rights: ContentProtectionService.UserRights
    get() = protectionService?.rights
        ?: ContentProtectionService.UserRights.Unrestricted

/**
 * User-facing localized name for this Content Protection, e.g. "Readium LCP".
 * It could be used in a sentence such as "Protected by {name}".
 */
val Publication.protectionLocalizedName: LocalizedString?
    get() = protectionService?.name

/**
 * User-facing name for this Content Protection, e.g. "Readium LCP".
 * It could be used in a sentence such as "Protected by {name}".
 */
val Publication.protectionName: String?
    get() = protectionLocalizedName?.string


private sealed class RouteHandler {

    companion object {

        private val handlers = listOf(
            ContentProtectionHandler,
            RightsCopyHandler,
            RightsPrintHandler
        )

        val links = handlers.map { it.link }

        fun route(link: Link): RouteHandler? = handlers.firstOrNull { it.acceptRequest(link) }
    }

    abstract val link: Link

    abstract fun acceptRequest(link: Link): Boolean

    abstract fun handleRequest(link: Link, service: ContentProtectionService): Resource

    object ContentProtectionHandler : RouteHandler() {

        override val link = Link(
            href = "/~readium/content-protection",
            type = "application/vnd.readium.content-protection+json"
        )

        override fun acceptRequest(link: Link): Boolean = link.href == this.link.href

        override fun handleRequest(link: Link, service: ContentProtectionService): Resource =
            StringResource(link) {
                JSONObject().apply {
                    put("isRestricted", service.isRestricted)
                    putOpt("error", service.error?.localizedMessage)
                    putIfNotEmpty("name", service.name)
                    put("rights", service.rights.toJSON())
                }.toString()
            }
    }

    object RightsCopyHandler : RouteHandler() {

        override val link: Link = Link(
            href = "/~readium/rights/copy{?text,peek}",
            type = "application/vnd.readium.rights.copy+json",
            templated = true
        )

        override fun acceptRequest(link: Link): Boolean = link.href.startsWith("/~readium/rights/copy")

        override fun handleRequest(link: Link, service: ContentProtectionService): Resource {
            val parameters = link.href.queryParameters()
            val text = parameters["text"]
                ?: return FailureResource(link, Resource.Exception.BadRequest(
                    parameters,
                    IllegalArgumentException("'text' parameter is required")
                ))
            val peek = (parameters["peek"] ?: "false").toBooleanOrNull()
                ?: return FailureResource(link, Resource.Exception.BadRequest(
                    parameters,
                    IllegalArgumentException("if present, 'peek' must be true or false")
                ))

            val copyAllowed = with(service.rights) { if (peek) canCopy(text) else copy(text) }

            return if (copyAllowed)
                FailureResource(link, Resource.Exception.Forbidden)
            else
                StringResource(link, "true")
        }
    }

    object RightsPrintHandler : RouteHandler() {

        override val link = Link(
            href = "/~readium/rights/print{?pageCount,peek}",
            type = "application/vnd.readium.rights.print+json",
            templated = true
        )

        override fun acceptRequest(link: Link): Boolean = link.href.startsWith("/~readium/rights/print")

        override fun handleRequest(link: Link, service: ContentProtectionService): Resource {
            val parameters = link.href.queryParameters()
            val pageCountString = parameters["pageCount"]
                ?: return FailureResource(link, Resource.Exception.BadRequest(
                    parameters,
                    IllegalArgumentException("'pageCount' parameter is required")
                ))

            val pageCount = pageCountString.toIntOrNull()?.takeIf { it >= 0 }
                ?: return FailureResource(link, Resource.Exception.BadRequest(
                    parameters,
                    IllegalArgumentException("'pageCount' must be a positive integer")
                ))
            val peek = (parameters["peek"] ?: "false").toBooleanOrNull()
                ?: return FailureResource(link, Resource.Exception.BadRequest(
                    parameters,
                    IllegalArgumentException("if present, 'peek' must be true or false")
                ))

            val printAllowed = with(service.rights) { if (peek) canPrint(pageCount) else print(pageCount) }

            return if (printAllowed)
                FailureResource(link, Resource.Exception.Forbidden)
            else
                StringResource(link, "true")
        }
    }

    fun String.toBooleanOrNull(): Boolean? = when (this.toLowerCase(Locale.getDefault())) {
        "true" -> true
        "false" -> false
        else -> null
    }

    fun ContentProtectionService.UserRights.toJSON() = JSONObject().apply {
        put("canCopy", canCopy)
        put("canPrint", canPrint)
    }
}
