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

interface ContentFilter {

    val priority: Int

    val accepts: Collection<String>

    fun filter(input: InputStream, link: Link): InputStream

    fun acceptsLink(link: Link): Boolean {
        val normalizedLinkType = link.type?.let { Intent.normalizeMimeType(it) }
        return accepts
            .map {
                    val normalizedFilter =  Intent.normalizeMimeType(it)!! // null is returned only when input is null
                    MimeTypeFilter.matches(normalizedLinkType, normalizedFilter)
                }
            .any { it }
    }
}

class FilteredFetcher(val fetcher: Fetcher, val filters: Collection<ContentFilter>) : Fetcher {

    override fun fetch(link: Link): ResourceHandle? {
        val resource = fetcher.fetch(link) ?: return null
        val acceptedFilters = filters.filter { it.acceptsLink(link) }
        return FilteredHandle(link, resource, acceptedFilters)
    }

    override fun close() {
        fetcher.close()
    }
}

private class FilteredHandle(val link: Link, val resource: ResourceHandle, val filters: Collection<ContentFilter>)
    : ResourceHandle(resource.href) {

    override fun stream(): InputStream?  {
        val originalStream = resource.stream() ?: return null
        return filters.toList().fold(originalStream) { stream, filter -> filter.filter(stream, link) }
    }

    override val encoding = resource.encoding
}