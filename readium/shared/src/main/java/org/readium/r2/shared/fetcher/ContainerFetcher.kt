/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.fetcher

import java.io.File
import org.readium.r2.shared.error.Try
import org.readium.r2.shared.error.flatMap
import org.readium.r2.shared.error.getOrDefault
import org.readium.r2.shared.error.tryRecover
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.resource.ZipContainer
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.use

/** Provides access to entries of a [Container]. */
public class ContainerFetcher(
    private val container: Container,
    private val mediaTypeRetriever: MediaTypeRetriever
) : Fetcher {

    override suspend fun links(): List<Link> =
        container.entries()
            ?.map { it.toLink(mediaTypeRetriever) }
            ?: emptyList()

    override fun get(link: Link): Resource =
        EntryResource(link, container)

    override suspend fun close() {
        container.close()
    }

    private class EntryResource(
        val link: Link,
        val container: Container
    ) : Resource {

        override val url: Url? get() = Url(link.href)

        override suspend fun mediaType(): ResourceTry<MediaType?> =
            Try.success(link.mediaType)

        override suspend fun name(): ResourceTry<String?> =
            Try.success(Url(link.href)?.filename)

        override suspend fun properties(): ResourceTry<Resource.Properties> =
            withEntry { entry ->
                entry.properties().map {
                    Resource.Properties(link.properties.add(it).otherProperties)
                }
            }

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
            withEntry { entry -> entry.read(range) }

        override suspend fun length(): ResourceTry<Long> =
            metadataLength()
                .tryRecover { read().map { it.size.toLong() } }

        override suspend fun close() {
        }

        private suspend fun metadataLength(): ResourceTry<Long> =
            withEntry { entry -> entry.length() }

        suspend fun <T> withEntry(block: suspend (Container.Entry) -> ResourceTry<T>): ResourceTry<T> =
            link.href
                .let { href -> container.entry(href) }
                .let { entry -> entry.use { block(entry) } }
                .takeIf { result -> result.failureOrNull() !is Resource.Exception.NotFound }
                ?: run {
                    // Try again after removing query and fragment.
                    link.href
                        .takeWhile { it !in "#?" }
                        .let { href -> container.entry(href) }
                        .let { entry -> entry.use { block(entry) } }
                }

        override fun toString(): String =
            "${javaClass.simpleName}(${container::class.java.simpleName}, ${link.href})"
    }
}

private suspend fun Container.Entry.toLink(mediaTypeRetriever: MediaTypeRetriever): Link {
    return Link(
        href = path,
        type = mediaTypeRetriever.retrieve(fileExtension = File(path).extension)?.toString(),
        properties = properties().getOrNull()?.let { Properties(it) } ?: Properties()
    )
}
