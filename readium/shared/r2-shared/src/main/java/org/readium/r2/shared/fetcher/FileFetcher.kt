/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.addPrefix
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try
import timber.log.Timber
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

    override suspend fun links(): List<Link> =
        paths.toSortedMap().flatMap { (href, file) ->
            file.walk().mapNotNull {
                tryOrNull {
                    if (it.isDirectory) {
                        null
                    } else {
                        Link(
                            href = File(href, it.path.removePrefix(file.canonicalPath)).canonicalPath,
                            type = Format.of(fileExtension = it.extension)?.mediaType.toString()
                        )
                    }
                }
            }.toList()
        }

    override fun get(link: Link): Resource {
        val linkHref = link.href.addPrefix("/")
        for ((itemHref, itemFile) in paths) {
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
        return FailureResource(link, Resource.Error.NotFound)
    }

    override suspend fun close() {
        openedResources.mapNotNull(WeakReference<Resource>::get).forEach { it.close() }
        openedResources.clear()
    }

    private class FileResource(val link: Link, private val file: File) : StreamResource() {

        private val randomAccessFile: ResourceTry<RandomAccessFile> by lazy {
            try {
                Try.success(RandomAccessFile(file, "r"))
            } catch (e: FileNotFoundException) {
                Try.failure(Resource.Error.NotFound)
            } catch (e: SecurityException) {
                Try.failure(Resource.Error.Forbidden)
            } catch (e: Exception) {
                Try.failure(Resource.Error.Other(e))
            }
        }

        override suspend fun link(): Link = link

        override fun stream(): ResourceTry<InputStream> =
            randomAccessFile.map{
                Channels.newInputStream(it.channel).buffered()
            }

        override val metadataLength: Long? =
            try {
                if (file.isFile)
                    file.length()
                else
                    null
            } catch (e: Exception) {
                null
            }

        override suspend fun close() = withContext<Unit>(Dispatchers.IO) {
            randomAccessFile.onSuccess {
                try {
                    it.close()
                } catch (e: java.lang.Exception) {
                    Timber.e(e)
                }
            }
        }
    }
}
