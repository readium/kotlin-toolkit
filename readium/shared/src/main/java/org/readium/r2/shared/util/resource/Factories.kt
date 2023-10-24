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
 *
 * An exception must be returned if the url scheme is not supported or
 * the resource cannot be found.
 */
public interface ResourceFactory {

    public sealed class Error : SharedError {

        public class SchemeNotSupported(
            public val scheme: Url.Scheme,
            override val cause: SharedError? = null
        ) : Error() {

            public constructor(scheme: Url.Scheme, exception: Exception) : this(
                scheme,
                ThrowableError(exception)
            )

            override val message: String =
                "Url scheme $scheme is not supported."
        }
    }

    public suspend fun create(url: AbsoluteUrl): Resource?

    public suspend fun create(url: AbsoluteUrl, mediaType: MediaType): Try<Resource, Error>
}

/**
 * A factory to create [Container]s from [Url]s.
 *
 * An exception must be returned if the url scheme is not supported or
 * the url doesn't seem to point to a container.
 */
public interface ContainerFactory {

    public sealed class Error : SharedError {

        public class SchemeNotSupported(
            public val scheme: Url.Scheme,
            override val cause: SharedError? = null
        ) : Error() {

            public constructor(scheme: Url.Scheme, exception: Exception) : this(
                scheme,
                ThrowableError(exception)
            )

            override val message: String =
                "Url scheme $scheme is not supported."
        }
    }

    /**
     * Returns a [Container] to access the content if this factory claims that [url] points to
     * a resource it can provide access to and null otherwise.
     */
    public suspend fun create(url: AbsoluteUrl): Container?

    /**
     * Tries to create a [Container] giving access to a [Url] known to point to a directory
     * with the given [mediaType].
     *
     * An error must be returned if the url scheme or media type is not supported.
     */
    public suspend fun create(url: AbsoluteUrl, mediaType: MediaType): Try<Container, Error>
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

        public class ResourceReading(
            cause: SharedError?,
            public val resourceException: Resource.Exception
        ) : Error("An error occurred while attempting to read the resource.", cause) {

            public constructor(exception: Resource.Exception) : this(
                ThrowableError(exception),
                exception
            )
        }
    }

    /**
     * Returns a [Container] to access the archive content if this factory claims that [resource] is
     * an archive that it supports and null otherwise.
     */
    public suspend fun create(resource: Resource, password: String?): Container?

    /**
     * Tries to create a [Container] from a [Resource] known to be an archive
     * with the given [mediaType].
     *
     * An error must be returned if the resource type, password or media type is not supported.
     */
    public suspend fun create(resource: Resource, password: String?, mediaType: MediaType): Try<Container, Error>
}

/**
 * A composite archive factory which first tries [primaryFactory]
 * and falls back on [fallbackFactory] if it doesn't support the resource.
 */
public class CompositeArchiveFactory(
    private val primaryFactory: ArchiveFactory,
    private val fallbackFactory: ArchiveFactory
) : ArchiveFactory {

    override suspend fun create(resource: Resource, password: String?): Container? {
        return primaryFactory.create(resource, password)
            ?: fallbackFactory.create(resource, password)
    }

    override suspend fun create(resource: Resource, password: String?, mediaType: MediaType): Try<Container, ArchiveFactory.Error> {
        return primaryFactory.create(resource, password, mediaType)
            .tryRecover { error ->
                if (error is ArchiveFactory.Error.FormatNotSupported) {
                    fallbackFactory.create(resource, password, mediaType)
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

    override suspend fun create(url: AbsoluteUrl): Resource? {
        return primaryFactory.create(url) ?: fallbackFactory.create(url)
    }

    override suspend fun create(url: AbsoluteUrl, mediaType: MediaType): Try<Resource, ResourceFactory.Error> {
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

/**
 * A composite container factory which first tries [primaryFactory]
 * and falls back on [fallbackFactory] if it doesn't support the scheme.
 */
public class CompositeContainerFactory(
    private val primaryFactory: ContainerFactory,
    private val fallbackFactory: ContainerFactory
) : ContainerFactory {

    override suspend fun create(url: AbsoluteUrl): Container? {
        return primaryFactory.create(url) ?: fallbackFactory.create(url)
    }

    override suspend fun create(url: AbsoluteUrl, mediaType: MediaType): Try<Container, ContainerFactory.Error> {
        return primaryFactory.create(url, mediaType)
            .tryRecover { error ->
                if (error is ContainerFactory.Error.SchemeNotSupported) {
                    fallbackFactory.create(url, mediaType)
                } else {
                    Try.failure(error)
                }
            }
    }
}
