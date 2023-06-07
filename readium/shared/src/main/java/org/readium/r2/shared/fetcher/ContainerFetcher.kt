/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.resource.ArchiveFactory
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.resource.ZipContainer
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrDefault
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.use
import timber.log.Timber

/** Provides access to entries of an archive. */
class ContainerFetcher(
    private val container: Container,
    private val mediaTypeRetriever: MediaTypeRetriever
) : Fetcher {

    override suspend fun links(): List<Link> =
        tryOr(emptyList()) { container.entries() }
            ?.map { it.toLink(mediaTypeRetriever) }
            ?: emptyList()

    override fun get(link: Link): Fetcher.Resource =
        EntryResource(link, container)

    override suspend fun close() = withContext(Dispatchers.IO) {
        try {
            container.close()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    companion object {

        suspend fun create(
            resource: Resource,
            archiveFactory: ArchiveFactory,
            mediaTypeRetriever: MediaTypeRetriever
        ): Try<ContainerFetcher, Exception> =
            withContext(Dispatchers.IO) {
                archiveFactory.create(resource, password = null)
                    .map { ContainerFetcher(it, mediaTypeRetriever) }
            }
    }

    private class EntryResource(
        val originalLink: Link,
        val container: Container
    ) : Fetcher.Resource {

        suspend fun <T> withEntry(block: suspend (Container.Entry) -> ResourceTry<T>): ResourceTry<T> =
            originalLink.href
                .removePrefix("/")
                .let { href -> container.entry(href) }
                .let { entry -> entry.use { block(entry) } }
                .takeIf { result -> result.exceptionOrNull() !is Resource.Exception.NotFound }
                ?: run {
                    originalLink.href
                        .removePrefix("/")
                        .takeWhile { it !in "#?" }
                        .let { href -> container.entry(href) }
                        .let { entry -> entry.use { block(entry) } }
                }

        override suspend fun link(): Link =
            withEntry { entry ->
                val enhancedLink = (entry as? ZipContainer.Entry)
                    ?.let { originalLink.addProperties(entry.toLinkProperties()) }
                    ?: originalLink

                Try.success(enhancedLink)
            }.getOrDefault(originalLink)

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
            withEntry { entry -> entry.read(range) }

        override suspend fun length(): ResourceTry<Long> =
            metadataLength()?.let { Try.success(it) }
                ?: read().map { it.size.toLong() }

        override suspend fun close() {
        }

        private suspend fun metadataLength(): Long? =
            (withEntry { entry -> entry.length() }).getOrNull()

        override fun toString(): String =
            "${javaClass.simpleName}(${container::class.java.simpleName}, ${originalLink.href})"
    }
}

private suspend fun Container.Entry.toLink(mediaTypeRetriever: MediaTypeRetriever): Link {
    return Link(
        href = path.addPrefix("/"),
        type = mediaTypeRetriever.of(fileExtension = File(path).extension)?.toString(),
        properties = Properties((this as? ZipContainer.Entry)?.toLinkProperties().orEmpty())
    )
}

private suspend fun ZipContainer.Entry.toLinkProperties(): Map<String, Any> {
    return mutableMapOf<String, Any>(
        "archive" to mapOf<String, Any>(
            "entryLength" to (compressedLength ?: length().getOrNull() ?: 0),
            "isEntryCompressed" to (compressedLength != null)
        )
    )
}
