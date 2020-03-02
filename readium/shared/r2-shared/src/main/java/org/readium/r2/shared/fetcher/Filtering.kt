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

    fun filter(resource: ResourceHandle, link: Link): ResourceHandle

    fun acceptsLink(resource: ResourceHandle): Boolean {
        val normalizedLinkType = resource.mimeType?.let { Intent.normalizeMimeType(it) }
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
        val acceptedFilters = filters.filter { it.acceptsLink(resource) }
        return FilteredHandle(link, resource, acceptedFilters)
    }

    override fun close() {
        fetcher.close()
    }
}

private class FilteredHandle(link: Link, val resource: ResourceHandle, val filters: Collection<ContentFilter>)
    : ResourceHandle(link) {

    private val filteredResource: ResourceHandle? by lazy {
        filters.toList().fold(resource) { acc, filter -> filter.filter(acc, link) }
    }

    override fun stream(): InputStream? = filteredResource?.stream()

    override val encoding =  filteredResource?.encoding

    override val mimeType: String? = filteredResource?.mimeType

    override val metadataLength: Long? = filteredResource?.metadataLength

}