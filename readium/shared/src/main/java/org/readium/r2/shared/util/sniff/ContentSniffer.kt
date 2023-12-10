/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.sniff

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.getOrElse
import timber.log.Timber

/**
 * Tries to refine a [Format] from media type and file extension hints.
 */
public interface FormatHintsSniffer {

    public fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format?
}

/**
 * Tries to refine a [Format] by sniffing a [Readable] blob.
 */
public interface BlobSniffer {

    public suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError>
}

/**
 * Tries to Refine a [Format] by sniffing a [Container].
 */
public interface ContainerSniffer {

    public suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError>
}

public interface ContentSniffer :
    FormatHintsSniffer,
    BlobSniffer,
    ContainerSniffer {

    public override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? =
        null

    public override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> =
        Try.success(format)
        

    public override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> =
        Try.success(format)
}

public class CompositeContentSniffer(
    private val sniffers: List<ContentSniffer>
) : ContentSniffer {

    override fun sniffHints(
        format: Format?,
        hints: FormatHints
    ): Format? {
        for (sniffer in sniffers) {
            Timber.d("Trying hints ${sniffer.javaClass.simpleName}")
            sniffer.sniffHints(format, hints)
                .takeIf { it != format }
                ?.let { return it }
        }

        return format
    }

    override suspend fun sniffBlob(
        format: Format?,
        source: Readable
    ): Try<Format?, ReadError> {
        for (sniffer in sniffers) {
            Timber.d("Trying blob ${sniffer.javaClass.simpleName}")
            sniffer.sniffBlob(format, source)
                .getOrElse { return Try.failure(it) }
                .takeIf { it != format }
                ?.let { return Try.success(it) }
        }

        return Try.success(format)
    }

    override suspend fun sniffContainer(
        format: Format?,
        container: Container<Readable>
    ): Try<Format?, ReadError> {
        for (sniffer in sniffers) {
            Timber.d("Trying container ${sniffer.javaClass.simpleName}")
            sniffer.sniffContainer(format, container)
                .getOrElse { return Try.failure(it) }
                .takeIf { it != format }
                ?.let { return Try.success(it) }
        }

        return Try.success(format)
    }
}
