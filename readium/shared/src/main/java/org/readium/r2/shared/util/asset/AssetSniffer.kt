/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.asset

import java.io.File
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.file.FileResource
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.borrow
import org.readium.r2.shared.util.sniff.ContentSniffer
import org.readium.r2.shared.util.sniff.FormatHints
import org.readium.r2.shared.util.tryRecover
import org.readium.r2.shared.util.use

public class AssetSniffer(
    private val contentSniffer: ContentSniffer,
    private val archiveOpener: ArchiveOpener
) {
    public suspend fun sniffOpen(
        source: Resource,
        hints: FormatHints = FormatHints()
    ): Try<Asset, SniffError> =
        sniff(null, Either.Left(source), hints)

    public suspend fun sniffOpen(file: File, hints: FormatHints): Try<Asset, SniffError> =
        sniff(null, Either.Left(FileResource(file)), hints)

    public suspend fun sniff(
        source: Resource,
        hints: FormatHints = FormatHints()
    ): Try<Format, SniffError> =
        sniffOpen(source.borrow(), hints).map { it.format }

    public suspend fun sniff(file: File, hints: FormatHints): Try<Format, SniffError> =
        FileResource(file).use { sniff(it, hints) }

    public suspend fun sniff(
        container: Container<Resource>,
        hints: FormatHints = FormatHints()
    ): Try<Format, SniffError> =
        sniff(null, Either.Right(container), hints).map { it.format }

    private suspend fun sniff(
        format: Format?,
        source: Either<Resource, Container<Resource>>,
        hints: FormatHints
    ): Try<Asset, SniffError> {
        contentSniffer.sniffHints(format, hints)
            ?.takeIf { format == null || it.conformsTo(format) }
            ?.takeIf { it != format}
            ?.let { return sniff(it, source, hints) }

        when (source) {
            is Either.Left ->
                contentSniffer.sniffBlob(format, source.value)
            is Either.Right ->
                contentSniffer.sniffContainer(format, source.value)
        }
            .getOrElse { return Try.failure(SniffError.Reading(it)) }
            ?.takeIf { format == null || it.conformsTo(format) }
            ?.takeIf { it != format }
            ?.let { return sniff(it, source, hints) }

        if (source is Either.Left) {
            tryOpenArchive(format, source.value)
                .getOrElse { return Try.failure(SniffError.Reading(it)) }
                ?.let { return sniff(it.format, Either.Right(it.container), hints) }
        }

        format?.let {
            val asset = when (source) {
                is Either.Left -> ResourceAsset(it, source.value)
                is Either.Right -> ContainerAsset(it, source.value)
            }
            return Try.success(asset)
        }

        return Try.failure(SniffError.NotRecognized)
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
