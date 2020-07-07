/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util.archive

/**
 * Represents an immutable archive.
 */
interface Archive {

    companion object {

        suspend fun open(path: String): Archive? =
            JavaZip.open(path) ?: ExplodedArchive.open(path)
    }

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
         * Reads the whole content of this entry if it's a file.
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
