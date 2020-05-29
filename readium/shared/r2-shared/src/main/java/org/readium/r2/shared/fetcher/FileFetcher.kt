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
class FileFetcher(private val paths: Map<String, String>) : Fetcher {

    /** Provides access to the given local [path] at [href]. */
    constructor(href: String, path: String) : this(mapOf(href to path))

    private val openedResources: MutableList<WeakReference<Resource>> = LinkedList()

    override val links: List<Link> by lazy {
        paths.toSortedMap().flatMap { (href, path) ->
            File(path).walk().mapNotNull { file ->
                if (file.isDirectory) {
                    null
                } else {
                    Link(
                        href = File(href, file.path.removePrefix(path)).path,
                        type = Format.of(fileExtension = file.extension)?.mediaType.toString()
                    )
                }
            }.toList()
        }
    }

    override fun get(link: Link, parameters: HrefParameters): Resource {
        for ((href, path) in paths) {
            if (link.href.startsWith(href)) {
                val resourcePath = File(path, link.href.removePrefix(href)).canonicalPath
                // Make sure that the requested resource is [path] or one of its descendant.
                if (resourcePath.startsWith(path)) {
                    return try {
                        val file = RandomAccessFile(resourcePath, "r")
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
