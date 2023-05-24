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
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.tryRecover

interface ArchiveFactory {

    suspend fun open(resource: Resource, password: String?): Try<Package, Exception>

    /** Opens an archive from a local [file]. */
    suspend fun open(file: File, password: String?): Try<Package, Exception> =
        open(FileFetcher.FileResource(Link(href = file.path), file), password)
}

class DefaultArchiveFactory : ArchiveFactory {

    private val javaZipFactory = JavaZipArchiveFactory()

    override suspend fun open(resource: Resource, password: String?): Try<Package, Exception> {
        return resource.file
            ?.let { javaZipFactory.open(it) }
            ?: Try.failure(Exception("Resource unsupported"))
    }
}

class CompositeArchiveFactory(
    private val primaryFactory: ArchiveFactory,
    private val fallbackFactory: ArchiveFactory
) : ArchiveFactory {

    override suspend fun open(resource: Resource, password: String?): Try<Package, Exception> {
        return primaryFactory.open(resource, password)
            .tryRecover { fallbackFactory.open(resource, password) }
    }
}

/**
 * Represents an immutable archive.
 */
interface Package : SuspendingCloseable {

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
