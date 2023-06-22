/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.protection

import java.io.File
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.resource.StringResource
import org.readium.r2.shared.util.Try

class TestContainer(resources: Map<String, String> = emptyMap()) : Container {

    private val entries: Map<String, Entry> =
        resources.mapValues { Entry(it.key, StringResource(it.value)) }

    override suspend fun name(): ResourceTry<String?> =
        Try.success(null)

    override suspend fun entries(): Iterable<Container.Entry> =
        entries.values

    override suspend fun entry(path: String): Container.Entry =
        entries[path] ?: NotFoundEntry(path)

    override suspend fun close() {}

    private class NotFoundEntry(
        override val path: String
    ) : Container.Entry {

        override suspend fun name(): ResourceTry<String?> =
            ResourceTry.success(File(path).name)

        override suspend fun length(): ResourceTry<Long> =
            Try.failure(Resource.Exception.NotFound())

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
            Try.failure(Resource.Exception.NotFound())

        override suspend fun close() {
        }
    }

    private class Entry(
        override val path: String,
        private val resource: StringResource
    ) : Resource by resource, Container.Entry
}
