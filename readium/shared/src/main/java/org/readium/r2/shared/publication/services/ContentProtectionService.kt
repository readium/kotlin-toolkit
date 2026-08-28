/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.services

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.ServiceFactory
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.util.Error

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
    public val error: Error?

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
    public val name: String? get() = null

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
        public suspend fun copy(text: String): Boolean

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
        public suspend fun print(pageCount: Int): Boolean

        /**
         * A [UserRights] without any restriction.
         */
        public object Unrestricted : UserRights {
            override val canCopy: Boolean = true

            override fun canCopy(text: String): Boolean = true

            override suspend fun copy(text: String): Boolean = true

            override val canPrint: Boolean = true

            override fun canPrint(pageCount: Int): Boolean = true

            override suspend fun print(pageCount: Int): Boolean = true
        }

        /**
         * A [UserRights] which forbids any right.
         */
        public object AllRestricted : UserRights {
            override val canCopy: Boolean = false

            override fun canCopy(text: String): Boolean = false

            override suspend fun copy(text: String): Boolean = false

            override val canPrint: Boolean = false

            override fun canPrint(pageCount: Int): Boolean = false

            override suspend fun print(pageCount: Int): Boolean = false
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
public val Publication.protectionError: Error?
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
 * User-facing name for this Content Protection, e.g. "Readium LCP".
 * It could be used in a sentence such as "Protected by {name}".
 */
public val Publication.protectionName: String?
    get() = protectionService?.name
