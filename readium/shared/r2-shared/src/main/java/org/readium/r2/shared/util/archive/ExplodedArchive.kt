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
import org.readium.r2.shared.extensions.isParentOf
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.readRange
import org.readium.r2.shared.extensions.tryOr
import java.io.File
import java.lang.IllegalArgumentException

/**
 * An archive exploded on the file system as a directory.
 */
internal class ExplodedArchive(private val directory: File)  : Archive {

    private inner class Entry(private val file: File) : Archive.Entry {

        override val path: String get() = file.relativeTo(directory).path

        override val length: Long? = file.length()

        override val compressedLength: Long? = null

        override suspend fun read(range: LongRange?): ByteArray {
            val stream = withContext(Dispatchers.IO) {
                file.inputStream()
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
        directory.walk()
            .filter { it.isFile }
            .map { Entry(it) }
            .toList()

    override suspend fun entry(path: String): Archive.Entry {
        val file = File(directory, path)

        if (!directory.isParentOf(file) || !file.isFile)
            throw Exception("No file entry at path $path.")

        return Entry(file)
    }

    override suspend fun close() {}
}

internal class ExplodedArchiveFactory : ArchiveFactory {

    override suspend fun open(file: File, password: String?): Archive = withContext(Dispatchers.IO) {
        file.takeIf { tryOr(false) { it.isDirectory } }
            ?.let { ExplodedArchive(it) }
            ?: throw IllegalArgumentException("[path] must be a directory to be opened as an exploded archive")
    }

}