/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import java.io.File
import org.readium.r2.shared.util.SuspendingCloseable

/**
 * A resource container as an archive or a directory.
 */
public interface Container : SuspendingCloseable {

    /**
     * Holds a container entry's.
     */
    public interface Entry : Resource {

        /**
         * Absolute path to the entry in the archive.
         * It MUST start with /.
         */
        public val path: String
    }

    /**
     * Direct file to this container, when available.
     */
    public val file: File? get() = null

    /**
     * Gets the container name if any.
     */
    public suspend fun name(): ResourceTry<String?>

    /**
     * List of all the container entries of null if such a list is not available.
     */
    public suspend fun entries(): Iterable<Entry>?

    /**
     * Returns the [Entry] at the given [path].
     *
     * A [Entry] is always returned, since for some cases we can't know if it exists before
     * actually fetching it, such as HTTP. Therefore, errors are handled at the Entry level.
     */
    public suspend fun entry(path: String): Entry
}
