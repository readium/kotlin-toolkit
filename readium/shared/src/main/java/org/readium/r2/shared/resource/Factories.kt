package org.readium.r2.shared.resource

import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.tryRecover

fun interface ResourceFactory {

    suspend fun create(url: Url): Try<Resource, Exception>
}

fun interface ContainerFactory {

    suspend fun create(url: Url): Try<Container, Exception>
}

fun interface ArchiveFactory {

    suspend fun create(resource: Resource, password: String?): Try<Container, Exception>
}

class CompositeArchiveFactory(
    private val primaryFactory: ArchiveFactory,
    private val fallbackFactory: ArchiveFactory
) : ArchiveFactory {

    override suspend fun create(resource: Resource, password: String?): Try<Container, Exception> {
        return primaryFactory.create(resource, password)
            .tryRecover { fallbackFactory.create(resource, password) }
    }
}

class CompositeResourceFactory(
    private val primaryFactory: ResourceFactory,
    private val fallbackFactory: ResourceFactory
) : ResourceFactory {

    override suspend fun create(url: Url): Try<Resource, Exception> {
        return primaryFactory.create(url)
            .tryRecover { fallbackFactory.create(url) }
    }
}

class CompositeContainerFactory(
    private val primaryFactory: ContainerFactory,
    private val fallbackFactory: ContainerFactory
) : ContainerFactory {

    override suspend fun create(url: Url): Try<Container, Exception> {
        return primaryFactory.create(url)
            .tryRecover { fallbackFactory.create(url) }
    }
}
