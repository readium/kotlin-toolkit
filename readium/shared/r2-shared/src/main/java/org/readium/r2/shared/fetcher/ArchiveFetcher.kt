/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Provides access to entries of an archive. */
class ArchiveFetcher private constructor(private val archive: ZipFile) : Fetcher {

    override val links: List<Link>
        get() = archive.entries().toList().mapNotNull {
            Link(href = it.name)
        }

    override fun get(link: Link, parameters: HrefParameters): Resource = ZipResource(link, archive)

    override fun close() = archive.close()

    companion object {
        fun fromPath(path: String): ArchiveFetcher? = try {
            ArchiveFetcher(ZipFile(path))
        } catch (e: Exception) {
            null
        }
    }

    private class ZipResource(override val link: Link, val archive: ZipFile) : StreamResource() {

        override fun stream(): Try<InputStream, Resource.Error> {
            val entry = entryForHref(link.href)
            return if (entry == null)
                Try.failure(Resource.Error.NotFound)
            else
                Try.success(archive.getInputStream(entry))
        }

        override val metadataLength: Long? by lazy {
            entryForHref(link.href)?.size?.takeIf { it != -1L }
        }

        override fun close() {}

        private fun entryForHref(href: String): ZipEntry? =
            archive.getEntry(href.removePrefix("/"))
    }
}



