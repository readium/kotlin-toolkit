/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.isParentOf
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

/**
 * A file system directory as a [Container].
 */
internal class DirectoryContainer(
    private val root: File,
    private val mediaTypeRetriever: MediaTypeRetriever
) : Container {

    private inner class FileEntry(override val url: Url, file: File) :
        Container.Entry, Resource by FileResource(file, mediaTypeRetriever) {

        override suspend fun close() {}
    }

    private val _entries: Set<Container.Entry>? by lazy {
        tryOrNull {
            root.walk()
                .filter { it.isFile }
                .mapNotNull { it.toEntry() }
                .toSet()
        }
    }

    private fun File.toEntry(): Container.Entry? =
        Url.fromDecodedPath(this.relativeTo(root).path)
            ?.let { url -> FileEntry(url, this) }

    override suspend fun entries(): Set<Container.Entry>? {
        return withContext(Dispatchers.IO) {
            _entries
        }
    }

    override fun get(url: Url): Container.Entry {
        val file = (url as? RelativeUrl)?.path
            ?.let { File(root, it) }

        return if (file == null || !root.isParentOf(file)) {
            FailureResource(ResourceError.NotFound()).toEntry(url)
        } else {
            FileEntry(url, file)
        }
    }

    override suspend fun close() {}
}

public class DirectoryContainerFactory(
    private val mediaTypeRetriever: MediaTypeRetriever
) : ContainerFactory {

    override suspend fun create(url: AbsoluteUrl): Container? {
        val file = url.toFile()
            ?: return null

        if (!tryOr(false) { file.isDirectory }) {
            return null
        }

        return create(file).getOrNull()
    }

    override suspend fun create(
        url: AbsoluteUrl,
        mediaType: MediaType
    ): Try<Container, ContainerFactory.Error> {
        val file = url.toFile()
            ?: return Try.failure(ContainerFactory.Error.SchemeNotSupported(url.scheme))

        return create(file)
    }

    // Internal for testing purpose
    internal suspend fun create(file: File): Try<Container, ContainerFactory.Error> {
        val container = DirectoryContainer(file, mediaTypeRetriever)

        return Try.success(container)
    }
}
