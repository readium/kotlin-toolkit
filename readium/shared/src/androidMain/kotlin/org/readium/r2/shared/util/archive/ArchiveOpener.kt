/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.archive

import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.resource.Resource

/**
 * A factory to create [Container]s from archive [Resource]s.
 */
public interface ArchiveOpener {

    public sealed class OpenError(
        override val message: String,
        override val cause: Error?,
    ) : Error {

        public class FormatNotSupported(
            public val format: Format,
            cause: Error? = null,
        ) : OpenError("Format not supported.", cause)

        public class Reading(
            override val cause: ReadError,
        ) : OpenError("An error occurred while attempting to read the resource.", cause)
    }

    public sealed class SniffOpenError(
        override val message: String,
        override val cause: Error?,
    ) : Error {

        public data object NotRecognized :
            SniffOpenError("Format of resource could not be inferred.", null)

        public data class Reading(override val cause: ReadError) :
            SniffOpenError("An error occurred while trying to read content.", cause)
    }

    /**
     * Creates a new [Container] to access the entries of an archive with a known format.
     */
    public suspend fun open(
        format: Format,
        source: Readable,
    ): Try<ContainerAsset, OpenError>

    /**
     * Creates a new [ContainerAsset] to access the entries of an archive after sniffing its format.
     */
    public suspend fun sniffOpen(
        source: Readable,
    ): Try<ContainerAsset, SniffOpenError>
}

/**
 * A composite [ArchiveOpener] which tries several factories until it finds one which supports
 * the format.
*/
public class CompositeArchiveOpener(
    private val openers: List<ArchiveOpener>,
) : ArchiveOpener {

    public constructor(vararg factories: ArchiveOpener) :
        this(factories.toList())

    override suspend fun open(
        format: Format,
        source: Readable,
    ): Try<ContainerAsset, ArchiveOpener.OpenError> {
        for (factory in openers) {
            factory.open(format, source)
                .getOrElse { error ->
                    when (error) {
                        is ArchiveOpener.OpenError.FormatNotSupported -> null
                        else -> return Try.failure(error)
                    }
                }
                ?.let { return Try.success(it) }
        }

        return Try.failure(ArchiveOpener.OpenError.FormatNotSupported(format))
    }

    override suspend fun sniffOpen(
        source: Readable,
    ): Try<ContainerAsset, ArchiveOpener.SniffOpenError> {
        for (factory in openers) {
            factory.sniffOpen(source)
                .getOrElse { error ->
                    when (error) {
                        is ArchiveOpener.SniffOpenError.NotRecognized -> null
                        else -> return Try.failure(error)
                    }
                }
                ?.let { return Try.success(it) }
        }

        return Try.failure(ArchiveOpener.SniffOpenError.NotRecognized)
    }
}
