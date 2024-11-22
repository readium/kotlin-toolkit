/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.protection

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError

/**
 * Bridge between a Content Protection technology and the Readium toolkit.
 *
 * Its responsibilities are to:
 * - Create a [Container] one can access the publication through.
 * - Create a [ContentProtectionService] publication service.
 */
public interface ContentProtection {

    public sealed class OpenError(
        override val message: String,
        override val cause: Error?,
    ) : Error {

        public class Reading(
            override val cause: ReadError,
        ) : OpenError("An error occurred while trying to read asset.", cause)

        public class AssetNotSupported(
            override val cause: Error? = null,
        ) : OpenError("Asset is not supported.", cause)
    }

    /**
     * Holds the result of opening an [Asset] with a [ContentProtection].
     *
     * @property asset Asset pointing to a publication.
     * @property onCreatePublication Called on every parsed Publication.Builder
     * It can be used to modify the `Manifest`, the root [Container] or the list of service
     * factories of a [Publication].
     */
    public data class OpenResult(
        val asset: Asset,
        val onCreatePublication: Publication.Builder.() -> Unit = {},
    )

    /**
     * Attempts to unlock a potentially protected publication asset.
     *
     * @return A [Asset] in case of success or an [OpenError] if the
     * asset can't be successfully opened even in restricted mode.
     */
    public suspend fun open(
        asset: Asset,
        credentials: String?,
        allowUserInteraction: Boolean,
    ): Try<OpenResult, OpenError>

    /**
     * Represents a specific Content Protection technology, uniquely identified with an [uri].
     */
    @JvmInline
    public value class Scheme(
        public val uri: String,
    ) {

        public companion object {
            /** Readium LCP DRM scheme. */
            public val Lcp: Scheme = Scheme(uri = "http://readium.org/2014/01/lcp")

            /** Adobe ADEPT DRM scheme. */
            public val Adept: Scheme = Scheme(uri = "http://ns.adobe.com/adept")
        }
    }
}
