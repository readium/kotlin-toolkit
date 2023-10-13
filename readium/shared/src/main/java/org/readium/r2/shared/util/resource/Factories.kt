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
import org.readium.r2.shared.util.tryRecover

/**
 * A factory to read [Resource]s from [Url]s.
 *
 * An exception must be returned if the url scheme is not supported or
 * the resource cannot be found.
 */
public fun interface ResourceFactory {

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

        public class NotAResource(
            public val url: AbsoluteUrl,
            override val cause: SharedError? = null
        ) : Error() {

            public constructor(url: AbsoluteUrl, exception: Exception) : this(
                url,
                ThrowableError(exception)
            )

            override val message: String =
                "No resource found at url $url."
        }

        public class Forbidden(
            override val cause: SharedError
        ) : Error() {

            public constructor(exception: Exception) : this(ThrowableError(exception))

            override val message: String =
                "Access to the container is forbidden."
        }
    }

    public suspend fun create(url: AbsoluteUrl): Try<Resource, Error>
}

/**
 * A factory to create [Container]s from [Url]s.
 *
 * An exception must be returned if the url scheme is not supported or
 * the url doesn't seem to point to a container.
 */
public fun interface ContainerFactory {

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

        public class NotAContainer(
            public val url: Url,
            override val cause: SharedError? = null
        ) : Error() {

            public constructor(url: Url, exception: Exception) : this(
                url,
                ThrowableError(exception)
            )

            override val message: String =
                "No container found at url $url."
        }

        public class Forbidden(
            override val cause: SharedError
        ) : Error() {

            public constructor(exception: Exception) : this(ThrowableError(exception))

            override val message: String =
                "Access to the container is forbidden."
        }
    }

    public suspend fun create(url: AbsoluteUrl): Try<Container, Error>
}

/**
 * A factory to create [Container]s from archive [Resource]s.
 *
 * An exception must be returned if the resource type, password or media type is not supported.
 */
public fun interface ArchiveFactory {

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

    override suspend fun create(url: AbsoluteUrl): Try<Resource, ResourceFactory.Error> {
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

    override suspend fun create(url: AbsoluteUrl): Try<Container, ContainerFactory.Error> {
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
