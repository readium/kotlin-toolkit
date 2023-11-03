/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse

/**
 * A factory to create [Container]s from archive [Resource]s.
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
            override val cause: org.readium.r2.shared.util.resource.ResourceError
        ) : Error("An error occurred while attempting to read the resource.", cause)
    }

    /**
     * Creates a new archive [Container] to access the entries of the given archive.
     */
    public suspend fun create(
        resource: Resource,
        password: String? = null
    ): Try<Container, Error>
}

public class CompositeArchiveFactory(
    private val factories: List<ArchiveFactory>
) : ArchiveFactory {

    public constructor(vararg factories: ArchiveFactory) : this(factories.toList())

    override suspend fun create(
        resource: Resource,
        password: String?
    ): Try<Container, ArchiveFactory.Error> {
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
