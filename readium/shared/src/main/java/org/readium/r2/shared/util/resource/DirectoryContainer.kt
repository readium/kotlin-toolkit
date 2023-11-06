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
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ClosedContainer
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.FileBlob
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever
import org.readium.r2.shared.util.toUrl

/**
 * A file system directory as a [Container].
 */
public class DirectoryContainer(
    private val root: File,
    private val mediaTypeRetriever: MediaTypeRetriever
) : ClosedContainer<ResourceEntry> {

    private val _entries: Set<Url> by lazy {
        tryOrNull {
            root.walk()
                .filter { it.isFile }
                .mapNotNull { it.toUrl() }
                .toSet()
        }.orEmpty()
    }

    private fun File.toEntry(): ResourceEntry? {
        val url = Url.fromDecodedPath(this.relativeTo(root).path)
            ?: return null

        val resource = GuessMediaTypeResourceAdapter(
            FileBlob(this),
            mediaTypeRetriever,
            MediaTypeHints(fileExtension = extension)
        )
        return DelegatingResourceEntry(url, resource)
    }

    override suspend fun entries(): Set<Url> {
        return withContext(Dispatchers.IO) {
            _entries
        }
    }

    override fun get(url: Url): ResourceEntry? {
        val file = (url as? RelativeUrl)?.path
            ?.let { File(root, it) }
            ?.takeIf { !root.isParentOf(it) }

        return file?.toEntry()
    }

    override suspend fun close() {}
}
