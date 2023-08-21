package org.readium.r2.shared.resource

import org.readium.r2.shared.util.mediatype.ContainerMediaTypeSnifferContent
import org.readium.r2.shared.util.mediatype.ResourceMediaTypeSnifferContent

public class ResourceMediaTypeSnifferContent(
    private val resource: Resource
) : ResourceMediaTypeSnifferContent {

    override suspend fun read(range: LongRange?): ByteArray? =
        resource.read(range).getOrNull()
}

public class ContainerMediaTypeSnifferContent(
    private val container: Container
) : ContainerMediaTypeSnifferContent {

    override suspend fun entries(): Set<String>? =
        container.entries()?.map { it.path }?.toSet()

    override suspend fun read(path: String, range: LongRange?): ByteArray? =
        container.get(path).read(range).getOrNull()
}
