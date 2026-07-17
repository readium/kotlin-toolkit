/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveOpener
import org.readium.r2.shared.util.data.CachingContainer
import org.readium.r2.shared.util.data.CachingReadable
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.FormatSniffer
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.tryRecover

internal class AssetSniffer(
    private val formatSniffer: FormatSniffer = DefaultFormatSniffer(),
    private val archiveOpener: ArchiveOpener = DefaultArchiveOpener(),
) {

    sealed class SniffError(
        override val message: String,
        override val cause: Error?,
    ) : Error {

        data object NotRecognized :
            SniffError("Format of resource could not be inferred.", null)

        data class Reading(override val cause: ReadError) :
            SniffError("An error occurred while trying to read content.", cause)
    }

    suspend fun sniff(
        source: Either<Resource, Container<Resource>>,
        hints: FormatHints,
    ): Try<Asset, SniffError> {
        val initialFormat = formatSniffer
            .sniffHints(hints)
            ?: Format(
                specification = FormatSpecification(emptySet()),
                mediaType = MediaType.BINARY,
                fileExtension = FileExtension("")
            )

        val cachingSource: Either<Readable, Container<Readable>> = when (source) {
            is Either.Left -> Either.Left(CachingReadable(source.value))
            is Either.Right -> Either.Right(CachingContainer(source.value))
        }

        val asset = sniffContent(initialFormat, source, cachingSource, hints, forceRefine = false)
            .getOrElse { return Try.failure(SniffError.Reading(it)) }

        return asset
            .takeIf { it.format.isValid() }
            ?.let { Try.success(it) }
            ?: Try.failure(SniffError.NotRecognized)
    }

    private suspend fun sniffContent(
        format: Format,
        source: Either<Resource, Container<Resource>>,
        cache: Either<Readable, Container<Readable>>,
        hints: FormatHints,
        forceRefine: Boolean,
    ): Try<Asset, ReadError> {
        when (cache) {
            is Either.Left ->
                formatSniffer
                    .sniffBlob(format, cache.value)
                    .getOrElse { return Try.failure(it) }
                    .takeIf { !forceRefine || it.refines(format) }
                    ?.let { return sniffContent(it, source, cache, hints, forceRefine = true) }

            is Either.Right ->
                formatSniffer
                    .sniffContainer(format, cache.value)
                    .getOrElse { return Try.failure(it) }
                    .takeIf { !forceRefine || it.refines(format) }
                    ?.let { return sniffContent(it, source, cache, hints, forceRefine = true) }
        }

        if (source is Either.Left) {
            tryOpenArchive(format, source.value)
                .getOrElse { return Try.failure(it) }
                ?.let {
                    return sniffContent(
                        it.format,
                        Either.Right(it.container),
                        Either.Right(CachingContainer(it.container)),
                        hints,
                        forceRefine = true
                    )
                }
        }

        return Try.success(
            when (source) {
                is Either.Left -> ResourceAsset(format, source.value)
                is Either.Right -> ContainerAsset(format, source.value)
            }
        )
    }

    private suspend fun tryOpenArchive(
        format: Format,
        source: Readable,
    ): Try<ContainerAsset?, ReadError> =
        if (!format.isValid()) {
            archiveOpener.sniffOpen(source)
                .tryRecover {
                    when (it) {
                        is ArchiveOpener.SniffOpenError.NotRecognized ->
                            Try.success(null)
                        is ArchiveOpener.SniffOpenError.Reading ->
                            Try.failure(it.cause)
                    }
                }
        } else {
            archiveOpener.open(format, source)
                .tryRecover {
                    when (it) {
                        is ArchiveOpener.OpenError.FormatNotSupported ->
                            Try.success(null)
                        is ArchiveOpener.OpenError.Reading ->
                            Try.failure(it.cause)
                    }
                }
        }

    private fun Format.isValid(): Boolean =
        specification.specifications.isNotEmpty() &&
            mediaType != MediaType.BINARY &&
            fileExtension.value.isNotBlank()
}
