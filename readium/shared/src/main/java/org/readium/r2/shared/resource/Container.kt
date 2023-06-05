package org.readium.r2.shared.resource

import java.io.File
import org.readium.r2.shared.util.SuspendingCloseable

interface Container : SuspendingCloseable {

    /**
     * Holds an archive entry's metadata.
     */
    interface Entry : Resource {

        /**
         * Absolute path to the entry in the archive.
         * It MUST start with /.
         */
        val path: String

        /**
         *  Compressed data length.
         */
        val compressedLength: Long?
    }

    /**
     * Direct file to this container, when available.
     */
    val file: File? get() = null

    /** List of all the archived file entries. */
    suspend fun entries(): Iterable<Entry>?

    /** Gets the entry at the given `path`. */
    suspend fun entry(path: String): Entry
}
