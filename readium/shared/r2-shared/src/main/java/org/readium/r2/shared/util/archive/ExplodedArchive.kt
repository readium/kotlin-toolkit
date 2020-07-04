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
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.extensions.tryOrNull
import java.io.File

internal class ExplodedArchive(private val file: File)  : Archive {

    companion object {

        suspend fun open(path: String): Archive? =
            tryOrNull {
                File(path).takeIf {
                    withContext(Dispatchers.IO) { it.isDirectory }
                }
            }
            ?.let { ExplodedArchive(it) }
    }

    private inner class Entry(private val file: File) : Archive.Entry {

        override val path: String get() = file.path

        override val length: Long?
            get() = tryOrNull {
                if (file.isFile)
                    file.length()
                else
                    null
            }

        override val compressedLength: Long?
            get() = length

        override val isDirectory: Boolean
            get() = tryOr(false ) { file.isDirectory }

        override suspend fun read(range: LongRange?): ByteArray? {
            val stream = withContext(Dispatchers.IO) {
                file.inputStream()
            }

            return if (range == null)
                stream.readFully()
            else
                stream.readRange(range)
        }
    }

    override suspend fun entries(): List<Archive.Entry> =
        file.walk()
            .mapNotNull { Entry(it.relativeTo(file)).takeIf { it.path.isNotEmpty() } }
            .toList()

    override suspend fun entry(path: String): Archive.Entry? =
        tryOrNull { File(file, path).takeIf { it.exists() } }
            ?.let { Entry(it) }

    override suspend fun close() {}
}