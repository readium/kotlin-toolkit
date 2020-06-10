/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.extensions

import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.fetcher.ArchiveFetcher
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import java.io.File

/** Returns the resource data at the given [Link]'s HREF, or throws a [Resource.Error] */
@Throws(Resource.Error::class)
internal fun Fetcher.readBytes(link: Link): ByteArray =
    get(link).read().get()

/** Returns the resource data at the given [href], or throws a [Resource.Error] */
@Throws(Resource.Error::class)
internal fun Fetcher.readBytes(href: String): ByteArray =
    get(href).read().get()

/** Creates a [Fetcher] from either an archive file, or an exploded directory. **/
internal fun Fetcher.Companion.fromArchiveOrDirectory(path: String): Fetcher? {
    val file = File(path)
    val isDirectory = tryOrNull { file.isDirectory } ?: return null

    return if (isDirectory) {
        FileFetcher(href = "/", file = file)
    } else {
        ArchiveFetcher.fromPath(path)
    }
}
