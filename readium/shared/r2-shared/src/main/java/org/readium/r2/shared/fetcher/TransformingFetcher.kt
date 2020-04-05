/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import android.content.Intent
import androidx.core.content.MimeTypeFilter
import org.readium.r2.shared.publication.Link
import java.io.InputStream

interface ResourceTransformer {

    val priority: Int

    fun transform(resource: Resource): Resource
}

class ResourceTransformerChain(val transformers: Collection<ResourceTransformer>, override val priority: Int)
    : ResourceTransformer {

    override fun transform(resource: Resource): Resource =
        transformers
            .toList().sortedByDescending(ResourceTransformer::priority)
            .fold(resource) { acc, filter -> filter.transform(acc) }

}

class TransformingFetcher(val fetcher: Fetcher, val transformerChain: ResourceTransformerChain) : Fetcher {

    constructor(fetcher: Fetcher, transformers: Collection<ResourceTransformer>)
            : this(fetcher, ResourceTransformerChain(transformers, 0))

    constructor(fetcher: Fetcher, transformer: ResourceTransformer)
            : this(fetcher, listOf(transformer))

    override fun get(link: Link): Resource {
        val resource = fetcher.get(link)
        return transformerChain.transform(resource)
    }

    override fun close() {
        fetcher.close()
    }
}

internal class BytesResourceTransformer(
    override val priority: Int,
    val accepts: Collection<String>,
    val byteFilter: (ByteArray) -> ByteArray
) : ResourceTransformer {

    override fun transform(resource: Resource): Resource =
        object : Resource by resource {
            override fun stream(): InputStream? = bytes?.inputStream()

            override val bytes: ByteArray? by lazy {
                resource.bytes?.let { byteFilter(it) }
            }

            override val length: Long? = bytes?.size?.toLong()
        }

    fun accepts(resource: Resource): Boolean {
        if (accepts.isEmpty()) return true

        val normalizedLinkType = resource.link.type?.let { Intent.normalizeMimeType(it) }
        return accepts
            .map {
                val normalizedFilter = Intent.normalizeMimeType(it)!! // null is returned only when input is null
                MimeTypeFilter.matches(normalizedLinkType, normalizedFilter)
            }
            .any { it }
    }
}