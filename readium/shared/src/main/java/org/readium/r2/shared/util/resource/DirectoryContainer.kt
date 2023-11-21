/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.FileBlob
import org.readium.r2.shared.util.data.FileSystemError
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.toUrl

/**
 * A file system directory as a [Container].
 */
public class DirectoryContainer(
    private val root: File,
    private val mediaTypeRetriever: MediaTypeRetriever,
    override val entries: Set<Url>
) : Container<Resource> {

    private fun File.toResource(): Resource {
        return BlobResourceAdapter(
            FileBlob(this),
            Resource.Properties(
                Resource.Properties.Builder()
                    .also { it.filename = name }
            ),
            mediaTypeRetriever
        )
    }

    override fun get(url: Url): Resource? = url
        .takeIf { it in entries }
        ?.let { (it as? RelativeUrl)?.path }
        ?.let { File(root, it) }
        ?.toResource()

    override suspend fun close() {}

    public companion object {

        public suspend operator fun invoke(root: File, mediaTypeRetriever: MediaTypeRetriever): Try<DirectoryContainer, FileSystemError> {
            val entries =
                try {
                    withContext(Dispatchers.IO) {
                        root.walk()
                            .filter { it.isFile }
                            .map { it.toUrl() }
                            .toSet()
                    }
                } catch (e: SecurityException) {
                    return Try.failure(FileSystemError.Forbidden(e))
                }
            val container = DirectoryContainer(root, mediaTypeRetriever, entries)
            return Try.success(container)
        }
    }
}
