/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.isParentOf
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

/**
 * A file system directory as a [Container].
 */
internal class DirectoryContainer(
    private val root: File,
    private val entries: List<File>,
    private val mediaTypeRetriever: MediaTypeRetriever
) : Container {

    private inner class FileEntry(override val url: Url, file: File) :
        Container.Entry, Resource by FileResource(file, mediaTypeRetriever) {

        override suspend fun close() {}
    }

    override suspend fun entries(): Set<Container.Entry> =
        entries.mapNotNull { file ->
            Url.fromDecodedPath(file.relativeTo(root).path)
                ?.let { url -> FileEntry(url, file) }
        }.toSet()

    override fun get(url: Url): Container.Entry {
        val file = (url as? RelativeUrl)
            ?.let { File(root, it.path) }

        return if (file == null || !root.isParentOf(file)) {
            FailureResource(Resource.Exception.NotFound()).toEntry(url)
        } else {
            FileEntry(url, file)
        }
    }

    override suspend fun close() {}
}

public class DirectoryContainerFactory(
    private val mediaTypeRetriever: MediaTypeRetriever
) : ContainerFactory {

    override suspend fun create(url: AbsoluteUrl): Try<Container, ContainerFactory.Error> {
        if (!url.isFile) {
            return Try.failure(ContainerFactory.Error.SchemeNotSupported(url.scheme))
        }

        val file = File(url.path)

        if (!tryOr(false) { file.isDirectory }) {
            return Try.failure(ContainerFactory.Error.NotAContainer(url))
        }

        return create(file)
    }

    // Internal for testing purpose
    internal suspend fun create(file: File): Try<Container, ContainerFactory.Error> {
        val entries =
            try {
                withContext(Dispatchers.IO) {
                    file.walk()
                        .filter { it.isFile }
                        .toList()
                }
            } catch (e: Exception) {
                return Try.failure(ContainerFactory.Error.Forbidden(e))
            }

        val container = DirectoryContainer(file, entries, mediaTypeRetriever)

        return Try.success(container)
    }
}
