/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ClosedContainer
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ContainerEntry
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.mediatype.MediaType

public typealias ResourceTry<SuccessT> = Try<SuccessT, ReadError>

public interface ResourceEntry : ContainerEntry, Resource

public typealias ResourceContainer = Container<ResourceEntry>

public class FailureResourceEntry(
    override val url: Url,
    private val error: ReadError
) : ResourceEntry {

    override val source: AbsoluteUrl? = null

    override suspend fun mediaType(): ResourceTry<MediaType> =
        Try.failure(error)

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        Try.failure(error)

    override suspend fun length(): ResourceTry<Long> =
        Try.failure(error)

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
        Try.failure(error)

    override suspend fun close() {
    }
}

/** A [Container] for a single [Resource]. */
public class SingleResourceContainer(
    private val url: Url,
    resource: Resource
) : ClosedContainer<ResourceEntry> {
    public interface Entry : ResourceEntry

    private val entry = resource.toResourceEntry(url)

    override suspend fun entries(): Set<Url> = setOf(url)

    override fun get(url: Url): ResourceEntry? {
        if (url.removeFragment().removeQuery() != entry.url) {
            return null
        }

        return entry
    }

    override suspend fun close() {
        entry.close()
    }
}

public class DelegatingResourceEntry(
    override val url: Url,
    private val resource: Resource
) : ResourceEntry, Resource by resource

/** Convenience helper to wrap a [Resource] and a [url] into a [Container.Entry]. */
internal fun Resource.toResourceEntry(url: Url): ResourceEntry =
    DelegatingResourceEntry(url, this)
