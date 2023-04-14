/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import androidx.annotation.StringRes
import org.readium.r2.shared.R
import org.readium.r2.shared.UserException
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.util.Try

/**
 * Bridge between a Content Protection technology and the Readium toolkit.
 *
 * Its responsibilities are to:
 * - Unlock a publication by returning a customized [Fetcher].
 * - Create a [ContentProtectionService] publication service.
 */
interface ContentProtection {

    /**
     * Attempts to unlock a potentially protected publication asset.
     *
     * @return A [ProtectedAsset] in case of success, null if the asset is not protected by this
     * technology or a [Publication.OpeningException] if the asset can't be successfully opened,
     * even in restricted mode.
     */
    suspend fun open(
        asset: PublicationAsset,
        credentials: String?,
        allowUserInteraction: Boolean,
        sender: Any?
    ): Try<ProtectedAsset, Publication.OpeningException>?

    /**
     * Holds the result of opening a [PublicationAsset] with a [ContentProtection].
     *
     * @property asset Protected asset which will be provided to the parsers.
     * It is likely to be different from the asset provided to ContentProtection::open():
     * - If the original fetcher is wrapped into a new fetcher able to decipher content.
     * - If the original asset has a media type that can't be recognized by parsers,
     *   the Content Protection must return an asset with the matching unprotected media type.
     * - If the Content Protection technology needs to redirect the Streamer to a different file.
     *   For example, this could be used to decrypt a publication to a temporary secure location.
     *
     * @property onCreatePublication Called on every parsed Publication.Builder.
     * It can be used to modify the `Manifest`, the root [Fetcher] or the list of service factories
     * of a [Publication].
     */
    data class ProtectedAsset(
        val asset: PublicationAsset,
        val onCreatePublication: Publication.Builder.() -> Unit = {}
    )

    /**
     * Represents a specific Content Protection technology, uniquely identified with an [uri].
     */
    class Scheme(
        val uri: String,
        val name: LocalizedString?
    ) {
        override fun hashCode(): Int = uri.hashCode()
        override fun equals(other: Any?): Boolean = (other as? Scheme)?.uri == uri

        companion object {
            /** Readium LCP DRM scheme. */
            val Lcp = Scheme(uri = "http://readium.org/2014/01/lcp", name = LocalizedString("Readium LCP"))
            /** Adobe ADEPT DRM scheme. */
            val Adept = Scheme(uri = "http://ns.adobe.com/adept", name = LocalizedString("Adobe ADEPT"))
        }
    }

    sealed class Exception(
        userMessageId: Int,
        vararg args: Any?,
        quantity: Int? = null,
        cause: Throwable? = null
    ) : UserException(userMessageId, quantity, *args, cause = cause) {
        constructor(@StringRes userMessageId: Int, vararg args: Any?, cause: Throwable? = null) : this(userMessageId, *args, quantity = null, cause = cause)

        /**
         * Exception returned when the given Content Protection [scheme] is not supported by the
         * app.
         */
        class SchemeNotSupported(val scheme: Scheme? = null) : Exception(
            if (scheme?.name == null) R.string.r2_shared_publication_content_protection_exception_not_supported_unknown
            else R.string.r2_shared_publication_content_protection_exception_not_supported,
            scheme?.name?.string
        )
    }
}
