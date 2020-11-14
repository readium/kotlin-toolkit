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
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.isLazyInitialized
import org.readium.r2.shared.util.mediatype.MediaType
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
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
            file.walk().toList().mapNotNull {
                tryOrNull {
                    if (it.isDirectory) {
                        null
                    } else {
                        Link(
                            href = File(href, it.canonicalPath.removePrefix(file.canonicalPath)).canonicalPath,
                            type = MediaType.ofFile(file, fileExtension = it.extension)?.toString()
                        )
                    }
                }
            }
        }

    override fun get(link: Link): Resource {
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
        return FailureResource(link, Resource.Exception.NotFound)
    }

    override suspend fun close() {
        openedResources.mapNotNull(WeakReference<Resource>::get).forEach { it.close() }
        openedResources.clear()
    }

    class FileResource(val link: Link, override val file: File) : Resource {

        private val randomAccessFile by lazy {
            ResourceTry.catching {
                RandomAccessFile(file, "r")
            }
        }

        override suspend fun link(): Link = link

        override suspend fun close() = withContext<Unit>(Dispatchers.IO) {
            if (::randomAccessFile.isLazyInitialized) {
                randomAccessFile.onSuccess {
                    try {
                        it.close()
                    } catch (e: java.lang.Exception) {
                        Timber.e(e)
                    }
                }
            }
        }

        override suspend fun read(range: LongRange?): ResourceTry<ByteArray> =
            withContext(Dispatchers.IO) {
                ResourceTry.catching {
                    readSync(range)
                }
            }

        private fun readSync(range: LongRange?): ByteArray {
            if (range == null) {
                return file.readBytes()
            }

            @Suppress("NAME_SHADOWING")
            val range = range
                .coerceFirstNonNegative()
                .requireLengthFitInt()

            if (range.isEmpty()) {
                return ByteArray(0)
            }

            return randomAccessFile.getOrThrow().run {
                channel.position(range.first)

                // The stream must not be closed here because it would close the underlying
                // [FileChannel] too. Instead, [close] is responsible for that.
                Channels.newInputStream(channel).run {
                    val length = range.last - range.first + 1
                    read(length)
                }
            }
        }

        override suspend fun length(): ResourceTry<Long> =
            metadataLength?.let { Try.success(it) }
                ?: read().map { it.size.toLong() }

        private val metadataLength: Long? =
            try {
                if (file.isFile)
                    file.length()
                else
                    null
            } catch (e: Exception) {
                null
            }

        private inline fun <T> Try.Companion.catching(closure: () -> T): ResourceTry<T> =
            try {
                success(closure())
            } catch (e: FileNotFoundException) {
                failure(Resource.Exception.NotFound)
            } catch (e: SecurityException) {
                failure(Resource.Exception.Forbidden)
            } catch (e: Exception) {
                failure(Resource.Exception.wrap(e))
            } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
                failure(Resource.Exception.wrap(e))
            }

        override fun toString(): String =
            "${javaClass.simpleName}(${file.path})"

    }
}
