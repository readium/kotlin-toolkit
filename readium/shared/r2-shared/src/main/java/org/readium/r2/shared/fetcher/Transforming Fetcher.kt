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

    val accepts: Collection<String>

    fun transform(resource: Resource): Resource

    fun accepts(resource: Resource): Boolean {
        if (accepts.isEmpty()) return true

        val normalizedLinkType = resource.link.type?.let { Intent.normalizeMimeType(it) }
        return accepts
            .map {
                    val normalizedFilter =  Intent.normalizeMimeType(it)!! // null is returned only when input is null
                    MimeTypeFilter.matches(normalizedLinkType, normalizedFilter)
                }
            .any { it }
    }
}

class TransformingFetcher(val fetcher: Fetcher, val filters: Collection<ResourceTransformer>) : Fetcher {

    override fun get(link: Link): Resource {
        val resource = fetcher.get(link)
        val acceptedFilters = filters
            .filter { it.accepts(resource) }
            .toList().sortedByDescending(ResourceTransformer::priority)
        return acceptedFilters.fold(resource) { acc, filter -> filter.transform(acc) }
    }

    override fun close() {
        fetcher.close()
    }
}

class BytesResourceTransformer(override val priority: Int, override val accepts: Collection<String>, val byteFilter: (ByteArray) -> ByteArray) : ResourceTransformer {
    override fun transform(resource: Resource): Resource =
       object : Resource by resource {
           override fun stream(): InputStream? = bytes?.inputStream()

           override val bytes: ByteArray? by lazy {
               resource.bytes?.let { byteFilter(it) }
           }

           override val length: Long? = bytes?.size?.toLong()
       }
}