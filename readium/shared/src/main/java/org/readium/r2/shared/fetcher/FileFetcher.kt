/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

/**
 * Provides access to resources on the local file system.
 *
 * [paths] contains the reachable local paths, indexed by the exposed HREF. Sub-paths are reachable
 * as well, to be able to access a whole directory.
 */
class FileFetcher(
    private val paths: Map<String, File>,
    private val mediaTypeRetriever: MediaTypeRetriever
) : Fetcher {

    /** Provides access to the given local [file] at [href]. */
    constructor(href: String, file: File, mediaTypeRetriever: MediaTypeRetriever) :
        this(mapOf(href to file), mediaTypeRetriever)

    private val openedResources: MutableList<WeakReference<FileResource>> = LinkedList()

    override suspend fun links(): List<Link> =
        paths.toSortedMap().flatMap { (href, file) ->
            file.walk().toList().mapNotNull {
                tryOrNull {
                    if (it.isDirectory) {
                        null
                    } else {
                        Link(
                            href = File(href, it.canonicalPath.removePrefix(file.canonicalPath)).canonicalPath,
                            type = mediaTypeRetriever.retrieve(it)?.toString()
                        )
                    }
                }
            }
        }

    override fun get(link: Link): Fetcher.Resource {
        val linkHref = link.href.addPrefix("/")
        for ((itemHref, itemFile) in paths) {
            @Suppress("NAME_SHADOWING")
            val itemHref = itemHref.addPrefix("/")
            if (linkHref.startsWith(itemHref)) {
                val resourceFile = File(itemFile, linkHref.removePrefix(itemHref))
                // Make sure that the requested resource is [path] or one of its descendant.
                if (resourceFile.canonicalPath.startsWith(itemFile.canonicalPath)) {
                    val resource = FileResource(link, resourceFile)
                    openedResources.add(WeakReference(resource))
                    return resource
                }
            }
        }
        return FailureResource(link, Resource.Exception.NotFound())
    }

    override suspend fun close() {
        openedResources.mapNotNull(WeakReference<FileResource>::get).forEach { it.close() }
        openedResources.clear()
    }

    class FileResource(val link: Link, val resource: org.readium.r2.shared.resource.FileResource) :
        Resource by resource, Fetcher.Resource {

        companion object {

            operator fun invoke(link: Link, file: File): FileResource =
                FileResource(link, org.readium.r2.shared.resource.FileResource(file))
        }

        override suspend fun link(): Link = link

        override fun toString(): String =
            "${javaClass.simpleName}(${resource.file.path})"
    }
}
