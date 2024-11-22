/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.publication.protection.FallbackContentProtection
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.streamer.parser.PublicationParser

/**
 * Opens a [Publication] from an [Asset].
 *
 * @param publicationParser Parses the content of a publication [Asset].
 * @param contentProtections Opens DRM-protected publications.
 * @param onCreatePublication Called on every parsed [Publication.Builder]. It can be used to modify
 *   the manifest, the root container or the list of service factories of a [Publication].
 */
public class PublicationOpener(
    private val publicationParser: PublicationParser,
    contentProtections: List<ContentProtection> = emptyList(),
    private val onCreatePublication: Publication.Builder.() -> Unit = {},
) {
    public sealed class OpenError(
        override val message: String,
        override val cause: Error?,
    ) : Error {

        public class Reading(
            override val cause: ReadError,
        ) : OpenError("An error occurred while trying to read asset.", cause)

        public class FormatNotSupported(
            override val cause: Error? = null,
        ) : OpenError("Asset is not supported.", cause)
    }

    private val contentProtections: List<ContentProtection> =
        contentProtections + FallbackContentProtection()

    /**
     * Opens a [Publication] from the given asset.
     *
     * If you are opening the publication to render it in a Navigator, you must set [allowUserInteraction]
     * to true to prompt the user for its credentials when the publication is protected. However,
     * set it to false if you just want to import the [Publication] without reading its content, to
     * avoid prompting the user.
     *
     * The [warnings] logger can be used to observe non-fatal parsing warnings, caused by
     * publication authoring mistakes. This can be useful to warn users of potential rendering
     * issues.
     *
     * @param asset Digital medium (e.g. a file) used to access the publication.
     * @param credentials Credentials that Content Protections can use to attempt to unlock a
     *   publication, for example a password.
     * @param allowUserInteraction Indicates whether the user can be prompted, for example for its
     *   credentials.
     * @param onCreatePublication Transformation which will be applied on the Publication Builder.
     *   It can be used to modify the manifest, the root container or the list of service
     *   factories of the [Publication].
     * @param warnings Logger used to broadcast non-fatal parsing warnings.
     * @return A [Publication] or an [OpenError] in case of failure.
     */
    public suspend fun open(
        asset: Asset,
        credentials: String? = null,
        allowUserInteraction: Boolean,
        onCreatePublication: Publication.Builder.() -> Unit = {},
        warnings: WarningLogger? = null,
    ): Try<Publication, OpenError> {
        var protectionOnCreatePublication: Publication.Builder.() -> Unit = {}

        var transformedAsset: Asset = asset

        for (protection in contentProtections) {
            val openResult = protection
                .open(asset, credentials, allowUserInteraction)
                .getOrElse {
                    when (it) {
                        is ContentProtection.OpenError.Reading ->
                            return Try.failure(OpenError.Reading(it.cause))
                        is ContentProtection.OpenError.AssetNotSupported ->
                            null
                    }
                }

            if (openResult != null) {
                transformedAsset = openResult.asset
                protectionOnCreatePublication = openResult.onCreatePublication
                break
            }
        }

        val builder = publicationParser.parse(transformedAsset, warnings)
            .getOrElse { return Try.failure(wrapParserException(it)) }

        builder.apply {
            protectionOnCreatePublication()
            this.onCreatePublication()
            onCreatePublication()
        }

        val publication = builder.build()
        return Try.success(publication)
    }

    private fun wrapParserException(e: PublicationParser.ParseError): OpenError =
        when (e) {
            is PublicationParser.ParseError.FormatNotSupported ->
                OpenError.FormatNotSupported(DebugError("Cannot find a parser for this asset."))
            is PublicationParser.ParseError.Reading ->
                OpenError.Reading(e.cause)
        }
}
