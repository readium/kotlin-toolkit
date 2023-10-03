package org.readium.r2.shared.resource

import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrDefault
import org.readium.r2.shared.util.mediatype.ContainerMediaTypeSnifferContent
import org.readium.r2.shared.util.mediatype.ResourceMediaTypeSnifferContent

public class ResourceMediaTypeSnifferContent(
    private val resource: Resource
) : ResourceMediaTypeSnifferContent {

    override suspend fun read(range: LongRange?): ByteArray? =
        resource.safeRead(range)
}

public class ContainerMediaTypeSnifferContent(
    private val container: Container
) : ContainerMediaTypeSnifferContent {

    override suspend fun entries(): Set<String>? =
        container.entries()?.mapNotNull { it.url.path }?.toSet()

    override suspend fun read(path: String, range: LongRange?): ByteArray? =
        Url.fromDecodedPath(path)?.let { url ->
            container.get(url).safeRead(range)
        }
}

private suspend fun Resource.safeRead(range: LongRange?): ByteArray? {
    try {
        // We only read files smaller than 5MB to avoid an [OutOfMemoryError].
        if (range == null && length().getOrDefault(0) > 5 * 1000 * 1000) {
            return null
        }
        return read(range).getOrNull()
    } catch (e: OutOfMemoryError) {
        return null
    }
}
