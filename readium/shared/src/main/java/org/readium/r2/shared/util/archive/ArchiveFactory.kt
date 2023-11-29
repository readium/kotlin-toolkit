/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.archive

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource

/**
 * A factory to create [Container]s from archive [Resource]s.
 */
public interface ArchiveFactory {

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?
    ) : org.readium.r2.shared.util.Error {

        public class FormatNotSupported(
            public val mediaType: MediaType,
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("Media type not supported.", cause)

        public class Reading(
            override val cause: org.readium.r2.shared.util.data.ReadError
        ) : Error("An error occurred while attempting to read the resource.", cause)
    }

    /**
     * Creates a new [Container] to access the entries of the given archive.
     */
    public suspend fun create(
        mediaType: MediaType,
        readable: Readable
    ): Try<Container<Resource>, Error>
}

/**
 * A composite [ArchiveFactory] which tries several factories until it finds one which supports
 * the format.
*/
public class CompositeArchiveFactory(
    private val factories: List<ArchiveFactory>
) : ArchiveFactory {

    public constructor(vararg factories: ArchiveFactory) :
        this(factories.toList())

    override suspend fun create(
        mediaType: MediaType,
        readable: Readable
    ): Try<Container<Resource>, ArchiveFactory.Error> {
        for (factory in factories) {
            factory.create(mediaType, readable)
                .getOrElse { error ->
                    when (error) {
                        is ArchiveFactory.Error.FormatNotSupported -> null
                        else -> return Try.failure(error)
                    }
                }
                ?.let { return Try.success(it) }
        }

        return Try.failure(ArchiveFactory.Error.FormatNotSupported(mediaType))
    }
}
