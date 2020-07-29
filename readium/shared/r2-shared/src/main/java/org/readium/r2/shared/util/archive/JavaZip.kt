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
import org.readium.r2.shared.extensions.coerceToPositiveIncreasing
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.requireLengthFitInt
import java.io.InputStream
import java.lang.Exception
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

internal class JavaZip(private val archive: ZipFile) : Archive {

    companion object {

        suspend fun open(path: String): Archive? =
            try {
                withContext(Dispatchers.IO) {
                    ZipFile(path)
                }
            } catch (e: Exception) {
                null
            }?.let { JavaZip(it) }
        }

    inner class Entry(private val entry: ZipEntry) : Archive.Entry {
        override val path: String get() = entry.name

        override val length: Long? get() = entry.size.takeUnless { it == -1L }

        override val compressedLength: Long? get() = entry.compressedSize.takeUnless { it == -1L }

        override val isDirectory: Boolean get() = entry.isDirectory

        override suspend fun read(range: LongRange?): ByteArray? {
            val stream = archive.getInputStream(entry)

            return if (range == null)
                readFully(stream)
            else
                readRange(range, stream)
        }

        private fun readFully(stream: InputStream): ByteArray? =
            try {
                stream.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }

        private fun readRange(range: LongRange, stream: InputStream): ByteArray? {
            @Suppress("NAME_SHADOWING")
            val range = range
                .coerceToPositiveIncreasing()
                .requireLengthFitInt()

            stream.use {
                return try {
                    val skipped = it.skip(range.first)
                    val length = range.last - range.first + 1
                    val bytes = it.read(length)
                    if (skipped != range.first && bytes.isNotEmpty()) {
                        throw Exception("Unable to skip enough bytes")
                    }
                    bytes
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override suspend fun entries(): List<Archive.Entry> =
        archive.entries().toList().mapNotNull { Entry(it) }

    override suspend fun entry(path: String): Archive.Entry? =
        archive.getEntry(path)?.let { Entry(it) }

    override suspend fun close() = archive.close()

}

