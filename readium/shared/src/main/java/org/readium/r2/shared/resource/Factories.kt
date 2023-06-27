/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.tryRecover

/**
 * A factory to read [Resource]s from [Url]s.
 *
 * An exception must be returned if the url scheme is not supported or
 * the resource cannot be found.
 */
fun interface ResourceFactory {

    sealed class Error {

        class UnsupportedScheme(val scheme: String) : Error()

        class NotAResource(val url: Url) : Error()

        class ResourceError(val exception: Resource.Exception) : Error()
    }

    suspend fun create(url: Url): Try<Resource, Error>
}

/**
 * A factory to create [Container]s from [Url]s.
 *
 * An exception must be returned if the url scheme is not supported or
 * the url doesn't seem to point to a container.
 */
fun interface ContainerFactory {

    sealed class Error {

        class UnsupportedScheme(val scheme: String) : Error()

        class NotAContainer(val url: Url) : Error()

        class Forbidden(val exception: Exception) : Error()
    }

    suspend fun create(url: Url): Try<Container, Error>
}

/**
 * A factory to create [Container]s from archive [Resource]s.
 *
 * An exception must be returned if the resource type, password or media type is not supported.
 */
fun interface ArchiveFactory {

    sealed class Error {

        object ResourceNotSupported : Error()

        object FormatNotSupported : Error()

        object PasswordsNotSupported : Error()

        class ResourceError(val error: Resource.Exception) : Error()
    }

    suspend fun create(resource: Resource, password: String?): Try<Container, Error>
}

/**
 * A composite archive factory which first tries [primaryFactory]
 * and fall backs on [fallbackFactory] in case of failure.
 */
class CompositeArchiveFactory(
    private val primaryFactory: ArchiveFactory,
    private val fallbackFactory: ArchiveFactory
) : ArchiveFactory {

    override suspend fun create(resource: Resource, password: String?): Try<Container, ArchiveFactory.Error> {
        return primaryFactory.create(resource, password)
            .tryRecover { error ->
                if (error !is ArchiveFactory.Error.ResourceError) fallbackFactory.create(resource, password)
                else Try.failure(error)
            }
    }
}

/**
 * A composite resource factory which first tries [primaryFactory]
 * and fall backs on [fallbackFactory] in case of failure.
 */
class CompositeResourceFactory(
    private val primaryFactory: ResourceFactory,
    private val fallbackFactory: ResourceFactory
) : ResourceFactory {

    override suspend fun create(url: Url): Try<Resource, ResourceFactory.Error> {
        return primaryFactory.create(url)
            .tryRecover { error ->
                if (error is ResourceFactory.Error.UnsupportedScheme) fallbackFactory.create(url)
                else Try.failure(error)
            }
    }
}

/**
 * A composite container factory which first tries [primaryFactory]
 * and fall backs on [fallbackFactory] in case of failure.
 */
class CompositeContainerFactory(
    private val primaryFactory: ContainerFactory,
    private val fallbackFactory: ContainerFactory
) : ContainerFactory {

    override suspend fun create(url: Url): Try<Container, ContainerFactory.Error> {
        return primaryFactory.create(url)
            .tryRecover { error ->
                if (error is ContainerFactory.Error.UnsupportedScheme) fallbackFactory.create(url)
                else Try.failure(error)
            }
    }
}
