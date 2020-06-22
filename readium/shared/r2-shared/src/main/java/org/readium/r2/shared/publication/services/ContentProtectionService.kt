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
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.extensions.queryParameters
import org.readium.r2.shared.fetcher.FailureResource
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.StringResource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ServiceFactory
import java.net.URLDecoder
import java.util.Locale

private sealed class RouteHandler {

    companion object {
        fun route(link: Link): RouteHandler? {
            val path = URLDecoder.decode(link.href, "UTF-8").takeWhile { it != '?' }
            return handlers[path]
        }

        val handlers = mapOf(
            ContentProtectionHandler.link.href to ContentProtectionHandler,
            RightsCopyHandler.link.href to RightsCopyHandler,
            RightsPrintHandler.link.href to RightsPrintHandler
        )

        val links = handlers.values.map { it.link }
    }

    abstract val link: Link

    abstract fun handleRequest(link: Link, service: ContentProtectionService): Resource

    object ContentProtectionHandler : RouteHandler() {

        override val link = Link(
            href = "/~readium/content-protection",
            type = "application/vnd.readium.content-protection+json"
        )

        override fun handleRequest(link: Link, service: ContentProtectionService): Resource =
            StringResource(link) {
                JSONObject().apply {
                    put("isLocked", service.isLocked)
                    service.name?.let { putIfNotEmpty("name", it) }
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

        override fun handleRequest(link: Link, service: ContentProtectionService): Resource {
            val parameters = link.href.queryParameters()
            val text = parameters["text"]
                ?: return FailureResource(link, Resource.Error.BadRequest)
            val peek = parameters["peek"]?.toBooleanOrNull(false)
                ?: return FailureResource(link, Resource.Error.BadRequest)

            val returnLink = Link(href = link.href)
            val copyAllowed = with(service.rights) { if (peek) canCopy(text) else copy(text) }

            return if (copyAllowed)
                FailureResource(returnLink, Resource.Error.Forbidden)
            else
                StringResource(returnLink) { "" }
        }
    }

    object RightsPrintHandler : RouteHandler() {

        override val link = Link(
            href = "/~readium/rights/print{?pageCount,peek}",
            type = "application/vnd.readium.rights.print+json",
            templated = true
        )

        override fun handleRequest(link: Link, service: ContentProtectionService): Resource {
            val parameters = link.href.queryParameters()
            val pageCount = parameters["pageCount"]?.toIntOrNull()
                ?: return FailureResource(link, Resource.Error.BadRequest)
            val peek = parameters["peek"]?.toBooleanOrNull(false)
                ?: return FailureResource(link, Resource.Error.BadRequest)

            val returnLink = Link(href = link.href)
            val printAllowed = with(service.rights) { if (peek) canPrint(pageCount) else print(pageCount) }

            return if (printAllowed)
                FailureResource(returnLink, Resource.Error.Forbidden)
            else
                StringResource(returnLink) { "" }
        }
    }

    fun String?.toBooleanOrNull(default: Boolean): Boolean? = when (this?.toLowerCase(Locale.getDefault())) {
        "true" -> true
        "false" -> false
        null -> default
        else -> null
    }

    fun ContentProtectionService.UserRights.toJSON() = JSONObject().apply {
        put("canCopy", canCopy)
        put("canPrint", canPrint)
    }
}

interface ContentProtectionService: Publication.Service {

    override val links: List<Link>
        get() = RouteHandler.links

    val isLocked: Boolean

    val credentials: String?

    val rights: UserRights

    val name: LocalizedString?

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
         * Returns false if the copy right is all consumed.
         * Navigators and reading apps can use this to know if the "Copy" action should be greyed out or not.
         * This should be called every time the "Copy" action will be displayed, because the value might change during runtime.
         */
        val canCopy: Boolean

        /**
         * Returns whether the user is allowed to copy the given text to the pasteboard.
         *
         * This is more specific than the canCopy property, and can return false if the given text exceeds
         * the allowed amount of characters to copy.
         * To be used before presenting, for example, a pop-up to share a selected portion of content.
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
         * Returns false if the print right is all consumed.
         * Navigators and reading apps can use this to know if the "Print" action should be greyed out or not.
         */
        val canPrint: Boolean

        /**
         * Returns whether the user is allowed to print the given amount of pages.
         *
         * This is more specific than the canPrint property, and can return false if the given pageCount exceeds
         * the allowed amount of pages to print.
         * To be used before attempting to launch a print job, for example.
         */
        fun canPrint(pageCount: Int): Boolean

        /**
         * Consumes the given amount of pages with the print right.
         *
         * Returns whether the user is allowed to print the given amount of pages.
         */
        fun print(pageCount: Int): Boolean

        object UnrestrictedUserRights: UserRights {
            override val canCopy: Boolean = true

            override fun canCopy(text: String): Boolean = true

            override fun copy(text: String): Boolean = true

            override val canPrint: Boolean = true

            override fun canPrint(pageCount: Int): Boolean = true

            override fun print(pageCount: Int): Boolean = true
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
var Publication.ServicesBuilder.protectionServiceFactory: ServiceFactory?
    get() = get(ContentProtectionService::class)
    set(value) = set(ContentProtectionService::class, value)


/**
 * Returns whether this Publication is protected by a Content Protection technology.
 */
val Publication.isProtected: Boolean
    get() = protectionService != null

val Publication.isLocked: Boolean
    get() = protectionService?.isLocked
        ?: false

val Publication.rights: ContentProtectionService.UserRights
    get() = protectionService?.rights
        ?: ContentProtectionService.UserRights.UnrestrictedUserRights

val Publication.protectionLocalizedName: LocalizedString?
    get() = protectionService?.name

val Publication.protectionName: String?
    get() = protectionLocalizedName?.string
