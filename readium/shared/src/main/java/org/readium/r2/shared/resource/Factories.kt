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
 * An exception must be returned if the url scheme is not supported.
 */
fun interface ResourceFactory {

    suspend fun create(url: Url): Try<Resource, Exception>
}

/**
 * A factory to create [Container]s from [Url]s.
 *
 * An exception must be returned if the url scheme is not supported.
 */
fun interface ContainerFactory {

    suspend fun create(url: Url): Try<Container, Exception>
}

/**
 * A factory to create [Container]s from archive [Resource]s.
 *
 * An exception must be returned if the resource type, password or media type is not supported.
 */
fun interface ArchiveFactory {

    suspend fun create(resource: Resource, password: String?): Try<Container, Exception>
}

/**
 * A composite archive factory which first tries [primaryFactory]
 * and fall backs on [fallbackFactory] in case of failure.
 */
class CompositeArchiveFactory(
    private val primaryFactory: ArchiveFactory,
    private val fallbackFactory: ArchiveFactory
) : ArchiveFactory {

    override suspend fun create(resource: Resource, password: String?): Try<Container, Exception> {
        return primaryFactory.create(resource, password)
            .tryRecover { fallbackFactory.create(resource, password) }
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

    override suspend fun create(url: Url): Try<Resource, Exception> {
        return primaryFactory.create(url)
            .tryRecover { fallbackFactory.create(url) }
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

    override suspend fun create(url: Url): Try<Container, Exception> {
        return primaryFactory.create(url)
            .tryRecover { fallbackFactory.create(url) }
    }
}
