/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.file

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.toUrl

/**
 * A file system directory as a [Container].
 */
public class DirectoryContainer(
    private val root: File,
    override val entries: Set<Url>,
) : Container<Resource> {

    override fun get(url: Url): Resource? = url
        .takeIf { it in entries }
        ?.let { (it as? RelativeUrl)?.path }
        ?.let { File(root, it) }
        ?.let { FileResource(it) }

    override fun close() {}

    public companion object {

        public suspend operator fun invoke(root: File): Try<DirectoryContainer, FileSystemError> {
            val rootUrl = root.toUrl()
            val entries =
                try {
                    withContext(Dispatchers.IO) {
                        root.walk()
                            .filter { it.isFile }
                            .map { rootUrl.relativize(it.toUrl()) }
                            .toSet()
                    }
                } catch (e: SecurityException) {
                    return Try.failure(FileSystemError.Forbidden(e))
                }
            val container = DirectoryContainer(root, entries)
            return Try.success(container)
        }
    }
}
