/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.protection

import androidx.annotation.StringRes
import org.readium.r2.shared.R
import org.readium.r2.shared.UserException
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Bridge between a Content Protection technology and the Readium toolkit.
 *
 * Its responsibilities are to:
 * - Create a [Fetcher] one can access the publication through.
 * - Create a [ContentProtectionService] publication service.
 */
public interface ContentProtection {

    public val scheme: Scheme

    /**
     * Returns if this [ContentProtection] supports the given [asset].
     */
    public suspend fun supports(
        asset: org.readium.r2.shared.asset.Asset
    ): Boolean

    /**
     * Attempts to unlock a potentially protected publication asset.
     *
     * @return A [Asset] in case of success or a [Publication.OpeningException] if the
     * asset can't be successfully opened even in restricted mode.
     */
    public suspend fun open(
        asset: org.readium.r2.shared.asset.Asset,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<Asset, Publication.OpeningException>

    /**
     * Holds the result of opening an [Asset] with a [ContentProtection].
     *
     * @property name Asset name
     * @property mediaType Media type of the asset
     * @property fetcher Fetcher to access the publication through
     * @property onCreatePublication Called on every parsed Publication.Builder
     * It can be used to modify the `Manifest`, the root [Fetcher] or the list of service factories
     * of a [Publication].
     */
    public data class Asset(
        val name: String,
        val mediaType: MediaType,
        val fetcher: Fetcher,
        val onCreatePublication: Publication.Builder.() -> Unit = {}
    )

    /**
     * Represents a specific Content Protection technology, uniquely identified with an [uri].
     */
    @JvmInline
    public value class Scheme(
        public val uri: String,
    ) {

        @Deprecated("Define yourself the name to print to users.", level = DeprecationLevel.ERROR)
        public val name: LocalizedString? get() = null

        public companion object {
            /** Readium LCP DRM scheme. */
            public val Lcp: Scheme = Scheme(uri = "http://readium.org/2014/01/lcp")
            /** Adobe ADEPT DRM scheme. */
            public val Adept: Scheme = Scheme(uri = "http://ns.adobe.com/adept")
        }
    }

    public sealed class Exception(
        userMessageId: Int,
        vararg args: Any?,
        quantity: Int? = null,
        cause: Throwable? = null
    ) : UserException(userMessageId, quantity, *args, cause = cause) {
        protected constructor(
            @StringRes userMessageId: Int,
            vararg args: Any?,
            cause: Throwable? = null
        ) : this(userMessageId, *args, quantity = null, cause = cause)

        /**
         * Exception returned when the given Content Protection [scheme] is not supported by the
         * app.
         */
        public class SchemeNotSupported(public val scheme: Scheme? = null, name: String?) : Exception(
            if (name == null) R.string.readium_shared_publication_content_protection_exception_not_supported_unknown
            else R.string.readium_shared_publication_content_protection_exception_not_supported,
            name
        )
    }
}
