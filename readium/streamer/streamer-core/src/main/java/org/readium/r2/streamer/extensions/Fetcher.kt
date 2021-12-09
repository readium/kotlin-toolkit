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
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.archive.DefaultArchiveFactory
import org.readium.r2.shared.util.use
import java.io.File

/** Returns the resource data at the given [Link]'s HREF, or throws a [Resource.Exception] */
@Throws(Resource.Exception::class)
internal suspend fun Fetcher.readBytes(link: Link): ByteArray =
    get(link).use { it.read().getOrThrow() }

/** Returns the resource data at the given [href], or throws a [Resource.Exception] */
@Throws(Resource.Exception::class)
internal suspend fun Fetcher.readBytes(href: String): ByteArray =
    get(href).use { it.read().getOrThrow() }

/** Returns the resource data as an XML Document at the given [href], or null. */
internal suspend fun Fetcher.readAsXmlOrNull(href: String): ElementNode? =
    get(href).use { it.readAsXml().getOrNull() }

/** Returns the resource data as a JSON object at the given [href], or null. */
internal suspend fun Fetcher.readAsJsonOrNull(href: String): JSONObject? =
    get(href).use { it.readAsJson().getOrNull() }

/** Creates a [Fetcher] from either an archive file, or an exploded directory. **/
internal suspend fun Fetcher.Companion.fromArchiveOrDirectory(
    path: String,
    archiveFactory: ArchiveFactory = DefaultArchiveFactory()
): Fetcher? {
    val file = File(path)
    val isDirectory = tryOrNull { file.isDirectory } ?: return null

    return if (isDirectory) {
        FileFetcher(href = "/", file = file)
    } else {
        ArchiveFetcher.fromPath(path, archiveFactory)
    }
}

internal suspend fun Fetcher.guessTitle(): String? {
    val firstLink = links().firstOrNull() ?: return null
    val commonFirstComponent = links().hrefCommonFirstComponent() ?: return null

    if (commonFirstComponent.name == firstLink.href.removePrefix("/"))
       return null

    return commonFirstComponent.name
}
