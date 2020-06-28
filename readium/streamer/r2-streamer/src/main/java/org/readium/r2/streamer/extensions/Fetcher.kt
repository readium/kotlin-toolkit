/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.extensions

import org.json.JSONObject
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.fetcher.ArchiveFetcher
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.FileFetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.archive.Archive
import java.io.File
import java.io.FileNotFoundException

/** Returns the resource data at the given [Link]'s HREF, or throws a [Resource.Error] */
@Throws(Resource.Error::class)
internal suspend fun Fetcher.readBytes(link: Link): ByteArray =
    get(link).read().getOrThrow()

/** Returns the resource data at the given [href], or throws a [Resource.Error] */
@Throws(Resource.Error::class)
internal suspend fun Fetcher.readBytes(href: String): ByteArray =
    get(href).read().getOrThrow()

/** Returns the resource data as an XML Document at the given [href], or null. */
internal suspend fun Fetcher.readAsXmlOrNull(href: String): ElementNode? =
    get(href).readAsXml().getOrNull()

/** Returns the resource data as a JSON object at the given [href], or null. */
internal suspend fun Fetcher.readAsJsonOrNull(href: String): JSONObject? =
    get(href).readAsJson().getOrNull()

/** Creates a [Fetcher] from either an archive file, or an exploded directory. **/
internal suspend fun Fetcher.Companion.fromArchiveOrDirectory(
    path: String,
    openArchive: suspend (String) -> Archive? = { Archive.open(it) }
): Fetcher? {
    val file = File(path)
    val isDirectory = tryOrNull { file.isDirectory } ?: return null

    return if (isDirectory) {
        FileFetcher(href = "/", file = file)
    } else {
        ArchiveFetcher.fromPath(path, openArchive)
    }
}

/** Creates a [Fetcher] from either a file or an exploded directory.
 *
 * Can throw SecurityException or FileNotFoundException.
 */
internal suspend fun Fetcher.Companion.fromFile(
    file: File,
    openArchive: suspend (String) -> Archive? = { Archive.open(it) }
): Fetcher =
    when {
        file.isDirectory ->
            FileFetcher(href = "/", file = file)
        file.exists() ->
            ArchiveFetcher.fromPath(file.path, openArchive)
                ?: FileFetcher(href = "/${file.name}", file = file)
        else ->
            throw FileNotFoundException(file.path)
    }

internal suspend fun Fetcher.guessTitle(): String? {
    val firstLink = links().firstOrNull() ?: return null
    val commonFirstComponent = links().hrefCommonFirstComponent() ?: return null

    if (commonFirstComponent.name == firstLink.href.removePrefix("/"))
       return null

    return commonFirstComponent.toTitle()
}
