/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.resource.StringResource
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

class TestContainer(resources: Map<Url, String> = emptyMap()) : Container {

    private val entries: Map<Url, Entry> =
        resources.mapValues { Entry(it.key, StringResource(it.value, MediaType.TEXT)) }

    override suspend fun entries(): Set<Container.Entry> =
        entries.values.toSet()

    override fun get(url: Url): Container.Entry =
        entries[url] ?: NotFoundEntry(url)

    override suspend fun close() {}

    private class NotFoundEntry(
        override val url: Url
    ) : Container.Entry {

        override val source: AbsoluteUrl? = null

        override suspend fun mediaType(): ResourceTry<MediaType> =
            Try.failure(Resource.Exception.NotFound())

        override suspend fun properties(): ResourceTry<Resource.Properties> =
            Try.failure(Resource.Exception.NotFound())

        override suspend fun length(): ResourceTry<Long> =
            Try.failure(Resource.Exception.NotFound())

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
            Try.failure(Resource.Exception.NotFound())

        override suspend fun close() {
        }
    }

    private class Entry(
        override val url: Url,
        private val resource: StringResource
    ) : Resource by resource, Container.Entry
}
