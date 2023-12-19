/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import java.io.File
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.CachingContainer
import org.readium.r2.shared.util.data.CachingReadable
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.format.DefaultFormatSniffer
import org.readium.r2.shared.util.format.FileExtension
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.FormatSniffer
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.borrow
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.resource.mediaType
import org.readium.r2.shared.util.tryRecover
import org.readium.r2.shared.util.use
import org.readium.r2.shared.util.zip.ZipArchiveOpener

public class AssetSniffer(
    private val formatSniffer: FormatSniffer = DefaultFormatSniffer(),
    private val archiveOpener: ArchiveOpener = ZipArchiveOpener()
) {
    public suspend fun sniff(
        file: File,
        hints: FormatHints = FormatHints()
    ): Try<Format, SniffError> =
        FileResource(file).use { sniff(it, hints) }

    public suspend fun sniff(
        resource: Resource,
        hints: FormatHints = FormatHints()
    ): Try<Format, SniffError> =
        sniffOpen(resource.borrow(), hints).map { it.format }

    public suspend fun sniff(
        container: Container<Resource>,
        hints: FormatHints = FormatHints()
    ): Try<Format, SniffError> =
        sniff(Either.Right(container), hints).map { it.format }

    public suspend fun sniffOpen(
        resource: Resource,
        hints: FormatHints = FormatHints()
    ): Try<Asset, SniffError> {
        val properties = resource.properties()
            .getOrElse { return Try.failure(SniffError.Reading(it)) }

        val internalHints = FormatHints(
            mediaType = properties.mediaType,
            fileExtension = properties.filename
                ?.substringAfterLast(".")
                ?.let { FileExtension((it)) }
        )

        return sniff(Either.Left(resource), hints + internalHints)
    }

    public suspend fun sniffOpen(
        file: File,
        hints: FormatHints = FormatHints()
    ): Try<Asset, SniffError> =
        sniff(Either.Left(FileResource(file)), hints)

    private suspend fun sniff(
        source: Either<Resource, Container<Resource>>,
        hints: FormatHints
    ): Try<Asset, SniffError> {
        val cachedSource: Either<Readable, Container<Readable>> = when (source) {
            is Either.Left -> Either.Left(CachingReadable(source.value))
            is Either.Right -> Either.Right(CachingContainer(source.value))
        }

        val format = doSniff(null, cachedSource, hints)
            .getOrElse { return Try.failure(SniffError.Reading(it)) }

        return format
            ?.let {
                Try.success(
                    when (source) {
                        is Either.Left -> ResourceAsset(it, source.value)
                        is Either.Right -> ContainerAsset(it, source.value)
                    }
                )
            } ?: Try.failure(SniffError.NotRecognized)
    }

    private suspend fun doSniff(
        format: Format?,
        source: Either<Readable, Container<Readable>>,
        hints: FormatHints
    ): Try<Format?, ReadError> {
        formatSniffer
            .sniffHints(format, hints)
            ?.takeIf { format == null || it.conformsTo(format) }
            ?.takeIf { it != format }
            ?.let { return doSniff(it, source, hints) }

        when (source) {
            is Either.Left ->
                formatSniffer
                    .sniffBlob(format, source.value)
                    .getOrElse { return Try.failure(it) }
                    ?.takeIf { format == null || it.conformsTo(format) }
                    ?.takeIf { it != format }
                    ?.let { return doSniff(it, source, hints) }

            is Either.Right ->
                formatSniffer
                    .sniffContainer(format, source.value)
                    .getOrElse { return Try.failure(it) }
                    ?.takeIf { format == null || it.conformsTo(format) }
                    ?.takeIf { it != format }
                    ?.let { return doSniff(it, source, hints) }
        }

        if (source is Either.Left) {
            tryOpenArchive(format, source.value)
                .getOrElse { return Try.failure(it) }
                ?.let { return doSniff(it.format, Either.Right(it.container), hints) }
        }

        return Try.success(format)
    }

    private suspend fun tryOpenArchive(
        format: Format?,
        source: Readable
    ): Try<ContainerAsset?, ReadError> =
        if (format == null) {
            archiveOpener.sniffOpen(source)
                .tryRecover {
                    when (it) {
                        is SniffError.NotRecognized ->
                            Try.success(null)
                        is SniffError.Reading ->
                            Try.failure(it.cause)
                    }
                }
        } else {
            archiveOpener.open(format, source)
                .map { ContainerAsset(format, it) }
                .tryRecover {
                    when (it) {
                        is ArchiveOpener.OpenError.FormatNotSupported ->
                            Try.success(null)
                        is ArchiveOpener.OpenError.Reading ->
                            Try.failure(it.cause)
                    }
                }
        }
}
