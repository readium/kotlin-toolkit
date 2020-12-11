/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.Archive
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.DefaultArchiveFactory
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber
import java.io.File

/** Provides access to entries of an archive. */
class ArchiveFetcher private constructor(private val archive: Archive) : Fetcher {

    override suspend fun links(): List<Link> =
        tryOr(emptyList()) { archive.entries() }
            .map { it.toLink() }

    override fun get(link: Link): Resource =
        EntryResource(link, archive)

    override suspend fun close() = withContext(Dispatchers.IO) {
        try {
            archive.close()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    companion object {

        suspend fun fromPath(path: String, archiveFactory: ArchiveFactory = DefaultArchiveFactory()): ArchiveFetcher? =
            withContext(Dispatchers.IO) {
                tryOrNull { ArchiveFetcher(archiveFactory.open(File(path), password = null)) }
            }
    }

    private class EntryResource(val originalLink: Link, val archive: Archive) : Resource {

        private lateinit var _entry: ResourceTry<Archive.Entry>

        suspend fun entry(): ResourceTry<Archive.Entry> {
            if (!::_entry.isInitialized) {
                _entry = try {
                    val entry = archive.entry(originalLink.href.removePrefix("/"))
                    Try.success(entry)
                } catch (e: Exception) {
                    Try.failure(Resource.Exception.NotFound)
                }
            }

            return _entry
        }

        override suspend fun link(): Link {
            val compressedLength = entry().map { it.compressedLength }.getOrNull()
                ?: return originalLink

            return originalLink.addProperties(mapOf("compressedLength" to compressedLength))
        }

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
            entry().mapCatching {
                it.read(range)
            }

        override suspend fun length(): ResourceTry<Long>  =
            metadataLength()?.let { Try.success(it) }
                ?: read().map { it.size.toLong() }

        override suspend fun close() {}

        private suspend fun metadataLength(): Long? =
            entry().getOrNull()?.length

        override fun toString(): String =
            "${javaClass.simpleName}(${archive::class.java.simpleName}, ${originalLink.href})"

    }
}

private suspend fun Archive.Entry.toLink(): Link {
    val link = Link(
        href = path.addPrefix("/"),
        type = MediaType.of(fileExtension = File(path).extension)?.toString()
    )

    return compressedLength?.let { link.addProperties(mapOf("compressedLength" to it)) }
        ?: link
}
