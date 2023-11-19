/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import kotlin.String
import kotlin.let
import kotlin.takeIf
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.getOrElse

/**
 * Retrieves [ContentProtection] schemes of assets.
 */
public class ContentProtectionSchemeRetriever(
    contentProtections: List<ContentProtection>
) {
    private val contentProtections: List<ContentProtection> =
        contentProtections + listOf(
            LcpFallbackContentProtection(),
            AdeptFallbackContentProtection()
        )

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?
    ) : org.readium.r2.shared.util.Error {

        public object NoContentProtectionFound :
            Error("No content protection recognized the given asset.", null)

        public class AccessError(override val cause: ReadError) :
            Error("An error occurred while trying to read asset.", cause)
    }

    public suspend fun retrieve(asset: org.readium.r2.shared.util.asset.Asset): Try<ContentProtection.Scheme, Error> {
        for (protection in contentProtections) {
            protection.supports(asset)
                .getOrElse { return Try.failure(Error.AccessError(it)) }
                .takeIf { it }
                ?.let { return Try.success(protection.scheme) }
        }

        return Try.failure(Error.NoContentProtectionFound)
    }
}
