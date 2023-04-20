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
import java.io.IOException
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.toUrl

interface ArchiveFactory {

    /** Opens an archive from a local [file]. */
    suspend fun open(file: File, password: String?): Try<Archive, Exception> =
        open(file.toUrl(), password)

    /** Opens an archive from a local or remote [URL]. */
    suspend fun open(url: Url, password: String?): Try<Archive, Exception>
}

class DefaultArchiveFactory : ArchiveFactory {

    private val javaZipFactory by lazy { JavaZipArchiveFactory() }
    private val explodedArchiveFactory by lazy { ExplodedArchiveFactory() }

    override suspend fun open(url: Url, password: String?): Try<Archive, Exception> =
        if (url.protocol == "file") {
            openFile(File(url.path), password)
        } else {
            throw IOException("Cannot access ZIP archives through protocol ${url.protocol}.")
        }

    /** Opens a ZIP or exploded archive. */
    private suspend fun openFile(file: File, password: String?): Try<Archive, Exception> =
        withContext(Dispatchers.IO) {
            if (tryOr(false) { file.isDirectory }) {
                explodedArchiveFactory.open(file, password)
            } else {
                javaZipFactory.open(file, password)
            }
    }
}

class CompositeArchiveFactory(
    private val primaryFactory: ArchiveFactory,
    private val fallbackFactory: ArchiveFactory
) : ArchiveFactory {

    override suspend fun open(url: Url, password: String?): Try<Archive, Exception> =
        try {
            primaryFactory.open(url, password)
        } catch (e: Exception) {
            fallbackFactory.open(url, password)
        }
}

/**
 * Represents an immutable archive.
 */
interface Archive : SuspendingCloseable {

    /**
     * Holds an archive entry's metadata.
     */
    interface Entry : SuspendingCloseable {

        /**
         * Absolute path to the entry in the archive.
         * It MUST start with /.
         */
        val path: String

        /**
         * Uncompressed data length.
         */
        val length: Long?

        /**
         *  Compressed data length.
         */
        val compressedLength: Long?

        /**
         * Reads the whole content of this entry.
         * When [range] is null, the whole content is returned. Out-of-range indexes are clamped to the
         * available length automatically.
         */
        suspend fun read(range: LongRange? = null): ByteArray
    }

    /** List of all the archived file entries. */
    suspend fun entries(): List<Entry>

    /** Gets the entry at the given `path`. */
    suspend fun entry(path: String): Entry
}
