/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.archive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.readRange
import java.io.File
import java.lang.Exception
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

internal class JavaZip(private val archive: ZipFile) : Archive {

    private inner class Entry(private val entry: ZipEntry) : Archive.Entry {
        override val path: String get() = entry.name

        override val length: Long? get() = entry.size.takeUnless { it == -1L }

        override val compressedLength: Long?
            get() =
                if (entry.method == ZipEntry.STORED || entry.method == -1)
                    null
                else
                    entry.compressedSize.takeUnless { it == -1L }

        override suspend fun read(range: LongRange?): ByteArray {
            val stream = withContext(Dispatchers.IO) {
                archive.getInputStream(entry)
            }
            return stream.use {
                if (range == null)
                    it.readFully()
                else
                    it.readRange(range)
            }
        }
    }

    override suspend fun entries(): List<Archive.Entry> =
        archive.entries().toList().filterNot { it.isDirectory }.mapNotNull { Entry(it) }

    override suspend fun entry(path: String): Archive.Entry {
        val entry = archive.getEntry(path)
            ?: throw Exception("No file entry at path $path.")

        return Entry(entry)
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        archive.close()
    }

}

internal class JavaZipArchiveFactory : ArchiveFactory {

    override suspend fun open(file: File, password: String?): Archive = withContext(Dispatchers.IO) {
        JavaZip(ZipFile(file))
    }

}
