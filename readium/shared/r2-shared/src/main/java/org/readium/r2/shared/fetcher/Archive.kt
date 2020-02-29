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

class ZipFetcher(val archive: ZipFile) : Fetcher {

    override fun fetch(link: Link): ResourceHandle? = ZipEntryHandle(link.href, archive)

    override fun close() = archive.close()

    companion object {
        fun fromPath(path: String): ZipFetcher? = try {
            ZipFetcher(ZipFile(path))
        } catch (e: Exception) {
            null
        }
    }
}

private class ZipEntryHandle(href: String, val archive: ZipFile) : ResourceHandle(href) {

    override fun stream(): InputStream? {
        val entry = entryFromHref(href)
        return try {
            archive.getInputStream(entry)
        } catch (e: ZipException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    override val metadataLength: Long? by lazy {
        entryFromHref(href)?.size?.takeIf { it != -1L }
    }

    private fun entryFromHref(href: String): ZipEntry? =
        archive.getEntry(href.removePrefix("/"))
}


