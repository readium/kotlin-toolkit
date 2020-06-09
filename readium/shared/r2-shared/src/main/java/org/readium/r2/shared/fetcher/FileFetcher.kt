/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.format.Format
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.RandomAccessFile
import java.lang.ref.WeakReference
import java.nio.channels.Channels
import java.util.*

/**
 * Provides access to resources on the local file system.
 *
 * [paths] contains the reachable local paths, indexed by the exposed HREF. Sub-paths are reachable
 * as well, to be able to access a whole directory.
 */
class FileFetcher(private val paths: Map<String, File>) : Fetcher {

    /** Provides access to the given local [file] at [href]. */
    constructor(href: String, file: File) : this(mapOf(href to file))

    private val openedResources: MutableList<WeakReference<Resource>> = LinkedList()

    override val links: List<Link> by lazy {
        paths.toSortedMap().flatMap { (href, file) ->
            file.walk().mapNotNull {
                if (it.isDirectory) {
                    null
                } else {
                    Link(
                        href = File(href, it.path.removePrefix(file.canonicalPath)).canonicalPath,
                        type = Format.of(fileExtension = it.extension)?.mediaType.toString()
                    )
                }
            }.toList()
        }
    }

    override fun get(link: Link): Resource {
        for ((itemHref, itemFile) in paths) {
            if (link.href.startsWith(itemHref)) {
                val resourceFile = File(itemFile, link.href.removePrefix(itemHref))
                // Make sure that the requested resource is [path] or one of its descendant.
                if (resourceFile.canonicalPath.startsWith(itemFile.canonicalPath)) {
                    return try {
                        val file = RandomAccessFile(resourceFile, "r")
                        val resource = FileResource(link, file)
                        openedResources.add(WeakReference(resource))
                        return resource
                    } catch (e: FileNotFoundException) {
                        FailureResource(link, Resource.Error.NotFound)
                    } catch (e: SecurityException) {
                        FailureResource(link, Resource.Error.Forbidden)
                    } catch (e: Exception) {
                        FailureResource(link, Resource.Error.Other(e))
                    }
                }
            }
        }
        return FailureResource(link, Resource.Error.NotFound)
    }

    override fun close() {
        openedResources.mapNotNull(WeakReference<Resource>::get).forEach { it.close() }
        openedResources.clear()
    }

    private class FileResource(override val link: Link, private val file: RandomAccessFile) : StreamResource() {

        override fun stream(): ResourceTry<InputStream> {
            val stream = Channels.newInputStream(file.channel).buffered()
            return Try.success(stream)
        }

        override val metadataLength: Long? =
            try {
                file.length()
            } catch (e: Exception) {
                null
            }

        override fun close() = file.close()
    }
}
