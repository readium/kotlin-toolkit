/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.archive

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.isParentOf
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.readRange
import org.readium.r2.shared.resource.Container
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url

/**
 * An archive exploded on the file system as a directory.
 */
internal class DirectoryContainer(private val directory: File) : Container {

    private inner class Entry(override val file: File) : Container.Entry {

        override val path: String get() = file.relativeTo(directory).path

        override suspend fun length(): ResourceTry<Long> = try {
            Try.success(file.length())
        } catch (e: Exception) {
            Try.failure(Resource.Exception.wrap(e))
        }

        override val compressedLength: Long? = null

        override suspend fun read(range: LongRange?): Try<ByteArray, Resource.Exception> {
            val stream = withContext(Dispatchers.IO) {
                file.inputStream()
            }

            return try {
                val bytes = stream.use {
                    if (range == null)
                        it.readFully()
                    else
                        it.readRange(range)
                }
                Try.success(bytes)
            } catch (e: Exception) {
                Try.failure(Resource.Exception.wrap(e))
            }
        }

        override suspend fun close() {}
    }

    override suspend fun entries(): List<Container.Entry> =
        directory.walk()
            .filter { it.isFile }
            .map { Entry(it) }
            .toList()

    override suspend fun entry(path: String): Container.Entry {
        val file = File(directory, path)

        if (!directory.isParentOf(file) || !file.isFile)
            throw Exception("No file entry at path $path.")

        return Entry(file)
    }

    override suspend fun close() {}
}

internal class ExplodedArchiveFactory {

    suspend fun open(url: Url): Try<Container, Exception> =
        withContext(Dispatchers.IO) {
            try {
                if (url.scheme != "file") {
                    throw Exception("Unsupported protocol.")
                }

                val file = File(url.path)
                open(file)
            } catch (e: Exception) {
                Try.failure(e)
            }
        }

    suspend fun open(file: File): Try<Container, Exception> =
        withContext(Dispatchers.IO) {
            try {
                if (!file.isDirectory) {
                    throw Exception("Url is not a directory.")
                }
                Try.success(DirectoryContainer(file))
            } catch (e: Exception) {
                Try.failure(e)
            }
        }
}
