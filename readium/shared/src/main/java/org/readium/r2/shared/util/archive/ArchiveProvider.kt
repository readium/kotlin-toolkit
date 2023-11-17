/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.archive

import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaTypeSniffer
import org.readium.r2.shared.util.resource.ResourceContainer
import org.readium.r2.shared.util.resource.Resource

public interface ArchiveProvider : MediaTypeSniffer, ArchiveFactory

/**
 * A factory to create a [ResourceContainer]s from archive [Blob]s.
 *
 */
public interface ArchiveFactory {

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?
    ) : org.readium.r2.shared.util.Error {

        public class PasswordsNotSupported(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("Password feature is not supported.", cause) {

            public constructor(exception: Exception) : this(ThrowableError(exception))
        }

        public class UnsupportedFormat(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("Resource is not supported.", cause)

        public class ResourceError(
            override val cause: ReadError
        ) : Error("An error occurred while attempting to read the resource.", cause)
    }

    /**
     * Creates a new archive [ResourceContainer] to access the entries of the given archive.
     */
    public suspend fun create(
        resource: Blob,
        password: String? = null
    ): Try<Container<Resource>, Error>
}

public class CompositeArchiveFactory(
    private val factories: List<ArchiveFactory>
) : ArchiveFactory {

    public constructor(vararg factories: ArchiveFactory) : this(factories.toList())

    override suspend fun create(
        resource: Blob,
        password: String?
    ): Try<Container<Resource>, ArchiveFactory.Error> {
        for (factory in factories) {
            factory.create(resource, password)
                .getOrElse { error ->
                    when (error) {
                        is ArchiveFactory.Error.UnsupportedFormat -> null
                        else -> return Try.failure(error)
                    }
                }
                ?.let { return Try.success(it) }
        }

        return Try.failure(ArchiveFactory.Error.UnsupportedFormat())
    }
}
