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
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Provides access to entries of an archive. */
class ArchiveFetcher private constructor(private val archive: ZipFile) : Fetcher {

    override suspend fun links(): List<Link> =
        archive.entries().toList().mapNotNull {
            Link(
                href = it.name.addPrefix("/"),
                type = Format.of(fileExtension = File(it.name).extension)?.mediaType?.toString()
            )
        }

    override fun get(link: Link): Resource =
        ZipResource(link, archive)

    override suspend fun close() = withContext(Dispatchers.IO) { archive.close() }

    companion object {
        suspend fun fromPath(path: String): ArchiveFetcher? = try {
            val zipFile = withContext(Dispatchers.IO) {
                ZipFile(path)
            }
            ArchiveFetcher(zipFile)
        } catch (e: Exception) {
            null
        }
    }

    private class ZipResource(val originalLink: Link, val archive: ZipFile) : StreamResource() {

        override fun stream(): ResourceTry<InputStream> {
            return if (entry == null)
                Try.failure(Resource.Error.NotFound)
            else
                Try.success(archive.getInputStream(entry))
        }

        override suspend fun link(): Link =
            // Adds the compressed length to the original link.
            entry?.compressedSize?.takeIf { it != -1L }
                ?.let { originalLink.addProperties(mapOf("compressedLength" to it)) }
                ?: originalLink

        override val metadataLength: Long? by lazy {
            entry?.size?.takeIf { it != -1L }
        }

        override suspend fun close() {}

        private val entry: ZipEntry? by lazy {
            archive.getEntry(originalLink.href.removePrefix("/"))
        }

    }

}



