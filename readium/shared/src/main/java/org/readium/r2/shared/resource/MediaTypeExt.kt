package org.readium.r2.shared.resource

import org.readium.r2.shared.util.mediatype.ContainerMediaTypeSnifferContext as BaseContainerMediaTypeSnifferContext
import org.readium.r2.shared.util.mediatype.ContentMediaTypeSnifferContext
import org.readium.r2.shared.util.mediatype.MediaTypeHints

public class ResourceMediaTypeSnifferContext(
    private val resource: Resource,
    override val hints: MediaTypeHints = MediaTypeHints()
) : ContentMediaTypeSnifferContext {

    override suspend fun read(range: LongRange?): ByteArray? =
        resource.read(range).getOrNull()

    override suspend fun close() {
        // We don't own the resource, not our responsibility to close it.
    }
}

public class ContainerMediaTypeSnifferContext(
    private val container: Container,
    override val hints: MediaTypeHints = MediaTypeHints()
) : BaseContainerMediaTypeSnifferContext {

    override suspend fun entries(): Set<String>? =
        container.entries()?.map { it.path }?.toSet()

    override suspend fun read(path: String): ByteArray? =
        container.get(path).read().getOrNull()

    override suspend fun close() {
        // We don't own the container, not our responsibility to close it.
    }
}
