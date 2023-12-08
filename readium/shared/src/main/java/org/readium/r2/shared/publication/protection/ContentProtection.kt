/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.protection

import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.String
import kotlin.Unit
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

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
        override val cause: Error?
    ) : Error {

        public class Reading(
            override val cause: ReadError
        ) : OpenError("An error occurred while trying to read asset.", cause)

        public class AssetNotSupported(
            override val cause: Error?
        ) : OpenError("Asset is not supported.", cause)
    }

    public val scheme: Scheme

    /**
     * Returns if this [ContentProtection] supports the given [asset].
     */
    public suspend fun supports(
        asset: org.readium.r2.shared.util.asset.Asset
    ): Try<Boolean, ReadError>

    /**
     * Attempts to unlock a potentially protected publication asset.
     *
     * @return A [Asset] in case of success or a [OpenError] if the
     * asset can't be successfully opened even in restricted mode.
     */
    public suspend fun open(
        asset: org.readium.r2.shared.util.asset.Asset,
        credentials: String?,
        allowUserInteraction: Boolean
    ): Try<Asset, OpenError>

    /**
     * Holds the result of opening an [Asset] with a [ContentProtection].
     *
     * @property mediaType Media type of the asset
     * @property container Container to access the publication through
     * @property onCreatePublication Called on every parsed Publication.Builder
     * It can be used to modify the `Manifest`, the root [Container] or the list of service
     * factories of a [Publication].
     */
    public data class Asset(
        val mediaType: MediaType,
        val container: Container<Resource>,
        val onCreatePublication: Publication.Builder.() -> Unit = {}
    )

    /**
     * Represents a specific Content Protection technology, uniquely identified with an [uri].
     */
    @JvmInline
    public value class Scheme(
        public val uri: String
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
}
