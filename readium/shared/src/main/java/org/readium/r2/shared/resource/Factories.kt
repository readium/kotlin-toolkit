/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import org.readium.r2.shared.error.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.tryRecover

/**
 * A factory to read [Resource]s from [Url]s.
 *
 * An exception must be returned if the url scheme is not supported or
 * the resource cannot be found.
 */
public fun interface ResourceFactory {

    public sealed class Error : org.readium.r2.shared.error.Error {

        public class SchemeNotSupported(
            public val scheme: String,
            override val cause: org.readium.r2.shared.error.Error? = null
        ) : Error() {

            public constructor(scheme: String, exception: Exception) : this(
                scheme,
                ThrowableError(exception)
            )

            override val message: String =
                "Url scheme $scheme is not supported."
        }

        public class NotAResource(
            public val url: Url,
            override val cause: org.readium.r2.shared.error.Error? = null
        ) : Error() {

            public constructor(url: Url, exception: Exception) : this(
                url,
                ThrowableError(exception)
            )

            override val message: String =
                "No resource found at url $url."
        }

        public class Forbidden(
            override val cause: org.readium.r2.shared.error.Error
        ) : Error() {

            public constructor(exception: Exception) : this(ThrowableError(exception))

            override val message: String =
                "Access to the container is forbidden."
        }
    }

    public suspend fun create(url: Url): Try<Resource, Error>
}

/**
 * A factory to create [Container]s from [Url]s.
 *
 * An exception must be returned if the url scheme is not supported or
 * the url doesn't seem to point to a container.
 */
public fun interface ContainerFactory {

    public sealed class Error : org.readium.r2.shared.error.Error {

        public class SchemeNotSupported(
            public val scheme: String,
            override val cause: org.readium.r2.shared.error.Error? = null
        ) : Error() {

            public constructor(scheme: String, exception: Exception) : this(
                scheme,
                ThrowableError(exception)
            )

            override val message: String =
                "Url scheme $scheme is not supported."
        }

        public class NotAContainer(
            public val url: Url,
            override val cause: org.readium.r2.shared.error.Error? = null
        ) : Error() {

            public constructor(url: Url, exception: Exception) : this(
                url,
                ThrowableError(exception)
            )

            override val message: String =
                "No container found at url $url."
        }

        public class Forbidden(
            override val cause: org.readium.r2.shared.error.Error
        ) : Error() {

            public constructor(exception: Exception) : this(ThrowableError(exception))

            override val message: String =
                "Access to the container is forbidden."
        }
    }

    public suspend fun create(url: Url): Try<Container, Error>
}

/**
 * A factory to create [Container]s from archive [Resource]s.
 *
 * An exception must be returned if the resource type, password or media type is not supported.
 */
public fun interface ArchiveFactory {

    public sealed class Error : org.readium.r2.shared.error.Error {

        public class FormatNotSupported(
            override val cause: org.readium.r2.shared.error.Error? = null
        ) : Error() {

            public constructor(exception: Exception) : this(ThrowableError(exception))

            override val message: String =
                "Archive format not supported."
        }

        public class PasswordsNotSupported(
            override val cause: org.readium.r2.shared.error.Error? = null
        ) : Error() {

            public constructor(exception: Exception) : this(ThrowableError(exception))

            override val message: String =
                "Password feature is not supported."
        }

        public class ResourceReading(
            override val cause: org.readium.r2.shared.error.Error?,
            public val resourceException: Resource.Exception
        ) : Error() {

            public constructor(exception: Resource.Exception) : this(
                ThrowableError(exception),
                exception
            )

            override val message: String =
                "An error occurred while attempting to read the resource."
        }
    }

    public suspend fun create(resource: Resource, password: String?): Try<Container, Error>
}

/**
 * A composite archive factory which first tries [primaryFactory]
 * and falls back on [fallbackFactory] if it doesn't support the resource.
 */
public class CompositeArchiveFactory(
    private val primaryFactory: ArchiveFactory,
    private val fallbackFactory: ArchiveFactory
) : ArchiveFactory {

    override suspend fun create(resource: Resource, password: String?): Try<Container, ArchiveFactory.Error> {
        return primaryFactory.create(resource, password)
            .tryRecover { error ->
                if (error is ArchiveFactory.Error.FormatNotSupported) {
                    fallbackFactory.create(resource, password)
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

    override suspend fun create(url: Url): Try<Resource, ResourceFactory.Error> {
        return primaryFactory.create(url)
            .tryRecover { error ->
                if (error is ResourceFactory.Error.SchemeNotSupported) {
                    fallbackFactory.create(url)
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

    override suspend fun create(url: Url): Try<Container, ContainerFactory.Error> {
        return primaryFactory.create(url)
            .tryRecover { error ->
                if (error is ContainerFactory.Error.SchemeNotSupported) {
                    fallbackFactory.create(url)
                } else {
                    Try.failure(error)
                }
            }
    }
}
