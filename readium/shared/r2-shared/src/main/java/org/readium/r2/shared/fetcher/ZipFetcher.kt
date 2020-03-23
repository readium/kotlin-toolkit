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
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

class ZipFetcher : Fetcher {
    private val archive: ZipFile

    private constructor(archive: ZipFile) {
        this.archive = archive
    }

    override fun get(link: Link): Resource = ZipResource(link, archive)

    override fun close() = archive.close()

    companion object {
        fun fromPath(path: String): ZipFetcher? = try {
            ZipFetcher(ZipFile(path))
        } catch (e: Exception) {
            null
        }
    }

    private class ZipResource(override val link: Link, val archive: ZipFile) : ResourceImpl() {

        override fun stream(): InputStream? {
            val entry = entryForHref(link.href)
            return try {
                archive.getInputStream(entry)
            } catch (e: ZipException) {
                null
            } catch (e: IOException) {
                null
            }
        }

        override val metadataLength: Long? by lazy {
            entryForHref(link.href)?.size?.takeIf { it != -1L }
        }

        private fun entryForHref(href: String): ZipEntry? =
            archive.getEntry(href.removePrefix("/"))
    }
}



