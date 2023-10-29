/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Error as SharedError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.tryRecover

/**
 * A factory to read [Resource]s from [Url]s.
 */
public interface ResourceFactory {

    public sealed class Error(
        override val message: String,
        override val cause: SharedError?
    ) : SharedError {

        public class SchemeNotSupported(
            public val scheme: Url.Scheme,
            cause: SharedError? = null
        ) : Error("Url scheme $scheme is not supported.", cause)
    }

    public suspend fun create(
        url: AbsoluteUrl,
        mediaType: MediaType? = null
    ): Try<Resource, Error>
}

/**
 * A factory to create [Container]s from archive [Resource]s.
 *
 */
public interface ArchiveFactory {

    public sealed class Error(
        override val message: String,
        override val cause: SharedError?
    ) : SharedError {

        public class FormatNotSupported(
            cause: SharedError? = null
        ) : Error("Archive format not supported.", cause) {

            public constructor(exception: Exception) : this(ThrowableError(exception))
        }

        public class PasswordsNotSupported(
            cause: SharedError? = null
        ) : Error("Password feature is not supported.", cause) {

            public constructor(exception: Exception) : this(ThrowableError(exception))
        }

        public class ResourceError(
            override val cause: org.readium.r2.shared.util.resource.ResourceError
        ) : Error("An error occurred while attempting to read the resource.", cause)
    }

    public data class Result(
        val mediaType: MediaType,
        val container: Container
    )

    /**
     * Creates a new archive [Container] to access the entries of the given archive.
     */
    public suspend fun create(
        resource: Resource,
        archiveType: MediaType? = null,
        password: String? = null
    ): Try<Result, Error>
}

/**
 * A composite archive factory which first tries [primaryFactory]
 * and falls back on [fallbackFactory] if it doesn't support the resource.
 */
public class CompositeArchiveFactory(
    private val primaryFactory: ArchiveFactory,
    private val fallbackFactory: ArchiveFactory
) : ArchiveFactory {

    override suspend fun create(
        resource: Resource,
        archiveType: MediaType?,
        password: String?
    ): Try<ArchiveFactory.Result, ArchiveFactory.Error> {
        return primaryFactory.create(resource, archiveType, password)
            .tryRecover { error ->
                if (
                    error is ArchiveFactory.Error.FormatNotSupported ||
                    archiveType == null && error is ArchiveFactory.Error.ResourceError &&
                    error.cause is ResourceError.InvalidContent
                ) {
                    fallbackFactory.create(resource, archiveType)
                } else {
                    Try.failure(error)
                }
            }
    }
}

/**
 * A composite resource factory which first tries [primaryFactory]
 * and falls back on [fallbackFactory] if it doesn't support the scheme.
 */
public class CompositeResourceFactory(
    private val primaryFactory: ResourceFactory,
    private val fallbackFactory: ResourceFactory
) : ResourceFactory {

    override suspend fun create(
        url: AbsoluteUrl,
        mediaType: MediaType?
    ): Try<Resource, ResourceFactory.Error> {
        return primaryFactory.create(url, mediaType)
            .tryRecover { error ->
                if (error is ResourceFactory.Error.SchemeNotSupported) {
                    fallbackFactory.create(url, mediaType)
                } else {
                    Try.failure(error)
                }
            }
    }
}
