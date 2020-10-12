/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.archive

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.tryOr
import java.io.File

interface ArchiveFactory {

    /** Opens an archive from a local [file]. */
    suspend fun open(file: File, password: String?): Archive

}

class DefaultArchiveFactory : ArchiveFactory {

    private val javaZipFactory by lazy { JavaZipArchiveFactory() }
    private val explodedArchiveFactory by lazy { ExplodedArchiveFactory() }

    /** Opens a ZIP or exploded archive. */
    override suspend fun open(file: File, password: String?): Archive = withContext(Dispatchers.IO) {
        if (tryOr(false) { file.isDirectory }) {
            explodedArchiveFactory.open(file, password)
        } else {
            javaZipFactory.open(file, password)
        }
    }

}

/**
 * Represents an immutable archive.
 */
interface Archive {

    /**
     * Holds an archive entry's metadata.
     */
    interface Entry {

        /** Absolute path to the entry in the archive. */
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

    /** Closes the archive. */
    suspend fun close()

}
